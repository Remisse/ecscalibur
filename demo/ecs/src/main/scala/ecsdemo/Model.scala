package ecsdemo

import ecscalibur.core.*
import ecsdemo.components.*
import demoutil.transform.Vector2
import demoutil.Color

object model:
  trait Model extends SystemHolder:
    infix def bindEntitiesTo(world: World): Unit

  object Model:
    def apply()(using World): Model = new Model:
      import ecsutil.CSeq

      override def bindEntitiesTo(world: World): Unit =
        inline val interval = 2f
        val originalVelocity = Velocity(Vector2(5, 0))
        world.entity withComponents CSeq(
          Position(Vector2(-50, 0)),
          originalVelocity,
          WantsToStop(originalVelocity),
          Timer(interval)
        )

        world.entity withComponents CSeq(
          Position(Vector2(50, 0)),
          Velocity(Vector2(-5, 0)),
          WantsToChangeVelocity(),
          Timer(interval)
        )

        world.entity withComponents CSeq(
          Position(Vector2(0, -50)),
          Velocity(Vector2(0, 7.5)),
          Colorful(Color.White),
          WantsToChangeColor(),
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
        do world.withSystem(s)

  private inline val modelPriority = 1 

  private[ecsdemo] final class MovementSystem(using world: World) extends System("movement", modelPriority):
    override protected val process: Query =
      query all: (e: Entity, p: Position, v: Velocity) =>
        e <== Position(p.v + (v.v * world.context.deltaTime))
        ()

  private inline def timed(e: Entity, t: Timer)(using world: World)(f: => Unit): Unit =
    if t.currentSeconds >= t.intervalSeconds then
      e <== Timer(t.intervalSeconds)
      f
    else
      e <== Timer(t.intervalSeconds, t.currentSeconds + world.context.deltaTime)
      ()

  private[ecsdemo] final class StopSystem(using World) extends System("stop", modelPriority):
    override protected val process: Query =
      query none WantsToResume any Velocity all:
        (e: Entity, t: Timer, w: WantsToStop) =>
          timed(e, t):
            e <== Velocity(Vector2.zero)
              -= WantsToStop
              += StoppedEvent()
              += WantsToResume(w.originalVelocity)
            ()

  private[ecsdemo] final class ResumeSystem(using World) extends System("resume", modelPriority):
    override protected val process: Query =
      query none WantsToStop any Velocity all:
        (e: Entity, t: Timer, w: WantsToResume) =>
          timed(e, t):
            e <== w.originalVelocity
              -= WantsToResume
              += ResumedMovementEvent()
              += WantsToStop(w.originalVelocity)
            ()

  private[ecsdemo] final class ChangeVelocitySystem(using World) extends System("changeVelocity", modelPriority):
    override protected val process: Query =
      query any WantsToChangeVelocity all:
        (e: Entity, v: Velocity, t: Timer) =>
          timed(e, t):
            val newVelocity = Velocity(v.v.opposite)
            e <== newVelocity
              += ChangedVelocityEvent(newVelocity)
            ()

  private[ecsdemo] final class ChangeColorSystem(using World) extends System("changeColor", modelPriority):
    import scala.util.Random

    override protected val process: Query =
      query any WantsToChangeColor all:
        (e: Entity, c: Colorful, t: Timer) =>
          timed(e, t):
            val newColor = Color.random
            e <== Colorful(newColor)
              += ChangedColorEvent(newColor)
            ()
