package ecscalibur

import ecscalibur.core.*

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

import error.IllegalDefinitionException
import util.tpe.*

class ComponentTest extends AnyFlatSpec with should.Matchers:
  import ecscalibur.testutil.testclasses

  import testclasses.NotAnnotated

  "A component class" must "be annotated with @component" in:
    an[IllegalDefinitionException] shouldBe thrownBy(~NotAnnotated)

  it must "extend Component" in:
    """
    @component 
    class BadComponent
    """ shouldNot compile

  it must "define a companion object" in:
    """
    @component 
    class BadComponent extends Component
    """ shouldNot compile

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

  it should "not be generic" in:
    """
    @component
    class Generic[T] extends Component
    object Generic extends ComponentType
    """ shouldNot compile

  import testclasses.{C1, C2}

  it should "have a unique type ID" in:
    ~C1 shouldNot equal(~C2)
    C1 shouldNot equal(C2)
    ~C1 should equal(~C1)
    C1 should equal(C1)
    C1 shouldNot equal(components)

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

  "id0K[T]" should "return the component ID of a 0-kinded type" in:
    id0K[C1] should be(~C1)
    id0K[OneKinded[C1]] shouldNot be(~C1)

  "getId" should "return the correct type ID" in:
    val cls = classOf[C1]
    getId(cls) should be(~C1)
    getId(cls.getName) should be(~C1)
    getId(cls) should be(id0K[C1])
