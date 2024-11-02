package ecscalibur.testutil

import ecscalibur.core.*

object testclasses:
  @component
  class C1 extends Component
  object C1 extends ComponentType

  @component
  class C2 extends Component
  object C2 extends ComponentType

  @component
  class C3 extends Component
  object C3 extends ComponentType

  @component
  class C4 extends Component
  object C4 extends ComponentType

  @component
  class C5 extends Component
  object C5 extends ComponentType

  @component
  class C6 extends Component
  object C6 extends ComponentType

  @component
  case class Value(x: Int) extends Component
  object Value extends ComponentType

  class NotAnnotated extends Component
  object NotAnnotated extends ComponentType

  class OneKinded[T] extends Component

  case class Vec2D(val x: Float, val y: Float):
    def +(that: Vec2D): Vec2D = Vec2D(x + that.x, y + that.y)
    def *(scalar: Float): Vec2D = Vec2D(x * scalar, y * scalar)

  @component
  class Position(val vec: Vec2D) extends Component
  object Position extends ComponentType

  @component
  class Velocity(val vec: Vec2D) extends Component
  object Velocity extends ComponentType

  class TestSystem(logic: () => Unit)(using World)
      extends ecscalibur.core.systems.System("test"):
    override protected val process: Query =
      routine:
        logic()

  @component
  class TestEvent extends Event
  object TestEvent extends EventType
