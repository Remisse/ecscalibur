package ecsdemo

import ecscalibur.core.*
import ecscalibur.core.world.Loop.forever

import ecsdemo.model.Model
import ecsdemo.controller.Controller
import ecsdemo.view.View

@main def main(): Unit =
  given world: World = World(iterationsPerSecond = 60)
  val model = Model(interval = 2f)
  model bindEntitiesTo world
  model bindSystemsTo world
  given view: View = View.terminal()
  Controller() bindSystemsTo world
  world loop forever
