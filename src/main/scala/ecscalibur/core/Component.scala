package ecscalibur.core

object Components:
  /** Type representing unique component IDs.
    */
  type ComponentId = Int
  val noId: ComponentId = -1

  trait Component:
    val _id: ComponentId = noId
    inline def id = if _id != noId then _id
    else throw IllegalStateException(s"$getClass must be annotated with @component.")
    inline def unary_~ = id

  object Annotations:
    import scala.annotation.MacroAnnotation
    import scala.quoted.*
    import ecscalibur.core
    import ecscalibur.id.IdGenerator
    import java.util.concurrent.atomic.AtomicReference

    private[Annotations] val idGenerator = AtomicReference(IdGenerator())

    class component extends MacroAnnotation:
      def transform(using Quotes)(
          definition: quotes.reflect.Definition,
          companion: Option[quotes.reflect.Definition]
      ): List[quotes.reflect.Definition] =
        import quotes.reflect.*
        definition match
          case ClassDef(name, ctr, parents, selfOpt, body) =>
            import core.Components.Component

            def ensureClassExtendsComponent(cls: Symbol)(using Quotes): Unit =
              cls.typeRef.asType match
                case '[Component] => ()
                case _            => report.error(s"${cls.toString()} does not implement the Component trait.")

            def recreateIdField(cls: Symbol, rhs: Term)(using Quotes): ValDef =
              val fieldName = "_id"
              val idSym = cls.fieldMember(fieldName)
              val idOverrideSym =
                Symbol.newVal(cls, fieldName, idSym.info, Flags.Override, Symbol.noSymbol)
              ValDef(idOverrideSym, Some(rhs))

            val newRhs = Literal(IntConstant(idGenerator.getAcquire().next))

            val cls = definition.symbol
            ensureClassExtendsComponent(cls)
            val newClsDef = ClassDef(cls, parents, recreateIdField(cls, newRhs) :: body)

            if companion.isEmpty then
              report.error(s"$name should define a companion object extending Component.")
            val compCls = companion.head.symbol
            ensureClassExtendsComponent(compCls)
            val newCompClsDef = companion.head match
              case ClassDef(name, ctr, parents, selfOpt, body) =>
                ClassDef(compCls, parents, recreateIdField(compCls, newRhs) :: body)
              case _ => report.errorAndAbort("impossible")

            List(newClsDef, newCompClsDef)
          case _ =>
            report.error("Annotation only supports classes.")
            List(definition)
