package ecsdemo

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import ecsdemo.model.*
import ecsdemo.components.*
import demoutil.transform.Vector2
import demoutil.Color
import ecscalibur.core.*

import scala.reflect.ClassTag

inline val singleValidator = "singleValidator"
inline val validatorAdd = "validatorAdd"
inline val validatorRemove = "validatorRemove"

val producersIterations = 2.times
val consumersIterations = 3.times

inline val controllerPriority = controller.Controller.controllerPriority
inline val modelPriority = model.Model.modelPriority

class DemoTest extends AnyFlatSpec with should.Matchers:
  "StopSystem" should "work correctly" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world += (StopMovementIntention(
      Velocity(Vector2.zero)
    ) :: fixture.baseComponents)
    world.system(StopSystem(), modelPriority)

    world.system(validatorAdd):
      query all: (_: Entity, _: ResumeMovementIntention) =>
        fixture.markAsSuccessfullyAdded()
    world.system(validatorRemove):
      query none StopMovementIntention all: _ =>
        fixture.markAsSuccessfullyRemoved()
    world loop producersIterations
    fixture.wasTestSuccessful should be(true)

  "ResumeSystem" should "work correctly" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world += (ResumeMovementIntention(
      Velocity(Vector2.zero)
    ) :: fixture.baseComponents)
    world.system(ResumeSystem(), modelPriority)

    world.system(validatorAdd):
      query all: (_: Entity, _: StopMovementIntention) =>
        fixture.markAsSuccessfullyAdded()
    world.system(validatorRemove):
      query none ResumeMovementIntention all: _ =>
        fixture.markAsSuccessfullyRemoved()
    world loop producersIterations
    fixture.wasTestSuccessful should be(true)

  "ChangeVelocitySystem" should "work correctly" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world += (ChangeVelocityIntention() :: fixture.baseComponents)
    world.system(ChangeVelocitySystem(), modelPriority)

    var currentVelocity = Velocity(Vector2.zero)
    world.system(validatorAdd, modelPriority + 1):
      query all: (_: Entity, v: Velocity) =>
        currentVelocity = v
    world loop once
    val prevVelocity = Velocity(currentVelocity.vec)
    world loop once
    currentVelocity shouldNot be(prevVelocity)

  "ChangeColorSystem" should "work correctly" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world += (Colorful(
      Color.White
    ) :: ChangeColorIntention() :: fixture.baseComponents)
    world.system(ChangeColorSystem(), modelPriority)

    var currentColor = Color.Red
    world.system(validatorAdd, modelPriority + 1):
      query all: (_: Entity, c: Colorful) =>
        currentColor = c.c
    world loop once
    val prevColor = currentColor
    world loop once
    currentColor shouldNot be(prevColor)

  "StopSystem and RemoveSystem" should "work together successfully" in:
    val fixture = Fixture()
    given world: World = fixture.world

    world += (StopMovementIntention(
      Velocity(Vector2.zero)
    ) :: fixture.baseComponents)
    world.system(StopSystem(), modelPriority)
    world.system(ResumeSystem(), modelPriority)

    world.system(singleValidator):
      query all: (e: Entity) =>
        if e ?> ResumeMovementIntention then fixture.markAsSuccessfullyRemoved()
        else fixture.markAsSuccessfullyAdded()
    world loop consumersIterations
    fixture.wasTestSuccessful should be(true)

  val listenerName = "listener"

  def testConsumeEventSystem[E <: Event: ClassTag](intention: Component, producer: System)(using
      Fixture
  ): Assertion =
    val fixture: Fixture = summon[Fixture]

    given world: World = fixture.world

    world += (intention :: fixture.baseComponents)
    world.system(producer, modelPriority)
    world.subscribe(listenerName): (e: Entity, event: E) =>
      fixture.markAsSuccessfullyAdded()
      fixture.markAsSuccessfullyRemoved()
      ()
    world loop once
    fixture.wasTestSuccessful should be(true)

  "StoppedEvent" should "be emitted correctly" in:
    given fixture: Fixture = Fixture()
    given world: World = fixture.world
    testConsumeEventSystem[StoppedEvent](
      StopMovementIntention(Velocity(Vector2.zero)),
      StopSystem()
    )

  "ResumedMovementEvent" should "be emitted correctly" in:
    given fixture: Fixture = Fixture()
    given world: World = fixture.world
    testConsumeEventSystem[ResumedMovementEvent](
      ResumeMovementIntention(Velocity(Vector2.zero)),
      ResumeSystem()
    )

  "ChangedVelocityEvent" should "be emitted correctly" in:
    given fixture: Fixture = Fixture()
    given world: World = fixture.world
    testConsumeEventSystem[ChangedVelocityEvent](
      intention = ChangeVelocityIntention(),
      producer = ChangeVelocitySystem()
    )

  "ChangedColorEvent" should "be emitted correctly" in:
    given fixture: Fixture = Fixture(extraComponents = Colorful(Color.Black))
    given world: World = fixture.world
    testConsumeEventSystem[ChangedColorEvent](
      intention = ChangeColorIntention(),
      producer = ChangeColorSystem()
    )
