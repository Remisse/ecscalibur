package ecscalibur.testutil

import ecscalibur.core.component.{Component, ComponentType}

object testclasses:
  class C1 extends Component
  object C1 extends ComponentType

  class C2 extends Component
  object C2 extends ComponentType

  class C3 extends Component
  object C3 extends ComponentType

  class C4 extends Component
  object C4 extends ComponentType

  class C5 extends Component
  object C5 extends ComponentType

  class C6 extends Component
  object C6 extends ComponentType

  case class Value(x: Int) extends Component
  object Value extends ComponentType

  class WrongGiven extends Component
  object WrongGiven extends ComponentType

  case class IntWrapper(n: Int)

  class OneKinded[T <: Component] extends Component
  object OneKinded extends ComponentType
