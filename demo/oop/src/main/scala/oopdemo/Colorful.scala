package oopdemo

import oopdemo.extenders.Extender
import oopdemo.objects.SceneObject
import oopdemo.objects.DeltaTime
import demoutil.Color

trait Colorful(var color: Color) extends Extender

object Colorful:
  def apply(owner: SceneObject, color: Color): Colorful = new Colorful(color) with Extender(owner):
    override def onUpdate(using DeltaTime): Unit = ()