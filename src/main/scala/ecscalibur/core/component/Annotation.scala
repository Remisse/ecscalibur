package ecscalibur.core.component

object annotations:
  import scala.annotation.MacroAnnotation
  import scala.quoted.*
  import ecscalibur.core

  import tpe.getId

  /** Assigns a unique type ID to classes extending [[Component]].
    */
  final class component extends MacroAnnotation:
    override def transform(using Quotes)(
        definition: quotes.reflect.Definition,
        companion: Option[quotes.reflect.Definition]
    ): List[quotes.reflect.Definition] =
      import quotes.reflect.*

      def ensureExtends[T](cls: Symbol)(using Quotes, Type[T]): Unit =
        cls.typeRef.asType match
          case '[T] => ()
          case _    => report.error(s"${cls.toString} must extend ${TypeRepr.of[T].show}.")

      def recreateField(fieldName: String, cls: Symbol, rhs: Term)(using Quotes): ValDef =
        // Works as long as this field is non-private (even protected is fine).
        val idSym = cls.fieldMember(fieldName)
        val idOverrideSym =
          Symbol.newVal(
            cls,
            idSym.name,
            idSym.info,
            Flags.Override | Flags.Protected,
            Symbol.noSymbol
          )
        ValDef(idOverrideSym, Some(rhs))

      val newRhs = Literal(IntConstant(getId(definition.symbol.fullName)))

      def newClassDefinitionWithOverriddenField[typeToExtend](toCopy: Definition)(using
          Type[typeToExtend]
      ): ClassDef =
        toCopy match
          case ClassDef(name, ctr, parents, selfOpt, body) =>
            val cls = toCopy.symbol
            ensureExtends[typeToExtend](cls)
            ClassDef.copy(toCopy)(
              name,
              ctr,
              parents,
              selfOpt,
              recreateField("_typeId", cls, newRhs) :: body
            )
          case _ => report.errorAndAbort("This annotation only works on classes.")

      companion match
        case None => report.errorAndAbort("This class should define a companion object.")
        case _    => ()

      List(
        newClassDefinitionWithOverriddenField[Component](definition),
        newClassDefinitionWithOverriddenField[ComponentType](companion.head)
      )
