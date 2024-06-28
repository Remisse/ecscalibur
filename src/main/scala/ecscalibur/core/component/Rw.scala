package ecscalibur.core.component

import ecscalibur.core.archetype.Archetypes.Fragment
import ecscalibur.core.Entity

class Rw[T <: Component](c: T)(fragment: Fragment, entity: Entity) extends Component(using Rw):
  private var _component: T = c

  override def typeId: ComponentId = _component.typeId

  inline def get : T = _component
  inline def apply(): T = _component
  inline infix def <==(c: T) =
    _component = c
    fragment.update(entity, c)

object Rw extends ComponentType
