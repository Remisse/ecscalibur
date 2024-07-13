package oopdemo

import oopdemo.objects.*
import oopdemo.model.Events

trait View extends Observer, Updatable:
  def bind(obj: SceneObject): Unit

object View:
  def empty(): View = new View:
    override def bind(obj: SceneObject): Unit = ()
    override def onUpdate(using DeltaTime): Unit = ()
    override def signal(e: Event): Unit = ()

  def terminal(): View = new View:
    var objects: List[SceneObject] = List.empty

    override def bind(obj: SceneObject): Unit =
      objects = obj :: objects
      obj.repeatedAction.addObserver(this)

    override def onUpdate(using DeltaTime): Unit =
      ()

    private inline def format(s: SceneObject, message: String): String = s"$s\t$message"

    override def signal(e: Event): Unit =
      var message = ""
      e match
        case Events.Stop(s)              => message = format(s, "has stopped")
        case Events.ResumeMovement(s)    => message = format(s, "has resumed its movement")
        case Events.VelocityChange(s, v) => message = format(s, "has changed velocity to ($v)")
        case Events.ColorChange(s, newC) => message = format(s, "has changed color to $newC")
      println(message)
