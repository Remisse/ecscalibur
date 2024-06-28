package ecscalibur.util

private[util] inline def deriveCompanionName(clsName: String): String =
  s"${clsName}$$"

inline def companionNameOf(cls: Class[?]): String = deriveCompanionName(cls.getName)
inline def companionNameOf(clsName: String): String = deriveCompanionName(clsName)

import scala.reflect.ClassTag
import izumi.reflect.Tag

inline def nameOf[T: ClassTag]: String = summon[ClassTag[T]].runtimeClass.getName

inline def nameOfTag[T: Tag]: String = summon[Tag[T]].tag.longNameInternalSymbol

inline def companionNameOf[T: Tag]: String = companionNameOf(summon[Tag[T]].closestClass.getName)

inline def companionNameOf1K[T: Tag]: String = 
  val underlyingName = summon[Tag[T]].tag.typeArgs.head.longNameInternalSymbol
  val underlyingShortName = summon[Tag[T]].tag.typeArgs.head.shortName
  deriveCompanionName(underlyingName.replace(s".$underlyingShortName", underlyingShortName))
