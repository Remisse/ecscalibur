package ecscalibur.core.component

import ecscalibur.core.Rw
import ecscalibur.error.IllegalTypeParameterException
import izumi.reflect.Tag

import scala.util.hashing.MurmurHash3

private[ecscalibur] object tpe:
  /** Generates a ComponentId for the given class.
    *
    * @param cls
    *   the class for which a ComponentId will be generated
    * @return
    *   a unique ComponentId
    */
  inline def getId(cls: Class[?]): ComponentId = ComponentId(createId(cls.getName))

  /** Generates a ComponentId for the given class name.
    *
    * @param cls
    *   the class for which a ComponentId will be generated
    * @return
    *   a unique ComponentId
    */
  inline def getId(clsName: String): ComponentId = ComponentId(createId(clsName))

  private def createId(clsName: String): Int = MurmurHash3.stringHash(base(clsName))

  // Should be faster than replaceAll
  private inline def base(clsName: String): String = clsName.replace(".", "").replace("$", "")

  /** Retrieves the ComponentId of T, regardless of whether T is 0- or higher-kinded.
    *
    * For instance, if T is the component class 'B', then B's ComponentId will be returned. However,
    * if T is A[B], then A's ComponentId will be returned.
    *
    * @tparam T
    *   the type for which the ComponentId must be retrieved
    * @return
    *   the ComponentId of the outermost type of the given type parameter.
    */
  inline def id0K[T <: WithType: Tag]: ComponentId = getId(summon[Tag[T]].closestClass)

  /** @tparam T
    *   the type for which the ComponentId must be retrieved
    * @return
    *   the ComponentId of T's type parameter if T is Rw[_] and is 1-kinded, or T's ComponentId if T is
    *   0-kinded.
    */
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
