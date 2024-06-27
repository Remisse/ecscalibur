package ecscalibur.core.component

import ecscalibur.error.IllegalDefinitionException

/** Type representing unique component IDs.
  */
type ComponentId = Int

trait WithType:
  def typeId: ComponentId

  /** Equivalent to 'typeId'.
    *
    * @return
    *   the type ID of this component.
    */
  final inline def unary_~ = typeId

trait Component(using companion: ComponentType) extends WithType:
  private var verified = false

  final override def typeId: ComponentId =
    if !verified then verifyGiven
    companion.typeId

  final inline infix def isA(compType: ComponentType): Boolean = typeId == compType.typeId

  import ecscalibur.util.companionNameOf
  private inline def verifyGiven =
    if (!CompIdFactory.contains(companionNameOf(getClass)))
      throw IllegalDefinitionException(
        s"${getClass.getName} either does not define a companion object or has passed a different one as given to the constructor of Component."
      )
    verified = true

private[component] object CompIdFactory:
  import ecscalibur.id.IdGenerator
  private val idGen = IdGenerator()
  private var idsByClass = Map.empty[String, ComponentId]

  inline def generateId(cls: Class[? <: ComponentType]): ComponentId =
    val className = cls.getName
    if (idsByClass.contains(className)) throw IllegalStateException()
    val res = idGen.next
    idsByClass = idsByClass + (className -> res)
    res

  inline def contains(className: String) = idsByClass.contains(className)
  inline def getCached(className: String): ComponentId = idsByClass(className)

trait ComponentType extends WithType:
  private val _typeId: ComponentId = CompIdFactory.generateId(getClass)

  final override def typeId: ComponentId = _typeId

  final override def equals(other: Any): Boolean = other match
    case o: ComponentType => typeId == o.typeId
    case _                => false

object ComponentType:
  val Nil: ComponentId = -1

export TypeOrdering.given
object TypeOrdering:
  given Ordering[ComponentType] with
    override def compare(t1: ComponentType, t2: ComponentType): Int = t1.typeId - t2.typeId
