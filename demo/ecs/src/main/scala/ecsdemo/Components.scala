package ecsdemo

import ecscalibur.core.*
import demoutil.transform.Vector2
import demoutil.Color

object components:
  export intentions.*
  export events.*

  private[components] trait WithVec2(v: Vector2):
    private var _vec: Vector2 = v

    inline def vec: Vector2 = _vec
    inline def +=(v: Vector2): Unit = _vec = _vec + v
    inline def vec_=(v: Vector2): Unit = _vec = v

    override def toString: String = _vec.toString

  @component
  class Position(pos: Vector2) extends WithVec2(pos), Component
  object Position extends ComponentType

  @component
  class Velocity(vel: Vector2) extends WithVec2(vel), Component
  object Velocity extends ComponentType

  @component
  case class Colorful(c: Color) extends Component
  object Colorful extends ComponentType

  @component
  class Timer(val intervalSeconds: Float) extends Component:
    private var currentSeconds = 0f

    inline def tick(delta: Float): Unit = currentSeconds += delta
    inline def reset(): Unit = currentSeconds = 0f
    inline def isReady: Boolean = currentSeconds >= intervalSeconds

  object Timer extends ComponentType

  object intentions:
    @component
    class StopMovementIntention(val originalVelocity: Velocity) extends Component
    object StopMovementIntention extends ComponentType

    @component
    class ResumeMovementIntention(val originalVelocity: Velocity) extends Component
    object ResumeMovementIntention extends ComponentType

    @component
    class ChangeVelocityIntention extends Component
    object ChangeVelocityIntention extends ComponentType

    @component
    class ChangeColorIntention extends Component
    object ChangeColorIntention extends ComponentType

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
