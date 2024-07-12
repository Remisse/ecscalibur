package ecsdemo

import ecscalibur.core.*
import demoutil.transform.Vector2
import demoutil.Color

object components:
  export intentions.*
  export events.*

  @component
  case class Position(v: Vector2) extends Component
  object Position extends ComponentType

  @component
  case class Velocity(v: Vector2) extends Component:
    override def toString: String = v.toString
  object Velocity extends ComponentType

  @component
  case class Colorful(c: Color) extends Component
  object Colorful extends ComponentType

  @component
  class Timer(val intervalSeconds: Float, val currentSeconds: Float = 0f) extends Component
  object Timer extends ComponentType

  object intentions:
    @component
    class WantsToStop(val originalVelocity: Velocity) extends Component
    object WantsToStop extends ComponentType

    @component
    class WantsToResume(val originalVelocity: Velocity) extends Component
    object WantsToResume extends ComponentType

    @component
    class WantsToChangeVelocity extends Component
    object WantsToChangeVelocity extends ComponentType

    @component
    class WantsToChangeColor extends Component
    object WantsToChangeColor extends ComponentType

  object events:
    // Scalable alternative: events with a 'consumedBy' parameter. Systems interested in a certain event
    // can check if their name is present in consumedBy: if it isn't, the systems will consume the event 
    // and add themselves to consumedBy. Conversely, if their name is present, they will ignore the event.
    @component
    class StoppedEvent extends Component
    object StoppedEvent extends ComponentType

    @component
    class ResumedMovementEvent extends Component
    object ResumedMovementEvent extends ComponentType

    @component
    class ChangedVelocityEvent(val newVelocity: Velocity) extends Component
    object ChangedVelocityEvent extends ComponentType

    @component
    class ChangedColorEvent(val newColor: Color) extends Component
    object ChangedColorEvent extends ComponentType
