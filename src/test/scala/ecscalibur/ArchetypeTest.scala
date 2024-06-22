package ecscalibur.core

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import Components.*
import Annotations.component
import ecscalibur.core.archetype.Archetypes.Archetype
import ecscalibur.core.archetype.Signature
import ecscalibur.core.Entities.Entity
import ecscalibur.core.Components.CSeqs.{CSeq, <<}

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
    archetype.signature shouldNot be (Signature(C1))
    archetype.signature shouldNot be (Signature(C1, C2, C3))

  it should "report which component types it owns" in:
    val archetype = Archetype(C1, C2)
    archetype.handles(C1) shouldBe true
    archetype.handles(C2, C1) shouldBe true
    archetype.handles(C3) shouldBe false

  it should "have a signature made of distinct component types only" in:
    an[IllegalArgumentException] shouldBe thrownBy(Archetype(C1, C1, C2))

  @component
  case class WithValue(val x: Int) extends Component
  object WithValue extends ComponentType

  it should "correctly store entities and their components" in:
    val e1 = Entity(0)
    val archetype = Archetype(C1, C2)
    archetype.add(e1, CSeq(C1(), C2()))
    archetype.contains(e1) shouldBe true

  it should "not accept entities that do not satisfy its signature" in:
    val e1 = Entity(0)
    val archetype = Archetype(WithValue, C2)
    an[IllegalArgumentException] shouldBe thrownBy(archetype.add(e1, CSeq(C2())))
    an[IllegalArgumentException] shouldBe thrownBy(
      archetype.add(e1, CSeq(WithValue(1), C2(), C3()))
    )

  it should "correctly remove stored entities" in:
    val e1 = Entity(0)
    val archetype = Archetype(C1)
    archetype.add(e1, CSeq(C1()))
    archetype.remove(e1)
    archetype.contains(e1) shouldBe false

  it should "return all components of an entity when that entity is removed" in:
    val e1 = Entity(0)
    val archetype = Archetype(WithValue, C1, C2)
    val wv = WithValue(3)
    val c1 = C1() 
    val c2 = C2()
    archetype.add(e1, CSeq(wv, c1, c2))
    archetype.remove(e1).underlying should contain allOf(wv, c1, c2)

  it should "correctly soft-remove entities" in:
    val e1 = Entity(0)
    val archetype = Archetype(C1)
    val c1 = C1() 
    archetype.add(e1, CSeq(c1))
    archetype.softRemove(e1)
    archetype.contains(e1) shouldBe false

  it should "correctly iterate over all selected entities and components in read-only mode" in:
    val arch = Archetype(WithValue, C2)
    val e1 = Entity(0)
    val wv = WithValue(5)
    arch.add(e1, CSeq(wv, C2()))
    arch.readAll(
      _ == ~WithValue, 
      (e, comps) =>
        val c = comps.get[WithValue]
        c shouldBe wv
        val _ = an[IllegalArgumentException] shouldBe thrownBy(comps.get[C1])
    )

  it should "correctly iterate over all selected entities and components in RW mode" in:
    val arch = Archetype(WithValue, C2)
    val e1 = Entity(0)
    val wv = WithValue(5)
    val editedWv = WithValue(0)
    arch.add(e1, CSeq(wv, C2()))
    arch.writeAll(
      _ == ~WithValue, 
      (e, comps) =>
        given CSeq = comps
        val c = <<[WithValue] // Equivalent to comps.get[WithValue]
        c shouldBe wv
        CSeq(editedWv)
    )
    arch.readAll(
      _ == ~WithValue, 
      (e, comps) =>
        val _ = comps.get[WithValue] shouldBe editedWv
    )
