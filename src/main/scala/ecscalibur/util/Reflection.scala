package ecscalibur.util

private[util] inline def deriveCompanionName(clsName: String): String =
  s"${clsName}$$"

inline def companionNameOf(cls: Class[?]): String = deriveCompanionName(cls.getName)
inline def companionNameOf(clsName: String): String = deriveCompanionName(clsName)

import scala.reflect.ClassTag
import izumi.reflect.Tag
import ecscalibur.error.IllegalTypeParameterException

inline def nameOf[T: ClassTag]: String = summon[ClassTag[T]].runtimeClass.getName

inline def nameOfTag[T: Tag]: String = summon[Tag[T]].tag.longNameInternalSymbol

inline def companionNameOf0K[T: Tag]: String = companionNameOf(summon[Tag[T]].closestClass.getName)

inline def companionNameOf1K[T: Tag]: String = 
  val t = summon[Tag[T]]
  if (t.tag.typeArgs.isEmpty) throw IllegalTypeParameterException(s"${t.toString} is 0-kinded.")
  val underlyingName = t.tag.typeArgs.head.longNameInternalSymbol
  val underlyingShortName = t.tag.typeArgs.head.shortName
  deriveCompanionName(underlyingName.replace(s".$underlyingShortName", underlyingShortName))
