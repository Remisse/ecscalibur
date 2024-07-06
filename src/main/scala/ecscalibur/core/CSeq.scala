package ecscalibur.core

import ecscalibur.util.array._
import izumi.reflect.Tag

import scala.annotation.targetName
import scala.reflect.ClassTag

opaque type CSeq[T] = Array[T]

object CSeq:
  inline def empty[T: ClassTag] = CSeq(Array.empty[T])

  inline def apply[T: ClassTag](elements: T*): CSeq[T] = elements.toArray[T]

  inline def apply[T: ClassTag](elements: Array[T]): CSeq[T] = elements

  inline def apply[T: ClassTag](elements: Iterable[T]): CSeq[T] = elements.toArray

  extension [T: ClassTag](l: CSeq[T])
    inline def toArray: Array[T] = l

    inline def apply(i: Int): T = l(i)

    inline def update(i: Int, c: T) = l.update(i, c)

    inline def isEmpty: Boolean = l.toArray.length == 0

    inline def nonEmpty: Boolean = l.toArray.length != 0

    inline def foreach(inline f: T => Unit): Unit = l.toArray.aForeach(f)

    inline def forall(inline p: T => Boolean): Boolean = l.toArray.aForall(p)

    inline def sameElements(other: Array[T]): Boolean = l.toArray.aSameElements(other)

    inline def map[U: ClassTag](inline f: T => U): CSeq[U] = l.toArray.aMap(f)

    inline def containsSlice(that: Array[T]): Boolean = l.toArray.aContainsSlice(that)

    inline def indexWhere(inline p: T => Boolean): Int = l.toArray.aIndexWhere(p)

    inline def indexOf(elem: T): Int = l.toArray.aIndexOf(elem)

    inline def contains(elem: T): Boolean = l.toArray.aContains(elem)

    inline def exists(inline p: T => Boolean): Boolean = l.toArray.aExists(p)

    inline def findUnsafe(inline p: T => Boolean): T = l.toArray.aFindUnsafe(p)

    inline def findOfType[C <: T: Tag]: C = l.toArray.aFindOfType[C]

    inline def filter(inline p: T => Boolean): CSeq[T] = l.toArray.aFilter(p)

    inline def filterNot(inline p: T => Boolean): CSeq[T] = l.toArray.aFilterNot(p)

    inline infix def concat(that: CSeq[T]): CSeq[T] = CSeq(l.toArray.aConcat(that.toArray))

  extension [T: ClassTag](elem: T)
    @targetName("prepended")
    inline infix def +:(cseq: CSeq[T]): CSeq[T] = CSeq(cseq.toArray.prepended(elem))

    @targetName("appended")
    inline infix def :+(cseq: CSeq[T]): CSeq[T] = CSeq(cseq.toArray.appended(elem))
