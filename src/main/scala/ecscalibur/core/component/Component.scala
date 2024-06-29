package ecscalibur.core.component

import ecscalibur.error.IllegalDefinitionException
import izumi.reflect.Tag
import ecscalibur.util.companionNameOf
import ecscalibur.error.IllegalTypeParameterException

/** Type representing unique component IDs.
  */
type ComponentId = Int

sealed trait WithType:
  def typeId: ComponentId

  /** Equivalent to 'typeId'.
    *
    * @return
    *   the type ID of this component.
    */
  final inline def unary_~ = typeId

trait Component(using companion: ComponentType) extends WithType:
  private var verified = false

  override def typeId: ComponentId =
    if (!verified)
      val expectedCompanionName = companionNameOf(getClass)
      if (expectedCompanionName != companion.getClass.getName)
        throw IllegalDefinitionException(
          s"${getClass.getName}: expected given companion of class $expectedCompanionName, got ${companion.getClass.getName}."
        )
      verified = true
    companion.typeId

  final inline infix def isA(compType: ComponentType): Boolean = typeId == compType.typeId

trait ComponentType extends WithType:
  private val _typeId: ComponentId = CompIdFactory.generateId(getClass)

  final override def typeId: ComponentId = _typeId

  final override def equals(other: Any): Boolean = other match
    case o: ComponentType => typeId == o.typeId
    case _                => false

private[component] object CompIdFactory:
  import ecscalibur.id.IdGenerator
  private val idGen = IdGenerator()
  private var idsByClass = Map.empty[String, ComponentId]

  inline def generateId(cls: Class[?]): ComponentId = generateId(cls.getName)

  inline def generateId(clsName: String): ComponentId =
    if (idsByClass.contains(clsName)) idsByClass(clsName)
    else
      val res = idGen.next
      idsByClass = idsByClass + (clsName-> res)
      res

object ComponentType:
  val Nil: ComponentId = -1

export TypeOrdering.given
object TypeOrdering:
  given Ordering[ComponentType] with
    override def compare(t1: ComponentType, t2: ComponentType): Int = t1.typeId - t2.typeId

import ecscalibur.util.{companionNameOf0K, companionNameOf1K}

inline def id0K[T <: Component: Tag] = CompIdFactory.generateId(companionNameOf0K[T])
inline def idRw[T <: Component: Tag]: ComponentId =
  val t = summon[Tag[T]]
  val id0k = id0K[T]
  if (t.tag.typeArgs.isEmpty) id0k
  else if (id0k == ~Rw) CompIdFactory.generateId(companionNameOf1K[T])
  else throw IllegalTypeParameterException(s"Used 1-kinded component ${t.closestClass}, but only Rw[T] is allowed to be used.")
