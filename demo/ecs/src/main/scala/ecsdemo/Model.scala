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
            MovementSystem(),
            StopSystem(),
            ResumeSystem(),
            ChangeVelocitySystem(),
            ChangeColorSystem(),
            UpdateTimerSystem()
          )
        do world.system(s, modelPriority)

  private[ecsdemo] final class MovementSystem(using world: World) extends System("movement"):
    override protected val process: Query =
      query all: (e: Entity, p: Position, v: Velocity) =>
        p += v.vec * world.context.deltaTime
        ()

  private[ecsdemo] final class UpdateTimerSystem(using world: World) extends System("updateTimer"):
    override protected val process: Query =
      query all: (e: Entity, t: Timer) =>
        if t.isReady then t.reset()
        else t.tick(world.context.deltaTime)
        ()

  private inline def whenReady(t: Timer)(inline f: => Unit): Unit =
    if t.isReady then f

  private[ecsdemo] final class StopSystem(using World) extends System("stop"):
    override protected val process: Query =
      query none ResumeMovementIntention all:
        (e: Entity, v: Velocity, w: StopMovementIntention, t: Timer) =>
          whenReady(t):
            v.vec = Vector2.zero
            e -= StopMovementIntention
              += ResumeMovementIntention(w.originalVelocity)
            e >> StoppedEvent()
            ()

  private[ecsdemo] final class ResumeSystem(using World) extends System("resume"):
    override protected val process: Query =
      query none StopMovementIntention any Velocity all:
        (e: Entity, v: Velocity, w: ResumeMovementIntention, t: Timer) =>
          whenReady(t):
            v.vec = w.originalVelocity.vec
            e -= ResumeMovementIntention
              += StopMovementIntention(w.originalVelocity)
            e >> ResumedMovementEvent()
            ()

  private[ecsdemo] final class ChangeVelocitySystem(using World) extends System("changeVelocity"):
    override protected val process: Query =
      query any ChangeVelocityIntention all: (e: Entity, v: Velocity, t: Timer) =>
        whenReady(t):
          v.vec = v.vec.opposite
          e >> ChangedVelocityEvent(Velocity(v.vec))
          ()

  private[ecsdemo] final class ChangeColorSystem(using World) extends System("changeColor"):
    import scala.util.Random

    override protected val process: Query =
      query any ChangeColorIntention all: (e: Entity, c: Colorful, t: Timer) =>
        whenReady(t):
          var newColor = c.c
          while newColor == c.c do
            newColor = Color.random
          e <== Colorful(newColor)
          e >> ChangedColorEvent(newColor)
          ()
