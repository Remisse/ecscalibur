package ecscalibur.core

import ecscalibur.core.components.Component
import ecscalibur.core.components.ComponentType

export entity.*

object entity:
  /** Identifiers that do not hold any state. They can be created through a [[World]] instance,
    * which will then handle their lifetime and [[Component]]s.
    */
  opaque type Entity <: Int = Int

  private[ecscalibur] object Entity:
    def apply(id: Int): Entity = id

  extension (e: Entity)
    /** @return
      *   the unique ID of this Entity.
      */
    inline def id: Int = e

    /** Syntactic sugar for [[World.update]].
      *
      * @param c
      *   the Component whose reference must be updated
      */
    inline def <==(c: Component)(using World): Unit =
      summon[World].update(e, c)

    /** Syntactic sugar for [[World.hasComponents]].
      *
      * @param c
      *   the ComponentTypes of the Components this entity should have
      */
    inline def ?>(tpe: ComponentType)(using World): Boolean =
      summon[World].hasComponents(e, tpe)

    /** Syntactic sugar for [[Mutator.defer]] [[EntityRequest.removeComponent]].
      *
      * @param tpe
      *   ComponentType of the Component to be removed
      * @param orElse
      *   callback executed if the deferred request fails
      * @return
      *   this Entity
      */
    inline def -=(tpe: ComponentType, inline orElse: () => Unit)(using World): Entity =
      val _ = summon[World].mutator defer EntityRequest.removeComponent(e, tpe, orElse)
      e

    /** Syntactic sugar for [[Mutator.defer]] [[EntityRequest.addComponent]].
      *
      * @param c
      *   Component to be added
      * @param orElse
      *   callback executed if the deferred request fails
      * @return
      *   this Entity
      */
    inline def +=(c: Component, inline orElse: () => Unit)(using World): Entity =
      val _ = summon[World].mutator defer EntityRequest.addComponent(e, c, orElse)
      e
