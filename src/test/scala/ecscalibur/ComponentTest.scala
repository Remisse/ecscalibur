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

  it should "have a companion object extending Component" in:
    """
    import ecscalibur.core.Components.*
    import Annotations.component
    @component 
    class BadComponent extends Component
    object BadComponent extends Component
    """ should compile

  @component
  class Comp1 extends Component
  object Comp1 extends Component

  @component
  class Comp2 extends Component
  object Comp2 extends Component

  it should "have a unique type ID" in:
    Comp1.id shouldNot be(Components.noId)
    val c11 = Comp1()
    c11.id shouldBe Comp1.id
    val c12 = Comp1()
    ~c11 shouldBe ~c12 // Equivalent to 'c11.id shouldBe c12.id'.
    ~Comp1 shouldNot equal(~Comp2)
