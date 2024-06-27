package ecscalibur.testutil

import ecscalibur.core.component.{Component, ComponentType}

object testclasses:
  class C1 extends Component(using C1)
  object C1 extends ComponentType

  class C2 extends Component(using C2)
  object C2 extends ComponentType

  class C3 extends Component(using C3)
  object C3 extends ComponentType

  case class Value(x: Int) extends Component(using Value)
  object Value extends ComponentType

  class WrongGiven extends Component(using C1)
  object WrongGiven extends ComponentType

  class NoCompanionObject extends Component(using C1)

  case class IntWrapper(n: Int)
