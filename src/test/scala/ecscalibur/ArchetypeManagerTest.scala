package ecscalibur.core

import ecscalibur.core.archetype.{ro, rw, any}
import ecscalibur.error.IllegalTypeParameterException
import ecscalibur.testutil.shouldNotBeExecuted
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.*

import component.*
import CSeq.Extensions.*
import archetype.ArchetypeManager

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
    val am = ArchetypeManager()
    val (v1, v2) = (Value(1), Value(2))
    val toAdd = Map(
      Entity(0) -> CSeq(v1, C2()),
      Entity(1) -> CSeq(v2, C1(), C2())
    )
    for (entity, comps) <- toAdd do am.addEntity(entity, comps)
    var sum = 0
    am.iterate(ro(Value, C2)): (e, comps) =>
      given CSeq = comps
      val (v, c) = (<<[Value], <<[C2])
      v isA Value shouldBe true
      c isA C2 shouldBe true
      an[IllegalTypeParameterException] shouldBe thrownBy(<<[C1])
      sum += v.x
      ()
    sum shouldBe (v1.x + v2.x)

  it should "correctly iterate over the selected entities and update their component values" in:
    val am = ArchetypeManager()
    val toAdd = Map(
      Entity(0) -> CSeq(Value(1), C2()),
      Entity(1) -> CSeq(Value(2), C1(), C2())
    )
    for (entity, comps) <- toAdd do am.addEntity(entity, comps)
    val editedValue = Value(3)
    var sum = 0
    am.iterate(rw(Value)): (_, comps) =>
      given CSeq = comps
      >>[Value] <== editedValue
    am.iterate(ro(Value)): (_, comps) =>
      given CSeq = comps
      sum += <<[Value].x
    sum shouldBe editedValue.x * toAdd.size

  it should "not add the same entity more than once" in:
    val am = ArchetypeManager()
    val entity = Entity(0)
    am.addEntity(entity, CSeq(C1()))
    an[IllegalArgumentException] shouldBe thrownBy(am.addEntity(entity, CSeq(C1())))

  it should "add components to an existing entity" in:
    val am = ArchetypeManager()
    val entity = Entity(0)
    am.addEntity(entity, CSeq(C1(), C2()))
    val v = Value(1)
    am.addComponents(entity, CSeq(v))
    var sum = 0
    am.iterate(ro(Value)): (e, comps) =>
      given CSeq = comps
      val v = <<[Value]
      v isA Value shouldBe true
      sum += v.x
      ()
    sum shouldBe v.x

  it should "not add the same component to an entity more than once" in:
    val am = ArchetypeManager()
    val entity = Entity(0)
    am.addEntity(entity, CSeq(C1()))
    an[IllegalArgumentException] shouldBe thrownBy(am.addComponents(entity, CSeq(C1())))

  it should "remove components from an existing entity" in:
    val am = ArchetypeManager()
    val entity = Entity(0)
    am.addEntity(entity, CSeq(C1(), C2(), Value(0)))
    am.removeComponents(entity, C1, Value)
    am.iterate(any(C1, Value)): (_, _) =>
      shouldNotBeExecuted

  it should "not remove non-existing components from an entity" in:
    val am = ArchetypeManager()
    val entity = Entity(0)
    am.addEntity(entity, CSeq(C1()))
    an[IllegalArgumentException] shouldBe thrownBy(am.removeComponents(entity, C2))

  it should "correctly delete existing entities" in:
    val am = ArchetypeManager()
    val entity = Entity(0)
    am.addEntity(entity, CSeq(C1()))
    am.delete(entity)
    am.iterate(ro(Value)): (_, _) =>
      shouldNotBeExecuted

  import ArchetypeManagerTest.IterateNFixture

  it should "correctly iterate over all entities when supplying 1 type parameter" in:
    val fixture = IterateNFixture(extraComponents = CSeq.empty)
    fixture.archManager.iterate[Rw[Value]]: (e, v) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    fixture.archManager.iterate[Value]: (e, v) =>
      fixture.onIterationStart(v)
    fixture.isSuccess shouldBe true

  it should "correctly iterate over all entities when supplying 2 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1()))
    fixture.archManager.iterate[C1, Rw[Value]]: (e, _, v) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    fixture.archManager.iterate[C1, Value]: (e, _, v) =>
      fixture.onIterationStart(v)
    fixture.isSuccess shouldBe true

  it should "correctly iterate over all entities when supplying 3 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2()))
    fixture.archManager.iterate[C1, C2, Rw[Value]]: (e, _, _, v) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    fixture.archManager.iterate[C1, C2, Value]: (e, _, _, v) =>
      fixture.onIterationStart(v)
    fixture.isSuccess shouldBe true

  import ecscalibur.testutil.testclasses.C3
  it should "correctly iterate over all entities when supplying 4 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2(), C3()))
    fixture.archManager.iterate[C1, C2, C3, Rw[Value]]: (e, _, _, _, v) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    fixture.archManager.iterate[C1, C2, C3, Value]: (e, _, _, _, v) =>
      fixture.onIterationStart(v)
    fixture.isSuccess shouldBe true

  import ecscalibur.testutil.testclasses.C4
  it should "correctly iterate over all entities when supplying 5 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2(), C3(), C4()))
    fixture.archManager.iterate[C1, C2, C3, C4, Rw[Value]]: (e, _, _, _, _, v) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    fixture.archManager.iterate[C1, C2, C3, C4, Value]: (e, _, _, _, _, v) =>
      fixture.onIterationStart(v)
    fixture.isSuccess shouldBe true

  import ecscalibur.testutil.testclasses.C5
  it should "correctly iterate over all entities when supplying 6 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2(), C3(), C4(), C5()))
    fixture.archManager.iterate[C1, C2, C3, C4, C5, Rw[Value]]: (e, _, _, _, _, _, v) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    fixture.archManager.iterate[C1, C2, C3, C4, C5, Value]: (e, _, _, _, _, _, v) =>
      fixture.onIterationStart(v)
    fixture.isSuccess shouldBe true

  import ecscalibur.testutil.testclasses.C6
  it should "correctly iterate over all entities when supplying 7 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2(), C3(), C4(), C5(), C6()))
    fixture.archManager.iterate[C1, C2, C3, C4, C5, C6, Rw[Value]]: (e, _, _, _, _, _, _, v) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    fixture.archManager.iterate[C1, C2, C3, C4, C5, C6, Value]: (e, _, _, _, _, _, _, v) =>
      fixture.onIterationStart(v)
    fixture.isSuccess shouldBe true
