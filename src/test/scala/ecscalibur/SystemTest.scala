package ecscalibur

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

import ecscalibur.core.archetype.ArchetypeManager
import ecscalibur.core.systems.System
import ecscalibur.core.queries.Query
import ecscalibur.core.{query, Mutator}
import ecscalibur.testutil.testclasses.Value
import ecscalibur.core.context.MetaContext
import ecscalibur.testutil.shouldNotBeExecuted

class SystemTest extends AnyFlatSpec with should.Matchers:

  import fixtures.SystemFixture

  inline val s1 = "test"

  "A System" should "execute its update logic when called" in:
    val fixture = SystemFixture()
    given am: ArchetypeManager = fixture.am
    given context: MetaContext = fixture.context
    given Mutator = fixture.mutator

    var sum = Int.MinValue
    val s = new System(name = s1, priority = 0):
      override protected val onStart: () => Unit =
        () => sum = 0
      override protected val process: Query =
        query on: (e, v: Value) =>
          sum += v.x
      override protected val onStop: () => Unit =
        () => shouldNotBeExecuted

    s.update()
    sum shouldBe fixture.defaultValue.x

  it should "make use of the MetaContext given to it" in:
    val fixture = SystemFixture()
    given am: ArchetypeManager = fixture.am
    given context: MetaContext = fixture.context
    given Mutator = fixture.mutator

    var sum = 0.0
    val s = new System(name = s1, priority = 0):
      override protected val process: Query =
        query on: (e, v: Value) =>
          sum += v.x * context.deltaTime

    s.update()
    sum shouldBe fixture.defaultValue.x * context.deltaTime

  it should "pause if asked to while running" in:
    val fixture = SystemFixture()
    given am: ArchetypeManager = fixture.am
    given context: MetaContext = fixture.context
    given Mutator = fixture.mutator

    var sum = Int.MinValue
    val s = new System(name = s1, priority = 0):
      override protected val onStart: () => Unit =
        () =>
      override protected val process: Query =
        query on: (e, v: Value) =>
          sum = 0
      override protected val onStop: () => Unit =
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
    given am: ArchetypeManager = fixture.am
    given context: MetaContext = fixture.context
    given Mutator = fixture.mutator

    var sum = Int.MinValue
    val s = new System(name = s1, priority = 0):
      override protected val onStart: () => Unit =
        () => sum = 0
      override protected val process: Query =
        query on: (e, v: Value) =>
          sum += v.x
      override protected val onStop: () => Unit =
        () => sum = Int.MaxValue

    an[IllegalStateException] shouldBe thrownBy(s.resume())
    s.update()
    s.pause()
    s.update()
    s.resume()
    s.update()
    sum shouldBe fixture.defaultValue.x
