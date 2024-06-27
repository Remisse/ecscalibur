package ecscalibur

import ecscalibur.error.IllegalDefinitionException
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

import core.component.*

class ComponentTest extends AnyFlatSpec with should.Matchers:
  import ecscalibur.testutil.testclasses

  import testclasses.NoCompanionObject

  it must "define a companion object" in:
    "class Bad extends Component" shouldNot compile
    an[IllegalDefinitionException] shouldBe thrownBy (NoCompanionObject().typeId)

  it should "pass its companion object as given to the constructor of Component" in:
    """
    class Bad extends Component
    object Bad extends ComponentType
    """ shouldNot compile
    """
    class C extends Component(using C)
    object C extends ComponentType
    """ should compile

  import ecscalibur.testutil.testclasses.WrongGiven

  it should "not use the companion object of another Component class as given" in:
    an[IllegalDefinitionException] shouldBe thrownBy (WrongGiven().typeId)

  import testclasses.{C1, C2}

  it should "have a unique type ID" in:
    C1.typeId shouldNot be(ComponentType.Nil)
    // Equivalent to 'C1.typeId shouldNot equal(C2.typeId)'.
    ~C1 shouldNot equal(~C2)

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

  // import testclasses.CompExtended

  // "A subclass of a component class" should "have a different type ID from that of its superclass" in:
  //   ~CompExtended shouldNot equal(~C1)
  //   val c1 = C1()
  //   val c1ex = CompExtended()
  //   ~c1 shouldNot equal(~c1ex)
  //   c1ex isA C1 shouldBe false
