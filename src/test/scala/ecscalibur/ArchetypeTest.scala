package ecscalibur

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import ecscalibur.core.Components.*
import ecscalibur.core.Components.Annotations.component
import ecscalibur.core.archetype.Archetypes.Archetype

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
    archetype.contains(C1) shouldBe true
    archetype.contains(C2, C1) shouldBe true
    archetype.contains(C3) shouldBe false

  it should "have a signature made of distinct component types only" in:
    an[IllegalArgumentException] shouldBe thrownBy(Archetype(C1, C1, C2))
