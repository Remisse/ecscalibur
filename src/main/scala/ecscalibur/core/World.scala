package ecscalibur.core

import ecscalibur.core.systems.System
import ecscalibur.core.archetype.ArchetypeManager
import ecscalibur.core.component.{Component, ComponentType, CSeq}
import ecscalibur.core.context.MetaContext
import ecscalibur.core.queries.Query

object world:
  import builders.*

  trait World:
    private[ecscalibur] val context: MetaContext

    def entity: EntityBuilder
    infix def withSystem(name: String, priority: Int = 0)(qb: QueryBuilder => Query): Unit
    infix def withSystem(s: System): Unit
    infix def loop(loopType: Loop): Unit

  enum Loop:
    case Forever
    case Times(times: Int)

  object Loop:
    val forever = Forever
    val once = Times(1)
    
    extension (n: Int)
      inline def times = Times(n)

  object World:
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
      private val pendingSystems: ArrayBuffer[System] = ArrayBuffer.empty

      private val entityCreate: mutable.Map[Entity, CSeq] = mutable.Map.empty
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

      override def withSystem(s: System): Unit = pendingSystems += s

      override def loop(loopType: Loop): Unit =
        inline def _loop(): Unit =
          updateDeltaTime()
          if (areBuffersDirty)
            areBuffersDirty = false
            processPendingEntityOperations()
          if (pendingSystems.nonEmpty)
            processPendingSystems()
          for s <- activeSystems do s.update()
        loopType match
          case Loop.Forever      => while (true) _loop()
          case Loop.Times(times) => for _ <- (0 until times) do _loop()

      private inline def processPendingEntityOperations() =
        for (e, comps) <- entityCreate do am.addEntity(e, comps)
        entityCreate.clear

        for e <- entityDelete do
          am.delete(e)
          entityIdGenerator.erase(e.id)
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

      private inline def processPendingSystems() =
        for s <- pendingSystems do activeSystems += s
        pendingSystems.clear()

      import EntityRequest.*
      import SystemRequest.*

      override def defer(q: SystemRequest | EntityRequest): Boolean =
        var res = false
        q match
          case SystemRequest.stop(systemName) =>
            forwardCommandToSystem(systemName, _.pause())
            res = true
          case SystemRequest.resume(systemName) =>
            forwardCommandToSystem(systemName, _.resume())
            res = true

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

      private inline def forwardCommandToSystem(systemName: String, inline command: System => Unit) =
        activeSystems.find(_.name == systemName) match
          case Some(s) => command(s)
          case _       => ()

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

      private inline def updateDeltaTime() = ctx.setDeltaTime(pacer.pace())

  private[world] object builders:
    trait EntityBuilder:
      infix def withComponents(components: CSeq): Unit

    object EntityBuilder:
      def apply()(using Mutator): EntityBuilder = EntityBuilderImpl()

      private class EntityBuilderImpl(using Mutator) extends EntityBuilder:
        override def withComponents(components: CSeq): Unit =
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
