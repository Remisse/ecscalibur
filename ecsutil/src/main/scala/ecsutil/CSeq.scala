package ecsutil

import scala.reflect.ClassTag
import scala.annotation.targetName
import ecsutil.array.*

/** Mutable collection based on arrays.
  *
  * Provides a few reimplemented array operations due to weird performance issues when using library
  * methods on arrays of primitives.
  */
class CSeq[T: ClassTag](elems: T*):
  private val underlying: Array[T] = Array(elems*)

  inline def toArray: Array[T] = underlying

  inline def apply(i: Int): T = underlying(i)

  inline def update(i: Int, c: T): Unit = underlying.update(i, c)

  inline def isEmpty: Boolean = underlying.isEmpty

  inline def nonEmpty: Boolean = underlying.nonEmpty

  inline def foreach(f: T => Unit): Unit = underlying.aForeach(f)

  inline def forall(p: T => Boolean): Boolean = underlying.aForall(p)

  inline def sameElements(other: CSeq[T]): Boolean = underlying.aSameElements(other.underlying) 

  inline def map[U: ClassTag](f: T => U): CSeq[U] = CSeq(underlying.aMap(f))

  inline def containsSlice(that: CSeq[T]): Boolean = underlying.aContainsSlice(that.underlying)

  inline def indexWhere(p: T => Boolean): Int = underlying.aIndexWhere(p)

  inline def indexOf(elem: T): Int = underlying.aIndexOf(elem)

  inline def contains(elem: T): Boolean = underlying.aContains(elem)

  inline def exists(p: T => Boolean): Boolean = underlying.aExists(p)

  /** Returns the first element of this collection which satisfies the given predicate, or throws
    * if no elements are found.
    *
    * @param p
    *   the predicate against which the elements of this collection will be tested
    * @throws NoSuchElementException
    *   if no element in this collection satisfies the given predicate
    * @return
    *   the first element satisfying the given predicate
    */
  inline def findUnsafe(p: T => Boolean): T = underlying.aFindUnsafe(p)

  /** Returns the first element of this collection that is instance of the given type parameter.
    *
    * @tparam C
    *   type of the desired element
    * @throws NoSuchElementException
    *   if no element in this collection is of type C
    * @return
    *   the first element that is instance of the given type
    */
  inline def findOfType[C <: T: ClassTag]: C = underlying.aFindOfType[C]

  inline def filter(p: T => Boolean): CSeq[T] = CSeq(underlying.aFilter(p))

  inline def filterNot(p: T => Boolean): CSeq[T] = CSeq(underlying.aFilterNot(p))

  inline infix def concat(that: CSeq[T]): CSeq[T] = CSeq(underlying concat that.underlying)

  inline def length: Int = underlying.length

  export ext.*
  object ext:
    extension [U >: T: ClassTag](elem: U)
      @targetName("prepended")
      inline infix def +:(self: CSeq[T]): CSeq[U] = CSeq(elem +: self.underlying)

      @targetName("appended")
      inline infix def :+(self: CSeq[T]): CSeq[U] = CSeq(self.underlying.appended(elem))

object CSeq:
  inline def empty[T: ClassTag]: CSeq[T] = new CSeq()

  inline def apply[T: ClassTag](elements: T*): CSeq[T] = new CSeq(elements*) 

  inline def apply[T: ClassTag](elements: Array[T]): CSeq[T] = new CSeq(elements*)

  inline def ofDim[T: ClassTag](dim: Int): CSeq[T] = new CSeq(Array.ofDim[T](dim)*)

  inline def fill[T: ClassTag](n: Int)(f: => T): CSeq[T] = new CSeq(Array.fill(n)(f)*)