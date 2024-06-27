package ecscalibur.util

inline def companionNameOf(cls: Class[?]): String = s"${cls.getName.replace("class ", "")}$$"
