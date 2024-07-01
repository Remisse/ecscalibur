package ecscalibur.core

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
