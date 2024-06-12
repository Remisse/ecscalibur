package ecscalibur.core

import scala.reflect.ClassTag
import Components.Component
import Worlds.World

object Entities:
  case class Entity(val id: Int):
    inline def +=[T <: Component](c: T)(using world: World)(using tag: ClassTag[T]): Entity =
      if has[T] then throw IllegalStateException(s"Entity $this already has $tag.")
      world.addComponent(this, c)
      this

    inline def remove[T <: Component](using world: World)(using tag: ClassTag[T]): Entity =
      if !has[T] then throw new IllegalStateException(s"Entity $this does not have $tag.")
      world.removeComponent(this)
      this

    inline def -=[T <: Component](tag: ClassTag[T])(using world: World): Entity =
      remove[T](using world)(using tag)

    inline def has[T <: Component](using world: World)(using tag: ClassTag[T]): Boolean =
      world.hasComponent[T](this)
