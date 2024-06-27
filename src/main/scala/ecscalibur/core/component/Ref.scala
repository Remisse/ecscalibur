package ecscalibur.core.component

import ecscalibur.core.archetype.Archetypes.Fragment
import ecscalibur.core.Entity

class Ref[T <: Component](c: T)(fragment: Fragment, entity: Entity) extends Component(using Ref):
  private var _component: T = c

  def component: T = _component
  inline infix def <==(c: T) =
    _component = c
    fragment.update(entity, c)

object Ref extends ComponentType