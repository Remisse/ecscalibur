package ecscalibur.core

import ecscalibur.core.archetype.ArchetypeManager
import ecscalibur.core.archetype.Signature
import ecscalibur.core.components.*
import ecscalibur.util.tpe.*

import scala.reflect.ClassTag
import ecsutil.array.aFindOfType

export queries.*

object queries:
  /** Queries allow users to iterate on a subset of the Components of every Entity stored in a
    * [[World]], read or update their values and perform structural changes to the World's state.
    *
    * They make up the logic of all [[System]]s.
    *
    * Queries have to be built through Systems: users must create either a class extending the
    * [[System]] trait and override [[System.process]] by calling the [[query]] factory method, or a
    * simple System by calling the [[World.withSystem]] method. A Query without a System cannot
    * exist.
    */
  opaque type Query <: () => Unit = () => Unit

  /** Factory for [[Query]].
    */
  object Query:
    /** Creates a new [[Query]] with the given lambda function.
      *
      * @param q
      *   the lambda function of the new Query
      * @return
      *   a new Query instance
      */
    inline def apply(q: () => Unit): Query = q

    /** Creates an empty Query.
      *
      * @return
      *   an empty Query
      */
    def None: Query = Query(() => ())

  /** Factory method for [[Query]]. Needed when overriding [[System.process]].
    *
    * @return
    *   a new Query
    */
  def query(using World): QueryBuilder = new QueryBuilderImpl(summon[World].archetypeManager)

  /** Factory method for a [[Query]] that executes only once per World loop. No Entities will be
    * selected.
    *
    * @param f
    *   the logic of this Query
    * @return
    *   a new Query
    */
  def routine(f: => Unit): Query = Query(() => f)

import ecscalibur.core.queries.Query

/** Builder for [[Query]].
  */
private[ecscalibur] trait QueryBuilder:
  /** Excludes from the final Query all Components whose types match the given ones. Entities with
    * at least one of the specified Components will not be selected.
    *
    * @param types
    *   the types of Components to exclude
    * @return
    *   this QueryBuilder instance
    */
  infix def none(types: ComponentType*): QueryBuilder

  /** Includes in the final Query all entities with at least one Component whose type matches any of
    * the given ones.
    *
    * @param types
    *   the types of Components to include
    * @return
    *   this QueryBuilder instance
    */
  infix def any(types: ComponentType*): QueryBuilder

  /** Iterates on every Entity in the World without selecting any Components.
    *
    * @param f
    *   the logic of this Query
    * @return
    *   a new Query
    */
  infix def all(f: Entity => Unit): Query

  /** Iterates on all Entities with a Component that is instance of the given type parameter.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def all[C0 <: Component: ClassTag](f: (Entity, C0) => Unit): Query

  /** Iterates on all Entities with Components that are instances of the 2 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def all[C0 <: Component: ClassTag, C1 <: Component: ClassTag](
      f: (Entity, C0, C1) => Unit
  ): Query

  /** Iterates on all Entities with Components that are instances of the 3 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def all[C0 <: Component: ClassTag, C1 <: Component: ClassTag, C2 <: Component: ClassTag](
      f: (Entity, C0, C1, C2) => Unit
  ): Query

  /** Iterates on all Entities with Components that are instances of the 4 given type parameters.
    *
    * @param f
    *   the logic of this query
    * @return
    *   a new Query
    */
  infix def all[
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
  infix def all[
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
  infix def all[
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
  infix def all[
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

  private var idCache: Array[ComponentId] = Array.empty

  private inline def multipleCallsErrorMsg(methodName: String) =
    s"Called '$methodName' multiple times."

  override infix def none(types: ComponentType*): QueryBuilder =
    ensureFirstCallToNone()
    _none = Signature(types*)
    this

  override infix def any(types: ComponentType*): QueryBuilder =
    ensureFirstCallToAny()
    _any = Signature(types*)
    this

  override infix def all(f: Entity => Unit): Query =
    Query: () =>
      am.iterate(matches): (e, _) =>
        f(e)

  private inline def initSignature(cache: Array[ComponentId]): Unit =
    selected = Signature(cache*)

  override infix def all[C0 <: Component: ClassTag](f: (Entity, C0) => Unit): Query =
    idCache = Array(id0K[C0])
    initSignature(idCache)
    Query(() =>
      am.iterate(matches): (e, components) =>
        f(e, components.aFindOfType[C0])
    )

  override infix def all[C0 <: Component: ClassTag, C1 <: Component: ClassTag](
      f: (Entity, C0, C1) => Unit
  ): Query =
    idCache = Array(id0K[C0], id0K[C1])
    initSignature(idCache)
    Query(() =>
      am.iterate(matches): (e, components) =>
        f(
          e,
          components.aFindOfType[C0],
          components.aFindOfType[C1]
        )
    )

  override infix def all[
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag
  ](
      f: (Entity, C0, C1, C2) => Unit
  ): Query =
    idCache = Array(id0K[C0], id0K[C1], id0K[C2])
    initSignature(idCache)
    Query(() =>
      am.iterate(matches): (e, components) =>
        f(
          e,
          components.aFindOfType[C0],
          components.aFindOfType[C1],
          components.aFindOfType[C2]
        )
    )

  override infix def all[
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag,
      C3 <: Component: ClassTag
  ](f: (Entity, C0, C1, C2, C3) => Unit): Query =
    idCache = Array(id0K[C0], id0K[C1], id0K[C2], id0K[C3])
    initSignature(idCache)
    Query(() =>
      am.iterate(matches): (e, components) =>
        f(
          e,
          components.aFindOfType[C0],
          components.aFindOfType[C1],
          components.aFindOfType[C2],
          components.aFindOfType[C3]
        )
    )

  override infix def all[
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag,
      C3 <: Component: ClassTag,
      C4 <: Component: ClassTag
  ](f: (Entity, C0, C1, C2, C3, C4) => Unit): Query =
    idCache = Array(id0K[C0], id0K[C1], id0K[C2], id0K[C3], id0K[C4])
    initSignature(idCache)
    Query(() =>
      am.iterate(matches): (e, components) =>
        f(
          e,
          components.aFindOfType[C0],
          components.aFindOfType[C1],
          components.aFindOfType[C2],
          components.aFindOfType[C3],
          components.aFindOfType[C4]
        )
    )

  override infix def all[
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag,
      C3 <: Component: ClassTag,
      C4 <: Component: ClassTag,
      C5 <: Component: ClassTag
  ](f: (Entity, C0, C1, C2, C3, C4, C5) => Unit): Query =
    idCache = Array(id0K[C0], id0K[C1], id0K[C2], id0K[C3], id0K[C4], id0K[C5])
    initSignature(idCache)
    Query(() =>
      am.iterate(matches): (e, components) =>
        f(
          e,
          components.aFindOfType[C0],
          components.aFindOfType[C1],
          components.aFindOfType[C2],
          components.aFindOfType[C3],
          components.aFindOfType[C4],
          components.aFindOfType[C5]
        )
    )

  override infix def all[
      C0 <: Component: ClassTag,
      C1 <: Component: ClassTag,
      C2 <: Component: ClassTag,
      C3 <: Component: ClassTag,
      C4 <: Component: ClassTag,
      C5 <: Component: ClassTag,
      C6 <: Component: ClassTag
  ](f: (Entity, C0, C1, C2, C3, C4, C5, C6) => Unit): Query =
    idCache = Array(id0K[C0], id0K[C1], id0K[C2], id0K[C3], id0K[C4], id0K[C5], id0K[C6])
    initSignature(idCache)
    Query(() =>
      am.iterate(matches): (e, components) =>
        f(
          e,
          components.aFindOfType[C0],
          components.aFindOfType[C1],
          components.aFindOfType[C2],
          components.aFindOfType[C3],
          components.aFindOfType[C4],
          components.aFindOfType[C5],
          components.aFindOfType[C6]
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
