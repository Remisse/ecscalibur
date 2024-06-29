package ecscalibur

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import ecscalibur.util.{companionNameOf0K, companionNameOf1K, nameOf}
import ecscalibur.testutil.testclasses
import ecscalibur.error.IllegalTypeParameterException

class ReflectionTest extends AnyFlatSpec with should.Matchers:
  import testclasses.{C1, OneKinded}

  "nameOf" should "return the name of a class" in:
    nameOf[C1] shouldBe classOf[C1].getName

  "nameOf" should "not return the name of a 1-kinded class's type parameter" in:
    nameOf[OneKinded[?]] shouldBe classOf[OneKinded[?]].getName
    nameOf[OneKinded[C1]] shouldBe classOf[OneKinded[?]].getName

  "companionNameOf0K" should "return the name of the companion object of a class" in:
    companionNameOf0K[C1] shouldBe C1.getClass.getName
    companionNameOf0K[C1] shouldNot be (classOf[C1].getName)

  "companionNameOf0K" should "not return the name of a 1-kinded class's type parameter" in:
    companionNameOf0K[OneKinded[C1]] shouldNot be (C1.getClass.getName)
    companionNameOf0K[OneKinded[C1]] shouldBe (OneKinded.getClass.getName)

  "companionNameOf1K" should "return the name of the companion object of a 1-kinded class's type parameter" in:
    companionNameOf1K[OneKinded[C1]] shouldBe C1.getClass.getName
    an[IllegalTypeParameterException] shouldBe thrownBy (companionNameOf1K[C1])
