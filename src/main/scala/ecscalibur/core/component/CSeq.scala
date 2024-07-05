package ecscalibur.core.component

import ecscalibur.util.array.*
import scala.reflect.ClassTag

opaque type CSeq = Array[Component]

object CSeq:
  def empty = CSeq(Array.empty[Component])

  def apply(comps: Component*): CSeq = comps.toArray
  def apply(comps: Array[Component]): CSeq = comps
  def apply(comps: Iterable[Component]): CSeq = comps.toArray

  object Extensions:
    extension (l: CSeq)
      inline def apply(i: Int): Component = l(i)

      inline def update(i: Int, c: Component) = l.update(i, c)

      inline def underlying: Array[Component] = l

      inline def toTypes: Array[ComponentId] = l.aMap(_.typeId)

      // Warns about an infinite loop when using underlying.isEmpty, which
      // makes no sense.
      inline def isEmpty: Boolean = l.underlying.length == 0

      inline def nonEmpty: Boolean = l.underlying.length != 0
