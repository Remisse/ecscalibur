package oopdemo

import oopdemo.objects.*
import oopdemo.model.Model
import oopdemo.controller.Controller

@main def main(): Unit =
  val model = Model(interval = 2f)
  val objects = model.objects
  val view: View = View.terminal()
  val controller = Controller(iterationsPerSecond = 60)
  for o <- objects do view.bind(o)
  controller.loop: deltaTime =>
    given DeltaTime = deltaTime
    view.onUpdate
    for o <- objects do o.onUpdate
