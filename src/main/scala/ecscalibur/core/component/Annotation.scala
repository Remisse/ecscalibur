package ecscalibur.core.component

object annotations:
  import scala.annotation.MacroAnnotation
  import scala.quoted.*
  import ecscalibur.core

  import tpe.createId

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
          Symbol.newVal(cls, idSym.name, idSym.info, Flags.Override | Flags.Protected, Symbol.noSymbol)
        ValDef(idOverrideSym, Some(rhs))

      definition match
        case ClassDef(name, ctr, parents, selfOpt, body) =>
          val cls = definition.symbol
          val id = createId(cls.fullName)
          val newRhs = Literal(IntConstant(id))
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

          report.info(s"${cls.fullName} id: $id")
          List(newClsDef, newCompClsDef)
        case _ =>
          report.error("Annotation only supports classes.")
          List(definition)
