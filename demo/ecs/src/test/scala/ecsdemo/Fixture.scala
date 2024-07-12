package ecsdemo

import ecscalibur.core.world.World
import ecsutil.CSeq
import ecsdemo.components.*
import demoutil.transform.Vector2
import ecsdemo.view.View

final class Fixture:
  val world = World()
  val view = View.terminal()
  val baseComponents =
    CSeq(Position(Vector2.zero), Velocity(Vector2.zero), ecsdemo.components.Timer(0, 0))
  private var hasAddedAll = false
  private var hasRemovedAll = false

  def markAsSuccessfullyAdded(): Unit = hasAddedAll = true
  def markAsSuccessfullyRemoved(): Unit = hasRemovedAll = true

  def wasTestSuccessful: Boolean = hasAddedAll && hasRemovedAll 