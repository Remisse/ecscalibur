package ecscalibur

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import ecscalibur.annotation.component

class ComponentTest extends AnyFlatSpec with should.Matchers:
  import ecscalibur.core.*
  import Components.*

  // TODO Need to find a way to test if component classes are annotated with @component

  "A component class" should "always extend Component" in:
    "@component class BadComponent" shouldNot compile

  it should "always define a companion object" in:
    "@component class BadComponent extends Component" shouldNot compile

  // TODO Does not run
  // it should "have a companion object extending Component" in:
  //   """
  //   @component
  //   class BadComponent extends Component
  //   object BadComponent""" shouldNot compile

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
