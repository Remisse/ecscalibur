package ecscalibur.annotation

import scala.annotation.MacroAnnotation
import scala.quoted.*
import ecscalibur.core
import scala.annotation.experimental

class component extends MacroAnnotation:
  def transform(using Quotes)(
      definition: quotes.reflect.Definition,
      companion: Option[quotes.reflect.Definition]
  ): List[quotes.reflect.Definition] =
    import quotes.reflect.*
    definition match
      case ClassDef(name, ctr, parents, selfOpt, body) =>
        import scala.util.hashing.MurmurHash3
        import core.Components.Component

        def recreateIdField(cls: Symbol, rhs: Term)(using Quotes): ValDef =
          val idSym = cls.fieldMember("id")
          val idOverrideSym = Symbol.newVal(cls, "id", idSym.info, Flags.Override, Symbol.noSymbol)
          ValDef(idOverrideSym, Some(rhs))

        val cls = definition.symbol
        val newRhs = Literal(IntConstant(MurmurHash3.stringHash(cls.typeRef.toString())))
        cls.typeRef.asType match
          case '[Component] => ()
          case _            => report.error(s"$name does not implement the Component trait.")
        val newClsDef = ClassDef(cls, parents, recreateIdField(cls, newRhs) :: body)

        if companion.isEmpty then
          report.error(s"$name should define a companion object extending Companion.")
        val compSym = companion.head.symbol
        compSym.typeRef.asType match
          case '[Component] => ()
          case _ =>
            report.error(s"Companion object ${compSym.name} does not implement the Component trait.")
        val compClsDef = companion.head match
          case ClassDef(name, ctr, parents, selfOpt, body) =>
            ClassDef(compSym, parents, recreateIdField(compSym, newRhs) :: body)
          case _ => report.errorAndAbort("impossible")

        List(newClsDef, compClsDef)
      case _ =>
        report.error("Annotation only supports classes.")
        List(definition)
