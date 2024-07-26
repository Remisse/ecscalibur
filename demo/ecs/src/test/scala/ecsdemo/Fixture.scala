package ecsdemo

import ecscalibur.core.*
import ecsdemo.components.*
import demoutil.transform.Vector2
import ecsdemo.view.View

final class Fixture(val extraComponents: Component*):
  val world: World = World()
  val view: View = View.terminal()
  val baseComponents: List[Component] =
    List(Position(Vector2.zero), Velocity(Vector2(1, 1)), ecsdemo.components.Timer(0f)) ++: extraComponents.toList
  private var hasAddedAll = false
  private var hasRemovedAll = false

  def markAsSuccessfullyAdded(): Unit = hasAddedAll = true
  def markAsSuccessfullyRemoved(): Unit = hasRemovedAll = true

  def wasTestSuccessful: Boolean = hasAddedAll && hasRemovedAll
