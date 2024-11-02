package ecscalibur

import ecscalibur.core.*
import ecscalibur.testutil.testclasses.C1
import ecscalibur.testutil.testclasses.Value
import ecsutil.shouldNotBeExecuted
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

import Loop.*

class WorldTest extends AnyFlatSpec with should.Matchers:
  inline val s1 = "test1"
  inline val s2 = "test2"

  "A World" should "correctly add simple systems and let them execute" in:
    given world: World = World()
    var res = false
    world.system(s1):
      routine:
        res = true
    world loop once
    (world isSystemRunning s1) shouldBe true
    res should be(true)

  import ecscalibur.testutil.testclasses.TestSystem

  it should "correctly add instances of more complex systems" in:
    given world: World = World()
    var res = false
    val s = TestSystem(() => res = true)
    world.system(s)
    world loop once
    (world isSystemRunning s.name) shouldBe true
    res should be(true)

  val testValue: Value = Value(1)

  it should "correctly create an entity with the supplied components" in:
    given world: World = World()
    world += testValue
    var test = Value(0)
    world.system(s1):
      query all: (e: Entity, v: Value) =>
        test = v

    world loop once
    test shouldBe testValue

  import ecscalibur.testutil.testclasses.{Vec2D, Position, Velocity}

  it should "correctly execute a classic ECS example program" in:
    given world: World = World()

    val pos = Position(Vec2D(10, 20))
    val vel = Velocity(Vec2D(1, 2))
    world += (pos, vel)

    world.system(s1):
      query all: (e: Entity, v: Velocity, p: Position) =>
        e <== Position(p.vec + v.vec)
        ()

    world.system(s2, priority = 1):
      query all: (_, p: Position) =>
        p.vec should be(pos.vec + vel.vec)
        ()

    world loop once

  def hasComponentsTest(action: (Entity, Seq[ComponentType]) => Boolean)(using world: World): Unit =
    world += (testValue, C1())
    world.system(s1):
      query any Value all: (e: Entity) =>
        action(e, Seq(Value, C1)) should be(true)
        ()

    world loop once

  it should "correctly report whether an entity has specific components using Entity.?>" in:
    given world: World = World()
    hasComponentsTest: (e: Entity, types: Seq[ComponentType]) =>
      e.?>(types*)

  it should "correctly report whether an entity has specific components using World.hasComponents" in:
    given world: World = World()
    hasComponentsTest: (e: Entity, types: Seq[ComponentType]) =>
      world.hasComponents(e, types*)

  inline val Tolerance = 1e-8f

  it should "update its delta time value on every iteration" in:
    inline val fps = 60
    given world: World = World(iterationsPerSecond = fps)
    world.system(s1):
      routine:
        ()

    world loop once
    world.context.deltaTime === (1f / fps) +- Tolerance shouldBe true

  it should "not accept negative delta time values" in:
    val world: World = World()
    an[IllegalArgumentException] should be thrownBy (world.context.setDeltaTime(-1f))

  import ecscalibur.core.ImmediateRequest.*

  it should "correctly defer pausing a system" in:
    given world: World = World()
    var sum = 0
    world.system(s1):
      routine:
        sum += 1
        if world isSystemRunning s1 then
          val success = world.pauseSystem(s1)
          if !success then shouldNotBeExecuted

    world loop once
    sum shouldBe 1
    world loop once
    sum shouldBe 1

  it should "correctly defer resuming a system" in:
    given world: World = World()
    var sum = 0

    world.system(s1):
      routine:
        sum += 1
        if world isSystemRunning s1 then
          (world.pauseSystem(s1)) shouldNot be(false)
          ()

    world.system(s2):
      routine:
        if world isSystemPaused s1 then
          (world.resumeSystem(s1)) shouldNot be(false)
          ()

    world loop 2.times
    sum shouldBe 1
    world loop once
    sum shouldBe 2

  import ecscalibur.core.DeferredRequest.*

  it should "correctly defer creating a new entity" in:
    given world: World = World()
    world.system(s1, priority = 0):
      routine:
        world += testValue
        world.mutator doImmediately pause(s1)
        ()

    var test = Value(0)
    world.system(s2, priority = 1):
      query all: (_, v: Value) =>
        test = v

    world loop once
    test shouldNot be(testValue)
    world loop once
    test shouldBe testValue

  it should "correctly defer deleting an entity" in:
    given world: World = World()
    world += C1()
    var sum = 0
    world.system(s1):
      query all: (e: Entity, _: C1) =>
        world -= e
        sum += 1
    world.system(s2):
      query all: (_, _: C1) =>
        sum += 1

    world loop 2.times
    sum shouldBe 2

  def addComponentTest(action: (Entity, Component) => Unit)(using world: World): Assertion =
    world += C1()
    world.system(s1):
      query all: (e: Entity, _: C1) =>
        action(e, testValue)
        val _ = world.mutator doImmediately pause(s1)

    var test = Value(0)
    world.system(s2):
      query all: (_, v: Value) =>
        test = v

    world loop once
    test shouldNot be(testValue)
    world loop once
    test shouldBe testValue

  it should "correctly defer adding components to an entity using Mutator" in:
    given world: World = World()
    addComponentTest: (e: Entity, c: Component) =>
      world.mutator defer addComponent(e, c)
      ()

  it should "correctly defer adding components to an entity using Entity.+=" in:
    given world: World = World()
    addComponentTest: (e: Entity, c: Component) =>
      e += c
      ()

  def removeComponentTest(action: (Entity, ComponentType) => Unit)(using world: World): Assertion =
    world += C1()

    world.system(s1):
      query all: (e: Entity, _: C1) =>
        action(e, C1)
        val _ = world.pauseSystem(s1)

    var sum = 0
    world.system(s2):
      query all: (e: Entity, _: C1) =>
        sum += 1

    world loop once
    sum shouldBe 1
    world loop once
    sum shouldBe 1

  it should "correctly defer removing components from an entity using Mutator" in:
    given world: World = World()
    removeComponentTest: (e: Entity, t: ComponentType) =>
      world.mutator defer removeComponent(e, t)
      ()

  it should "correctly defer removing components from an entity using Entity.-=" in:
    given world: World = World()
    removeComponentTest: (e: Entity, t: ComponentType) =>
      e -= t
      ()

  import ecscalibur.testutil.testclasses.C2

  inline val defersCount = 10
  val deferTestIterations: Loop = 10.times

  it should "only execute the first of many 'addComponent' requests if they do the same thing" in:
    given world: World = World()
    world += C1()
    world.system(s1):
      query all: (e: Entity, _: C1) =>
        for _ <- 0 until defersCount do world.mutator defer addComponent(e, C2())

    noException shouldBe thrownBy(world loop deferTestIterations)

  it should "only execute the first of many 'removeComponent' requests if they do the same thing" in:
    given world: World = World()
    world += C1()
    world.system(s1):
      query all: (e: Entity, _: C1) =>
        for _ <- 0 until defersCount do world.mutator defer removeComponent(e, C1)

    noException shouldBe thrownBy(world loop deferTestIterations)

  it should "only execute the first of many 'delete' requests if they refer to the same Entity" in:
    given world: World = World()
    world += C1()
    world.system(s1):
      query all: (e: Entity, _: C1) =>
        for _ <- 0 until defersCount do world.mutator defer deleteEntity(e)

    noException shouldBe thrownBy(world loop deferTestIterations)

  inline val none = ""
  inline val s3 = "test3"

  it should "execute its systems sorted by priority" in:
    given world: World = World()
    var systemName = none

    def test(thisName: String, prevName: String): Unit =
      systemName shouldBe prevName
      systemName = thisName

    world.system(s2, priority = 2):
      routine:
        test(thisName = s2, prevName = s1)
    world.system(s1, priority = 1):
      routine:
        test(thisName = s1, prevName = none)
    world.system(s3, priority = 3):
      routine:
        test(thisName = s3, prevName = s2)

    world loop once
    systemName shouldBe s3

  it should "not contain two systems with the same name" in:
    given world: World = World()
    world.system(s1):
      routine:
        ()
    an[IllegalArgumentException] shouldBe thrownBy:
      world.system(s1):
        routine:
          ()

  import ecscalibur.testutil.testclasses.TestEvent

  val listener = "listener"

  it should "correctly register listeners" in:
    given world: World = World()

    world += C1()
    var emitted = false
    world.subscribe(listener): (_, _: TestEvent) =>
      emitted = true
    world.system(s1):
      query all: e =>
        e >> TestEvent()
        ()

    world loop once
    emitted should be(true)

  it should "correctly unregister listeners" in:
    given world: World = World()

    world += C1()
    var emitted = false
    world.subscribe(listener): (_, _: TestEvent) =>
      emitted = true
    world.unsubscribe(listener, TestEvent)
    world.system(s1):
      query all: e =>
        e >> TestEvent()
        ()

    world loop once
    emitted should be(false)
