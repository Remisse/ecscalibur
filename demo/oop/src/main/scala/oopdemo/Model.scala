package oopdemo

import oopdemo.objects.*
import scala.util.Random
import oopdemo.extenders.Extender
import demoutil.transform.Vector2
import demoutil.Color

object model:
  trait Model:
    def objects: Seq[SceneObject]

  object Model:
    def apply(interval: Float): Model = new Model:
      override def objects: Seq[SceneObject] = 
        Seq(
          ObjectWithStop(
            pos = Vector2(50, 0),
            vel = Vector2(-5, 0),
            intervalSeconds = interval
          ),
          ObjectWithChangingVelocity(
            pos = Vector2(-50, 0),
            vel = Vector2(5, 0),
            intervalSeconds = interval
          ),
          ObjectWithChangingColor(
            pos = Vector2(0, 100),
            vel = Vector2(0, -7.5),
            intervalSeconds = interval,
            Color.Red
          )
        )
  
  trait MovingObject(pos: Vector2, vel: Vector2) extends SceneObject:
    position = pos
    val movement: MovementExtension = MovementExtension(this)
    movement.velocity = vel

  final class ObjectWithStop(pos: Vector2, vel: Vector2, intervalSeconds: Float)
      extends MovingObject(pos, vel)
      with SceneObject("ObjectWithStop"):
    override val repeatedAction: RepeatedAction = new RepeatedAction(intervalSeconds) with Extender(this):
      override def execute(): Unit =
        if movement.velocity.magnitude > 0f then movement.velocity = Vector2.zero
        else movement.velocity = vel

      override def makeEvent(): Event =
        if movement.velocity.magnitude > 0f then Events.ResumeMovement(owner)
        else Events.Stop(owner)

  final class ObjectWithChangingVelocity(pos: Vector2, vel: Vector2, intervalSeconds: Float)
      extends MovingObject(pos, vel)
      with SceneObject("ObjectWithChangingVelocity"):
    override val repeatedAction: RepeatedAction = new RepeatedAction(intervalSeconds) with Extender(this):
      override def execute(): Unit =
        movement.velocity = movement.velocity.opposite

      override def makeEvent(): Event =
        Events.VelocityChange(owner, movement.velocity)

  final class ObjectWithChangingColor(
      pos: Vector2,
      vel: Vector2,
      intervalSeconds: Float,
      color: Color
  ) extends MovingObject(pos, vel)
      with SceneObject("ObjectWithChangingColor"):
    private val colorExtension = Colorful(this, color)
    override val repeatedAction: RepeatedAction = new RepeatedAction(intervalSeconds) with Extender(this):
      override def execute(): Unit =
        colorExtension.color = Color.random
        movement.velocity = movement.velocity.opposite

      override def makeEvent(): Event =
        Events.ColorChange(owner, colorExtension.color)

  enum Events extends Event:
    case Stop(source: SceneObject)
    case ResumeMovement(source: SceneObject)
    case VelocityChange(source: SceneObject, newVelocity: Vector2)
    case ColorChange(source: SceneObject, newColor: Color)
