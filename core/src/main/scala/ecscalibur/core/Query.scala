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
  /** Queries allow users to iterate on a subset of the Components of every Entity stored in a
    * [[World]], read or update their values and perform structural changes to the World's state
    * through a [[Mutator]].
    *
    * They make up the logic of all [[System]]s.
    *
    * Queries have to be built through Systems: users must create either a class extending the
    * [[System]] trait and override [[System.process]] by calling the [[query]] factory method, or a
    * simple System through the [[World.withSystem]] method. A Query without a System cannot exist.
    */
  final case class Query private[queries] (val query: () => Unit):
    inline def apply(): Unit = query()

  object Query:
    /** Makes an empty Query. Does nothing when executed.
      *
      * @return
      *   an empty Query
      */
    def None: Query = Query(() => ())

  private[core] inline def make(q: () => Unit): Query = Query(q)

  /** Factory method for [[Query]]. Needed when overriding [[System.process]].
    *
    * @return
    *   a new Query
    */
  def query(using ArchetypeManager, MetaContext, Mutator): QueryBuilder = new QueryBuilderImpl(
    summon[ArchetypeManager]
  )

import ecscalibur.core.queries.Query

/** Builder for [[Query]].
  */
trait QueryBuilder:
  /** @return
    *   a reference to the current World's [[MetaContext]].
    */
  given context: MetaContext

  /** @return
    *   a reference to the current World's [[Mutator]].
    */
  given mutator: Mutator

  /** Excludes all the Components with the given types from the final Query. Entities with at least
    * one of such Components will not be selected.
    *
    * @param types
    *   the types of Components to exclude
    * @return
    *   this QueryBuilder instance
    */
  infix def except(types: ComponentType*): QueryBuilder

  /** Includes all entities with at least one Component with the given types.
    *
    * @param types
    *   the types of Components to include
    * @return
    *   this QueryBuilder instance
    */
  infix def any(types: ComponentType*): QueryBuilder

  /** Builds a Query that executes only once per World loop. No Entities will be selected. Previous
    * calls to [[QueryBuilder.any]] or [[QueryBuilder.except]] will be ignored.
    *
    * @param f
    *   the logic of this Query
    * @return
    *   a new Query
    */
  infix def routine(f: () => Unit): Query

  /** Iterates on every Entity in the World without selecting any Components.
    *
    * @param f
    *   the logic of this Query
    * @return
    *   a new Query
    */
  infix def on(f: Entity => Unit): Query

  /** Iterates on all Entities with a Component that is instance of the given type parameter.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def on[C0 <: Component: Tag](f: (Entity, C0) => Unit): Query

  /** Iterates on all Entities with Components that are instances of the 2 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def on[C0 <: Component: Tag, C1 <: Component: Tag](f: (Entity, C0, C1) => Unit): Query

  /** Iterates on all Entities with Components that are instances of the 3 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def on[C0 <: Component: Tag, C1 <: Component: Tag, C2 <: Component: Tag](
      f: (Entity, C0, C1, C2) => Unit
  ): Query

  /** Iterates on all Entities with Components that are instances of the 4 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def on[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3) => Unit): Query

  /** Iterates on all Entities with Components that are instances of the 5 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def on[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4) => Unit): Query

  /** Iterates on all Entities with Components that are instances of the 6 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def on[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag,
      C5 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4, C5) => Unit): Query

  /** Iterates on all Entities with Components that are instances of the 7 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
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
      am.iterate(matches, selected): (e, _, _) =>
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
