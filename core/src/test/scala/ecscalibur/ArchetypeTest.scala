package ecscalibur

import ecscalibur.core.*
import ecsutil.CSeq
import ecsutil.array.*
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

import archetype.Signature
import archetype.archetypes.Aggregate

class ArchetypeTest extends AnyFlatSpec with should.Matchers:
  import ecscalibur.testutil.testclasses.*

  val DefaultFragmentSize = archetype.archetypes.Archetype.DefaultFragmentSize
  inline val KindaSmallFragmentSize = 10
  inline val ExtremelySmallFragmentSize = 1

  val testValue: Value = Value(1)

  "An Aggregate archetype with no signature" should "throw when created" in:
    an[IllegalArgumentException] should be thrownBy Aggregate()(DefaultFragmentSize)

  "An Aggregate archetype" should "be identified by the component classes it holds" in:
    val archetype = Aggregate(C1, C2)(DefaultFragmentSize)
    archetype.signature shouldBe Signature(C1, C2)
    archetype.signature shouldBe Signature(C2, C1)
    archetype.signature shouldNot be(Signature(C1))
    archetype.signature shouldNot be(Signature(C1, C2, C3))

  it should "have a signature made of distinct component types only" in:
    an[IllegalArgumentException] shouldBe thrownBy(Aggregate(C1, C1, C2)(DefaultFragmentSize))

  it should "correctly store entities and their components" in:
    val fixture = fixtures.StandardArchetypeFixture(C1(), C2())(nEntities = 1)
    fixture.archetype.contains(fixture.entities.head) shouldBe true

  it should "not accept entities that do not satisfy its signature" in:
    val fixture = fixtures.StandardArchetypeFixture(testValue, C2())(nEntities = 0)
    val entity = fixture.nextEntity
    an[IllegalArgumentException] should be thrownBy fixture.archetype.add(entity, CSeq(C2()))
    an[IllegalArgumentException] should be thrownBy
      fixture.archetype.add(entity, CSeq(testValue, C2(), C3()))

  it should "correctly remove stored entities" in:
    val fixture = fixtures.StandardArchetypeFixture(C1())(nEntities = 1)
    val entity = fixture.entities(0)
    fixture.archetype.remove(entity)
    fixture.archetype.contains(entity) shouldBe false

  it should "return all components of an entity when that entity is removed" in:
    val components = Seq(testValue, C1(), C2())
    val fixture = fixtures.StandardArchetypeFixture(components*)(nEntities = 1)
    val entity = fixture.entities(0)
    fixture.archetype.remove(entity).forall(components.contains) shouldBe true
    fixture.archetype.contains(entity) shouldBe false

  it should "correctly soft-remove entities" in:
    val fixture = fixtures.StandardArchetypeFixture(C1())(nEntities = 1)
    val entity = fixture.entities(0)
    fixture.archetype.softRemove(entity)
    fixture.archetype.contains(entity) shouldBe false

  inline val nEntities = 1000

  it should "correctly iterate over all selected entities and components in read-only mode" in:
    val fixture = fixtures.StandardArchetypeFixture(testValue, C1())(nEntities = nEntities)
    var sum = 0
    fixture.archetype.iterate: (e, comps) =>
      val c = comps.findOfType[Value]
      sum += c.x
    sum shouldBe testValue.x * nEntities

  it should "correctly perform load balancing when fragments reach their limit" in:
    noException shouldBe thrownBy(fixtures.StandardArchetypeFixture(Value(0))(nEntities = 10000))

  val defaultComponent = C1()

  it should "correctly return its Fragments" in:
    val fixture =
      fixtures.StandardArchetypeFixture(defaultComponent)(
        nEntities = nEntities,
        KindaSmallFragmentSize
      )
    val expectedNumberOfFragments = nEntities / KindaSmallFragmentSize
    fixture.archetype.fragments.size shouldBe expectedNumberOfFragments

  it should "remove all empty Fragments but one" in:
    val fixture =
      fixtures.StandardArchetypeFixture(defaultComponent)(
        nEntities = nEntities,
        KindaSmallFragmentSize
      )
    for e <- fixture.entities do fixture.archetype.remove(e)
    fixture.archetype.fragments.size shouldBe 1

  // it should "throw if the size of a component is greather than the maximum size limit" in:
  //   an[IllegalStateException] shouldBe thrownBy(
  //     fixtures.StandardArchetypeFixture(Value(0))(nEntities = 1, ExtremelySmallFragmentSize)
  //   )

  "A Fragment" should "correctly report whether it is full or not" in:
    val fixture = fixtures.StandardFragmentFixture(defaultComponent)(nEntities = 0, maxEntities = 1)
    val frag = fixture.fragment
    frag.isEmpty shouldBe true
    frag.isFull shouldBe !frag.isEmpty
    val e = fixture.nextEntity
    frag.add(e, CSeq(defaultComponent))
    frag.isEmpty shouldBe false
    frag.isFull shouldBe !frag.isEmpty
    frag.remove(e)
    frag.isEmpty shouldBe true
    frag.isFull shouldBe !frag.isEmpty

  it should "throw when attempting to add more entities than it can store" in:
    val fixture = fixtures.StandardFragmentFixture(defaultComponent)(nEntities = 1, maxEntities = 1)
    an[IllegalArgumentException] should be thrownBy fixture.fragment.add(
      fixture.nextEntity,
      CSeq(defaultComponent)
    )
