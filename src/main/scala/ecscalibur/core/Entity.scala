package ecscalibur.core

opaque type Entity <: Int = Int

/**
  * Factory for [[Entity]].
  */
private[ecscalibur] object Entity:
  def apply(id: Int): Entity = id

export Extensions.*
object Extensions:
  extension (e: Entity)
    inline def id: Int = e
