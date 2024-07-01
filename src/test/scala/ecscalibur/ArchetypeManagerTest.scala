package ecscalibur.core

import ecscalibur.error.IllegalTypeParameterException
import ecscalibur.testutil.shouldNotBeExecuted
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.*

import ecscalibur.core.component.Rw
import ecscalibur.core.component.CSeq
import CSeq.Extensions.*
import archetype.ArchetypeManager
import ecscalibur.core.queries.*

object ArchetypeManagerTest:
  import ecscalibur.testutil.testclasses.Value
  private class IterateNFixture(nEntities: Int = 100, extraComponents: CSeq):
    require(nEntities > 0)

    val archManager = ArchetypeManager()
    val testValue = Value(10)
    private val entities: Vector[Entity] = (0 until nEntities).map(Entity(_)).toVector
    private val values: Vector[Value] = (0 until nEntities).map(Value(_)).toVector
    private var sum = 0

    for (e, idx) <- entities.zipWithIndex do
      archManager.addEntity(e, CSeq(values(idx) +: extraComponents.underlying))

    def onIterationStart(v: Value) = sum += v.x
    def isSuccess = sum == values.map(_.x).sum + nEntities * testValue.x

class ArchetypeManagerTest extends AnyFlatSpec with should.Matchers:
  import ecscalibur.testutil.testclasses.{Value, C1, C2}

  "An archetype manager" should "correctly add new entities and iterate over them" in:
    given am: ArchetypeManager = ArchetypeManager()
    val (v1, v2) = (Value(1), Value(2))
    val toAdd = Map(
      Entity(0) -> CSeq(v1, C2()),
      Entity(1) -> CSeq(v2, C1(), C2())
    )
    for (entity, comps) <- toAdd do am.addEntity(entity, comps)
    var sum = 0
    (query withNone C1 withAll: (e: Entity, v: Value, c: C2) =>
      v isA Value shouldBe true
      c isA C2 shouldBe true
      sum += v.x
    ).apply
    sum shouldBe v1.x

  it should "correctly iterate over the selected entities and update their component values" in:
    given am: ArchetypeManager = ArchetypeManager()
    val toAdd = Map(
      Entity(0) -> CSeq(Value(1), C2()),
      Entity(1) -> CSeq(Value(2), C1(), C2())
    )
    for (entity, comps) <- toAdd do am.addEntity(entity, comps)
    val editedValue = Value(3)
    var sum = 0
    (query withAll: (e, v: Rw[Value]) =>
      v <== editedValue).apply
    (query withAll: (e, v: Value) =>
      sum += v.x).apply
    sum shouldBe editedValue.x * toAdd.size

  it should "not add the same entity more than once" in:
    val am = ArchetypeManager()
    val entity = Entity(0)
    am.addEntity(entity, CSeq(C1()))
    an[IllegalArgumentException] shouldBe thrownBy(am.addEntity(entity, CSeq(C1())))

  // it should "add components to an existing entity" in:
  //   val am = ArchetypeManager()
  //   val entity = Entity(0)
  //   am.addEntity(entity, CSeq(C1(), C2()))
  //   val v = Value(1)
  //   am.addComponents(entity, CSeq(v))
  //   var sum = 0
  //   am.iterate(ro(Value)): (e, comps) =>
  //     given CSeq = comps
  //     val v = <<[Value]
  //     v isA Value shouldBe true
  //     sum += v.x
  //     ()
  //   sum shouldBe v.x

  it should "not add the same component to an entity more than once" in:
    val am = ArchetypeManager()
    val entity = Entity(0)
    am.addEntity(entity, CSeq(C1()))
    an[IllegalArgumentException] shouldBe thrownBy(am.addComponents(entity, CSeq(C1())))

  // it should "remove components from an existing entity" in:
  //   val am = ArchetypeManager()
  //   val entity = Entity(0)
  //   am.addEntity(entity, CSeq(C1(), C2(), Value(0)))
  //   am.removeComponents(entity, C1, Value)
  //   am.iterate(any(C1, Value)): (_, _) =>
  //     shouldNotBeExecuted

  it should "not remove non-existing components from an entity" in:
    val am = ArchetypeManager()
    val entity = Entity(0)
    am.addEntity(entity, CSeq(C1()))
    an[IllegalArgumentException] shouldBe thrownBy(am.removeComponents(entity, C2))

  // it should "correctly delete existing entities" in:
  //   val am = ArchetypeManager()
  //   val entity = Entity(0)
  //   am.addEntity(entity, CSeq(C1()))
  //   am.delete(entity)
  //   am.iterate(ro(Value)): (_, _) =>
  //     shouldNotBeExecuted

  import ArchetypeManagerTest.IterateNFixture

  it should "correctly iterate over all entities when supplying 1 type parameter" in:
    val fixture = IterateNFixture(extraComponents = CSeq.empty)
    given ArchetypeManager = fixture.archManager
    (query withAll: (e, v: Rw[Value]) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply
    (query withAll: (e, v: Value) =>
      fixture.onIterationStart(v)).apply
    fixture.isSuccess shouldBe true

  it should "correctly iterate over all entities when supplying 2 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1()))
    given ArchetypeManager = fixture.archManager
    (query withAll: (e, v: Rw[Value], _: C1) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply
    (query withAll: (e, v: Value, _: C1) =>
      fixture.onIterationStart(v)).apply
    fixture.isSuccess shouldBe true

  it should "correctly iterate over all entities when supplying 3 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2()))
    given ArchetypeManager = fixture.archManager
    (query withAll: (e, v: Rw[Value], _: C1, _: C2) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply
    (query withAll: (e, v: Value, _: C1, _: C2) =>
      fixture.onIterationStart(v)).apply
    fixture.isSuccess shouldBe true

  import ecscalibur.testutil.testclasses.C3
  it should "correctly iterate over all entities when supplying 4 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2(), C3()))
    given ArchetypeManager = fixture.archManager
    (query withAll: (e, v: Rw[Value], _: C1, _: C2, _: C3) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply
    (query withAll: (e, v: Value, _: C1, _: C2, _: C3) =>
      fixture.onIterationStart(v)).apply
    fixture.isSuccess shouldBe true

  import ecscalibur.testutil.testclasses.C4
  it should "correctly iterate over all entities when supplying 5 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2(), C3(), C4()))
    given ArchetypeManager = fixture.archManager
    (query withAll: (e, v: Rw[Value], _: C1, _: C2, _: C3, _: C4) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply
    (query withAll: (e, v: Value, _: C1, _: C2, _: C3, _: C4) =>
      fixture.onIterationStart(v)).apply
    fixture.isSuccess shouldBe true

  import ecscalibur.testutil.testclasses.C5
  it should "correctly iterate over all entities when supplying 6 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2(), C3(), C4(), C5()))
    given ArchetypeManager = fixture.archManager
    (query withAll: (e, v: Rw[Value], _: C1, _: C2, _: C3, _: C4, _: C5) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply
    (query withAll: (e, v: Value, _: C1, _: C2, _: C3, _: C4, _: C5) =>
      fixture.onIterationStart(v)).apply
    fixture.isSuccess shouldBe true

  import ecscalibur.testutil.testclasses.C6
  it should "correctly iterate over all entities when supplying 7 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2(), C3(), C4(), C5(), C6()))
    given ArchetypeManager = fixture.archManager
    (query withAll: (e, v: Rw[Value], _: C1, _: C2, _: C3, _: C4, _: C5, _: C6) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply
    (query withAll: (e, v: Value, _: C1, _: C2, _: C3, _: C4, _: C5, _: C6) =>
      fixture.onIterationStart(v)).apply
    fixture.isSuccess shouldBe true
