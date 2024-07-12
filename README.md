# ecscalibur

An archetype-based ECS framework for Scala projects.

## Getting started

1) Add this framework to your project's dependencies:  
  ```
  TBD
  ```
2) Add the `-experimental` compiler option to your project
3) Run this self-contained code snippet to ensure everything has been set up correctly:

```scala
import ecscalibur.core.*
import ecsutil.CSeq

@main def main(): Unit =
  given world: World = World()
  world.entity withComponents CSeq(Position(12, 6), Velocity(4, 2))
  world.withSystem("movement"):
    _ all: (e: Entity, p: Position, v: Velocity) =>
      e <== Position(
        p.x + v.x * world.context.deltaTime,
        p.y + v.y * world.context.deltaTime,
      )
      ()
  world loop 10.times

@component
case class Position(x: Float, y: Float) extends Component
object Position extends ComponentType
@component
case class Velocity(x: Float, y: Float) extends Component
object Velocity extends ComponentType
```
