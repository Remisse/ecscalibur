package ecscalibur

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

class IdGeneratorTests extends AnyFlatSpec with should.Matchers:
  import ecscalibur.id.IdGenerator

  "An ID generator" should "generate consecutive IDs" in:
    val gen = IdGenerator()
    val cons = 0 to 9
    val ids = cons.map(_ => gen.next)
    ids should contain allElementsOf (cons)

  it should "recognize all IDs it has generated" in:
    val gen = IdGenerator()
    val ids = (0 to 9).map(_ => gen.next)
    (ids forall gen.isValid) shouldBe true

  it should "not recognize any IDs it has not generated" in:
    val gen = IdGenerator()
    gen.isValid(0) shouldBe false

  it should "correctly erase a previously generated ID" in:
    val gen = IdGenerator()
    val id = gen.next
    gen.erase(id)
    gen.isValid(id) shouldBe false

  it should "prioritize the reuse of previously erased IDs" in:
    val gen = IdGenerator()
    val id = gen.next
    val _ = gen.next
    gen.erase(id)
    val reused = gen.next
    id shouldBe reused
