package ecsutil

/** Mutable collection based on arrays.
  *
  * Provides a few reimplemented array operations due to weird performance issues when using library
  * methods on arrays of primitives.
  */
opaque type CSeq[T] = Array[T]

object CSeq:
  import ecsutil.array.*

  import scala.annotation.targetName
  import scala.reflect.ClassTag

  inline def empty[T: ClassTag] = CSeq(Array.empty[T])

  inline def apply[T: ClassTag](elements: T*): CSeq[T] = elements.toArray[T]

  inline def apply[T: ClassTag](elements: Array[T]): CSeq[T] = elements

  inline def apply[T: ClassTag](elements: Iterable[T]): CSeq[T] = elements.toArray

  inline def ofDim[T: ClassTag](dim: Int): CSeq[T] = Array.ofDim[T](dim)

  inline def fill[T: ClassTag](n: Int)(f: => T): CSeq[T] = Array.fill(n)(f)

  extension [T: ClassTag](l: CSeq[T])
    inline def toArray: Array[T] = l

    inline def apply(i: Int): T = l(i)

    inline def update(i: Int, c: T) = l.update(i, c)

    inline def isEmpty: Boolean = l.toArray.length == 0

    inline def nonEmpty: Boolean = l.toArray.length != 0

    inline def foreach(inline f: T => Unit): Unit = l.toArray.aForeach(f)

    inline def forall(inline p: T => Boolean): Boolean = l.toArray.aForall(p)

    inline def sameElements(other: CSeq[T]): Boolean = l.toArray.aSameElements(other.toArray)

    inline def map[U: ClassTag](inline f: T => U): CSeq[U] = l.toArray.aMap(f)

    inline def containsSlice(that: CSeq[T]): Boolean = l.toArray.aContainsSlice(that.toArray)

    inline def indexWhere(inline p: T => Boolean): Int = l.toArray.aIndexWhere(p)

    inline def indexOf(elem: T): Int = l.toArray.aIndexOf(elem)

    inline def contains(elem: T): Boolean = l.toArray.aContains(elem)

    inline def exists(inline p: T => Boolean): Boolean = l.toArray.aExists(p)

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
    inline def findUnsafe(inline p: T => Boolean): T = l.toArray.aFindUnsafe(p)

    /** Returns the first element of this collection that is instance of the given type parameter.
      *
      * @tparam C
      *   type of the desired element
      * @throws NoSuchElementException
      *   if no element in this collection is of type C
      * @return
      *   the first element that is instance of the given type
      */
    inline def findOfType[C <: T: ClassTag]: C = l.toArray.aFindOfType[C]

    inline def filter(inline p: T => Boolean): CSeq[T] = l.toArray.aFilter(p)

    inline def filterNot(inline p: T => Boolean): CSeq[T] = l.toArray.aFilterNot(p)

    inline infix def concat(that: CSeq[T]): CSeq[T] = CSeq(l.toArray.aConcat(that.toArray))

    inline def length: Int = l.toArray.length

  extension [T: ClassTag](elem: T)
    @targetName("prepended")
    inline infix def +:(cseq: CSeq[T]): CSeq[T] = CSeq(cseq.toArray.prepended(elem))

    @targetName("appended")
    inline infix def :+(cseq: CSeq[T]): CSeq[T] = CSeq(cseq.toArray.appended(elem))
