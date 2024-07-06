package ecscalibur.core

import ecscalibur.core.Entity
import ecscalibur.core.archetype.ArchetypeManager
import ecscalibur.core.archetype.Signature
import ecscalibur.core.archetype.archetypes.Archetype
import ecscalibur.core.component.Component
import ecscalibur.core.component.ComponentId
import ecscalibur.core.component.ComponentType
import ecscalibur.core.component.tpe._
import ecscalibur.core.context.MetaContext
import ecscalibur.util.array._
import izumi.reflect.Tag

import CSeq._

object queries:
  final case class Query(val query: () => Unit):
    inline def apply(): Unit = query()

  object Query:
    def None: Query = Query(() => ())

  private[core] inline def make(q: () => Unit): Query = Query(q)

  def query(using ArchetypeManager, MetaContext, Mutator): QueryBuilder = new QueryBuilderImpl(
    summon[ArchetypeManager]
  )

import ecscalibur.core.queries.Query

trait QueryBuilder:
  given context: MetaContext
  given mutator: Mutator

  infix def except(types: ComponentType*): QueryBuilder

  infix def any(types: ComponentType*): QueryBuilder

  infix def routine(f: () => Unit): Query

  infix def on(f: Entity => Unit): Query
  infix def on[C0 <: Component: Tag](f: (Entity, C0) => Unit): Query
  infix def on[C0 <: Component: Tag, C1 <: Component: Tag](f: (Entity, C0, C1) => Unit): Query
  infix def on[C0 <: Component: Tag, C1 <: Component: Tag, C2 <: Component: Tag](
      f: (Entity, C0, C1, C2) => Unit
  ): Query
  infix def on[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3) => Unit): Query
  infix def on[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4) => Unit): Query
  infix def on[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag,
      C5 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4, C5) => Unit): Query
  infix def on[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag,
      C5 <: Component: Tag,
      C6 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4, C5, C6) => Unit): Query

private final class QueryBuilderImpl(am: ArchetypeManager)(using MetaContext, Mutator)
    extends QueryBuilder:
  override given context: MetaContext = summon[MetaContext]
  override given mutator: Mutator = summon[Mutator]

  private var selected: Signature = Signature.Nil
  private var _none: Signature = Signature.Nil
  private var _any: Signature = Signature.Nil

  private var id0kCache: CSeq[ComponentId] = CSeq.empty
  private var idRwCache: CSeq[ComponentId] = CSeq.empty

  private inline def multipleCallsErrorMsg(methodName: String) =
    s"Called '$methodName' multiple times."

  override infix def except(types: ComponentType*): QueryBuilder =
    ensureFirstCallToNone()
    _none = Signature(types*)
    this

  override infix def any(types: ComponentType*): QueryBuilder =
    ensureFirstCallToAny()
    _any = Signature(types*)
    this

  override infix def routine(f: () => Unit): Query =
    queries.make:
      f

  override infix def on(f: Entity => Unit): Query =
    queries.make: () =>
      am.iterate(matches, selected): (e, components, arch) =>
        f(e)

  private inline def initSignatureAndId0kCache(): Unit =
    id0kCache = CSeq.fill[ComponentId](idRwCache.length)(ComponentId.Nil)
    selected = Signature(idRwCache.toArray)

  override infix def on[C0 <: Component: Tag](f: (Entity, C0) => Unit): Query =
    idRwCache = CSeq(idRw[C0])
    initSignatureAndId0kCache()
    queries.make(() =>
      am.iterate(matches, selected): (e, components, arch) =>
        f(e, findOfType[C0](0)(components, arch, e))
    )

  override infix def on[C0 <: Component: Tag, C1 <: Component: Tag](
      f: (Entity, C0, C1) => Unit
  ): Query =
    idRwCache = CSeq(idRw[C0], idRw[C1])
    initSignatureAndId0kCache()
    queries.make(() =>
      am.iterate(matches, selected): (e, components, arch) =>
        f(
          e,
          findOfType[C0](0)(components, arch, e),
          findOfType[C1](1)(components, arch, e)
        )
    )

  override infix def on[C0 <: Component: Tag, C1 <: Component: Tag, C2 <: Component: Tag](
      f: (Entity, C0, C1, C2) => Unit
  ): Query =
    idRwCache = CSeq(idRw[C0], idRw[C1], idRw[C2])
    initSignatureAndId0kCache()
    queries.make(() =>
      am.iterate(matches, selected): (e, components, arch) =>
        f(
          e,
          findOfType[C0](0)(components, arch, e),
          findOfType[C1](1)(components, arch, e),
          findOfType[C2](2)(components, arch, e)
        )
    )

  override infix def on[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3) => Unit): Query =
    idRwCache = CSeq(idRw[C0], idRw[C1], idRw[C2], idRw[C3])
    initSignatureAndId0kCache()
    queries.make(() =>
      am.iterate(matches, selected): (e, components, arch) =>
        f(
          e,
          findOfType[C0](0)(components, arch, e),
          findOfType[C1](1)(components, arch, e),
          findOfType[C2](2)(components, arch, e),
          findOfType[C3](3)(components, arch, e)
        )
    )

  override infix def on[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4) => Unit): Query =
    idRwCache = CSeq(idRw[C0], idRw[C1], idRw[C2], idRw[C3], idRw[C4])
    initSignatureAndId0kCache()
    queries.make(() =>
      am.iterate(matches, selected): (e, components, arch) =>
        f(
          e,
          findOfType[C0](0)(components, arch, e),
          findOfType[C1](1)(components, arch, e),
          findOfType[C2](2)(components, arch, e),
          findOfType[C3](3)(components, arch, e),
          findOfType[C4](4)(components, arch, e)
        )
    )

  override infix def on[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag,
      C5 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4, C5) => Unit): Query =
    idRwCache = CSeq(idRw[C0], idRw[C1], idRw[C2], idRw[C3], idRw[C4], idRw[C5])
    initSignatureAndId0kCache()
    queries.make(() =>
      am.iterate(matches, selected): (e, components, arch) =>
        f(
          e,
          findOfType[C0](0)(components, arch, e),
          findOfType[C1](1)(components, arch, e),
          findOfType[C2](2)(components, arch, e),
          findOfType[C3](3)(components, arch, e),
          findOfType[C4](4)(components, arch, e),
          findOfType[C5](5)(components, arch, e)
        )
    )

  override infix def on[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag,
      C5 <: Component: Tag,
      C6 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4, C5, C6) => Unit): Query =
    idRwCache = CSeq(idRw[C0], idRw[C1], idRw[C2], idRw[C3], idRw[C4], idRw[C5], idRw[C6])
    initSignatureAndId0kCache()
    queries.make(() =>
      am.iterate(matches, selected): (e, components, arch) =>
        f(
          e,
          findOfType[C0](0)(components, arch, e),
          findOfType[C1](1)(components, arch, e),
          findOfType[C2](2)(components, arch, e),
          findOfType[C3](3)(components, arch, e),
          findOfType[C4](4)(components, arch, e),
          findOfType[C5](5)(components, arch, e),
          findOfType[C6](6)(components, arch, e)
        )
    )

  private inline def matches(s: Signature): Boolean =
    (selected.isNil || s.containsAll(selected)) &&
      (_none.isNil || !s.containsAny(_none)) &&
      (_any.isNil || s.containsAny(_any))

  private inline def ensureFirstCallToNone(): Unit =
    require(_none.isNil, multipleCallsErrorMsg("none"))

  private inline def ensureFirstCallToAny(): Unit =
    require(_any.isNil, multipleCallsErrorMsg("any"))

  private inline def findOfType[C <: Component: Tag](
      index: Int
  )(components: CSeq[Component], arch: Archetype, e: Entity): C =
    val c = components.findUnsafe(_.typeId == idRwCache(index))
    if (id0kCache(index) == ComponentId.Nil) id0kCache(index) = id0K[C]
    (if id0kCache(index) == ~Rw then Rw(c)(arch, e) else c).asInstanceOf[C]
