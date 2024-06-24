package ecscalibur.core.component

opaque type CSeq = Array[Component]

import CSeq.Extensions.get
import scala.reflect.ClassTag
inline def <<[T <: Component: ClassTag](using l: CSeq): T = l.get[T]

object CSeq:
  def apply(comps: Component*): CSeq = comps.toArray
  def apply(comps: Array[Component]): CSeq = comps
  def apply(comps: Iterable[Component]): CSeq = comps.toArray

  object Extensions:
    extension (l: CSeq)
      inline def underlying: Array[Component] = l
      inline def toTypes: Array[ComponentId] = l.map(_.typeId)
      inline def get[T <: Component: ClassTag]: T =
        val idx = l.indexWhere:
          case t: T => true
          case _    => false
        if idx == -1 then
          throw IllegalArgumentException(s"No component of class ${summon[ClassTag[T]]} found.")
        l(idx).asInstanceOf[T]
