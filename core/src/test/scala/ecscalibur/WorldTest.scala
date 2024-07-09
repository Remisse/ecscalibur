package ecscalibur

import ecscalibur.core.*
import ecscalibur.testutil.testclasses.C1
import ecscalibur.testutil.testclasses.Value
import ecsutil.CSeq
import ecsutil.shouldNotBeExecuted
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

import Loop.*

class WorldTest extends AnyFlatSpec with should.Matchers:
  inline val s1 = "test1"
  inline val s2 = "test2"

  "A World" should "correctly add simple systems and let them execute" in:
    val world = World()
    var res = false
    world.withSystem(s1):
      _ routine: () =>
        res = true
    world loop once
    (world isSystemRunning s1) shouldBe true
    res should be(true)

  import ecscalibur.testutil.testclasses.TestSystem

  it should "correctly add instances of more complex systems" in:
    given world: World = World()
    var res = false
    val s = TestSystem(() => res = true)
    world.withSystem(s)
    world loop once
    (world isSystemRunning s.name) shouldBe true
    res should be(true)

  val testValue: Value = Value(1)

  it should "correctly create an entity with the supplied components" in:
    val world = World()
    world.entity withComponents CSeq(testValue)
    var test = Value(0)
    world.withSystem(s1):
      _ on: (e: Entity, v: Value) =>
        test = v

    world loop once
    test shouldBe testValue

  import ecscalibur.testutil.testclasses.{Vec2D, Position, Velocity}

  it should "correctly execute a classic ECS example program" in:
    given world: World = World()

    val pos = Position(Vec2D(10, 20))
    val vel = Velocity(Vec2D(1, 2))
    world.entity withComponents CSeq(pos, vel)

    world.withSystem(s1):
      _ on: (e: Entity, v: Velocity, p: Position) =>
        e <== Position(p.vec + v.vec)

    world.withSystem(s2, priority = 1):
      _ on: (_, p: Position) =>
        p.vec should be(pos.vec + vel.vec)
        ()

    world loop once

  inline val Tolerance = 1e-8

  it should "correctly report whether an entity has specific components" in:
    given world: World = World()
    world.entity withComponents CSeq(testValue)
    world.withSystem(s1):
      _ any Value on: (e: Entity) =>
        e ?> Value should be(true)
        ()

    world loop once

  it should "update its delta time value on every iteration" in:
    val world = World(frameCap = 60)
    var dt: Float = 0.0
    world.withSystem(s1):
      _ routine: () =>
        dt = world.context.deltaTime

    // TODO Find out why dt === 0.0 +- Tolerance returns false after 1 iteration
    world loop 2.times
    dt === 0.0 +- Tolerance shouldBe false
    dt should be(world.context.deltaTime)

  import ecscalibur.core.SystemRequest.*

  it should "correctly defer pausing a system" in:
    val world = World()
    var sum = 0
    world.withSystem(s1):
      _ routine: () =>
        sum += 1
        if world isSystemRunning s1 then
          val success = world.mutator defer pause(s1)
          if !success then shouldNotBeExecuted

    world loop once
    sum shouldBe 1
    world loop once
    sum shouldBe 1

  it should "correctly defer resuming a system" in:
    val world = World()
    var sum = 0

    world.withSystem(s1):
      _ routine: () =>
        sum += 1
        if world isSystemRunning s1 then
          (world.mutator defer pause(s1)) shouldNot be(false)
          ()

    world.withSystem(s2):
      _ routine: () =>
        if world isSystemPaused s1 then
          (world.mutator defer resume(s1)) shouldNot be(false)
          ()

    world loop 2.times
    sum shouldBe 1
    world loop once
    sum shouldBe 2

  import ecscalibur.core.EntityRequest.*

  it should "correctly defer creating a new entity" in:
    val world = World()
    world.withSystem(s1, priority = 0):
      _ routine: () =>
        world.mutator defer create(CSeq(testValue))
        world.mutator defer pause(s1)
        ()

    var test = Value(0)
    world.withSystem(s2, priority = 1):
      _ on: (_, v: Value) =>
        test = v

    world loop once
    test shouldNot be(testValue)
    world loop once
    test shouldBe testValue

  it should "correctly defer deleting an entity" in:
    val world = World()
    world.entity withComponents CSeq(C1())
    var sum = 0
    world.withSystem(s1):
      _ on: (e: Entity, _: C1) =>
        world.mutator defer delete(e)
        sum += 1
    world.withSystem(s2):
      _ on: (_, _: C1) =>
        sum += 1

    world loop 2.times
    sum shouldBe 2

  it should "correctly defer adding components to an entity" in:
    given world: World = World()
    world.entity withComponents CSeq(C1())
    world.withSystem(s1):
      _ on: (e: Entity, _: C1) =>
        e += (testValue, () => shouldNotBeExecuted)
        // world.mutator defer addComponent(e, testValue, () => shouldNotBeExecuted)
        val _ = world.mutator defer pause(s1)

    var test = Value(0)
    world.withSystem(s2):
      _ on: (_, v: Value) =>
        test = v

    world loop once
    test shouldNot be(testValue)
    world loop once
    test shouldBe testValue

  it should "correctly defer removing components from an entity" in:
    given world: World = World()
    world.entity withComponents CSeq(C1())

    world.withSystem(s1):
      _ on: (e: Entity, _: C1) =>
        e -= (C1, () => shouldNotBeExecuted)
        // world.mutator defer removeComponent(e, C1, () => shouldNotBeExecuted)
        val _ = world.mutator defer pause(s1)

    var sum = 0
    world.withSystem(s2):
      _ on: (e: Entity, _: C1) =>
        sum += 1

    world loop once
    sum shouldBe 1
    world loop once
    sum shouldBe 1

  import ecscalibur.testutil.testclasses.C2

  inline val defersCount = 10
  val deferTestIterations: Loop = 10.times

  it should "only execute the first of many 'addComponent' requests if they do the same thing" in:
    val world = World()
    world.entity withComponents CSeq(C1())
    world.withSystem(s1):
      _ on: (e: Entity, _: C1) =>
        for _ <- 0 until defersCount do world.mutator defer addComponent(e, C2())

    noException shouldBe thrownBy(world loop deferTestIterations)

  it should "only execute the first of many 'removeComponent' requests if they do the same thing" in:
    val world = World()
    world.entity withComponents CSeq(C1())
    world.withSystem(s1):
      _ on: (e: Entity, _: C1) =>
        for _ <- 0 until defersCount do world.mutator defer removeComponent(e, C1)

    noException shouldBe thrownBy(world loop deferTestIterations)

  it should "only execute the first of many 'delete' requests if they refer to the same Entity" in:
    val world = World()
    world.entity withComponents CSeq(C1())
    world.withSystem(s1):
      _ on: (e: Entity, _: C1) =>
        for _ <- 0 until defersCount do world.mutator defer delete(e)

    noException shouldBe thrownBy(world loop deferTestIterations)

  inline val none = ""
  inline val s3 = "test3"

  it should "execute its systems sorted by priority" in:
    val world = World()
    var systemName = none

    def test(thisName: String, prevName: String): Unit =
      systemName shouldBe prevName
      systemName = thisName

    world.withSystem(s2, priority = 2):
      _ routine: () =>
        test(thisName = s2, prevName = s1)
    world.withSystem(s1, priority = 1):
      _ routine: () =>
        test(thisName = s1, prevName = none)
    world.withSystem(s3, priority = 3):
      _ routine: () =>
        test(thisName = s3, prevName = s2)

    world loop once
    systemName shouldBe s3

  it should "not contain two systems with the same name" in:
    val world = World()
    world.withSystem(s1):
      _ routine: () =>
        ()
    an[IllegalArgumentException] shouldBe thrownBy:
      world.withSystem(s1):
        _ routine: () =>
          ()
