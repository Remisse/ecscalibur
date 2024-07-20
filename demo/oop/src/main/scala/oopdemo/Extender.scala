package oopdemo

import oopdemo.objects.SceneObject
import oopdemo.objects.Updatable

object extenders:
  trait Extender(final val owner: SceneObject) extends Updatable:
    owner.addExtender(this)
