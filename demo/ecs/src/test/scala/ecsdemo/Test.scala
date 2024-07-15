package ecsdemo

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

import ecsdemo.model.*
import ecsdemo.components.*
import ecsdemo.view.View

import demoutil.transform.Vector2
import demoutil.Color

import ecscalibur.core.*

import ecsutil.shouldNotBeExecuted

inline val singleValidator = "singleValidator"
inline val validatorAdd = "validatorAdd"
inline val validatorRemove = "validatorRemove"

val producersIterations = 2.times
val consumersIterations = 3.times

class FramePacerTest extends AnyFlatSpec with should.Matchers:
  "StopSystem" should "work correctly" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world.entity withComponents (StopMovementIntention(Velocity(Vector2.zero)) :: fixture.baseComponents)
    world system StopSystem()

    world.system(validatorAdd):
      query all: (_: Entity, _: StoppedEvent, _: ResumeMovementIntention) =>
        fixture.markAsSuccessfullyAdded()
    world.system(validatorRemove):
      query none StopMovementIntention all: _ =>
        fixture.markAsSuccessfullyRemoved()
    world loop producersIterations
    fixture.wasTestSuccessful should be(true)

  "ResumeSystem" should "work correctly" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world.entity withComponents (ResumeMovementIntention(Velocity(Vector2.zero)) :: fixture.baseComponents) 
    world system ResumeSystem()

    world.system(validatorAdd):
      query all: (_: Entity, _: ResumedMovementEvent, _: StopMovementIntention) =>
        fixture.markAsSuccessfullyAdded()
    world.system(validatorRemove):
      query none ResumeMovementIntention all: _ =>
        fixture.markAsSuccessfullyRemoved()
    world loop producersIterations
    fixture.wasTestSuccessful should be(true)

  "ChangeVelocitySystem" should "work correctly" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world.entity withComponents (ChangeVelocityIntention() :: fixture.baseComponents)
    world system ChangeVelocitySystem()

    world.system(validatorAdd):
      query all: (_: Entity, _: ChangedVelocityEvent) =>
        fixture.markAsSuccessfullyAdded()
    world loop producersIterations
    fixture.markAsSuccessfullyRemoved()
    fixture.wasTestSuccessful should be(true)

  "ChangeColorSystem" should "work correctly" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world.entity withComponents (Colorful(Color.White) :: ChangeColorIntention() :: fixture.baseComponents)
    world system ChangeColorSystem()

    world.system(validatorAdd):
      query all: (_: Entity, _: ChangedColorEvent) =>
        fixture.markAsSuccessfullyAdded()
    world loop producersIterations
    fixture.markAsSuccessfullyRemoved()
    fixture.wasTestSuccessful should be(true)

  import ecsdemo.controller.*

  "StopSystem and RemoveSystem" should "work together successfully" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world.entity withComponents (StopMovementIntention(Velocity(Vector2.zero)) :: fixture.baseComponents)
    world system StopSystem()
    world system ResumeSystem()

    world.system(singleValidator):
      query any (StoppedEvent, ResumedMovementEvent) all: (e: Entity) =>
        if e ?> StoppedEvent then fixture.markAsSuccessfullyAdded()
        if e ?> ResumedMovementEvent then fixture.markAsSuccessfullyRemoved()
    world loop consumersIterations
    fixture.wasTestSuccessful should be(true)

  "ConsumeParameterlessEventsSystem" should "work correctly" in:
    val fixture = Fixture()
    given world: World = fixture.world
    given View = fixture.view

    world.entity withComponents (StopMovementIntention(Velocity(Vector2.zero)) :: fixture.baseComponents)
    world system StopSystem()
    world system ResumeSystem()
    world system ConsumeParameterlessEventsSystem()
    world.system(singleValidator):
      query all: (_, _: StoppedEvent, _: ResumedMovementEvent) =>
        shouldNotBeExecuted

    world loop consumersIterations

  def testConsumeEventSystem(
      intention: Component,
      eventType: ComponentType,
      producer: System,
      consumer: System
  )(using Fixture): Unit =
    val fixture: Fixture = summon[Fixture]
    given world: World = fixture.world

    world.entity withComponents (intention :: fixture.baseComponents)
    world system producer
    world system consumer
    world loop once

    world.mutator defer SystemRequest.pause(producer.name)
    world loop once

    world.system(singleValidator):
      query any eventType all: _ =>
        shouldNotBeExecuted
    world loop once

  "ConsumeChangedVelocityEvent" should "work correctly" in:
    given fixture: Fixture = Fixture()
    given world: World = fixture.world
    given View = fixture.view
    testConsumeEventSystem(
      intention = ChangeVelocityIntention(),
      eventType = ChangedVelocityEvent,
      producer = ChangeVelocitySystem(),
      consumer = ConsumeChangedVelocityEventSystem()
    )

  "ConsumeChangedColorEvent" should "work correctly" in:
    given fixture: Fixture = Fixture()
    given world: World = fixture.world
    given View = fixture.view
    testConsumeEventSystem(
      intention = ChangeColorIntention(),
      eventType = ChangedColorEvent,
      producer = ChangeColorSystem(),
      consumer = ConsumeChangedColorEventSystem()
    )
