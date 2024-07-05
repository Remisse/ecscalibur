package ecscalibur.core.component

import izumi.reflect.Tag
import ecscalibur.error.IllegalTypeParameterException
import scala.util.hashing.MurmurHash3
import ecscalibur.core.Rw

private[ecscalibur] object tpe:
  inline def createId(clsName: String): Int = MurmurHash3.stringHash(base(clsName))

  inline def getId(cls: Class[?]): ComponentId = ComponentId(createId(cls.getName))

  inline def getId(clsName: String): ComponentId = ComponentId(createId(clsName))

  inline def id0K[T <: WithType: Tag] = getId(summon[Tag[T]].closestClass)

  inline def idRw[T <: WithType: Tag]: ComponentId =
    val t = summon[Tag[T]]
    val id0k = id0K[T]
    if (t.tag.typeArgs.isEmpty) id0k
    else if (id0k == ~Rw && t.tag.typeArgs.head.typeArgs.isEmpty)
      getId(base(t.tag.typeArgs.head.longNameInternalSymbol))
    else
      throw IllegalTypeParameterException(
        s"Used 1- or higher-kinded component $t when only Rw[T] is allowed, where T is a 0-kinded type."
      )

  private inline def base(clsName: String): String = clsName.replace(".", "").replace("$", "")
