package ecscalibur.core

import scala.annotation.targetName
import component.{ComponentType, Component}

type Entity = Int

/**
  * Factory for [[Entity]].
  */
private[core] object Entity:
  def apply(id: Int): Entity = id

export Extensions.*
object Extensions:
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
