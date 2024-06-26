package ecscalibur.core

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import component.*
import CSeq.Extensions.*
import component.Annotations.component
import ecscalibur.core.archetype.Archetypes.Aggregate
import ecscalibur.core.archetype.Signature
import ecscalibur.core.archetype.Archetypes.Fragment
import ecscalibur.util.sizeOf.sizeOf

object ArchetypeTest:
  @component
  class C1 extends Component
  object C1 extends ComponentType

  @component
  class C2 extends Component
  object C2 extends ComponentType

  @component
  class C3 extends Component
  object C3 extends ComponentType

  @component
  case class Value(val x: Int) extends Component
  object Value extends ComponentType

class ArchetypeTest extends AnyFlatSpec with should.Matchers:
  import ArchetypeTest.*

  inline val DefaultFragmentSizeBytes = 16384
  inline val KindaSmallFragmentSizeBytes = 64
  inline val ExtremelySmallFragmentSizeBytes = 1

  "An archetype Aggregate with no signature" should "throw when created" in:
    an[IllegalArgumentException] should be thrownBy (Aggregate(DefaultFragmentSizeBytes))

  "An Aggregate archetype" should "be identified by the component classes it holds" in:
    val archetype = Aggregate(DefaultFragmentSizeBytes, C1, C2)
    archetype.signature shouldBe Signature(C1, C2)
    archetype.signature shouldBe Signature(C2, C1)
    archetype.signature shouldNot be(Signature(C1))
    archetype.signature shouldNot be(Signature(C1, C2, C3))

  it should "have a signature made of distinct component types only" in:
    an[IllegalArgumentException] shouldBe thrownBy(Aggregate(DefaultFragmentSizeBytes, C1, C1, C2))

  it should "correctly store entities and their components" in:
    val e1 = Entity(0)
    val archetype = Aggregate(DefaultFragmentSizeBytes, C1, C2)
    archetype.add(e1, >>(C1(), C2()))
    archetype.contains(e1) shouldBe true

  it should "not accept entities that do not satisfy its signature" in:
    val e1 = Entity(0)
    val archetype = Aggregate(DefaultFragmentSizeBytes, Value, C2)
    an[IllegalArgumentException] should be thrownBy (archetype.add(e1, >>(C2())))
    an[IllegalArgumentException] should be thrownBy (
      archetype.add(e1, >>(Value(1), C2(), C3()))
    )

  it should "correctly remove stored entities" in:
    val e1 = Entity(0)
    val archetype = Aggregate(DefaultFragmentSizeBytes, C1)
    archetype.add(e1, >>(C1()))
    archetype.remove(e1)
    archetype.contains(e1) shouldBe false

  it should "return all components of an entity when that entity is removed" in:
    val e1 = Entity(0)
    val archetype = Aggregate(DefaultFragmentSizeBytes, Value, C1, C2)
    val wv = Value(3)
    val c1 = C1()
    val c2 = C2()
    archetype.add(e1, >>(wv, c1, c2))
    archetype.remove(e1).underlying should contain allOf (wv, c1, c2)

  it should "correctly soft-remove entities" in:
    val e1 = Entity(0)
    val archetype = Aggregate(DefaultFragmentSizeBytes, C1)
    val c1 = C1()
    archetype.add(e1, >>(c1))
    archetype.softRemove(e1)
    archetype.contains(e1) shouldBe false

  it should "correctly iterate over all selected entities and components in read-only mode" in:
    val arch = Aggregate(DefaultFragmentSizeBytes, Value, C2)
    val (v1, v2) = (Value(1), Value(2))
    val toAdd: Map[Entity, CSeq] = Map(
      Entity(0) -> >>(v1, C2()),
      Entity(1) -> >>(v2, C2())
    )
    for (entity, comps) <- toAdd do arch.add(entity, comps)
    var sum = 0
    arch.iterate(_ == ~Value): (e, comps) =>
      val c = comps.get[Value]
      an[IllegalArgumentException] should be thrownBy (comps.get[C1])
      sum += c.x
      /
    sum shouldBe (v1.x + v2.x)

  it should "correctly iterate over all selected entities and components in RW mode" in:
    val arch = Aggregate(DefaultFragmentSizeBytes, Value, C2)
    val e1 = Entity(0)
    val wv = Value(5)
    val editedWv = Value(0)
    arch.add(e1, >>(wv, C2()))
    arch.iterate(_ == ~Value): (e, comps) =>
      given CSeq = comps
      val c = <<[Value]
      c shouldBe wv
      >>(editedWv)
    arch.iterate(_ == ~Value): (e, comps) =>
      val _ = comps.get[Value] shouldBe editedWv
      /

  it should "correctly perform load balancing when fragments reach their limit" in:
    // sizeOf incorrectly reports sizes greater than 4900 bytes for classes
    // declared within test classes.
    val arch = Aggregate(KindaSmallFragmentSizeBytes, Value)
    val components = CSeq(Value(0))
    inline val numberOfEntities = 1000
    for i <- (0 until numberOfEntities) do
      noException should be thrownBy (arch.add(Entity(i), components))

  it should "correctly return its Fragments" in:
    val arch = Aggregate(KindaSmallFragmentSizeBytes, C1)
    arch.fragments.isEmpty shouldBe true
    val c = C1()
    inline val numberOfEntities = 1000
    for i <- (0 until numberOfEntities) do arch.add(Entity(i), CSeq(c))
    val expectedNumberOfFragments = numberOfEntities / (KindaSmallFragmentSizeBytes / sizeOf(c))
    arch.fragments.size shouldBe expectedNumberOfFragments

  it should "remove all empty Fragments but one" in:
    val arch = Aggregate(KindaSmallFragmentSizeBytes, C1)
    val c = C1()
    inline val numberOfEntities = 1000
    val entities = (0 until numberOfEntities).map(Entity(_))
    for e <- entities do arch.add(e, CSeq(c))
    for e <- entities do arch.remove(e)
    arch.fragments.size shouldBe 1

  it should "throw if the size of a component is greather than the maximum size limit" in:
    val arch = Aggregate(ExtremelySmallFragmentSizeBytes, Value)
    val components = CSeq(Value(0))
    an[IllegalStateException] should be thrownBy (arch.add(Entity(0), components))

  "A Fragment" should "correctly report whether it is full or not" in:
    inline val maxEntities = 1
    val frag = Fragment(Signature(Value), maxEntities)
    val e = Entity(0)
    frag.isEmpty shouldBe true
    frag.isFull shouldBe !frag.isEmpty
    frag.add(e, CSeq(Value(0)))
    frag.isEmpty shouldBe false
    frag.isFull shouldBe !frag.isEmpty
    frag.remove(e)
    frag.isEmpty shouldBe true
    frag.isFull shouldBe !frag.isEmpty

  it should "throw when attempting to add more entities than it can store" in:
    inline val maxEntities = 1
    val frag = Fragment(Signature(Value), maxEntities)
    val v = Value(0)
    noException should be thrownBy (frag.add(Entity(0), CSeq(v)))
    an[IllegalArgumentException] should be thrownBy (frag.add(Entity(1), CSeq(v)))
