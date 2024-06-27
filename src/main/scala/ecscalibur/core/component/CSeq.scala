package ecscalibur.core.component

opaque type CSeq = Array[Component]

import CSeq.Extensions.{readonly, readwrite}
import scala.reflect.ClassTag
import ecscalibur.util.array.*
import ecscalibur.error.IllegalTypeParameterException

inline def <<[T <: Component: ClassTag](using l: CSeq): T = l.readonly[T]
inline def >>[T <: Component: ClassTag](using l: CSeq): Ref[T] = l.readwrite[T]

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

      inline def readonly[T <: Component: ClassTag]: T =
        val idx = l.aIndexWhere:
          case _: T => true
          case _    => false
        if idx == -1 then
          throw IllegalTypeParameterException(
            s"No component of class ${summon[ClassTag[T]]} found."
          )
        l(idx).asInstanceOf[T]

      inline def readwrite[T <: Component: ClassTag]: Ref[T] = l.readonly[Ref[T]]
