package ecscalibur.core

import scala.annotation.targetName

export entity.Entity

object entity:
  /** Identifiers that do not hold any state. They can be created through a [[World]] instance,
    * which will then handle their lifetime and [[Component]]s.
    * @param id
    *   the unique ID of this Entity.
    */
  sealed trait Entity(val id: Int):
    /** Syntactic sugar for [[Mutator.doImmediately]] [[ImmediateRequest.update]].
      *
      * @param c
      *   the Component whose reference must be updated
      */
    @targetName("update")
    inline def <==(c: Component)(using World): Entity =
      summon[World].mutator doImmediately ImmediateRequest.update(this, c)
      this

    /** Syntactic sugar for [[World.hasComponents]].
      *
      * @param tpe
      *   the ComponentTypes of the Components this entity should have
      */
    @targetName("has")
    inline def ?>(tpe: ComponentType*)(using World): Boolean =
      summon[World].hasComponents(this, tpe*)

    /** Syntactic sugar for [[Mutator.defer]] [[DeferredRequest.removeComponent]].
      *
      * @param tpe
      *   ComponentType of the Component to be removed
      * @param orElse
      *   callback executed if the deferred request fails
      * @return
      *   this Entity
      */
    @targetName("remove")
    inline def -=(tpe: ComponentType)(using World): Entity =
      val _ = summon[World].mutator defer DeferredRequest.removeComponent(this, tpe)
      this

    /** Syntactic sugar for [[Mutator.defer]] [[DeferredRequest.addComponent]].
      *
      * @param c
      *   Component to be added
      * @param orElse
      *   callback executed if the deferred request fails
      * @return
      *   this Entity
      */
    @targetName("add")
    inline def +=(c: Component)(using World): Entity =
      val _ = summon[World].mutator defer DeferredRequest.addComponent(this, c)
      this

    override def equals(that: Any): Boolean = that match
      case e: Entity => id == e.id
      case _ => false

    override def hashCode(): Int = id

    override def toString(): String = s"Entity $id"

  object Entity:
    val Nil = Entity(-1)

    private[ecscalibur] def apply(id: Int) = new Entity(id) {}
