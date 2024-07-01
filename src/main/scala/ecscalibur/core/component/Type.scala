package ecscalibur.core.component

import ecscalibur.id.IdGenerator
import izumi.reflect.Tag
import ecscalibur.error.IllegalTypeParameterException
import ecscalibur.util.base

object tpe:
  def getId(cls: Class[?]): ComponentId =
    TypeIdGen.generate(base(cls.getName))

  def getId(clsName: String): ComponentId =
    TypeIdGen.generate(base(clsName))

  private object TypeIdGen:
    private val generator = IdGenerator()
    private var cache: Map[String, ComponentId] = Map.empty

    inline def generate(tpeName: String) =
      cache getOrElse (tpeName, {
        val id = generator.next
        cache = cache + (tpeName -> id)
        id
      })

  inline def id0K[T <: WithType: Tag] = getId(summon[Tag[T]].closestClass)
  inline def idRw[T <: WithType: Tag]: ComponentId =
    val t = summon[Tag[T]]
    val id0k = id0K[T]
    if (t.tag.typeArgs.isEmpty) id0k
    else if (id0k == ~Rw && t.tag.typeArgs.head.typeArgs.isEmpty)
      getId(base(t.tag.typeArgs.head.longNameInternalSymbol))
    else
      throw IllegalTypeParameterException(
        s"Used 1- or higher-kinded component $t while only Rw[T] is allowed, where T is a 0-kinded type."
      )
