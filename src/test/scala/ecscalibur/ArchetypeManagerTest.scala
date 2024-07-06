package ecscalibur

import ecscalibur.core.CSeq
import ecscalibur.core.Entity
import ecscalibur.core.Mutator
import ecscalibur.core.Rw
import ecscalibur.core.archetype.ArchetypeManager
import ecscalibur.core.context.MetaContext
import ecscalibur.core.queries.query
import ecscalibur.testutil.shouldNotBeExecuted
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers._

class ArchetypeManagerTest extends AnyFlatSpec with should.Matchers:
  import ecscalibur.testutil.testclasses.{Value, C1, C2}
  import ecscalibur.fixtures.ArchetypeManagerFixture

  val testValue: Value = Value(1)
  val defaultEntity: Entity = Entity(0)

  "An ArchetypeManager" should "correctly add new entities and iterate over them" in:
    val fixture = ArchetypeManagerFixture(
      CSeq(testValue, C2()),
      CSeq(testValue, C1(), C2())
    )
    given am: ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    var sum = 0
    (query except C1 on: (e: Entity, v: Value, c: C2) =>
      v isA Value shouldBe true
      c isA C2 shouldBe true
      sum += v.x
    ).apply()
    sum shouldBe testValue.x

  val editedValue: Value = Value(3)

  it should "correctly iterate over the selected entities and update their component values" in:
    val fixture = ArchetypeManagerFixture(
      CSeq(testValue, C2()),
      CSeq(testValue, C1(), C2())
    )
    given am: ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    var sum = 0
    (query on: (e, v: Rw[Value]) =>
      v <== editedValue).apply()
    (query on: (e, v: Value) =>
      sum += v.x).apply()
    sum shouldBe editedValue.x * fixture.entitiesCount

  it should "not add the same entity more than once" in:
    val am = ArchetypeManager()
    am.addEntity(defaultEntity, CSeq(C1()))
    an[IllegalArgumentException] shouldBe thrownBy(am.addEntity(defaultEntity, CSeq(C1())))

  it should "add components to an existing entity" in:
    val fixture = ArchetypeManagerFixture(
      CSeq(C1())
    )
    given am: ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    am.addComponents(fixture.entities.head, CSeq(testValue))
    var sum = 0
    (query on: (e, v: Value) =>
      sum += v.x).apply()
    sum shouldBe testValue.x

  it should "do nothing when adding the same component to an entity more than once" in:
    val fixture = ArchetypeManagerFixture(CSeq(testValue))
    given am: ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    am.addComponents(fixture.entities.head, CSeq(testValue))
    (query on: (_, v: Value) =>
      v shouldBe testValue
      ()
    ).apply()

  it should "remove components from an existing entity" in:
    val fixture = ArchetypeManagerFixture(CSeq(C1(), C2(), testValue))
    given am: ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    am.removeComponents(fixture.entities.head, C1, Value)
    (query any (C1, Value) on: _ =>
      shouldNotBeExecuted).apply()

  it should "do nothing when attempting to remove non-existing components from an entity" in:
    val fixture = ArchetypeManagerFixture(CSeq(C1()))
    val am = fixture.archManager
    noException shouldBe thrownBy(am.removeComponents(fixture.entities.head, C2))

  it should "correctly delete existing entities" in:
    val fixture = ArchetypeManagerFixture(CSeq(C1()))
    given am: ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    am.delete(fixture.entities.head)
    (query any C1 on: _ =>
      shouldNotBeExecuted).apply()

  val nonExisting: Entity = Entity(1)

  it should "throw when adding components to a non-existing entity" in:
    val fixture = ArchetypeManagerFixture(
      CSeq(C1())
    )
    val am = fixture.archManager
    an[IllegalArgumentException] shouldBe thrownBy(am.addComponents(nonExisting, CSeq(C2())))

  it should "throw when removing components from a non-existing entity" in:
    val fixture = ArchetypeManagerFixture(
      CSeq(C1())
    )
    val am = fixture.archManager
    an[IllegalArgumentException] shouldBe thrownBy(am.removeComponents(nonExisting, C2))

  it should "throw when deleting a non-existing entity" in:
    val fixture = ArchetypeManagerFixture(
      CSeq(C1())
    )
    val am = fixture.archManager
    an[IllegalArgumentException] shouldBe thrownBy(am.delete(nonExisting))

  import fixtures.IterateNFixture

  it should "iterate only once regardless of the number of entities when creating a routine" in:
    val fixture = IterateNFixture(nEntities = 100, extraComponents = CSeq.empty)
    given ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    var executionsCount = 0
    (query routine: () =>
      executionsCount += 1).apply()
    executionsCount shouldBe 1

  it should "correctly iterate over all entities across all fragments if no components are specified" in:
    inline val nEntities = 100
    val fixture = IterateNFixture(nEntities, extraComponents = CSeq.empty)
    given ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    var executionsCount = 0
    (query on: _ =>
      executionsCount += 1).apply()
    executionsCount shouldBe nEntities

  it should "correctly iterate over all entities when supplying 1 type parameter" in:
    val fixture = IterateNFixture(extraComponents = CSeq.empty)
    given ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    (query on: (e, v: Rw[Value]) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply()
    (query on: (e, v: Value) =>
      fixture.onIterationStart(v)).apply()
    fixture.isSuccess shouldBe true

  it should "correctly iterate over all entities when supplying 2 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1()))
    given ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    (query on: (e, v: Rw[Value], _: C1) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply()
    (query on: (e, v: Value, _: C1) =>
      fixture.onIterationStart(v)).apply()
    fixture.isSuccess shouldBe true

  it should "correctly iterate over all entities when supplying 3 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2()))
    given ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    (query on: (e, v: Rw[Value], _: C1, _: C2) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply()
    (query on: (e, v: Value, _: C1, _: C2) =>
      fixture.onIterationStart(v)).apply()
    fixture.isSuccess shouldBe true

  import ecscalibur.testutil.testclasses.C3
  it should "correctly iterate over all entities when supplying 4 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2(), C3()))
    given ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    (query on: (e, v: Rw[Value], _: C1, _: C2, _: C3) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply()
    (query on: (e, v: Value, _: C1, _: C2, _: C3) =>
      fixture.onIterationStart(v)).apply()
    fixture.isSuccess shouldBe true

  import ecscalibur.testutil.testclasses.C4
  it should "correctly iterate over all entities when supplying 5 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2(), C3(), C4()))
    given ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    (query on: (e, v: Rw[Value], _: C1, _: C2, _: C3, _: C4) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply()
    (query on: (e, v: Value, _: C1, _: C2, _: C3, _: C4) =>
      fixture.onIterationStart(v)).apply()
    fixture.isSuccess shouldBe true

  import ecscalibur.testutil.testclasses.C5
  it should "correctly iterate over all entities when supplying 6 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2(), C3(), C4(), C5()))
    given ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    (query on: (e, v: Rw[Value], _: C1, _: C2, _: C3, _: C4, _: C5) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply()
    (query on: (e, v: Value, _: C1, _: C2, _: C3, _: C4, _: C5) =>
      fixture.onIterationStart(v)).apply()
    fixture.isSuccess shouldBe true

  import ecscalibur.testutil.testclasses.C6
  it should "correctly iterate over all entities when supplying 7 type parameters" in:
    val fixture = IterateNFixture(extraComponents = CSeq(C1(), C2(), C3(), C4(), C5(), C6()))
    given ArchetypeManager = fixture.archManager
    given MetaContext = fixture.context
    given Mutator = fixture.mutator
    (query on: (e, v: Rw[Value], _: C1, _: C2, _: C3, _: C4, _: C5, _: C6) =>
      fixture.onIterationStart(v.get)
      v <== fixture.testValue
    ).apply()
    (query on: (e, v: Value, _: C1, _: C2, _: C3, _: C4, _: C5, _: C6) =>
      fixture.onIterationStart(v)).apply()
    fixture.isSuccess shouldBe true
