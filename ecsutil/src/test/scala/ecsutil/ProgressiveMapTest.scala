package ecsutil

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

class ProgressiveMapTest extends AnyFlatSpec with should.Matchers:
  inline val defaultValue = 10
  val cons = -defaultValue until defaultValue

  "A ProgressiveMap" should "be correctly initialized with no elements" in:
    val map = ProgressiveMap[Int]()
    map.isEmpty should be(true)
    map.size should be(0)

  it should "be correctly initialized with the given elements" in:
    val map = ProgressiveMap.from(cons*)
    map.size should be(cons.length)
    map.isEmpty should be(false)
    for n <- cons do map.contains(n) should be(true)

  it should "correctly add new elements and provide mappings for them" in:
    val map = ProgressiveMap[Int]()
    map += defaultValue
    map.contains(defaultValue) should be(true)
    map(defaultValue) should be(0)
    map.ofId(0) should be(defaultValue)

  it should "correctly remove existing elements" in:
    val map = ProgressiveMap.from(defaultValue)
    map -= defaultValue
    map.contains(defaultValue) should be(false)
    an[IllegalArgumentException] shouldBe thrownBy(map(defaultValue))
    an[IllegalArgumentException] shouldBe thrownBy(map.ofId(0))

  it should "correctly iterate over all mapped elements" in:
    val map = ProgressiveMap.from(cons*)
    for (elem, mapping) <- map do
      map(elem) should be(mapping)
