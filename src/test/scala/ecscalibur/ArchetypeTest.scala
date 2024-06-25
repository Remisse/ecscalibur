package ecscalibur.core

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import component.*
import CSeq.Extensions.*
import component.Annotations.component
import ecscalibur.core.archetype.Archetypes.Archetype
import ecscalibur.core.archetype.Signature

class ArchetypeTest extends AnyFlatSpec with should.Matchers:
  @component
  class C1 extends Component
  object C1 extends ComponentType

  @component
  class C2 extends Component
  object C2 extends ComponentType

  @component
  class C3 extends Component
  object C3 extends ComponentType

  "An archetype with no signature" should "throw when created" in:
    an[IllegalArgumentException] shouldBe thrownBy(Archetype())

  "An archetype" should "be identified by the component classes it holds" in:
    val archetype = Archetype(C1, C2)
    archetype.signature shouldBe Signature(C1, C2)
    archetype.signature shouldBe Signature(C2, C1)
    archetype.signature shouldNot be(Signature(C1))
    archetype.signature shouldNot be(Signature(C1, C2, C3))

  it should "have a signature made of distinct component types only" in:
    an[IllegalArgumentException] shouldBe thrownBy(Archetype(C1, C1, C2))

  @component
  case class Value(val x: Int) extends Component
  object Value extends ComponentType

  it should "correctly store entities and their components" in:
    val e1 = Entity(0)
    val archetype = Archetype(C1, C2)
    archetype.add(e1, CSeq(C1(), C2()))
    archetype.contains(e1) shouldBe true

  it should "not accept entities that do not satisfy its signature" in:
    val e1 = Entity(0)
    val archetype = Archetype(Value, C2)
    an[IllegalArgumentException] shouldBe thrownBy(archetype.add(e1, CSeq(C2())))
    an[IllegalArgumentException] shouldBe thrownBy(
      archetype.add(e1, CSeq(Value(1), C2(), C3()))
    )

  it should "correctly remove stored entities" in:
    val e1 = Entity(0)
    val archetype = Archetype(C1)
    archetype.add(e1, CSeq(C1()))
    archetype.remove(e1)
    archetype.contains(e1) shouldBe false

  it should "return all components of an entity when that entity is removed" in:
    val e1 = Entity(0)
    val archetype = Archetype(Value, C1, C2)
    val wv = Value(3)
    val c1 = C1()
    val c2 = C2()
    archetype.add(e1, CSeq(wv, c1, c2))
    archetype.remove(e1).underlying should contain allOf (wv, c1, c2)

  it should "correctly soft-remove entities" in:
    val e1 = Entity(0)
    val archetype = Archetype(C1)
    val c1 = C1()
    archetype.add(e1, CSeq(c1))
    archetype.softRemove(e1)
    archetype.contains(e1) shouldBe false

  it should "correctly iterate over all selected entities and components in read-only mode" in:
    val arch = Archetype(Value, C2)
    val (v1, v2) = (Value(1), Value(2))
    val toAdd: Map[Entity, CSeq] = Map(
      Entity(0) -> CSeq(v1, C2()),
      Entity(1) -> CSeq(v2, C2())
    )
    for (entity, comps) <- toAdd do arch.add(entity, comps)
    var sum = 0
    arch.readAll(_ == ~Value): (e, comps) =>
      val c = comps.get[Value]
      an[IllegalArgumentException] shouldBe thrownBy(comps.get[C1])
      sum += c.x
    sum shouldBe (v1.x + v2.x)

  it should "correctly iterate over all selected entities and components in RW mode" in:
    val arch = Archetype(Value, C2)
    val e1 = Entity(0)
    val wv = Value(5)
    val editedWv = Value(0)
    arch.add(e1, CSeq(wv, C2()))
    arch.writeAll(_ == ~Value): (e, comps) =>
      given CSeq = comps
      val c = <<[Value] // Equivalent to comps.get[Value]
      c shouldBe wv
      CSeq(editedWv)
    arch.readAll(_ == ~Value): (e, comps) =>
      val _ = comps.get[Value] shouldBe editedWv
