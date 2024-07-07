package ecscalibur.core

import ecscalibur.core.archetype.ArchetypeManager
import ecscalibur.core.component.Component
import ecscalibur.core.component.ComponentType
import ecscalibur.core.context.MetaContext
import ecscalibur.core.queries.Query
import ecscalibur.core.queries.query
import ecscalibur.core.systems.System

object world:
  import builders.*

  /** A World is the main entry point into an ECS application. It lets the user create Entities, add
    * Components to them and define Systems to iterate over the created Entities.
    *
    * Ideally, there should only be one World instance per application.
    */
  trait World:
    private[ecscalibur] val context: MetaContext

    /** Starts the creation of a new [[Entity]]. Call [[EntityBuilder.withComponents]] after this to
      * define which Components this Entity should have.
      *
      * @return
      *   an [[EntityBuilder]] instance
      */
    def entity: EntityBuilder

    /** Creates a new System with [[System.process]] overridden by the given [[Query]]. Both
      * [[System.onResume]] and [[System.onPause]] do not contain any logic.
      *
      * @param name
      *   a unique name for this System
      * @param priority
      *   its priority value relative to the other Systems in the World
      * @param qb
      *   a function that returns a new Query created with the help of a [[QueryBuilder]]
      */
    infix def withSystem(name: String, priority: Int = 0)(qb: QueryBuilder => Query): Unit

    /** Adds a [[System]] instance to this World.
      *
      * @param s
      *   the System to be added.
      */
    infix def withSystem(s: System): Unit

    /** Performs one or more World iterations based on the given [[Loop]] type.
      *
      * @param loopType
      *   dictates how many iterations will be performed
      */
    infix def loop(loopType: Loop): Unit

  /** Represents the number of iterations a [[World]] will perform when calling [[World!.loop]].
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
    /** Returns a [[Loop!.Forever]] instance.
      */
    val forever: Loop = Forever

    /** Returns a [[Loop!.Times]] instance initialized to 1.
      */
    val once: Loop = Times(1)

    /** @return
      *   a [[Loop!.Times]] instance initialized with the given parameter.
      */
    extension (n: Int) inline def times: Loop = Times(n)

  object World:
    // TODO Pass a Configuration object instead

    /** @param frameCap
      *   Frame rate limit expressed as Frames Per Second (FPS). A value of 0 means no limit.
      * @return
      *   a new World instance
      */
    def apply(frameCap: Int = 0): World =
      WorldImpl(frameCap)(using ArchetypeManager())(using MetaContext())

    private class WorldImpl(frameCap: Int)(using am: ArchetypeManager)(using ctx: MetaContext)
        extends World,
          Mutator:
      import ecscalibur.id.IdGenerator
      import scala.collection.mutable

      given mutator: Mutator = this
      override val context: MetaContext = ctx

      import ecscalibur.util.FramePacer
      private val pacer = FramePacer(frameCap)

      private val entityIdGenerator = IdGenerator()

      import scala.collection.mutable.ArrayBuffer

      private val activeSystems: ArrayBuffer[System] = ArrayBuffer.empty

      private val entityCreate: mutable.Map[Entity, CSeq[Component]] = mutable.Map.empty
      private val entityDelete: ArrayBuffer[Entity] = ArrayBuffer.empty
      private val entityAddComps: mutable.Map[Entity, ArrayBuffer[(Component, () => Unit)]] =
        mutable.Map.empty
      private val entityRemoveComps: mutable.Map[Entity, ArrayBuffer[(ComponentType, () => Unit)]] =
        mutable.Map.empty
      private var areBuffersDirty = false

      override def entity: EntityBuilder = EntityBuilder()(using this)

      override def withSystem(name: String, priority: Int)(qb: QueryBuilder => Query): Unit =
        withSystem:
          LiteSystemBuilder(name, priority)(using am, ctx).executing(qb(query))

      override def withSystem(s: System): Unit =
        require(!activeSystems.contains(s), s"System ${s.name} already exists.")
        activeSystems += s
        activeSystems.sortInPlaceBy(_.priority)

      override def loop(loopType: Loop): Unit =
        inline def _loop(): Unit =
          updateDeltaTime()
          if (areBuffersDirty)
            areBuffersDirty = false
            processPendingEntityOperations()
          for s <- activeSystems do s.update()
        loopType match
          case Loop.Forever      => while (true) _loop()
          case Loop.Times(times) => for _ <- 0 until times do _loop()

      private inline def processPendingEntityOperations(): Unit =
        for (e, comps) <- entityCreate do am.addEntity(e, comps)
        entityCreate.clear

        for e <- entityDelete do
          am.delete(e)
          val _ = entityIdGenerator.erase(e.id)
          if (entityAddComps.contains(e))
            for (_, orElse) <- entityAddComps(e) do orElse()
            entityAddComps -= e
          if (entityRemoveComps.contains(e))
            for (_, orElse) <- entityRemoveComps(e) do orElse()
            entityRemoveComps -= e
        entityDelete.clear

        for (e, comps) <- entityAddComps do am.addComponents(e, CSeq(comps.map(_._1).toArray))
        entityAddComps.clear

        for (e, types) <- entityRemoveComps do am.removeComponents(e, types.map(_._1).toArray*)
        entityRemoveComps.clear

      import EntityRequest.*
      import SystemRequest.*

      override def defer(q: SystemRequest | EntityRequest): Boolean =
        var res = false
        q match
          case SystemRequest.pause(systemName) =>
            res = forwardCommandToSystem(systemName, _.pause())
          case SystemRequest.resume(systemName) =>
            res = forwardCommandToSystem(systemName, _.resume())

          case EntityRequest.create(components) =>
            entityCreate += (Entity(entityIdGenerator.next) -> components)
            res = true
            areBuffersDirty = true
          case EntityRequest.delete(e) =>
            if (isEntityValid(e) && !entityDelete.contains(e))
              entityDelete += e
              res = true
              areBuffersDirty = true
          case EntityRequest.addComponent(e, component, orElse) =>
            res = addOrRemoveComponent(entityAddComps, e, component, orElse)
            areBuffersDirty = res
          case EntityRequest.removeComponent(e, cType, orElse) =>
            res = addOrRemoveComponent(entityRemoveComps, e, cType, orElse)
            areBuffersDirty = res
        res

      override def isSystemRunning(name: String): Boolean =
        val idx = activeSystems.indexWhere(_.name == name)
        if (idx != -1) return activeSystems(idx).isRunning
        false

      override def isSystemPaused(name: String): Boolean =
        val idx = activeSystems.indexWhere(_.name == name)
        if (idx != -1) return activeSystems(idx).isPaused
        false

      private inline def isEntityValid(e: Entity) = entityIdGenerator.isValid(e.id)

      private inline def forwardCommandToSystem(
          systemName: String,
          inline command: System => Unit
      ): Boolean =
        activeSystems.find(_.name == systemName) match
          case Some(s) =>
            command(s)
            true
          case _ => false

      import ecscalibur.core.component.WithType

      private def addOrRemoveComponent[T <: WithType](
          buffer: mutable.Map[Entity, ArrayBuffer[(T, () => Unit)]],
          e: Entity,
          comp: T,
          orElse: () => Unit
      ): Boolean =
        if (!isEntityValid(e)) return false
        val arrayBuf: ArrayBuffer[(T, () => Unit)] = buffer.getOrElseUpdate(e, ArrayBuffer.empty)
        if (!arrayBuf.exists(_._1.typeId == comp.typeId))
          arrayBuf += ((comp, orElse))
          return true
        orElse()
        false

      private inline def updateDeltaTime(): Unit = ctx.setDeltaTime(pacer.pace())

  private[world] object builders:
    trait EntityBuilder:
      infix def withComponents(components: CSeq[Component]): Unit

    object EntityBuilder:
      def apply()(using Mutator): EntityBuilder = EntityBuilderImpl()

      private class EntityBuilderImpl(using Mutator) extends EntityBuilder:
        override def withComponents(components: CSeq[Component]): Unit =
          val _ = summon[Mutator] defer EntityRequest.create(components)

    object LiteSystemBuilder:
      def apply(name: String, priority: Int)(using
          ArchetypeManager,
          MetaContext
      ): LiteSystemBuilder = new LiteSystemBuilder(name, priority)

      final class LiteSystemBuilder(name: String, priority: Int)(using
          ArchetypeManager,
          MetaContext
      ):
        infix def executing(q: Query): System =
          new System(name, priority):
            override protected val process: Query = q
