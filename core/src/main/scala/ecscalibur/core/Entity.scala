package ecscalibur.core

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
