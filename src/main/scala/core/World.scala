package ecscalibur.core

object Worlds:
  import scala.reflect.ClassTag
  import scala.collection.mutable.HashMap
  import scala.collection.mutable.ArrayBuffer
  import Entities.Entity
  import Components.Component

  trait World:
    def spawn: Entity
    def isValid(e: Entity): Boolean
    def isValid(entityId: Int): Boolean
    def addComponent[T <: Component](e: Entity, c: T)(using tag: ClassTag[T]): Unit
    def removeComponent[T <: Component](e: Entity)(using tag: ClassTag[T]): Unit
    def hasComponent[T <: Component](e: Entity)(using tag: ClassTag[T]): Boolean

  object World:
    def apply(): World = WorldImpl()

  private class WorldImpl extends World:
    private val componentTypes: ArrayBuffer[ArrayBuffer[ClassTag[_]]] = ArrayBuffer.empty
    private val components: ArrayBuffer[ArrayBuffer[Component]] = ArrayBuffer.empty
    private var firstAvailableEntityIndex = 0
    private val entityIndexes = ArrayBuffer.empty[Int]

    override def spawn: Entity =
      val id = firstAvailableEntityIndex
      entityIndexes += id
      firstAvailableEntityIndex = findNextAvailableId

      if !componentTypes.contains(id) then componentTypes += ArrayBuffer.empty
      if !components.contains(id) then components += ArrayBuffer.empty
      Entity(id)

    private inline def findNextAvailableId: Int =
      import scala.util.boundary
      import boundary.break
      boundary:
        for
          i <- entityIndexes.view.drop(firstAvailableEntityIndex)
          j = i + 1
          if j < entityIndexes.length
        do if entityIndexes(j) - entityIndexes(i) > 1 then break(j)
        entityIndexes.length

    override inline def isValid(e: Entity): Boolean = isValid(e.id)
    override inline def isValid(entityId: Int): Boolean = entityIndexes.contains(entityId)

    override inline def addComponent[T <: Component](e: Entity, c: T)(using tag: ClassTag[T]) =
      components(e.id) += c
      componentTypes(e.id) += tag

    override inline def removeComponent[T <: Component](e: Entity)(using tag: ClassTag[T]): Unit =
      val i = componentTypes(e.id).indexOf(tag)
      components(e.id).remove(i)
      componentTypes(e.id).remove(i)

    override inline def hasComponent[T <: Component](e: Entity)(using tag: ClassTag[T]): Boolean =
      componentTypes(e.id).contains(tag)
