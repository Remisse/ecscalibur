package ecscalibur.core

object Worlds:
  import scala.collection.mutable.ArrayBuffer
  import Entities.Entity
  import Components.Component
  import Components.ComponentId

  trait World:
    def spawn: Entity
    def isValid(e: Entity): Boolean
    def addComponent[T <: Component](e: Entity, c: T): Unit
    def removeComponent(e: Entity, componentId: ComponentId): Unit
    def hasComponent(e: Entity, componentId: ComponentId): Boolean

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

    override inline def isValid(e: Entity): Boolean = idGenerator.isValid(e.id)

    override inline def addComponent[T <: Component](e: Entity, c: T) =
      if hasComponent(e, c.id) then
        throw IllegalStateException(s"Entity $this already has a component of type ${c.getClass}.")
      components(e.id) += c

    override inline def removeComponent(e: Entity, id: ComponentId): Unit =
      val i = findComponentIdx(e, id)
      if i == -1 then
        throw new IllegalStateException(s"Entity $this does not have a component of type ID $id.")
      components(e.id).remove(i)

    override inline def hasComponent(e: Entity, id: ComponentId): Boolean =
      findComponentIdx(e, id) != -1

    private inline def findComponentIdx(e: Entity, id: ComponentId): Int =
      components(e.id).indexWhere(_.id == id)
