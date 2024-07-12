package oopdemo

import oopdemo.objects.SceneObject
import oopdemo.objects.Updatable

object extenders:
  trait Extender(val owner: SceneObject) extends Updatable:
    owner.addExtender(this)
