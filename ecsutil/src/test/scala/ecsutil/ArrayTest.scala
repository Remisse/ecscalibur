package ecsutil

import ecsutil.array.*
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

class ArrayTest extends AnyFlatSpec with should.Matchers:
  inline val Min = 0
  inline val Max = 100
  def intArray: Array[Int] = (Min until Max).toArray
  def numArray: Array[IntWrapper] = intArray.map(n => IntWrapper(n)).toArray

  case class IntWrapper(n: Int)

  "aForeach" should "work correctly" in:
    val a = intArray
    var sum = 0
    a.aForeach: n =>
      sum += n
    sum shouldBe a.sum
    Array
      .empty[Int]
      .aForeach: _ =>
        shouldNotBeExecuted

  "aForeachIndex" should "work correctly" in:
    val a = intArray
    var sum = 0
    var currentIdx = 0
    a.aForeachIndex: (n, i) =>
      sum += n
      i shouldBe currentIdx
      currentIdx += 1
    sum shouldBe a.sum
    Array
      .empty[Int]
      .aForeachIndex: (_, _) =>
        shouldNotBeExecuted

  "aForall" should "work correctly" in:
    val a = intArray
    a.aForall(n => Min <= n && n < Max) shouldBe true
    a.aForall(n => n < 0) shouldBe false
    Array
      .empty[Int]
      .aForall: _ =>
        shouldNotBeExecuted

  "aSameElements" should "work correctly" in:
    val a = intArray
    a.aSameElements(intArray) shouldBe true
    a.aSameElements(intArray.map(_ => 0)) shouldBe false
    a.aSameElements(Array()) shouldBe false
    a.aSameElements(Array(1)) shouldBe false
    "a.aSameElements(numArray)" shouldNot typeCheck

  "aMap" should "work correctly" in:
    val a = intArray
    val plusOne = ((Min + 1) until (Max + 1)).toArray
    a.aMap(_ + 1).aSameElements(plusOne) shouldBe true
    val allAs = Array.fill(Max)("a")
    a.aMap(_ => "a").aSameElements(allAs) shouldBe true

  "aContainsSlice" should "work correctly" in:
    val a = intArray
    a.aContainsSlice((Min until (Max / 2)).toArray) shouldBe true
    a.aContainsSlice(Array.empty) shouldBe false
    a.aContainsSlice(intArray) shouldBe true
    a.aContainsSlice(Array(Min, Max)) shouldBe false

  "aIndexWhere" should "work correctly" in:
    val a = intArray
    a.aIndexWhere(_ == Min) shouldBe 0
    a.aIndexWhere(_ == Max - 1) shouldBe (a.length - 1)
    a.aIndexWhere(_ > Max) shouldBe -1

  "aIndexOf" should "work correctly" in:
    val a = intArray
    a.aIndexOf(Min) shouldBe 0
    a.aIndexOf(Max - 1) shouldBe (a.length - 1)
    a.aIndexOf(Max + 1) shouldBe -1

  "aContains" should "work correctly" in:
    val a = intArray
    a.aContains(Min) shouldBe true
    a.aContains(Max - 1) shouldBe true
    a.aContains(Max + 1) shouldBe false

  "aExists" should "work correctly" in:
    val a = intArray
    a.aExists(_ > Min) shouldBe true
    a.aExists(_ < Min) shouldBe false
    a.aExists(_ < Max) shouldBe true
    a.aExists(_ > Max) shouldBe false

  "aFindUnsafe" should "work correctly" in:
    val arr = intArray
    arr.aFindUnsafe(_ == Min) shouldBe Min
    a[NoSuchElementException] shouldBe thrownBy(arr.aFindUnsafe(_ == Max))

  class C1 extends AnyRef
  class C2 extends AnyRef
  class C3 extends AnyRef

  "aFindOfType" should "work correctly" in:
    val arr: Array[AnyRef] = Array(C1(), C2())
    (arr.aFindOfType[C1] match
      case _: C1 => true
      case _     => false
    ) should be(true)
    a[NoSuchElementException] shouldBe thrownBy(arr.aFindOfType[C3])

  "aFilter" should "work correctly" in:
    val predicate: Int => Boolean = _ % 2 == 0
    intArray.aFilter(predicate) shouldBe intArray.filter(predicate)
    val impossiblePredicate: Int => Boolean = _ > Max
    intArray.aFilter(impossiblePredicate) shouldBe Array.empty[Int]
    Array.empty[Int].aFilter(predicate) shouldBe Array.empty[Int]

  "aFilterNot" should "work correctly" in:
    val predicate: Int => Boolean = _ % 2 == 0
    intArray.aFilterNot(predicate) shouldBe intArray.filter(!predicate(_))
    val impossiblePredicate: Int => Boolean = _ > Max
    intArray.aFilter(impossiblePredicate) shouldBe Array.empty[Int]
    Array.empty[Int].aFilter(predicate) shouldBe Array.empty[Int]

  "aConcat" should "work correctly" in:
    intArray.aConcat(intArray) shouldBe intArray.concat(intArray)
