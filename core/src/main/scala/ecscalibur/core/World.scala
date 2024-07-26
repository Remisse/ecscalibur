package ecscalibur.core

import ecscalibur.core.archetype.ArchetypeManager
import ecscalibur.core.components.Component
import ecscalibur.core.components.ComponentType
import ecscalibur.core.context.MetaContext
import scala.reflect.ClassTag

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

    /** Reference to this World's [[EventBus]] instance.
      * @return
      *   this World's EventBus instance
      */
    given eventBus: EventBus

    private[ecscalibur] given archetypeManager: ArchetypeManager

    /** Starts the creation of a new [[Entity]]. Call [[EntityBuilder.withComponents]] after this to
      * define which Components this Entity should have.
      *
      * @return
      *   an [[EntityBuilder]] instance
      */
    def entity: EntityBuilder

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

    /** Subscribes a new listener to this World's EventBus.
      *
      * @param listenerName
      *   the name of the specified listener
      * @param callback
      *   the method this World's EventBus instance will execute when an event of type `E` is
      *   emitted
      * @tparam E
      *   the type of event to subscribe to
      */
    infix def listener[E <: Event: ClassTag](listenerName: String)(
        callback: (Entity, E) => Unit
    ): Unit

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
      WorldImpl(iterationsPerSecond)(using ArchetypeManager(), MetaContext(), EventBus())

    private class WorldImpl(frameCap: Int)(using ArchetypeManager, MetaContext, EventBus)
        extends World,
          Mutator:
      import scala.collection.mutable

      override given archetypeManager: ArchetypeManager = summon[ArchetypeManager]
      override given mutator: Mutator = this
      override given context: MetaContext = summon[MetaContext]
      override given eventBus: EventBus = summon[EventBus]

      import ecsutil.FramePacer
      private val pacer = FramePacer(frameCap)

      import ecsutil.IdGenerator
      private val entityIdGenerator = IdGenerator()

      private var activeSystems: Vector[System] = Vector.empty
      private var pendingSystems: Vector[System] = Vector.empty

      private val entityCreate: mutable.Map[Entity, Seq[Component]] = mutable.Map.empty
      private var entityDelete: List[Entity] = List.empty
      private val entityAddComps: mutable.Map[Entity, List[Component]] = mutable.Map.empty
      private val entityRemoveComps: mutable.Map[Entity, List[ComponentType]] = mutable.Map.empty
      private var areBuffersDirty = false

      override def entity: EntityBuilder = EntityBuilder()(using this)

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
        pendingSystems = pendingSystems :+ s

      override def listener[E <: Event: ClassTag](listenerName: String)(
          callback: (Entity, E) => Unit
      ): Unit =
        eventBus.subscribe(listenerName)(callback)

      override def loop(loopType: Loop): Unit =
        inline def _loop(): Unit =
          context.setDeltaTime(
            pacer.pace:
              if areBuffersDirty then
                areBuffersDirty = false
                processPendingEntityOperations()
              processPendingSystems()
              for s <- activeSystems do s.update()
          )
        loopType match
          case Loop.Forever      => while true do _loop()
          case Loop.Times(times) => for _ <- 0 until times do _loop()

      private inline def processPendingEntityOperations(): Unit =
        for (e, comps) <- entityCreate do archetypeManager.addEntity(e, comps*)
        entityCreate.clear

        for e <- entityDelete do
          archetypeManager.delete(e)
          val _ = entityIdGenerator.erase(e.id)
          entityAddComps -= e
          entityRemoveComps -= e
        entityDelete = List.empty

        for (e, comps) <- entityAddComps do
          if entityRemoveComps.contains(e) then
            archetypeManager.addRemove(e)(comps*)(entityRemoveComps(e)*)
            entityRemoveComps -= e
          else archetypeManager.addComponents(e, comps*)
        entityAddComps.clear

        for (e, types) <- entityRemoveComps do archetypeManager.removeComponents(e, types*)
        entityRemoveComps.clear

      private inline def processPendingSystems(): Unit =
        if pendingSystems.nonEmpty then
          for s <- pendingSystems do activeSystems = activeSystems :+ s
          pendingSystems = Vector.empty
          activeSystems = activeSystems.sortBy(_.priority)
          ()

      override def defer(q: DeferredRequest): Boolean = q match
        case DeferredRequest.createEntity(components*) =>
          entityCreate += (Entity(entityIdGenerator.next) -> components)
          areBuffersDirty = true
          true
        case DeferredRequest.deleteEntity(e) =>
          if isEntityValid(e) && !entityDelete.contains(e) then
            entityDelete = e +: entityDelete
            areBuffersDirty = true
            return true
          false
        case DeferredRequest.addComponent(e, component) =>
          val res = tryScheduleAddOrRemoveComponent(entityAddComps, e, component)
          areBuffersDirty = res
          res
        case DeferredRequest.removeComponent(e, cType) =>
          val res = tryScheduleAddOrRemoveComponent(entityRemoveComps, e, cType)
          areBuffersDirty = res
          res

      override def doImmediately(q: ImmediateRequest): Boolean = q match
        case ImmediateRequest.pause(systemName) =>
          tryForwardCommandToSystem(systemName, _.pause())
        case ImmediateRequest.resume(systemName) =>
          tryForwardCommandToSystem(systemName, _.resume())
        case ImmediateRequest.update(e, c) =>
          archetypeManager.update(e, c)
          true

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
          buffer: mutable.Map[Entity, List[T]],
          e: Entity,
          comp: T
      ): Boolean =
        var res = false
        if isEntityValid(e) then
          val l: List[T] = buffer.getOrElseUpdate(e, List.empty)
          if !l.exists(_.typeId == comp.typeId) then
            buffer(e) = comp +: l
            res = true
        res

  private[world] object builders:
    trait EntityBuilder:
      infix def withComponents(components: Component*): Unit

      infix def withComponents(components: List[Component]): Unit

    object EntityBuilder:
      def apply()(using Mutator): EntityBuilder = EntityBuilderImpl()

      private class EntityBuilderImpl(using Mutator) extends EntityBuilder:
        override def withComponents(components: Component*): Unit =
          val _ = summon[Mutator] defer DeferredRequest.createEntity(components*)

        override def withComponents(components: List[Component]): Unit =
          val _ = summon[Mutator] defer DeferredRequest.createEntity(components*)
