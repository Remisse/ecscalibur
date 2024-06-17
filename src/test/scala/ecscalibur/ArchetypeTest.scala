package ecscalibur.core

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import ecscalibur.core.Components.*
import ecscalibur.core.Components.Annotations.component
import ecscalibur.core.archetype.Archetypes.Archetype
import ecscalibur.core.Entities.Entity

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
    archetype.hasSignature(C1, C2) shouldBe true
    archetype.hasSignature(C2, C1) shouldBe true
    archetype.hasSignature(C1) shouldBe false
    archetype.hasSignature(C1, C2, C3) shouldBe false

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
    archetype.add(e1, Array(C1(), C2()))
    archetype.contains(e1) shouldBe true

  it should "correctly return any component values associated to its entities" in:
    val e1 = Entity(0)
    val e2 = Entity(1)
    val archetype = Archetype(WithValue, C2)
    val c1 = WithValue(5)
    val c2 = C2()
    archetype.add(e1, Array(c1, c2))
    archetype.add(e2, Array(c2, c1))
    archetype.get[WithValue](e1, WithValue) shouldBe c1
    archetype.get[C2](e2, C2) shouldBe c2
    a[MatchError] shouldBe thrownBy (archetype.get[C2](e2, WithValue))

  it should "not accept entities that do not satisfy its signature" in:
    val e1 = Entity(0)
    val archetype = Archetype(WithValue, C2)
    an[IllegalArgumentException] shouldBe thrownBy(archetype.add(e1, Array(C2())))
    an[IllegalArgumentException] shouldBe thrownBy(
      archetype.add(e1, Array(WithValue(1), C2(), C3()))
    )

  it should "correctly remove stored entities" in:
    val e1 = Entity(0)
    val archetype = Archetype(C1)
    archetype.add(e1, Array(C1()))
    archetype.remove(e1)
    archetype.contains(e1) shouldBe false

  it should "not return component values associated to deleted entities" in:
    val e1 = Entity(0)
    val c1 = WithValue(1)
    val archetype = Archetype(WithValue)
    archetype.add(e1, Array(c1))
    archetype.remove(e1)
    an[IllegalArgumentException] shouldBe thrownBy(archetype.get[WithValue](e1, WithValue))
