package ecscalibur

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import ecscalibur.util.{companionNameOf, companionNameOf1K, nameOf}
import ecscalibur.testutil.testclasses
import ecscalibur.core.component.Rw

class ReflectionTest extends AnyFlatSpec with should.Matchers:
  import testclasses.C1

  "nameOf" should "return the name of a class" in:
    nameOf[C1] shouldBe classOf[C1].getName

  "nameOf" should "not return the name of a 1-kinded class's type parameter" in:
    nameOf[Rw[C1]] shouldBe classOf[Rw[?]].getName

  "companionNameOf" should "return the name of the companion object of a class" in:
    companionNameOf[C1] shouldBe C1.getClass.getName
    companionNameOf[C1] shouldNot be (classOf[C1].getName)

  "companionNameOf" should "not return the name of a 1-kinded class's type parameter" in:
    companionNameOf[Rw[C1]] shouldNot be (C1.getClass.getName)

  "companionNameOf1K" should "return the name of the companion object of a 1-kinded class's type parameter" in:
    companionNameOf1K[Rw[C1]] shouldBe C1.getClass.getName
    a[RuntimeException] shouldBe thrownBy (companionNameOf1K[C1])
