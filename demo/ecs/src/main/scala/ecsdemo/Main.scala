package ecsdemo

import ecscalibur.core.*

import ecsdemo.model.Model
import ecsdemo.controller.Controller
import ecsdemo.view.View

@main def main(): Unit =
  given world: World = World(iterationsPerSecond = 60)
  val model = Model(interval = 1f)
  model bindEntitiesTo world
  model bindSystemsTo world
  given view: View = View.terminal()
  Controller() bindListenersTo world
  world loop forever
