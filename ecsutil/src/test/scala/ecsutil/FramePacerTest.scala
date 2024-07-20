package ecsutil

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

class FramePacerTest extends AnyFlatSpec with should.Matchers:
  inline val defaultCap = 60

  "A FramePacer" should "be initialized with a non-negative frame cap" in:
    noException shouldBe thrownBy(FramePacer(defaultCap))
    noException shouldBe thrownBy(FramePacer())
    inline val incorrect = -1
    an[IllegalArgumentException] shouldBe thrownBy(FramePacer(incorrect))

  inline val Tolerance = 1e-8f

  it should "return the correct delta time value after a call to pace()" in:
    val pacer = FramePacer(defaultCap)
    pacer.pace(()) === (1f / defaultCap) +- Tolerance should be(true)
    val pacerNoCap = FramePacer(cap = 0)
    pacerNoCap.pace(()) should be > 0f
