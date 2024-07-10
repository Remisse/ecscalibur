package ecsutil

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

class FramePacerTest extends AnyFlatSpec with should.Matchers:
  inline val defaultCap = 60

  "A FramePacer" should "be initialized with a non-negative frame cap" in:
    noException shouldBe thrownBy(FramePacer(defaultCap))
    noException shouldBe thrownBy(FramePacer())
    inline val incorrect = -1
    an[IllegalArgumentException] shouldBe thrownBy(FramePacer(incorrect))

  inline val Tolerance = 1e-4f

  it should "return the correct delta time value after a call to pace()" in:
    val pacer = FramePacer(defaultCap)
    pacer.pace() should be(0f)
    pacer.pace() === (1f / defaultCap) +- Tolerance should be(true)
