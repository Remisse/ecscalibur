# ecscalibur

[![coverage](https://codecov.io/github/Remisse/ecscalibur/graph/badge.svg?token=KH1U71TV5V)](https://codecov.io/github/Remisse/ecscalibur) [![CI Status](https://github.com/Remisse/ecscalibur/actions/workflows/ci.yaml/badge.svg)](https://github.com/Remisse/ecscalibur/actions/workflows/ci.yaml)

An archetype-based ECS framework for Scala projects.

## Getting started

1) Add this framework to your project's dependencies:  
```
TBD
```
2) Pass the `-experimental` option to the Scala compiler:
```scala
scalacOptions ++= Seq(
  "-experimental",
),
```
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

## Using the framework

If you're not sure about what the Entity Component System pattern is, you should read about it ([here](https://github.com/SanderMertens/ecs-faq) or somewhere else) before continuing on. Otherwise, the following section will just be a very confusing read.

### World

First of all, create a new World instance as a contextual parameter:
```scala
given world: World = World()
```

Optionally, you can specify the number of iterations per second (or frames per second, depending on how you want to see it) your program should run at:
```scala
World(iterationsPerSecond = 60)
```

### Components

Declaring components requires a little bit of boilerplate code:
```scala
@component
class MyComponent extends Component:
  // Your code here.
object MyComponent extends ComponentType:
  // Your code here.
```

The component class *must* extend `Component` and its companion object *must* extend `ComponentType`.  
Forgetting to annotate component classes with `@component` will result in a runtime error.

### Entities

Create new entities with the `World.entity` method:
```scala
world.entity withComponents (MyComponent(), MyComponent2())
```

All entities reside within the World instance they were created with.

### Systems

There are two ways to create a System:

1. By passing a **query** (explained in the next paragraph) to `World.system`:
```scala
world.system("my_system"):
  query all: (e: Entity, c: MyComponent) =>
    // do something with 'c'
  ()
```
2. By extending the `System` trait and overriding its methods:
```scala
class MySystem extends System("my_system", priority = 0):
  override def onStart(): Query = ...
  override def process(): Query = ...
  override def onPause(): Query = ...

// Somewhere else in your code...
world.system(MySystem())
```

### Queries

Queries allow you to iterate on a subset of all the entities by specifying which components to include and which to exclude. You can begin building a Query by calling the `query` factory method:
```scala
query none (C1, C2) any (C3, C4) all: (e: Entity, c5: C5, ...) =>
  ...
```
Let's break this down bit by bit:
- `none`: entities with **at least one** component type among those passed to `none` will be excluded from the selection
- `any`: entities with **at least one** component type among those passed to `any` will be included in the selection
  - you won't be able to read the contents of those components directly, but you can delete them with the `-=` Entity operator or replace their references with the `<==` Entity operator
- `all`: entities with **all** of the components specified as lambda parameters will be included in the selection
  - the first parameter is always the Entity itself
  - the contents of these components can be read and updated
  - this method will terminate the query building process

If you want to iterate over all entities indiscriminately:
```scala
query all: (e: Entity) =>
  ...
```

Lastly, if you want to create a system that doesn't iterate over any entities and executes once per World loop, you can create a special Query with the `routine` factory method:
```scala
routine:
  ...
```

### Mutators



### World loop

```scala
world loop once
world loop 2.times
world loop forever
```
