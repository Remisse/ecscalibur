package ecscalibur.core
import ecscalibur.error.MissingAnnotationError
import scala.annotation.targetName

/** Type representing unique component IDs.
  */
type ComponentId = Int

sealed trait WithType:
  protected val _typeId: ComponentId = ComponentType.nil

  inline def typeId: ComponentId = if _typeId != ComponentType.nil then _typeId
  else throw MissingAnnotationError(s"$getClass must be annotated with @component.")

  /** Equivalent to 'typeId'.
    *
    * @return
    *   the type ID of this component.
    */
  @targetName("typeId")
  inline def unary_~ : ComponentId = typeId

trait Component extends WithType:
  inline infix def isA(compType: ComponentType): Boolean = typeId == compType.typeId

trait ComponentType extends WithType:
  override def equals(other: Any): Boolean = other match
    case o: ComponentType => typeId == o.typeId
    case _                => false

object ComponentType:
  val nil: ComponentId = -1

object CSeqs:
  import scala.reflect.ClassTag

  opaque type CSeq = Array[Component]
  inline def <<[T <: Component: ClassTag](using l: CSeq): T = l.get[T]

  object CSeq:
    def apply(comps: Component*): CSeq = comps.toArray
    def apply(comps: Array[Component]): CSeq = comps
    def apply(comps: Iterable[Component]): CSeq = comps.toArray

  extension (l: CSeq)
    inline def underlying: Array[Component] = l
    inline def toTypes: Array[ComponentId] = l.map(_.typeId)
    inline def get[T <: Component: ClassTag]: T =
      val idx = l.indexWhere:
        case t: T => true
        case _    => false
      if idx == -1 then
        throw IllegalArgumentException(s"No component of class ${summon[ClassTag[T]]} found.")
      l(idx).asInstanceOf[T]

export TypeOrdering.given
object TypeOrdering:
  given Ordering[ComponentType] with
    override def compare(t1: ComponentType, t2: ComponentType): Int = ~t1 - ~t2

object Annotations:
  import scala.annotation.MacroAnnotation
  import scala.quoted.*
  import ecscalibur.core
  import ecscalibur.id.IdGenerator
  import java.util.concurrent.atomic.AtomicReference

  private[Annotations] val idGenerator = AtomicReference(IdGenerator())

  /** Assigns a unique type ID to classes extending [[Component]].
    */
  final class component extends MacroAnnotation:
    def transform(using Quotes)(
        definition: quotes.reflect.Definition,
        companion: Option[quotes.reflect.Definition]
    ): List[quotes.reflect.Definition] =
      import quotes.reflect.*

      def ensureExtends[T](cls: Symbol)(using Quotes, Type[T]): Unit =
        cls.typeRef.asType match
          case '[T] => ()
          case _    => report.error(s"${cls.toString} must extend ${TypeRepr.of[T].show}.")

      def recreateIdField(cls: Symbol, rhs: Term)(using Quotes): ValDef =
        val fieldName = "_typeId"
        // Works as long as this field is non-private (even protected is fine).
        val idSym = cls.fieldMember(fieldName)
        val idOverrideSym =
          Symbol.newVal(cls, fieldName, idSym.info, Flags.Override, Symbol.noSymbol)
        ValDef(idOverrideSym, Some(rhs))

      definition match
        case ClassDef(name, ctr, parents, selfOpt, body) =>
          val newRhs = Literal(IntConstant(idGenerator.getAcquire().next))
          val cls = definition.symbol
          ensureExtends[Component](cls)
          val newClsDef = ClassDef.copy(definition)(
            name,
            ctr,
            parents,
            selfOpt,
            recreateIdField(cls, newRhs) :: body
          )

          val newCompClsDef = companion match
            case None => report.errorAndAbort(s"$name should define a companion object.")
            case Some(companionDef) =>
              val compCls = companionDef.symbol
              ensureExtends[ComponentType](compCls)
              companionDef match
                case ClassDef(name, ctr, parents, selfOpt, body) =>
                  ClassDef.copy(companionDef)(
                    name,
                    ctr,
                    parents,
                    selfOpt,
                    recreateIdField(compCls, newRhs) :: body
                  )
                case _ => report.errorAndAbort("impossible")

          List(newClsDef, newCompClsDef)
        case _ =>
          report.error("Annotation only supports classes.")
          List(definition)
