package ecscalibur.core

import Components.*
import Worlds.World
import scala.annotation.targetName

object Entities:
  opaque type Entity = Int

  /**
    * Factory for [[ecscalibur.core.Entities.Entity]].
    */
  object Entity:
    def apply(id: Int): Entity = id

  extension (e: Entity)
    inline def id: Int = e

    @targetName("add")
    inline infix def +=[T <: Component](c: T)(using world: World): Entity =
      world.addComponent(e, c)
      e

    @targetName("remove")
    inline infix def -=(compType: ComponentType)(using world: World): Entity =
      world.removeComponent(e, compType)
      e

    inline infix def has(compType: ComponentType)(using world: World): Boolean =
      world.hasComponent(e, compType)

    inline infix def has(compTypes: ComponentType*)(using world: World): Boolean =
      require(compTypes.nonEmpty, "Given type sequence cannot be empty.")
      compTypes forall has
