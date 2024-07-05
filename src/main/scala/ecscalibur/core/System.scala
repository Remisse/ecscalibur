package ecscalibur.core

import ecscalibur.core.queries.Query
import context.MetaContext
import ecscalibur.core.archetype.ArchetypeManager

object systems:
  trait System(val name: String, val priority: Int)(using ArchetypeManager, MetaContext):
    protected val onStart: () => Unit = System.EmptyLogic
    protected val process: Query
    protected val onStop: () => Unit = System.EmptyLogic

    private var _status = Status.Starting
    final inline def update() = _status match
      case Status.Starting =>
        onStart()
        _status = Status.Running
        process()
      case Status.Running => process()
      case Status.Pausing => 
        onStop()
        _status = Status.Paused
      case Status.Paused => ()

    final inline def pause() = _status match
      case Status.Running => _status = Status.Pausing
      case _ => throw IllegalStateException(s"A System may only pause while Running ($name was $_status)")
    
    final inline def resume() = _status match
      case Status.Paused => _status = Status.Starting
      case _ => throw IllegalStateException(s"A System may only resume its execution when Paused ($name was $_status).")

    final inline def isRunning: Boolean = _status match
      case Status.Running => true
      case _ => false

    final inline def isPaused: Boolean = _status match
      case Status.Paused => true
      case _ => false

    enum Status:
      case Starting
      case Running
      case Pausing
      case Paused

  object System:
    inline def EmptyLogic = () => ()
