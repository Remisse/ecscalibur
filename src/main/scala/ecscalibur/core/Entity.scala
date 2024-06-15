package ecscalibur.core

import Components.Component
import Components.ComponentId
import Worlds.World

object Entities:
  opaque type Entity = Int

  object Entity:
    def apply(id: Int): Entity = id

  extension (e: Entity)
    inline def id: Int = e

    inline infix def +=[T <: Component](c: T)(using world: World): Entity =
      world.addComponent(e, c)
      e

    inline infix def -=(compId: ComponentId)(using world: World): Entity =
      world.removeComponent(e, compId)
      e

    inline infix def has(compId: ComponentId)(using world: World): Boolean =
      world.hasComponent(e, compId)
