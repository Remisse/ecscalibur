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

import ecsutil.CSeq
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

    world.entity withComponents (CSeq[Component](
      WantsToStop(Velocity(Vector2.zero))
    ) concat fixture.baseComponents)
    world withSystem StopSystem()

    world.withSystem(validatorAdd):
      _ all: (_: Entity, _: StoppedEvent, _: WantsToResume) =>
        fixture.markAsSuccessfullyAdded()
    world.withSystem(validatorRemove):
      _ none WantsToStop all: _ =>
        fixture.markAsSuccessfullyRemoved()
    world loop producersIterations
    fixture.wasTestSuccessful should be(true)

  "ResumeSystem" should "work correctly" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world.entity withComponents (CSeq[Component](
      WantsToResume(Velocity(Vector2.zero))
    ) concat fixture.baseComponents)
    world withSystem ResumeSystem()

    world.withSystem(validatorAdd):
      _ all: (_: Entity, _: ResumedMovementEvent, _: WantsToStop) =>
        fixture.markAsSuccessfullyAdded()
    world.withSystem(validatorRemove):
      _ none WantsToResume all: _ =>
        fixture.markAsSuccessfullyRemoved()
    world loop producersIterations
    fixture.wasTestSuccessful should be(true)

  "ChangeVelocitySystem" should "work correctly" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world.entity withComponents (CSeq[Component](
      WantsToChangeVelocity()
    ) concat fixture.baseComponents)
    world withSystem ChangeVelocitySystem()

    world.withSystem(validatorAdd):
      _ all: (_: Entity, _: ChangedVelocityEvent) =>
        fixture.markAsSuccessfullyAdded()
    world loop producersIterations
    fixture.markAsSuccessfullyRemoved()
    fixture.wasTestSuccessful should be(true)

  "ChangeColorSystem" should "work correctly" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world.entity withComponents (CSeq[Component](
      Colorful(Color.White),
      WantsToChangeColor()
    ) concat fixture.baseComponents)
    world withSystem ChangeColorSystem()

    world.withSystem(validatorAdd):
      _ all: (_: Entity, _: ChangedColorEvent) =>
        fixture.markAsSuccessfullyAdded()
    world loop producersIterations
    fixture.markAsSuccessfullyRemoved()
    fixture.wasTestSuccessful should be(true)

  import ecsdemo.controller.*

  "StopSystem and RemoveSystem" should "work together successfully" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world.entity withComponents (CSeq[Component](
      WantsToStop(Velocity(Vector2.zero))
    ) concat fixture.baseComponents)
    world withSystem StopSystem()
    world withSystem ResumeSystem()

    world.withSystem(singleValidator):
      _ any (StoppedEvent, ResumedMovementEvent) all: (e: Entity) =>
        if e ?> StoppedEvent then fixture.markAsSuccessfullyAdded()
        if e ?> ResumedMovementEvent then fixture.markAsSuccessfullyRemoved()
    world loop consumersIterations
    fixture.wasTestSuccessful should be(true)

  "ConsumeParameterlessEventsSystem" should "work correctly" in:
    val fixture = Fixture()
    given world: World = fixture.world
    given View = fixture.view

    world.entity withComponents (CSeq[Component](
      WantsToStop(Velocity(Vector2.zero))
    ) concat fixture.baseComponents)
    world withSystem StopSystem()
    world withSystem ResumeSystem()
    world withSystem ConsumeParameterlessEventsSystem()
    world.withSystem(singleValidator):
      _ all: (_, _: StoppedEvent, _: ResumedMovementEvent) =>
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

    world.entity withComponents (CSeq[Component](intention) concat fixture.baseComponents)
    world withSystem producer
    world withSystem consumer
    world loop once

    world.mutator defer SystemRequest.pause(producer.name)
    world loop once

    world.withSystem(singleValidator):
      _ any eventType all: _ =>
        shouldNotBeExecuted
    world loop once

  "ConsumeChangedVelocityEvent" should "work correctly" in:
    given fixture: Fixture = Fixture()
    given world: World = fixture.world
    given View = fixture.view
    testConsumeEventSystem(
      intention = WantsToChangeVelocity(),
      eventType = ChangedVelocityEvent,
      producer = ChangeVelocitySystem(),
      consumer = ConsumeChangedVelocityEventSystem()
    )

  "ConsumeChangedColorEvent" should "work correctly" in:
    given fixture: Fixture = Fixture()
    given world: World = fixture.world
    given View = fixture.view
    testConsumeEventSystem(
      intention = WantsToChangeColor(),
      eventType = ChangedColorEvent,
      producer = ChangeColorSystem(),
      consumer = ConsumeChangedColorEventSystem()
    )
