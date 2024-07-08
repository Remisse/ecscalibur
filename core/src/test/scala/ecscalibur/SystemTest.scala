package ecscalibur

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

import core.systems.System
import core.queries.Query
import core.queries.query
import testutil.testclasses.Value
import ecsutil.shouldNotBeExecuted
import ecscalibur.core.world.World

class SystemTest extends AnyFlatSpec with should.Matchers:
  import fixtures.SystemFixture

  inline val s1 = "test"

  "A System" should "execute its update logic when called" in:
    val fixture = SystemFixture()
    given World = fixture.world

    var sum = Int.MinValue
    val s = new System(name = s1, priority = 0):
      override protected val onStart: () => Unit =
        () => sum = 0
      override protected val process: Query =
        query on: (e, v: Value) =>
          sum += v.x
      override protected val onPause: () => Unit =
        () => shouldNotBeExecuted

    s.update()
    sum shouldBe fixture.defaultValue.x

  it should "pause if asked to while running" in:
    val fixture = SystemFixture()
    given World = fixture.world

    var sum = Int.MinValue
    val s = new System(name = s1, priority = 0):
      override protected val onStart: () => Unit =
        () => ()
      override protected val process =
        query on: (e, v: Value) =>
          sum = 0
      override protected val onPause: () => Unit =
        () => sum = Int.MaxValue

    an[IllegalStateException] shouldBe thrownBy(s.pause())
    s.update()
    s.pause()
    s.update()
    sum shouldBe Int.MaxValue
    s.update()
    sum shouldBe Int.MaxValue

  it should "resume if asked to while paused" in:
    val fixture = SystemFixture()
    given World = fixture.world

    var sum = Int.MinValue
    val s = new System(name = s1, priority = 0):
      override protected val onStart: () => Unit =
        () => sum = 0
      override protected val process =
        query on: (e, v: Value) =>
          sum += v.x
      override protected val onPause: () => Unit =
        () => sum = Int.MaxValue

    an[IllegalStateException] shouldBe thrownBy(s.resume())
    s.update()
    s.pause()
    s.update()
    s.resume()
    s.update()
    sum shouldBe fixture.defaultValue.x

  it should "correctly report whether it is running" in:
    val fixture = SystemFixture()
    given World = fixture.world

    val s = new System(name = s1, priority = 0):
      override protected val process =
        query routine: () =>
          ()

    s.isRunning should be(false)
    s.update()
    s.isRunning should be(true)
    s.pause()
    s.isRunning should be(false)

  it should "correctly report whether it is paused" in:
    val fixture = SystemFixture()
    given World = fixture.world

    val s = new System(name = s1, priority = 0):
      override protected val process =
        query routine: () =>
          ()

    s.isPaused should be(false)
    s.update()
    s.pause()
    s.isPaused should be(false)
    s.update()
    s.isPaused should be(true)
