package ecscalibur.core

import ecscalibur.core.archetype.ArchetypeManager
import ecscalibur.core.archetype.Signature
import ecscalibur.core.components.*
import ecscalibur.util.tpe.*
import ecsutil.CSeq.*

import scala.reflect.ClassTag

export queries.*

object queries:
  /** Queries allow users to iterate on a subset of the Components of every Entity stored in a
    * [[World]], read or update their values and perform structural changes to the World's state.
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

  private[core] inline def make(inline q: () => Unit): Query = Query(q)

  /** Factory method for [[Query]]. Needed when overriding [[System.process]].
    *
    * @return
    *   a new Query
    */
  def query(using World): QueryBuilder = new QueryBuilderImpl(summon[World].archetypeManager)

import ecscalibur.core.queries.Query
import ecsutil.CSeq

/** Builder for [[Query]].
  */
private[ecscalibur] trait QueryBuilder:
  /** Excludes all the Components with the given types from the final Query. Entities with at least
    * one of the specified Components will not be selected.
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
  infix def on[C0 <: Component: ClassTag](f: (Entity, C0) => Unit): Query

  /** Iterates on all Entities with Components that are instances of the 2 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def on[C0 <: Component: ClassTag, C1 <: Component: ClassTag](
      f: (Entity, C0, C1) => Unit
  ): Query

  /** Iterates on all Entities with Components that are instances of the 3 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def on[C0 <: Component: ClassTag, C1 <: Component: ClassTag, C2 <: Component: ClassTag](
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
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag,
      C3 <: Component: ClassTag
  ](f: (Entity, C0, C1, C2, C3) => Unit): Query

  /** Iterates on all Entities with Components that are instances of the 5 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def on[
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag,
      C3 <: Component: ClassTag,
      C4 <: Component: ClassTag
  ](f: (Entity, C0, C1, C2, C3, C4) => Unit): Query

  /** Iterates on all Entities with Components that are instances of the 6 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def on[
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag,
      C3 <: Component: ClassTag,
      C4 <: Component: ClassTag,
      C5 <: Component: ClassTag
  ](f: (Entity, C0, C1, C2, C3, C4, C5) => Unit): Query

  /** Iterates on all Entities with Components that are instances of the 7 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def on[
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag,
      C3 <: Component: ClassTag,
      C4 <: Component: ClassTag,
      C5 <: Component: ClassTag,
      C6 <: Component: ClassTag
  ](f: (Entity, C0, C1, C2, C3, C4, C5, C6) => Unit): Query

private[ecscalibur] object QueryBuilder:
  def apply(am: ArchetypeManager): QueryBuilder = new QueryBuilderImpl(am)

private final class QueryBuilderImpl(am: ArchetypeManager) extends QueryBuilder:
  private var selected: Signature = Signature.Nil
  private var _none: Signature = Signature.Nil
  private var _any: Signature = Signature.Nil

  private var idCache: CSeq[ComponentId] = CSeq.empty

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
      am.iterate(matches): (e, _) =>
        f(e)

  private inline def initSignature(cache: CSeq[ComponentId]): Unit =
    selected = Signature(cache)

  override infix def on[C0 <: Component: ClassTag](f: (Entity, C0) => Unit): Query =
    idCache = CSeq(id0K[C0])
    initSignature(idCache)
    queries.make(() =>
      am.iterate(matches): (e, components) =>
        f(e, components.findOfType[C0])
    )

  override infix def on[C0 <: Component: ClassTag, C1 <: Component: ClassTag](
      f: (Entity, C0, C1) => Unit
  ): Query =
    idCache = CSeq(id0K[C0], id0K[C1])
    initSignature(idCache)
    queries.make(() =>
      am.iterate(matches): (e, components) =>
        f(
          e,
          components.findOfType[C0],
          components.findOfType[C1]
        )
    )

  override infix def on[
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag
  ](
      f: (Entity, C0, C1, C2) => Unit
  ): Query =
    idCache = CSeq(id0K[C0], id0K[C1], id0K[C2])
    initSignature(idCache)
    queries.make(() =>
      am.iterate(matches): (e, components) =>
        f(
          e,
          components.findOfType[C0],
          components.findOfType[C1],
          components.findOfType[C2]
        )
    )

  override infix def on[
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag,
      C3 <: Component: ClassTag
  ](f: (Entity, C0, C1, C2, C3) => Unit): Query =
    idCache = CSeq(id0K[C0], id0K[C1], id0K[C2], id0K[C3])
    initSignature(idCache)
    queries.make(() =>
      am.iterate(matches): (e, components) =>
        f(
          e,
          components.findOfType[C0],
          components.findOfType[C1],
          components.findOfType[C2],
          components.findOfType[C3]
        )
    )

  override infix def on[
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag,
      C3 <: Component: ClassTag,
      C4 <: Component: ClassTag
  ](f: (Entity, C0, C1, C2, C3, C4) => Unit): Query =
    idCache = CSeq(id0K[C0], id0K[C1], id0K[C2], id0K[C3], id0K[C4])
    initSignature(idCache)
    queries.make(() =>
      am.iterate(matches): (e, components) =>
        f(
          e,
          components.findOfType[C0],
          components.findOfType[C1],
          components.findOfType[C2],
          components.findOfType[C3],
          components.findOfType[C4]
        )
    )

  override infix def on[
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag,
      C3 <: Component: ClassTag,
      C4 <: Component: ClassTag,
      C5 <: Component: ClassTag
  ](f: (Entity, C0, C1, C2, C3, C4, C5) => Unit): Query =
    idCache = CSeq(id0K[C0], id0K[C1], id0K[C2], id0K[C3], id0K[C4], id0K[C5])
    initSignature(idCache)
    queries.make(() =>
      am.iterate(matches): (e, components) =>
        f(
          e,
          components.findOfType[C0],
          components.findOfType[C1],
          components.findOfType[C2],
          components.findOfType[C3],
          components.findOfType[C4],
          components.findOfType[C5]
        )
    )

  override infix def on[
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag,
      C3 <: Component: ClassTag,
      C4 <: Component: ClassTag,
      C5 <: Component: ClassTag,
      C6 <: Component: ClassTag
  ](f: (Entity, C0, C1, C2, C3, C4, C5, C6) => Unit): Query =
    idCache = CSeq(id0K[C0], id0K[C1], id0K[C2], id0K[C3], id0K[C4], id0K[C5], id0K[C6])
    initSignature(idCache)
    queries.make(() =>
      am.iterate(matches): (e, components) =>
        f(
          e,
          components.findOfType[C0],
          components.findOfType[C1],
          components.findOfType[C2],
          components.findOfType[C3],
          components.findOfType[C4],
          components.findOfType[C5],
          components.findOfType[C6]
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
