package ecscalibur.core

import ecscalibur.core.archetype.ArchetypeManager
import ecscalibur.core.components.Component
import ecscalibur.core.components.ComponentType
import ecscalibur.core.context.MetaContext

export world.*
export world.Loop.once
export world.Loop.forever
export world.Loop.ext.times

object world:
  import builders.*

  /** A World is the main entry point into an ECS application. It lets the user create Entities, add
    * Components to them and define Systems to iterate over the created Entities.
    *
    * Ideally, there should only be one World instance per application.
    */
  trait World:
    /** Reference to this World's [[MetaContext]] instance.
      *
      * @return
      *   this World's MetaContext instance.
      */
    given context: MetaContext

    /** Reference to this World's [[Mutator]] instance.
      *
      * @return
      *   this World's Mutator instance.
      */
    given mutator: Mutator

    private[ecscalibur] given archetypeManager: ArchetypeManager

    /** Starts the creation of a new [[Entity]]. Call [[EntityBuilder.withComponents]] after this to
      * define which Components this Entity should have.
      *
      * @return
      *   an [[EntityBuilder]] instance
      */
    def entity: EntityBuilder

    /** Updates the reference to the given Component type for the given Entity.
      *
      * @param e
      *   the Entity for which the given Component must be updated
      * @param c
      *   the Component to update
      * @throws IllegalArgumentException
      *   if the given Entity does not exist
      */
    def update(e: Entity, c: Component): Unit

    /** Checks whether the given Entity has all of the given Components.
      *
      * @param e
      *   the Entity on which to perform the check
      * @param types
      *   the ComponentTypes of the Components the given Entity should have
      * @return
      *   true if the Entity has all of the specified Components, false otherwise
      */
    def hasComponents(e: Entity, types: ComponentType*): Boolean

    /** Creates a new System with [[System.process]] overridden by the given [[Query]]. Both
      * [[System.onResume]] and [[System.onPause]] do not contain any logic.
      *
      * @param name
      *   a unique name for this System
      * @param priority
      *   its priority value relative to the other Systems in the World
      * @param q
      *   a query
      * @throws IllegalArgumentException
      *   if a System with the same name already exists
      */
    infix def system(name: String, priority: Int = 0)(q: Query): Unit

    /** Adds a [[System]] instance to this World.
      *
      * @param s
      *   the System to be added.
      * @throws IllegalArgumentException
      *   if a System with the same name already exists
      */
    infix def system(s: System): Unit

    /** Checks whether the [[System]] identified by the given name is currently running.
      *
      * @param name
      *   name of the system to check
      * @return
      *   true if the system is running, false otherwise
      */
    infix def isSystemRunning(name: String): Boolean

    /** Checks whether the [[System]] identified by the given name is currently paused.
      *
      * @param name
      *   name of the system to check
      * @return
      *   true if the system is paused, false otherwise
      */
    infix def isSystemPaused(name: String): Boolean

    /** Performs one or more World iterations based on the given [[Loop]] type.
      *
      * @param loopType
      *   dictates how many iterations will be performed
      */
    infix def loop(loopType: Loop): Unit

  /** Represents the number of iterations a [[World]] will perform when calling [[World.loop]].
    */
  enum Loop:
    /** The [[World]] will loop forever, never stopping.
      */
    case Forever

    /** The [[World]] will loop a fixed number of times.
      *
      * @param times
      *   times the World will loop
      */
    case Times(times: Int)

  /** Factory for [[Loop]].
    */
  object Loop:
    /** Returns a [[Loop.Forever]] instance.
      */
    val forever: Loop = Forever

    /** Returns a [[Loop.Times]] instance initialized to 1.
      */
    val once: Loop = Times(1)

    object ext:
      /** @return
        *   a [[Loop.Times]] instance initialized with the given parameter.
        */
      extension (n: Int) inline def times: Loop = Times(n)

  object World:
    // TODO Pass a Configuration object instead

    /** @param iterationsPerSecond
      *   maximum number of iterations per second this World will perform. A value of 0 will not
      *   enforce any limit.
      * @return
      *   a new World instance
      */
    def apply(iterationsPerSecond: Int = 0): World =
      WorldImpl(iterationsPerSecond)(using ArchetypeManager(), MetaContext())

    private class WorldImpl(frameCap: Int)(using ArchetypeManager, MetaContext)
        extends World,
          Mutator:
      import scala.collection.mutable

      override given archetypeManager: ArchetypeManager = summon[ArchetypeManager]
      override given mutator: Mutator = this
      override given context: MetaContext = summon[MetaContext]

      import ecsutil.FramePacer
      private val pacer = FramePacer(frameCap)

      import ecsutil.IdGenerator
      private val entityIdGenerator = IdGenerator()

      private var activeSystems: Vector[System] = Vector.empty
      private var pendingSystems: Vector[System] = Vector.empty

      private val entityCreate: mutable.Map[Entity, Seq[Component]] = mutable.Map.empty
      private var entityDelete: List[Entity] = List.empty
      private val entityAddComps: mutable.Map[Entity, List[(Component, () => Unit)]] =
        mutable.Map.empty
      private val entityRemoveComps: mutable.Map[Entity, List[(ComponentType, () => Unit)]] =
        mutable.Map.empty
      private var areBuffersDirty = false

      override def entity: EntityBuilder = EntityBuilder()(using this)

      override def update(e: Entity, c: Component): Unit = archetypeManager.update(e, c)

      override def hasComponents(e: Entity, types: ComponentType*): Boolean =
        archetypeManager.hasComponents(e, types*)

      override def system(name: String, priority: Int)(q: Query): Unit =
        system:
          new System(name, priority):
            override protected val process: Query = q

      override def system(s: System): Unit =
        require(
          !(activeSystems.contains(s) || pendingSystems.contains(s)),
          s"System \"${s.name}\" already exists."
        )
        pendingSystems = s +: pendingSystems

      override def loop(loopType: Loop): Unit =
        inline def _loop(): Unit =
          context.setDeltaTime:
            pacer.pace:
              if areBuffersDirty then
                areBuffersDirty = false
                processPendingEntityOperations()
              processPendingSystems()
              for s <- activeSystems do s.update()
        loopType match
          case Loop.Forever      => while true do _loop()
          case Loop.Times(times) => for _ <- 0 until times do _loop()

      private inline def processPendingEntityOperations(): Unit =
        for (e, comps) <- entityCreate do archetypeManager.addEntity(e, comps*)
        entityCreate.clear

        for e <- entityDelete do
          archetypeManager.delete(e)
          val _ = entityIdGenerator.erase(e.id)
          if entityAddComps.contains(e) then
            for (_, orElse) <- entityAddComps(e) do orElse()
            entityAddComps -= e
          if entityRemoveComps.contains(e) then
            for (_, orElse) <- entityRemoveComps(e) do orElse()
            entityRemoveComps -= e
        entityDelete = List.empty

        for (e, comps) <- entityAddComps do
          archetypeManager.addComponents(e, comps.map(_._1)*)
        entityAddComps.clear

        for (e, types) <- entityRemoveComps do
          archetypeManager.removeComponents(e, types.map(_._1)*)
        entityRemoveComps.clear

      private inline def processPendingSystems(): Unit =
        if pendingSystems.nonEmpty then
          for s <- pendingSystems do activeSystems = activeSystems :+ s
          pendingSystems = Vector.empty
          activeSystems = activeSystems.sortBy(_.priority)
          ()

      import EntityRequest.*
      import SystemRequest.*

      override def defer(q: SystemRequest | EntityRequest): Boolean =
        q match
          case SystemRequest.pause(systemName) =>
            tryForwardCommandToSystem(systemName, _.pause())
          case SystemRequest.resume(systemName) =>
            tryForwardCommandToSystem(systemName, _.resume())

          case EntityRequest.create(components*) =>
            entityCreate += (Entity(entityIdGenerator.next) -> components)
            areBuffersDirty = true
            true
          case EntityRequest.delete(e) =>
            if isEntityValid(e) && !entityDelete.contains(e) then
              entityDelete = e +: entityDelete
              areBuffersDirty = true
              return true
            false
          case EntityRequest.addComponent(e, component, orElse) =>
            val res = tryScheduleAddOrRemoveComponent(entityAddComps, e, component, orElse)
            areBuffersDirty = res
            res
          case EntityRequest.removeComponent(e, cType, orElse) =>
            val res = tryScheduleAddOrRemoveComponent(entityRemoveComps, e, cType, orElse)
            areBuffersDirty = res
            res

      override def isSystemRunning(name: String): Boolean =
        val idx = activeSystems.indexWhere(_.name == name)
        if idx != -1 then return activeSystems(idx).isRunning
        false

      override def isSystemPaused(name: String): Boolean =
        val idx = activeSystems.indexWhere(_.name == name)
        if idx != -1 then return activeSystems(idx).isPaused
        false

      private inline def isEntityValid(e: Entity) = entityIdGenerator.isValid(e.id)

      private inline def tryForwardCommandToSystem(
          systemName: String,
          inline command: System => Unit
      ): Boolean =
        activeSystems.find(_.name == systemName) match
          case Some(s) =>
            command(s)
            true
          case _ => false

      import ecscalibur.core.components.WithType

      private inline def tryScheduleAddOrRemoveComponent[T <: WithType](
          buffer: mutable.Map[Entity, List[(T, () => Unit)]],
          e: Entity,
          comp: T,
          orElse: () => Unit
      ): Boolean =
        var res = false
        if isEntityValid(e) then
          val l: List[(T, () => Unit)] = buffer.getOrElseUpdate(e, List.empty)
          if !l.exists(_._1.typeId == comp.typeId) then
            buffer(e) = ((comp -> orElse)) :: l
            res = true
        if !res then orElse()
        res

  private[world] object builders:
    trait EntityBuilder:
      infix def withComponents(components: Component*): Unit

      infix def withComponents(components: List[Component]): Unit

    object EntityBuilder:
      def apply()(using Mutator): EntityBuilder = EntityBuilderImpl()

      private class EntityBuilderImpl(using Mutator) extends EntityBuilder:
        override def withComponents(components: Component*): Unit =
          val _ = summon[Mutator] defer EntityRequest.create(components*)

        override def withComponents(components: List[Component]): Unit =
          val _ = summon[Mutator] defer EntityRequest.create(components*)
