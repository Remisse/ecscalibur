package ecscalibur

import ecscalibur.error.IllegalDefinitionException
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

import core.component.*
import ecscalibur.error.IllegalTypeParameterException

class ComponentTest extends AnyFlatSpec with should.Matchers:
  import ecscalibur.testutil.testclasses

  import testclasses.NoCompanionObject

  "A Component class" must "define a companion object" in:
    "class Bad extends Component" shouldNot compile
    an[IllegalDefinitionException] shouldBe thrownBy(NoCompanionObject().typeId)

  it should "pass its companion object as a given instance to the constructor of Component" in:
    """
    class Bad extends Component
    object Bad extends ComponentType
    """ shouldNot compile
    """
    class C extends Component(using C)
    object C extends ComponentType
    """ should compile

  import ecscalibur.testutil.testclasses.WrongGiven

  it should "not use the companion object of another Component class as a given instance" in:
    an[IllegalDefinitionException] shouldBe thrownBy(WrongGiven().typeId)

  import testclasses.{C1, C2}

  it should "have a unique type ID" in:
    C1.typeId shouldNot be(ComponentType.Nil)
    // Equivalent to 'C1.typeId shouldNot equal(C2.typeId)'.
    ~C1 shouldNot equal(~C2)

  it should "have the same type ID as its companion object" in:
    id0K[C1] shouldBe C1.typeId
    id0K[C1] shouldNot be(C2.typeId)

  "A component instance" should "have the same type ID as its class" in:
    val c1 = C1()
    c1.typeId shouldBe C1.typeId
    c1 isA C1 shouldBe true // Equivalent to the above.

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
    an[IllegalTypeParameterException] should be thrownBy (idRw[OneKinded[C1]])
    idRw[Rw[C1]] shouldBe ~C1
    idRw[C1] shouldBe id0K[C1]

  "idRw[T]" should "throw if T is a 1- or higher-kinded type" in:
    an[IllegalTypeParameterException] should be thrownBy (idRw[Rw[OneKinded[C1]]])
