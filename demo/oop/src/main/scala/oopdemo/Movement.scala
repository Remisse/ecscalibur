package oopdemo

import oopdemo.extenders.Extender
import oopdemo.objects.*
import demoutil.transform.Vector2

trait MovementExtension extends Extender:
  var velocity: Vector2

object MovementExtension:
  def apply(ownerObject: SceneObject): MovementExtension = new MovementExtension with Extender(ownerObject):
    var velocity: Vector2 = Vector2.zero

    override def onUpdate(using dt: DeltaTime): Unit =
      owner.position = owner.position + (velocity * dt)
