package ecscalibur

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

import ecscalibur.util.FramePacer

class FramePacerTest extends AnyFlatSpec with should.Matchers:
  inline val defaultCap = 60

  "A FramePacer" should "be initialized with a non-negative frame cap" in:
    noException shouldBe thrownBy(FramePacer(defaultCap))
    noException shouldBe thrownBy(FramePacer())
    inline val incorrect = -1
    an[IllegalArgumentException] shouldBe thrownBy(FramePacer(incorrect))

  it should "return the correct delta time value after a call to pace()" in:
    def test(pacer: FramePacer) =
      pacer.pace() shouldBe 0
      pacer.pace() should be > 0.0f

    for _ <- Seq(FramePacer(defaultCap), FramePacer()) do test
