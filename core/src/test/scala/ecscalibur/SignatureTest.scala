package ecscalibur

import ecscalibur.core.archetype.Signature
import ecscalibur.core.components.ComponentId
import ecsutil.CSeq
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

class SignatureTest extends AnyFlatSpec with should.Matchers:
  import testutil.testclasses.{C1, C2, C3}

  "A Signature" should "correctly report whether it is nil" in:
    val signature = Signature.Nil
    signature.isNil should be(true)
    signature.underlying.isEmpty should be(true)

  it should "be non-empty and made of distinct component IDs" in:
    an[IllegalArgumentException] should be thrownBy (Signature(CSeq.empty[ComponentId]))
    val signature = Signature(C1, C2, C3)
    signature.isNil should be(false)
    signature.underlying.isEmpty should be(false)
    an[IllegalArgumentException] should be thrownBy (Signature(C1, C1))

  it should "correctly report whether it is part of another" in:
    val smallerSignature = Signature(C1, C2)
    val biggerSignature = Signature(C1, C2, C3)
    biggerSignature containsAll smallerSignature should be(true)
    smallerSignature containsAll biggerSignature should be(false)

  import testutil.testclasses.{C4, C5}

  it should "correctly report whether it contains parts of another signature" in:
    val smallerSignature = Signature(C2, C3, C5)
    val biggerSignature = Signature(C1, C2, C3, C4)
    biggerSignature containsAny smallerSignature should be(true)
    biggerSignature containsAll smallerSignature should be(false)
    smallerSignature containsAny biggerSignature should be(true)

  def getSignature = Signature(C1, C2, C3)

  it should "correctly report whether it is equal to another" in:
    getSignature should be(getSignature)
    getSignature shouldNot be(Signature.Nil)
    getSignature shouldNot be(Signature(C1))
