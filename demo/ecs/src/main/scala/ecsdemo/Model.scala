package ecsdemo

import ecscalibur.core.*
import ecsdemo.components.*
import demoutil.transform.Vector2
import demoutil.Color

object model:
  trait Model extends SystemHolder:
    infix def bindEntitiesTo(world: World): Unit

  object Model:
    private[ecsdemo] inline val modelPriority = 1

    def apply(interval: Float)(using World): Model = new Model:
      override def bindEntitiesTo(world: World): Unit =
        val originalVelocity = Velocity(Vector2(5, 0))
        world += (
          Position(Vector2(-50, 0)),
          originalVelocity,
          StopMovementIntention(originalVelocity),
          Timer(interval)
        )

        world += (
          Position(Vector2(50, 0)),
          Velocity(Vector2(-5, 0)),
          ChangeVelocityIntention(),
          Timer(interval)
        )

        world += (
          Position(Vector2(0, -50)),
          Velocity(Vector2(0, 7.5)),
          Colorful(Color.White),
          ChangeColorIntention(),
          Timer(interval)
        )

      override def bindSystemsTo(world: World): Unit =
        for s <- Seq(
            MovementSystem(modelPriority),
            StopSystem(modelPriority),
            ResumeSystem(modelPriority),
            ChangeVelocitySystem(modelPriority),
            ChangeColorSystem(modelPriority),
            UpdateTimerSystem(modelPriority)
          )
        do world.system(s)

  private[ecsdemo] final class MovementSystem(priority: Int)(using world: World)
      extends System("movement", priority):
    override protected val process: Query =
      query all: (e: Entity, p: Position, v: Velocity) =>
        p += v.vec * world.context.deltaTime
        ()

  private[ecsdemo] final class UpdateTimerSystem(priority: Int)(using world: World) 
      extends System("updateTimer", priority):
    override protected val process: Query =
      query all: (e: Entity, t: Timer) =>
        if t.isReady then t.reset()
        else t.tick(world.context.deltaTime)
        ()

  private inline def timed(t: Timer)(inline f: => Unit): Unit =
    if t.isReady then f

  private[ecsdemo] final class StopSystem(priority: Int)(using World) 
      extends System("stop", priority):
    override protected val process: Query =
      query none ResumeMovementIntention all:
        (e: Entity, v: Velocity, w: StopMovementIntention, t: Timer) =>
          timed(t):
            v.vec = Vector2.zero
            e -= StopMovementIntention
              += ResumeMovementIntention(w.originalVelocity)
            e >> StoppedEvent()
            ()

  private[ecsdemo] final class ResumeSystem(priority: Int)(using World) 
      extends System("resume", priority):
    override protected val process: Query =
      query none StopMovementIntention any Velocity all:
        (e: Entity, v: Velocity, w: ResumeMovementIntention, t: Timer) =>
          timed(t):
            v.vec = w.originalVelocity.vec
            e -= ResumeMovementIntention
              += StopMovementIntention(w.originalVelocity)
            e >> ResumedMovementEvent()
            ()

  private[ecsdemo] final class ChangeVelocitySystem(priority: Int)(using World)
      extends System("changeVelocity", priority):
    override protected val process: Query =
      query any ChangeVelocityIntention all: (e: Entity, v: Velocity, t: Timer) =>
        timed(t):
          v.vec = v.vec.opposite
          e >> ChangedVelocityEvent(Velocity(v.vec))
          ()

  private[ecsdemo] final class ChangeColorSystem(priority: Int)(using World)
      extends System("changeColor", priority):
    import scala.util.Random

    override protected val process: Query =
      query any ChangeColorIntention all: (e: Entity, c: Colorful, t: Timer) =>
        timed(t):
          val newColor = Color.random
          e <== Colorful(newColor)
          e >> ChangedColorEvent(newColor)
          ()
