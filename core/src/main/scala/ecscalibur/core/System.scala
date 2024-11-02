package ecscalibur.core

export systems.*

object systems:
  /** Systems contain logic that is executed every World loop.
    *
    * @param name
    *   a unique name identifying this System
    */
  trait System(final val name: String):
    /** Logic executed once when the system starts and once every time it resumes after being
      * paused.
      */
    protected val onStart: Query = Query.none

    /** Logic executed every World loop. Override by calling the [[query]] factory method.
      */
    protected val process: Query

    /** Logic executed once whenever the system pauses.
      */
    protected val onPause: Query = Query.none

    private var _status = Status.Starting

    /** Called every loop by a World to let this System update its state.
      */
    private[ecscalibur] final inline def update(): Unit = _status match
      case Status.Starting =>
        onStart()
        _status = Status.Running
        process()
      case Status.Running => process()
      case Status.Pausing =>
        onPause()
        _status = Status.Paused
      case Status.Paused => ()

    /** Pauses this System or throws if called while the System is not running. Also causes
      * [[System.onPause]] to execute.
      */
    private[ecscalibur] final inline def pause(): Unit = _status match
      case Status.Running => _status = Status.Pausing
      case _ =>
        throw IllegalStateException(s"A System may only pause while Running ($name was $_status)")

    /** Resumes this System or throws if called while the System is not paused. Also causes
      * [[System.onResume]] to execute.
      */
    private[ecscalibur] final inline def resume(): Unit = _status match
      case Status.Paused => _status = Status.Starting
      case _ =>
        throw IllegalStateException(
          s"A System may only resume its execution when Paused ($name was $_status)."
        )

    /** Checks whether this System is running.
      *
      * @return
      *   true if this system is running, false otherwise.
      */
    final inline def isRunning: Boolean = _status match
      case Status.Running => true
      case _              => false

    /** Checks whether this System is paused
      *
      * @return
      *   true if this system is paused, false otherwise.
      */
    final inline def isPaused: Boolean = _status match
      case Status.Paused => true
      case _             => false

    final override def equals(that: Any): Boolean = that match
      case s: System => name == s.name
      case _         => false

    final override def hashCode(): Int = name.##

    private enum Status:
      case Starting
      case Running
      case Pausing
      case Paused
