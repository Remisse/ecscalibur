package ecscalibur.util

inline def base(clsName: String): String = clsName.replace(".", "").replace("$", "")
