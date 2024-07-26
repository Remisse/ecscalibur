# ecscalibur

[![coverage](https://codecov.io/github/Remisse/ecscalibur/graph/badge.svg?token=KH1U71TV5V)](https://codecov.io/github/Remisse/ecscalibur) [![CI Status](https://github.com/Remisse/ecscalibur/actions/workflows/ci.yaml/badge.svg)](https://github.com/Remisse/ecscalibur/actions/workflows/ci.yaml)

An archetype-based ECS framework for Scala projects inspired by [flecs](https://github.com/SanderMertens/flecs) and [Unity DOTS](https://unity.com/dots).

## Show me an example!

```scala 3
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

## Getting started

Assuming you're using SBT as your build system:

1) Download the latest `ecscalibur.jar` from the Releases page and move it into your project's `lib` folder (Sonatype release coming soon)
2) Pass the `-experimental` option to the Scala compiler:
```scala 3
scalacOptions ++= Seq(
  "-experimental",
)
```

## Using the framework

If you're not sure about what Entity Component System is, you should read up on it before continuing on ([here](https://github.com/SanderMertens/ecs-faq), for instance). Otherwise, the following section will just be a very confusing read.

### World

First, create a new World instance as a contextual parameter:
```scala 3
given world: World = World()
```

Optionally, you can specify the number of iterations per second your program should run at (or frames per second, depending on how you want to see them):
```scala 3
World(iterationsPerSecond = 60)
```

### Components

Declaring components requires a little bit of boilerplate code:
```scala 3
@component
class MyComponent extends Component
object MyComponent extends ComponentType
```

The component class *must* extend `Component` and its companion object *must* extend `ComponentType`.  
Forgetting to annotate component classes with `@component` will result in a runtime error.

### Entities

Create new entities with the `World.entity` method:
```scala 3
world.entity withComponents (MyComponent(), MyComponent2())
```

Entities are stored within the World instance they are created with.

### Systems

There are two ways to create a System:

1. By passing a **query** (explained further below) to `World.system`:
```scala 3
world.system("my_system", priority = 0): // Priority can be omitted, defaults to 0
  query all: (e: Entity, c: MyComponent) =>
    // do something with 'c'
    ()
```
2. By extending the `System` trait and overriding its fields:
```scala 3
// It has to have World as a contextual parameter
class MySystem(priority: Int)(using World) extends System("my_system", priority):
  override protected val onStart: Query = ???
  override protected val process: Query = ???
  override protected val onPause: Query = ???

// Somewhere else in your code...
world.system(MySystem())
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
  - you won't be able to read the contents of those components directly, but you can delete them with the `-=` Entity operator or replace their references with the `<==` Entity operator
- `all`: entities with **all** the components specified as lambda parameters will be included in the selection
  - the first parameter is always the Entity itself
  - the contents of these components can be read and updated
  - this method will terminate the query building process

If you want to iterate over all entities indiscriminately:
```scala 3
query all: (e: Entity) =>
  ()
```

Lastly, if you want to create a system that doesn't iterate over any entities and executes once per World loop, you can create a special Query with the `routine` factory method:
```scala 3
routine:
  ()
```

### Mutator

The `Mutator` instance accessible through `World` allows you to schedule modifications to entities for processing at the start of the next world loop:

```scala 3
val mutator: Mutator = world.mutator

mutator defer DeferredRequest.createEntity(C1(), C2()) // Creates an entity with the specified components
mutator defer DeferredRequest.deleteEntity(e)          // Deletes an entity
// Both options are equivalent
mutator defer DeferredRequest.addComponent(e, C3())  // Adds a component to an entity
e += C3()
// Both options are equivalent
mutator defer DeferredRequest.removeComponent(e, C3) // Removes a component from  an entity
e -= C3
```

Requests to update the reference to an Entity's component are, instead, processed immediately:

```scala 3
// Both options are equivalent
mutator doImmediately ImmediateRequest.update(e, C1())
e <== C1()
// Both options are equivalent
world.hasComponents(e, C1, C2) // Does the entity have these components?
e ?> (C1, C2)
```

It also allows you to modify the state of a System:

```scala 3
if world.isSystemRunning("my_system") then
  mutator doImmediately ImmediateRequest.pause("my_system")  // Pauses a system
if world.isSystemPaused("my_system") then
  mutator doImmediately ImmediateRequest.resume("my_system") // Resumes a system
```

If the above `DeferredRequest` operations were to be executed instantly, they would affect performance quite heavily because of the archetype-based nature of this framework.

For instance, adding one single component to an entity would result in that entity being removed from the archetype it is stored in and moved to another. If the destination archetype does not exist, it has
to be created on the spot along with all of its internal data structures.  
This also means that adding multiple components to the same entity during the same world loop could lead to a lot of unnecessary back-and-forth between archetypes.

That is why all operations which would cause similar structural changes are batched, combined and then executed on the next world loop.

### Events

Declare new event classes similarly to how you would declare components:

```scala 3
@component
class MyEvent extends Event
object MyEvent extends EventType
```

You can easily emit events via the following syntax:

```scala 3
// From within a Query
entity >> MyEvent()
```

Subscribe to specific event types or unsubscribe from them:

```scala 3
world.listener("myListener"): (e: Entity, event: MyEvent) =>
  ()

world.eventBus unsubscribe ("myListener", MyEvent)
```

When adding listeners from within a System, it's good practice to remove them when the system gets paused:

```scala 3
private val listenerName = "myListener"
override protected val onStart() =
  routine:
    summon[World].listener(listenerName): (e: Entity, event: MyEvent) =>
      ()
override protected val onPause() =
  routine:
    summon[World].eventBus unsubscribe(listenerName, MyEvent)
    ()
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
