package ecscalibur.core

import ecscalibur.core.component.{ComponentType, ComponentId, Component, CSeq, shallowId, deepId}
import izumi.reflect.Tag
import ecscalibur.core.Entity
import CSeq.Extensions.*
import ecscalibur.util.array.*
import ecscalibur.core.archetype.{ArchetypeManager, Signature}
import Signature.Extensions.*

type Query = () => Unit

inline def query(using ArchetypeManager): QueryBuilder = new QueryBuilderImpl()

trait QueryBuilder:
  infix def withNone(types: ComponentType*): QueryBuilder

  infix def withAny(types: ComponentType*): QueryBuilder

  infix def withAll[C0 <: Component: Tag](f: (Entity, C0) => Unit): Query
  infix def withAll[C0 <: Component: Tag, C1 <: Component: Tag](f: (Entity, C0, C1) => Unit): Query
  infix def withAll[C0 <: Component: Tag, C1 <: Component: Tag, C2 <: Component: Tag](
      f: (Entity, C0, C1, C2) => Unit
  ): Query
  infix def withAll[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3) => Unit): Query
  infix def withAll[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4) => Unit): Query
  infix def withAll[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag,
      C5 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4, C5) => Unit): Query
  infix def withAll[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag,
      C5 <: Component: Tag,
      C6 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4, C5, C6) => Unit): Query

class QueryBuilderImpl(using am: ArchetypeManager) extends QueryBuilder:
  private var rw: Array[ComponentId] = Array.empty
  private var selected: Signature = Signature.Nil
  private var _none: Signature = Signature.Nil
  private var _any: Signature = Signature.Nil

  private inline def multipleCallsErrorMsg(methodName: String) =
    s"Called '$methodName' multiple times."

  override infix def withNone(types: ComponentType*): QueryBuilder =
    ensureFirstCallToNone
    _none = Signature(types*)
    this

  override infix def withAny(types: ComponentType*): QueryBuilder =
    ensureFirstCallToAny
    _any = Signature(types*)
    this

  override infix def withAll[C0 <: Component: Tag](f: (Entity, C0) => Unit): Query =
    val wrapped = Array(shallowId[C0])
    val trueIds = Array(deepId[C0])
    makeQuery(trueIds, wrapped): (e, components) =>
      f(e, findOfType[C0](trueIds(0))(components))

  override infix def withAll[C0 <: Component: Tag, C1 <: Component: Tag](
      f: (Entity, C0, C1) => Unit
  ): Query =
    val wrapped = Array(shallowId[C0], shallowId[C1])
    val trueIds = Array(deepId[C0], deepId[C1])
    makeQuery(trueIds, wrapped): (e, components) =>
      f(
        e,
        findOfType[C0](trueIds(0))(components),
        findOfType[C1](trueIds(1))(components)
      )

  override infix def withAll[C0 <: Component: Tag, C1 <: Component: Tag, C2 <: Component: Tag](
      f: (Entity, C0, C1, C2) => Unit
  ): Query =
    val wrapped = Array(shallowId[C0], shallowId[C1], shallowId[C2])
    val trueIds = Array(deepId[C0], deepId[C1], deepId[C2])
    makeQuery(trueIds, wrapped): (e, components) =>
      f(
        e,
        findOfType[C0](trueIds(0))(components),
        findOfType[C1](trueIds(1))(components),
        findOfType[C2](trueIds(2))(components)
      )

  override infix def withAll[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3) => Unit): Query =
    val wrapped = Array(shallowId[C0], shallowId[C1], shallowId[C2], shallowId[C3])
    val trueIds = Array(deepId[C0], deepId[C1], deepId[C2], deepId[C3])
    makeQuery(trueIds, wrapped): (e, components) =>
      f(
        e,
        findOfType[C0](trueIds(0))(components),
        findOfType[C1](trueIds(1))(components),
        findOfType[C2](trueIds(2))(components),
        findOfType[C3](trueIds(3))(components)
      )

  override infix def withAll[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4) => Unit): Query =
    val wrapped = Array(shallowId[C0], shallowId[C1], shallowId[C2], shallowId[C3], shallowId[C4])
    val trueIds = Array(deepId[C0], deepId[C1], deepId[C2], deepId[C3], deepId[C4])
    makeQuery(trueIds, wrapped): (e, components) =>
      f(
        e,
        findOfType[C0](trueIds(0))(components),
        findOfType[C1](trueIds(1))(components),
        findOfType[C2](trueIds(2))(components),
        findOfType[C3](trueIds(3))(components),
        findOfType[C4](trueIds(4))(components)
      )

  override infix def withAll[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag,
      C5 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4, C5) => Unit): Query =
    val wrapped = Array(
      shallowId[C0],
      shallowId[C1],
      shallowId[C2],
      shallowId[C3],
      shallowId[C4],
      shallowId[C5]
    )
    val trueIds = Array(deepId[C0], deepId[C1], deepId[C2], deepId[C3], deepId[C4], deepId[C5])
    makeQuery(trueIds, wrapped): (e, components) =>
      f(
        e,
        findOfType[C0](trueIds(0))(components),
        findOfType[C1](trueIds(1))(components),
        findOfType[C2](trueIds(2))(components),
        findOfType[C3](trueIds(3))(components),
        findOfType[C4](trueIds(4))(components),
        findOfType[C5](trueIds(5))(components)
      )

  override infix def withAll[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag,
      C5 <: Component: Tag,
      C6 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4, C5, C6) => Unit): Query =
    val wrapped = Array(
      shallowId[C0],
      shallowId[C1],
      shallowId[C2],
      shallowId[C3],
      shallowId[C4],
      shallowId[C5],
      shallowId[C6]
    )
    val trueIds =
      Array(deepId[C0], deepId[C1], deepId[C2], deepId[C3], deepId[C4], deepId[C5], deepId[C6])
    makeQuery(trueIds, wrapped): (e, components) =>
      f(
        e,
        findOfType[C0](trueIds(0))(components),
        findOfType[C1](trueIds(1))(components),
        findOfType[C2](trueIds(2))(components),
        findOfType[C3](trueIds(3))(components),
        findOfType[C4](trueIds(4))(components),
        findOfType[C5](trueIds(5))(components),
        findOfType[C6](trueIds(6))(components)
      )

  private inline def matches(s: Signature): Boolean =
    (selected.isNil || s.containsAll(selected)) &&
      (_none.isNil || !s.containsAny(_none)) &&
      (_any.isNil || s.containsAny(_any))

  private inline def makeQuery(selectedIds: Array[ComponentId], wrapped: Array[ComponentId])(
      f: (Entity, CSeq) => Unit
  ): Query =
    selected = selectedIds.toSignature
    rw = selectedIds.aFilterNot(wrapped.contains)
    // TODO Create a ComponentId wrapper that carries metadata such as 'isRw'.
    () => am.iterate(matches, selected, rw)(f)

  private inline def ensureFirstCallToNone: Unit =
    require(_none.isNil, multipleCallsErrorMsg("none"))

  private inline def ensureFirstCallToAny: Unit =
    require(_any.isNil, multipleCallsErrorMsg("any"))

  private inline def findOfType[C <: Component](id: ComponentId)(components: CSeq): C =
    components.underlying.aFindUnsafe(_.typeId == id).asInstanceOf[C]
