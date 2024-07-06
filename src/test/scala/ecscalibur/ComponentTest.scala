package ecscalibur

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

import error.{IllegalDefinitionException, IllegalTypeParameterException}
import core.Rw
import core.component._
import core.component.tpe._


class ComponentTest extends AnyFlatSpec with should.Matchers:
  import ecscalibur.testutil.testclasses

  import testclasses.NotAnnotated

  "A component class" must "be annotated with @component" in:
    an[IllegalDefinitionException] shouldBe thrownBy(~NotAnnotated)

  it must "extend Component" in:
    """
    @ecscalibur.core.component.annotations.component 
    class BadComponent
    """ shouldNot compile

  it must "define a companion object" in:
    """
    @ecscalibur.core.component.annotations.component 
    class BadComponent extends Component
    """ shouldNot compile

  it should "have a companion object extending ComponentType" in:
    // Compilation silently fails if object BadComponent extends nothing or
    // something other than Component (why?).
    """
    @ecscalibur.core.component.annotations.component 
    class BadComponent extends Component
    object BadComponent extends Component
    """ shouldNot compile
    """
    @ecscalibur.core.component.annotations.component 
    class C extends Component
    object C extends ComponentType
    """ should compile

  import testclasses.{C1, C2}

  it should "have a unique type ID" in:
    ~C1 shouldNot equal(~C2)

  it should "have the same type ID as its companion object" in:
    id0K[C1] shouldBe ~C1
    id0K[C1] shouldNot be(C2)

  "A component instance" should "have the same type ID as its class" in:
    val c1 = C1()
    c1.typeId shouldBe C1.typeId
    c1 isA C1 shouldBe true

  it should "have the same type ID as another instance of the same type" in:
    val c1 = C1()
    val c2 = C1()
    ~c1 shouldBe ~c2

  it should "have a different type ID from that of an instance of a different type" in:
    val c1 = C1()
    val c2 = C2()
    ~c1 shouldNot be(~c2)

  import testclasses.OneKinded

  "id0K[T]" should "not return the component ID of a 1-kinded class's type parameter" in:
    id0K[OneKinded[C1]] shouldNot be(~C1)
    id0K[OneKinded[C1]] shouldBe ~OneKinded

  "idRw[T]" should "return the component ID of Rw[T]'s type parameter or fall back to id0K" in:
    an[IllegalTypeParameterException] should be thrownBy idRw[OneKinded[C1]]
    idRw[Rw[C1]] shouldBe ~C1
    idRw[Rw[C1]] shouldNot be(~Rw)
    idRw[C1] shouldBe id0K[C1]

  "idRw[T]" should "throw if T is a 1- or higher-kinded type" in:
    an[IllegalTypeParameterException] should be thrownBy idRw[Rw[OneKinded[C1]]]
