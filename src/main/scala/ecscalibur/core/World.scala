package ecscalibur.core

object Worlds:
  import scala.collection.mutable.ArrayBuffer
  import Entities.Entity
  import Components.*

  trait World:
    def spawn: Entity
    def isValid(e: Entity): Boolean
    def addComponent[T <: Component](e: Entity, c: T): Unit
    def removeComponent(e: Entity, compType: ComponentType): Unit
    def hasComponent(e: Entity, compType: ComponentType): Boolean

  object World:
    def apply(): World = WorldImpl()

  private class WorldImpl extends World:
    private val components: ArrayBuffer[ArrayBuffer[Component]] = ArrayBuffer.empty
    import ecscalibur.id.IdGenerator
    private val idGenerator = IdGenerator()

    override def spawn: Entity =
      val id = idGenerator.next
      if !components.contains(id) then components += ArrayBuffer.empty
      Entity(id)

    override def isValid(e: Entity): Boolean = idGenerator.isValid(e.id)

    override def addComponent[T <: Component](e: Entity, c: T) =
      if hasComponent(e, c.typeId) then
        throw IllegalArgumentException(s"Entity $this already has a component of type ${c.getClass}.")
      components(e.id) += c

    private inline def hasComponent(e: Entity, id: ComponentId): Boolean =
      findComponentIdx(e, id) != -1

    override def removeComponent(e: Entity, compType: ComponentType): Unit =
      val i = findComponentIdx(e, ~compType)
      if i == -1 then
        throw new IllegalArgumentException(s"Entity $this does not have a component of type ${compType.getClass}.")
      val _ = components(e.id).remove(i)

    override def hasComponent(e: Entity, compType: ComponentType): Boolean =
      findComponentIdx(e, ~compType) != -1

    private inline def findComponentIdx(e: Entity, id: ComponentId): Int =
      components(e.id).indexWhere(_.typeId == id)
