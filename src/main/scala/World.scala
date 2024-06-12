package ecscalibur

import ecscalibur.Components.Component
import scala.reflect.ClassTag

object Worlds:
  import scala.collection.mutable.HashMap
  import scala.collection.mutable.ArrayBuffer
  import ecscalibur.Components.Component

  trait World:
    private[Worlds] def componentTypes: ArrayBuffer[ArrayBuffer[ClassTag[_]]]
    private[Worlds] def components: ArrayBuffer[ArrayBuffer[Component]]
    def spawn: Entity
    def exists(e: Entity): Boolean
    def exists(id: Int): Boolean

  object World:
    def apply(): World = WorldImpl()

  private class WorldImpl extends World:
    private var firstAvailableEntityIndex = 0
    private val entityIndexes = ArrayBuffer.empty[Int]
    override val componentTypes = ArrayBuffer.empty
    override val components = ArrayBuffer.empty

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

    override inline def exists(e: Entity): Boolean = exists(e.id)
    override inline def exists(id: Int): Boolean = entityIndexes.contains(id)

  case class Entity(val id: Int):
    inline def +=[T <: Component](c: T)(using world: World)(using tag: ClassTag[T]): Entity =
      if has[T] then
        throw IllegalStateException(s"Entity $this already has a component of type $tag")
      addComponent(c)(using world)
      this

    inline def has[T <: Component](using world: World)(using tag: ClassTag[T]): Boolean =
      world.componentTypes(id).contains(tag)

    private inline def addComponent[T <: Component](c: T)(using world: World)(using
        tag: ClassTag[T]
    ) =
      world.components(id) += c
      world.componentTypes(id) += tag

    inline def remove[T <: Component](using world: World)(using tag: ClassTag[T]): Entity =
      if !has[T] then
        throw new IllegalStateException(s"Entity $this does not have a component of type $tag")
      val i = world.componentTypes(id).indexOf(tag)
      world.components(id).remove(i)
      world.componentTypes(id).remove(i)
      this
