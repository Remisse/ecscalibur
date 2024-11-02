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

    /** Creates a new [[Entity]] with the given components. 
      * Calls `Mutator.defer DeferredRequest.createEntity(components)`; thus, execution 
      * will be delayed until the next world loop.
      * 
      * @param components
      *   components the new entity will have
      */
    def +=(components: Component*): Unit

    /** Creates a new [[Entity]] with the given components. 
      * Calls `Mutator.defer DeferredRequest.createEntity(components)`; thus, execution 
      * will be delayed until the next world loop.
      * 
      * @param components
      *   components the new entity will have
      */
    def +=(components: List[Component]): Unit

    /** Deletes the given [[Entity]].
      * Calls `Mutator.defer DeferredRequest.deleteEntity(e)`; thus, execution 
      * will be delayed until the next world loop.
      */
    def -=(e: Entity): Unit

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
    infix def system(name: String, priority: Int)(q: Query): Unit

    /** Creates a new System with [[System.process]] overridden by the given [[Query]]. Both
      * [[System.onResume]] and [[System.onPause]] do not contain any logic.
      * 
      * The priority of this newly created system will default to 0.
      *
      * @param name
      *   a unique name for this System
      * @param q
      *   a query
      * @throws IllegalArgumentException
      *   if a System with the same name already exists
      */
    infix def system(name: String)(q: Query): Unit

    /** Adds a [[System]] instance to this World.
      *
      * @param s
      *   the System to be added.
      * @param priority
      *   its priority value relative to the other Systems in the World
      * @throws IllegalArgumentException
      *   if a System with the same name already exists
      */
    infix def system(s: System, priority: Int = 0): Unit

    /** Subscribes a new listener to this World's EventBus.
      *
      * @param listenerName
      *   name of the new listener
      * @param callback
      *   the method this World's EventBus instance will execute when an event of type `E` is
      *   emitted
      * @tparam E
      *   the type of event to subscribe to
      */
    infix def subscribe[E <: Event: ClassTag](listenerName: String)(
        callback: (Entity, E) => Unit
    ): Unit

    /** Removes an existing listener from this World's EventBus.
      *
      * @param listenerName
      *   name of the listener to be removed
      * @param eventType
      *   type of event to unsubscribe from
      */
    infix def unsubscribe(listenerName: String, eventType: EventType): Unit

    /**
      * Pauses the execution of a system.
      *
      * @param system
      *   name of the system to pause
      * @return
      *   `true` if the system was succesfully paused, `false` otherwise.
      */
    infix def pauseSystem(system: String): Boolean

    /**
      * Resumes the execution of a system.
      *
      * @param system
      *   name of the system to resume
      * @return
      *   `true` if the system was succesfully resumed, `false` otherwise.
      */
    infix def resumeSystem(system: String): Boolean

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

      private var activeSystems: Vector[(System, Int)] = Vector.empty
      private var pendingSystems: Vector[(System, Int)] = Vector.empty

      private val entityCreate: mutable.Map[Entity, Seq[Component]] = mutable.Map.empty
      private var entityDelete: List[Entity] = List.empty
      private val entityAddComps: mutable.Map[Entity, List[Component]] = mutable.Map.empty
      private val entityRemoveComps: mutable.Map[Entity, List[ComponentType]] = mutable.Map.empty
      private var areBuffersDirty = false

      override def +=(components: Component*) = 
        summon[Mutator].defer(DeferredRequest.createEntity(components*))
        ()

      override def +=(components: List[Component]) = 
        summon[Mutator].defer(DeferredRequest.createEntity(components*))
        ()

      override def -=(e: Entity) = 
        summon[Mutator].defer(DeferredRequest.deleteEntity(e))
        ()

      override def hasComponents(e: Entity, types: ComponentType*): Boolean =
        archetypeManager.hasComponents(e, types*)

      private def makeSimpleSystem(name: String, priority: Int = 0)(q: Query): Unit =
        val newSystem = new System(name):
          override protected val process: Query = q

        system(newSystem, priority)

      override def system(name: String, priority: Int)(q: Query): Unit = makeSimpleSystem(name, priority)(q)

      override def system(name: String)(q: Query): Unit = makeSimpleSystem(name)(q)

      override def system(s: System, priority: Int): Unit =
        require(
          !(activeSystems.exists(_._1 == s) || pendingSystems.exists(_._1 == s)),
          s"System \"${s.name}\" already exists."
        )
        pendingSystems = pendingSystems :+ (s, priority)

      override def subscribe[E <: Event: ClassTag](listenerName: String)(
          callback: (Entity, E) => Unit
      ): Unit =
        eventBus.subscribe(listenerName)(callback)

      override def unsubscribe(listenerName: String, eventType: EventType): Unit =
        eventBus.unsubscribe(listenerName, eventType)

      override def loop(loopType: Loop): Unit =
        inline def _loop(): Unit =
          context.setDeltaTime(
            pacer.pace:
              if areBuffersDirty then
                areBuffersDirty = false
                processPendingEntityOperations()
              processPendingSystems()
              for s <- activeSystems do s._1.update()
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
          activeSystems = activeSystems.sortBy(_._2)
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
          areBuffersDirty |= res
          res
        case DeferredRequest.removeComponent(e, cType) =>
          val res = tryScheduleAddOrRemoveComponent(entityRemoveComps, e, cType)
          areBuffersDirty |= res
          res

      override def doImmediately(q: ImmediateRequest): Boolean = q match
        case ImmediateRequest.pause(systemName) =>
          tryForwardCommandToSystem(systemName, _.pause())
        case ImmediateRequest.resume(systemName) =>
          tryForwardCommandToSystem(systemName, _.resume())
        case ImmediateRequest.update(e, c) =>
          archetypeManager.update(e, c)
          true

      override def pauseSystem(system: String): Boolean = 
        if isSystemRunning(system) then return doImmediately(ImmediateRequest.pause(system))
        false

      override def resumeSystem(system: String): Boolean = 
        if isSystemPaused(system) then return doImmediately(ImmediateRequest.resume(system))
        false

      private inline def checkSystemExecutionStatus(name: String, inline p: System => Boolean): Boolean =
        var res = false
        val idx = activeSystems.indexWhere(_._1.name == name)
        if idx != -1 then res = p(activeSystems(idx)._1)
        res

      override def isSystemRunning(name: String): Boolean = checkSystemExecutionStatus(name, s => s.isRunning)

      override def isSystemPaused(name: String): Boolean = checkSystemExecutionStatus(name, s => s.isPaused)

      private inline def isEntityValid(e: Entity) = entityIdGenerator.isValid(e.id)

      private inline def tryForwardCommandToSystem(
          systemName: String,
          inline command: System => Unit
      ): Boolean =
        activeSystems.find(_._1.name == systemName) match
          case Some(s) =>
            command(s._1)
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
