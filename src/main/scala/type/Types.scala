package ecscalibur.`type`

object Types:
    import scala.reflect.ClassTag

    inline def t[T](using tag: ClassTag[T]): ClassTag[T] = tag
