package ecscalibur

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import error.MissingAnnotationError
import core.component.*

object ComponentTest:
  import Annotations.component

  class NotAnnotated extends Component
  object NotAnnotated extends ComponentType

  @component
  class Comp1 extends Component
  object Comp1 extends ComponentType

  @component
  class Comp2 extends Component
  object Comp2 extends ComponentType

  @component
  class CompExtended extends Comp1
  object CompExtended extends ComponentType

class ComponentTest extends AnyFlatSpec with should.Matchers:
  import ComponentTest.NotAnnotated

  "A component class" must "be annotated with @component" in:
    a[MissingAnnotationError] shouldBe thrownBy(~NotAnnotated)

  it must "extend Component" in:
    "@component class BadComponent" shouldNot compile

  it must "define a companion object" in:
    "@component class BadComponent extends Component" shouldNot compile

  it should "have a companion object extending ComponentType" in:
    // Compilation silently fails if object BadComponent extends nothing or
    // something other than Component (why?).
    """
    @component 
    class BadComponent extends Component
    object BadComponent extends Component
    """ shouldNot compile
    """
    @component 
    class C extends Component
    object C extends ComponentType
    """ should compile

  import ComponentTest.{Comp1, Comp2}

  it should "have a unique type ID" in:
    Comp1.typeId shouldNot be(ComponentType.Nil)
    // Equivalent to 'Comp1.typeId shouldNot equal(Comp2.typeId)'.
    ~Comp1 shouldNot equal(~Comp2)

  "A component instance" should "have the same type ID as its class" in:
    val c1 = Comp1()
    c1.typeId shouldBe Comp1.typeId
    c1 isA Comp1 shouldBe true // Equivalent to the above.

  it should "have the same type ID as another instance of the same type" in:
    val c1 = Comp1()
    val c2 = Comp1()
    ~c1 shouldBe ~c2

  it should "have a different type ID from that of an instance of a different type" in:
    val c1 = Comp1()
    val c2 = Comp2()
    ~c1 shouldNot be(~c2)

  import ComponentTest.CompExtended

  "A subclass of a component class" should "have a different type ID from that of its superclass" in:
    ~CompExtended shouldNot equal(~Comp1)
    val c1 = Comp1()
    val c1ex = CompExtended()
    ~c1 shouldNot equal(~c1ex)
    c1ex isA Comp1 shouldBe false
