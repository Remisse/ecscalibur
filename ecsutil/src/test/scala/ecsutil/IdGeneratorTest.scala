package ecsutil

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

class IdGeneratorTests extends AnyFlatSpec with should.Matchers:
  val cons: Seq[Int] = 0 to 9

  "An ID generator" should "generate consecutive IDs" in:
    val gen = IdGenerator()
    val ids = cons.map(_ => gen.next)
    ids should contain allElementsOf cons

  it should "recognize all IDs it has generated" in:
    val gen = IdGenerator()
    val ids = cons.map(_ => gen.next)
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
    gen.next shouldNot be(reused)

  it should "not recognize negative IDs" in:
    val gen = IdGenerator()
    val _ = gen.next
    gen.isValid(-1) should be(false)
