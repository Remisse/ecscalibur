package ecsdemo

import ecscalibur.core.*
import ecsdemo.components.events.*
import ecsdemo.view.View

object controller:
  trait Controller extends SystemHolder

  object Controller:
    def apply()(using View): Controller = (world: World) =>
      given World = world

      for s <- Seq(
        ConsumeParameterlessEventsSystem(),
        ConsumeChangedVelocityEventSystem(),
        ConsumeChangedColorEventSystem()
      )
      do world.system(s)

  private inline val controllerPriority = 0

  private[ecsdemo] final class ConsumeParameterlessEventsSystem(using World, View)
      extends System("viewNoParameters", controllerPriority):
    override protected val process: Query =
      query any (StoppedEvent, ResumedMovementEvent) all: (e: Entity) =>
        if e ?> StoppedEvent then
          e -= StoppedEvent
          summon[View].handleStoppedEvent(e)
        else if e ?> ResumedMovementEvent then
          e -= ResumedMovementEvent
          summon[View].handleResumedEvent(e)

  private[ecsdemo] final class ConsumeChangedVelocityEventSystem(using World, View)
      extends System("viewChangedVelocity", controllerPriority):
    override protected val process: Query =
      query all: (e: Entity, event: ChangedVelocityEvent) =>
        e -= ChangedVelocityEvent
        summon[View].handleChangedVelocityEvent(e, event)
        ()

  private[ecsdemo] final class ConsumeChangedColorEventSystem(using World, View)
      extends System("viewChangedColor", controllerPriority):
    override protected val process: Query =
      query all: (e: Entity, event: ChangedColorEvent) =>
        e -= ChangedColorEvent
        summon[View].handleChangedColorEvent(e, event)
        ()
