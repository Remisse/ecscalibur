package ecscalibur.core

object Worlds:
  import scala.reflect.ClassTag
  import scala.collection.mutable.HashMap
  import scala.collection.mutable.ArrayBuffer
  import Entities.Entity
  import Components.Component
  import Components.ComponentId

  trait World:
    def spawn: Entity
    def isValid(e: Entity): Boolean
    def isValid(entityId: Int): Boolean
    def addComponent[T <: Component](e: Entity, c: T): Unit
    def removeComponent(e: Entity, componentId: ComponentId): Unit
    def hasComponent(e: Entity, componentId: ComponentId): Boolean

  object World:
    def apply(): World = WorldImpl()

  private class WorldImpl extends World:
    private val componentTypes: ArrayBuffer[ArrayBuffer[ComponentId]] = ArrayBuffer.empty
    private val components: ArrayBuffer[ArrayBuffer[Component]] = ArrayBuffer.empty
    import ecscalibur.id.IdGenerator
    private val idGenerator = IdGenerator()

    override def spawn: Entity =
      val id = idGenerator.next
      if !componentTypes.contains(id) then componentTypes += ArrayBuffer.empty
      if !components.contains(id) then components += ArrayBuffer.empty
      Entity(id)

    override inline def isValid(e: Entity): Boolean = isValid(e.id)
    override inline def isValid(entityId: Int): Boolean = idGenerator.isValid(entityId)

    override inline def addComponent[T <: Component](e: Entity, c: T) =
      if hasComponent(e, c.id) then throw IllegalStateException(s"Entity $this already has a component of type ${c.getClass}.")
      components(e.id) += c
      componentTypes(e.id) += c.id

    override inline def removeComponent(e: Entity, id: ComponentId): Unit =
      val i = componentTypes(e.id).indexOf(id)
      if i == -1 then throw new IllegalStateException(s"Entity $this does not have a component of type ID $id.")
      components(e.id).remove(i)
      componentTypes(e.id).remove(i)

    override inline def hasComponent(e: Entity, id: ComponentId): Boolean =
      componentTypes(e.id).contains(id)
