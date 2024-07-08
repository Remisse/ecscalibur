package ecscalibur.core

import ecscalibur.core.component.Component
import ecscalibur.core.world.World

/** Identifiers that do not hold any state. They can be created through a [[World]] instance, which
  * will then handle their lifetime and [[Component]]s.
  */
opaque type Entity <: Int = Int

private[ecscalibur] object Entity:
  def apply(id: Int): Entity = id

export Extensions.*

object Extensions:
  extension (e: Entity)
    /** @return
      *   the unique ID of this Entity.
      */
    inline def id: Int = e

    /** Syntax sugar for [[World.update]].
      *
      * @param c
      *   the Component whose reference must be updated
      */
    inline def <==(c: Component)(using World): Unit =
      summon[World].update(e, c)
