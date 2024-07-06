package ecscalibur.util

import izumi.reflect.Tag

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

object array:
  extension [T](a: Array[T])
    inline def aForeach(inline f: T => Unit): Unit =
      @annotation.tailrec
      def each(i: Int): Unit = i match
        case i if i == a.length => ()
        case i =>
          f(a(i))
          each(i + 1)
      each(0)

    inline def aForeachIndex(inline f: (T, Int) => Unit): Unit =
      @annotation.tailrec
      def each(i: Int): Unit = i match
        case i if i == a.length => ()
        case i =>
          f(a(i), i)
          each(i + 1)
      each(0)

    inline def aForall(inline p: T => Boolean): Boolean =
      @annotation.tailrec
      def each(i: Int): Boolean = i match
        case i if i == a.length => true
        case i if p(a(i))       => each(i + 1)
        case _                  => false
      each(0)

    inline def aSameElements(other: Array[T]): Boolean =
      var res = false
      if (a.length != other.length || a.isEmpty || other.isEmpty) res = false
      else
        @annotation.tailrec
        def same(i: Int): Boolean = i match
          case i if i == a.length    => true
          case i if a(i) == other(i) => same(i + 1)
          case _                     => false
        res = same(0)
      res

    inline def aMap[U: ClassTag](inline f: T => U): Array[U] =
      val res = Array.ofDim[U](a.length)
      @annotation.tailrec
      def map(i: Int): Unit = i match
        case i if i == a.length => ()
        case i =>
          res(i) = f(a(i))
          map(i + 1)
      map(0)
      res

    inline def aContainsSlice(that: Array[T]): Boolean =
      var res = false
      if (that.length == 0 || a.length < that.length) res = false
      else if (a.length == that.length) res = a.aSameElements(that)
      else
        @annotation.tailrec
        def containsSlice(i: Int, j: Int): Boolean = j match
          case j if j == that.length => true
          case _ =>
            i match
              case i if i == a.length   => false
              case i if a(i) == that(j) => containsSlice(i + 1, j + 1)
              case _                    => containsSlice(i + 1, 0)
        res = containsSlice(0, 0)
      res

    inline def aIndexWhere(inline p: T => Boolean): Int =
      @annotation.tailrec
      def indexWhere(i: Int): Int = i match
        case i if i == a.length => -1
        case i if p(a(i))       => i
        case _                  => indexWhere(i + 1)
      if (a.nonEmpty) indexWhere(0) else -1

    inline def aIndexOf(elem: T): Int = a.aIndexWhere(elem == _)

    inline def aContains(elem: T): Boolean = a.aIndexWhere(elem == _) != -1

    inline def aExists(inline p: T => Boolean): Boolean = a.aIndexWhere(p) != -1

    inline def aFindUnsafe(inline p: T => Boolean): T =
      val idx = a.aIndexWhere(p)
      require(idx != -1, "Could not find any elements that satisfy the given predicate.")
      a(idx)

    inline def aFindOfType[C <: T: Tag]: C =
      val idx = a.aIndexWhere:
        case _: C => true
        case _ => false
      require(idx != -1, s"Could not find any elements of type ${summon[Tag[C]]}.")
      a(idx).asInstanceOf[C]

    inline def aFilter(inline p: T => Boolean)(using ClassTag[T]): Array[T] =
      if (a.isEmpty) a
      else
        val buffer = ArrayBuffer.empty[T]
        a.aForeach: elem =>
          if (p(elem)) buffer += elem
        buffer.toArray

    private inline def negate(inline p: T => Boolean)(using ClassTag[T]): T => Boolean = elem =>
      !p(elem)

    inline def aFilterNot(inline p: T => Boolean)(using ClassTag[T]): Array[T] =
      a.aFilter(negate(p))

    inline def aConcat(that: Array[T])(using ClassTag[T]): Array[T] = a.concat(that)
