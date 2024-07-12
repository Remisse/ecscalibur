package ecsdemo

import ecscalibur.core.world.World

trait SystemHolder:
  infix def bindSystemsTo(world: World): Unit
