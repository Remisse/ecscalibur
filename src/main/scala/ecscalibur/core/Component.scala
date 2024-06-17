package ecscalibur.core

import ecscalibur.exception.MissingAnnotationException

object Components:
  /** Type representing unique component IDs.
    */
  type ComponentId = Int
  val nil: ComponentId = -1

  trait Component extends ComponentInternal:
    inline infix def isA(tpe: ComponentType): Boolean = id == tpe.id

  trait ComponentType extends ComponentInternal:
    override def equals(other: Any): Boolean = other match
      case o: ComponentType => id == o.id
      case _                => false

  private[Components] trait ComponentInternal:
    val _id: ComponentId = nil

    inline def id = if _id != nil then _id
    else throw MissingAnnotationException(s"$getClass must be annotated with @component.")
    inline def unary_~ = id

  object TypeOrdering:
    given Ordering[ComponentType] with
      def compare(x: ComponentType, y: ComponentType): Int = ~x - ~y
  export TypeOrdering.given_Ordering_ComponentType

  object Annotations:
    import scala.annotation.MacroAnnotation
    import scala.quoted.*
    import ecscalibur.core
    import ecscalibur.id.IdGenerator
    import java.util.concurrent.atomic.AtomicReference

    private[Annotations] val idGenerator = AtomicReference(IdGenerator())

    /** Assigns a unique type ID to classes extending [[ecscalibur.core.Components.Component]]
      */
    class component extends MacroAnnotation:
      def transform(using Quotes)(
          definition: quotes.reflect.Definition,
          companion: Option[quotes.reflect.Definition]
      ): List[quotes.reflect.Definition] =
        import quotes.reflect.*
        definition match
          case ClassDef(name, ctr, parents, selfOpt, body) =>
            import core.Components.Component

            def ensureExtends[T](cls: Symbol)(using Quotes, Type[T]): Unit =
              cls.typeRef.asType match
                case '[T] => ()
                case _ =>
                  report.error(
                    s"${cls.toString()} does not implement the ${TypeRepr.of[T].show} trait."
                  )

            def recreateIdField(cls: Symbol, rhs: Term)(using Quotes): ValDef =
              val fieldName = "_id"
              val idSym = cls.fieldMember(fieldName)
              val idOverrideSym =
                Symbol.newVal(cls, fieldName, idSym.info, Flags.Override, Symbol.noSymbol)
              ValDef(idOverrideSym, Some(rhs))

            val newRhs = Literal(IntConstant(idGenerator.getAcquire().next))
            val cls = definition.symbol
            ensureExtends[Component](cls)
            val newClsDef = ClassDef.copy(definition)(name, ctr, parents, selfOpt, recreateIdField(cls, newRhs) :: body)

            if companion.isEmpty then report.error(s"$name should define a companion object.")
            val compCls = companion.head.symbol
            ensureExtends[ComponentType](compCls)
            val newCompClsDef = companion.head match
              case ClassDef(name, ctr, parents, selfOpt, body) =>
                ClassDef.copy(companion.head)(name, ctr, parents, selfOpt, recreateIdField(compCls, newRhs) :: body)
              case _ => report.errorAndAbort("impossible")

            List(newClsDef, newCompClsDef)
          case _ =>
            report.error("Annotation only supports classes.")
            List(definition)
