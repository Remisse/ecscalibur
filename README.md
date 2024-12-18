# ecscalibur

[![coverage](https://codecov.io/github/Remisse/ecscalibur/graph/badge.svg?token=KH1U71TV5V)](https://codecov.io/github/Remisse/ecscalibur) [![CI Status](https://github.com/Remisse/ecscalibur/actions/workflows/ci.yaml/badge.svg)](https://github.com/Remisse/ecscalibur/actions/workflows/ci.yaml)

An archetype-based ECS framework for Scala projects focusing on ease of use and minimization of boilerplate code.

## Show me an example!

```scala 3
import ecscalibur.core.*

@main def main(): Unit =
  given world: World = World()
  world += (Position(12, 6), Velocity(4, 2))
  world.system("movement"):
    query all: (e: Entity, p: Position, v: Velocity) =>
      val dt = world.context.deltaTime
      e <== Position(
        p.x + v.x * dt,
        p.y + v.y * dt,
      )
  world loop 10.times

@component
case class Position(x: Float, y: Float) extends Component
object Position extends ComponentType
@component
case class Velocity(x: Float, y: Float) extends Component
object Velocity extends ComponentType
```

## Getting started

Assuming you're using SBT as your build system:

1) Download the latest `ecscalibur.jar` from the Releases page and move it into your project's `lib` folder (Sonatype release coming soon)
2) Pass the `-experimental` option to the Scala compiler:
```scala 3
scalacOptions ++= Seq(
  "-experimental",
)
```

## Using this framework

If you're not familiar with the Entity Component System pattern, I recommend you read up on it before continuing on ([here](https://github.com/SanderMertens/ecs-faq), for instance).

### World

First, create a new World instance as a contextual parameter:
```scala 3
given world: World = World()
```

You can optionally specify the number of iterations per second your program should run at (or frames per second, depending on how you want to see it):
```scala 3
World(iterationsPerSecond = 60)
```

### Components

Declare components like so:
```scala 3
@component
class MyComponent extends Component
object MyComponent extends ComponentType
```

Component classes *must* extend `Component` and its companion object *must* extend `ComponentType`.  
Forgetting to annotate such classes with `@component` will result in a runtime error.

### Entities

Create a new entity with the `World.+=` method or delete one with `World.-=`:
```scala 3
world += (MyComponent1(), MyComponent2())
// From within a System
world -= e
```

Entities are stored within the World instance they are created with and can be accessed through *Systems* (explained further below).

#### Useful entity methods

```scala 3
e += C2()     // Adds components to an entity
e -= C2       // Removes components from an entity
e <== C1()    // Replaces an entity's component with a new instance of the same type
e ?> (C1, C2) // Checks if an entity has the given components
```

#### Delayed execution

Do note that the execution of the aforementioned methods (except for `<==` and `?>`) will be delayed until the next world loop. The reason for this has to do with how archetypes work.  

For instance, adding one single component to an entity would result in that entity being removed from the archetype it is stored in and moved to another. If the destination archetype does not exist, it has to be created on the spot along with all of its internal data structures.  
This means that adding multiple components to the same entity during the same world loop could lead to a lot of unnecessary back-and-forth between archetypes.

### Systems

There are two ways to create a System:

1. By passing a **query** (explained further below) to `World.system`:
```scala 3
world.system("my_system", priority = 0): // Priority defaults to 0 if omitted 
  query all: (e: Entity, c: MyComponent) =>
    // do something with 'c'
    ()
```
2. By extending the `System` trait and overriding its fields:
```scala 3
// It has to have World as a contextual parameter
class MySystem(using World) extends System("my_system"):
  // Executes the first time the system is run or whenever it is asked to resume after being paused
  override protected val onStart: Query = ???
  // Executes once per world loop
  override protected val process: Query = ??? 
  // Executes when the system is paused
  override protected val onPause: Query = ??? 

world.system(MySystem(), priority = 0) // Priority defaults to 0 if omitted
```

You can pause and resume the execution of your systems at any moment:

```scala 3
world.pause("my_system")
world.resume("my_system")
```

### Queries

Queries allow you to iterate on a subset of all the entities by specifying which components to include and which to exclude. You can begin building a Query by calling the `query` factory method:
```scala 3
query none (C1, C2) any (C3, C4) all: (e: Entity, c5: C5) =>
  ()
```
Let's break this down bit by bit:
- `none`: entities with **at least one** component type among those passed to `none` will be excluded from the selection
- `any`: entities with **at least one** component type among those passed to `any` will be included in the selection
  - you won't be able to read the contents of those components
  - however, you can delete the components using the `-=` (remove) Entity operator or replace their references with the `<==` (update) Entity operator
- `all`: entities with **all** the components specified as lambda parameters will be included in the selection
  - the first parameter is always the Entity itself
  - the contents of these components can be read and updated
  - this method will terminate the query building process

If you want to iterate over all entities indiscriminately:
```scala 3
query all: (e: Entity) =>
  ()
```

If you want a system that doesn't iterate over any entities but executes once per World loop, use the `routine` factory method:
```scala 3
routine:
  ()
```

### Events

Declare new event classes similarly to how you would declare components:

```scala 3
@component
class MyEvent extends Event
object MyEvent extends EventType
```

You can emit events via the following syntax:

```scala 3
// From within a Query
entity >> MyEvent()
```

Subscribe to specific event types or unsubscribe from them:

```scala 3
world.subscribe("myListener"): (e: Entity, event: MyEvent) =>
  ()

world.unsubscribe("myListener", MyEvent)
```

When adding listeners from within a System, it's good practice to remove them when the system pauses:

```scala 3
private val listenerName = "myListener"
override protected val onStart() =
  routine:
    summon[World].subscribe(listenerName): (e: Entity, event: MyEvent) =>
      ()
override protected val onPause() =
  routine:
    summon[World].unsubscribe(listenerName, MyEvent)
```

### Context

The world's `MetaContext` instance contains secondary data related to the world's execution. For now, it only exposes the amount of seconds
passed since the last iteration:

```scala 3
val dt: DeltaSeconds = world.context.deltaTime // it's actually a Float
```

### World loop

Execute your program's logic with `World.loop`:

```scala 3
world loop once    // Performs one iteration
world loop 2.times // Performs an arbitrary number of iterations
world loop forever // Iterates indefinitely
```
