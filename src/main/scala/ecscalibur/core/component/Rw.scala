package ecscalibur.core.component

import ecscalibur.core.Entity
import ecscalibur.core.archetype.Archetypes.Archetype

class Rw[T <: Component](c: T)(archetype: Archetype, entity: Entity) extends Component:
  private var _component: T = c

  inline def get : T = _component
  inline def apply(): T = _component
  inline infix def <==(c: T) =
    _component = c
    archetype.update(entity, c)

object Rw extends ComponentType
