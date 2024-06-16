package ecscalibur

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import ecscalibur.exception.MissingAnnotationException

class ComponentTest extends AnyFlatSpec with should.Matchers:
  import ecscalibur.core.*
  import Components.*
  import Annotations.component

  class NotAnnotated extends Component
  object NotAnnotated extends Component

  "A component class" should "be annotated with @component" in:
    a[MissingAnnotationException] shouldBe thrownBy(~NotAnnotated)

  it should "always extend Component" in:
    """
    import ecscalibur.core.Components.Annotations.component
    @component class BadComponent
    """ shouldNot compile

  it should "always define a companion object" in:
    """
    import ecscalibur.core.Components.*
    import Annotations.component
    @component
    class BadComponent extends Component
    """ shouldNot compile

  it should "have a companion object extending ComponentType" in:
    // Compilation silently fails if object BadComponent extends nothing or
    // something other than Component (why?).
    """
    import ecscalibur.core.Components.*
    import Annotations.component
    @component 
    class BadComponent extends Component
    object BadComponent extends Component
    """ shouldNot compile
    """
    import ecscalibur.core.Components.*
    import Annotations.component
    @component 
    class C extends Component
    object C extends ComponentType
    """ should compile

  @component
  class Comp1 extends Component
  object Comp1 extends ComponentType

  @component
  class Comp2 extends Component
  object Comp2 extends ComponentType

  it should "have a unique type ID" in:
    Comp1.id shouldNot be(Components.nil)
    val c11 = Comp1()
    c11.id shouldBe Comp1.id
    c11 isA Comp1 shouldBe true // Equivalent to the above.
    val c12 = Comp1()
    ~c11 shouldBe ~c12 // Equivalent to 'c11.id shouldBe c12.id'.
    ~Comp1 shouldNot equal(~Comp2)
