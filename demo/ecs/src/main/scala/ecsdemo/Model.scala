package ecsdemo

import ecscalibur.core.*
import ecsdemo.components.*
import demoutil.transform.Vector2
import demoutil.Color

object model:
  trait Model extends SystemHolder:
    infix def bindEntitiesTo(world: World): Unit

  object Model:
    def apply(interval: Float)(using World): Model = new Model:
      override def bindEntitiesTo(world: World): Unit =
        val originalVelocity = Velocity(Vector2(5, 0))
        world.entity withComponents (
          Position(Vector2(-50, 0)),
          originalVelocity,
          StopMovementIntention(originalVelocity),
          Timer(interval)
        )

        world.entity withComponents (
          Position(Vector2(50, 0)),
          Velocity(Vector2(-5, 0)),
          ChangeVelocityIntention(),
          Timer(interval)
        )

        world.entity withComponents (
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
            ChangeColorSystem()
          )
        do world.system(s)

  private inline val modelPriority = 1

  private[ecsdemo] final class MovementSystem(using world: World)
      extends System("movement", modelPriority):
    override protected val process: Query =
      query all: (e: Entity, p: Position, v: Velocity) =>
        p += v.vec * world.context.deltaTime
        ()

  private inline def timed(t: Timer)(using world: World)(f: => Unit): Unit =
    if t.isReady then
      t.reset(); f
    else
      t.tick(world.context.deltaTime)
      ()

  private[ecsdemo] final class StopSystem(using World) extends System("stop", modelPriority):
    override protected val process: Query =
      query none ResumeMovementIntention all:
        (e: Entity, v: Velocity, w: StopMovementIntention, t: Timer) =>
          timed(t):
            v.vec = Vector2.zero
            e -= StopMovementIntention
              += StoppedEvent()
              += ResumeMovementIntention(w.originalVelocity)
            ()

  private[ecsdemo] final class ResumeSystem(using World) extends System("resume", modelPriority):
    override protected val process: Query =
      query none StopMovementIntention any Velocity all:
        (e: Entity, v: Velocity, w: ResumeMovementIntention, t: Timer) =>
          timed(t):
            v.vec = w.originalVelocity.vec
            e -= ResumeMovementIntention
              += ResumedMovementEvent()
              += StopMovementIntention(w.originalVelocity)
            ()

  private[ecsdemo] final class ChangeVelocitySystem(using World)
      extends System("changeVelocity", modelPriority):
    override protected val process: Query =
      query any ChangeVelocityIntention all: (e: Entity, v: Velocity, t: Timer) =>
        timed(t):
          v.vec = v.vec.opposite
          e += ChangedVelocityEvent(Velocity(v.vec))
          ()

  private[ecsdemo] final class ChangeColorSystem(using World)
      extends System("changeColor", modelPriority):
    import scala.util.Random

    override protected val process: Query =
      query any ChangeColorIntention all: (e: Entity, c: Colorful, t: Timer) =>
        timed(t):
          val newColor = Color.random
          e <== Colorful(newColor)
            += ChangedColorEvent(newColor)
          ()
