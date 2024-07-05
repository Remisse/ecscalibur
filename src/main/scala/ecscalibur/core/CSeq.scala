package ecscalibur.core

import ecscalibur.util.array.*
import scala.reflect.ClassTag
import izumi.reflect.Tag

opaque type CSeq[T] = Array[T]

object CSeq:
  inline def empty[T: ClassTag] = CSeq(Array.empty[T])

  inline def apply[T: ClassTag](elements: T*): CSeq[T] = elements.toArray[T]

  inline def apply[T: ClassTag](elements: Array[T]): CSeq[T] = elements

  inline def apply[T: ClassTag](elements: Iterable[T]): CSeq[T] = elements.toArray

  extension [T: ClassTag](l: CSeq[T])
    private[CSeq] inline def underlying: Array[T] = l

    inline def apply(i: Int): T = l(i)

    inline def update(i: Int, c: T) = l.update(i, c)

    inline def isEmpty: Boolean = l.underlying.length == 0

    inline def nonEmpty: Boolean = l.underlying.length != 0

    inline def foreach(inline f: T => Unit): Unit = l.underlying.aForeach(f)

    inline def forall(inline p: T => Boolean): Boolean = l.underlying.aForall(p)

    inline def sameElements(other: Array[T]): Boolean = l.underlying.aSameElements(other)

    inline def map[U: ClassTag](inline f: T => U): Array[U] = l.underlying.aMap(f)

    inline def containsSlice(that: Array[T]): Boolean = l.underlying.aContainsSlice(that)

    inline def indexWhere(inline p: T => Boolean): Int = l.underlying.aIndexWhere(p)

    inline def indexOf(elem: T): Int = l.underlying.aIndexOf(elem)

    inline def contains(elem: T): Boolean = l.underlying.aContains(elem)

    inline def exists(inline p: T => Boolean): Boolean = l.underlying.aExists(p)

    inline def findUnsafe(inline p: T => Boolean): T = l.underlying.aFindUnsafe(p)

    inline def findOfType[C <: T: Tag]: C = l.underlying.aFindOfType[C]

    inline def filter(inline p: T => Boolean): Array[T] = l.underlying.aFilter(p)

    inline def filterNot(inline p: T => Boolean): Array[T] = l.underlying.aFilterNot(p)

    inline infix def concat(that: CSeq[T]): CSeq[T] = CSeq(l.underlying.aConcat(that.underlying))

  extension [T: ClassTag](elem: T)
    inline infix def +:(cseq: CSeq[T]): CSeq[T] = CSeq(cseq.underlying.prepended(elem))

    inline infix def :+(cseq: CSeq[T]): CSeq[T] = CSeq(cseq.underlying.appended(elem))
