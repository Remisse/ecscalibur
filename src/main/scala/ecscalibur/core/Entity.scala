package ecscalibur.core

import scala.reflect.ClassTag
import Components.Component
import Components.ComponentId
import Worlds.World

object Entities:
  case class Entity(val id: Int):
    inline def +=[T <: Component](c: T)(using world: World): Entity =
      world.addComponent(this, c)
      this

    inline def remove(id: ComponentId)(using world: World): Entity =
      world.removeComponent(this, id)
      this

    inline def -=(id: ComponentId)(using world: World): Entity =
      remove(id)(using world)

    inline def has(id: ComponentId)(using world: World): Boolean =
      world.hasComponent(this, id)
