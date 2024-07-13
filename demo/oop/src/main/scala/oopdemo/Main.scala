package oopdemo

import oopdemo.objects.*
import oopdemo.model.Model
import ecsutil.FramePacer

@main def main(): Unit =
  val objects: Seq[SceneObject] = Model(interval = 2).objects
  val view: View = View.terminal()
  val pacer: FramePacer = FramePacer(cap = 60)
  for o <- objects do view.bind(o)
  var dt: DeltaTime = 0f
  while true do
    given DeltaTime = dt
    dt = pacer.pace:
      view.onUpdate
      for o <- objects do o.onUpdate
