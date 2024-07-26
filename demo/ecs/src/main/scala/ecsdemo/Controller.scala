package ecsdemo

import ecscalibur.core.*
import ecsdemo.components.events.*
import ecsdemo.view.View

object controller:
  trait Controller:
    infix def bindListenersTo(world: World): Unit

  object Controller:
    private[ecsdemo] inline val controllerPriority = 0

    def apply()(using View): Controller = new Controller:
      private val view = summon[View]

      override def bindListenersTo(world: World): Unit =
        world.listener("StoppedEventListener"): (e: Entity, _: StoppedEvent) =>
          view.handleStoppedEvent(e)

        world.listener("ResumedMovementEventListener"): (e: Entity, _: ResumedMovementEvent) =>
          view.handleResumedEvent(e)

        world.listener("ChangedVelocityEventListener"): (e: Entity, event: ChangedVelocityEvent) =>
          view.handleChangedVelocityEvent(e, event)

        world.listener("ChangedColorEventListener"): (e: Entity, event: ChangedColorEvent) =>
          view.handleChangedColorEvent(e, event)
