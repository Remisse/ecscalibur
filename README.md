# ecscalibur

[![Coverage Status](https://coveralls.io/repos/github/Remisse/ecscalibur/badge.svg)](https://coveralls.io/github/Remisse/ecscalibur)  [![CI Status](https://github.com/Remisse/ecscalibur/actions/workflows/ci.yaml/badge.svg)](https://github.com/Remisse/ecscalibur/actions/workflows/ci.yaml)

An archetype-based ECS framework for Scala projects.

## Getting started

1) Add this framework to your project's dependencies:  
  ```
  TBD
  ```
2) Add the `-experimental` compiler option to your project
3) Run the following snippet to ensure everything has been set up correctly:

```scala
import ecscalibur.core.*

@main def main(): Unit =
  given world: World = World()
  world.entity withComponents (Position(12, 6), Velocity(4, 2))
  world.system("movement"):
    query all: (e: Entity, p: Position, v: Velocity) =>
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
