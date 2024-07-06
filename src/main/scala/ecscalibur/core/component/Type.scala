package ecscalibur.core.component

import ecscalibur.core.Rw
import ecscalibur.error.IllegalTypeParameterException
import izumi.reflect.Tag

import scala.util.hashing.MurmurHash3

private[ecscalibur] object tpe:
  private def createId(clsName: String): Int = MurmurHash3.stringHash(base(clsName))

  // Should be faster than replaceAll
  private inline def base(clsName: String): String = clsName.replace(".", "").replace("$", "")

  inline def getId(cls: Class[?]): ComponentId = ComponentId(createId(cls.getName))

  inline def getId(clsName: String): ComponentId = ComponentId(createId(clsName))

  inline def id0K[T <: WithType: Tag]: ComponentId = getId(summon[Tag[T]].closestClass)

  inline def idRw[T <: WithType: Tag]: ComponentId =
    val t = summon[Tag[T]]
    val id0k = id0K[T]
    if (isZeroKinded[T]) id0k
    else if (id0k == ~Rw && isOneKinded[T])
      getId(nameOfFirstTypeArg[T])
    else
      throw IllegalTypeParameterException(
        s"Used 1- or higher-kinded component $t when only Rw[T] is allowed, where T is a 0-kinded type."
      )

  private inline def isZeroKinded[T: Tag]: Boolean = summon[Tag[T]].tag.typeArgs.isEmpty

  private inline def isOneKinded[T: Tag]: Boolean =
    summon[Tag[T]].tag.typeArgs.head.typeArgs.isEmpty

  private inline def nameOfFirstTypeArg[T: Tag]: String =
    summon[Tag[T]].tag.typeArgs.head.longNameInternalSymbol
