package ecscalibur.core.archetype

import ecscalibur.core.component.ComponentType
import ecscalibur.util.array.aContains

inline def ro(types: ComponentType*): QueryBuilder = QueryBuilder(_ro = types.toArray)
inline def rw(types: ComponentType*): QueryBuilder = QueryBuilder(_rw = types.toArray)
inline def none(types: ComponentType*): QueryBuilder = QueryBuilder(_none = Signature(types*))
inline def any(types: ComponentType*): QueryBuilder = QueryBuilder(_any = Signature(types*))

class QueryBuilder(
    private[core] var _rw: Array[ComponentType] = Array.empty,
    private[core] var _ro: Array[ComponentType] = Array.empty,
    private[core] var _none: Signature = Signature.Nil,
    private[core] var _any: Signature = Signature.Nil
):
  inline val roRwConflictMsg = "Cannot request for a type to be both RO and RW."
  inline def multipleCallsErrorMsg(methodName: String) = s"Called '$methodName' multiple times."

  infix def ro(types: ComponentType*): QueryBuilder =
    require(_ro.isEmpty, multipleCallsErrorMsg("ro"))
    require(!_rw.exists(_ro.contains), roRwConflictMsg)
    _ro = types.toArray
    this

  infix def rw(types: ComponentType*): QueryBuilder =
    require(_rw.isEmpty, multipleCallsErrorMsg("rw"))
    require(!_ro.exists(_rw.contains), roRwConflictMsg)
    _rw = types.toArray
    this

  infix def none(types: ComponentType*): QueryBuilder =
    ensureFieldIsEmpty(_none, "none")
    _none = Signature(types*)
    this

  infix def any(types: ComponentType*): QueryBuilder =
    ensureFieldIsEmpty(_any, "any")
    _any = Signature(types*)
    this

  private inline def ensureFieldIsEmpty(field: Signature, methodName: String) =
    require(field.isNil, multipleCallsErrorMsg(methodName))

import ecscalibur.core.component.ComponentId
final class Query(
    private val all: Signature,
    private val none: Signature,
    private val any: Signature,
    private val rw: Array[ComponentId]
):
  inline def isSelected(id: ComponentId): Boolean = all.isNil || all.underlying.aContains(id)
  inline def matches(s: Signature): Boolean =
    (all.isNil || s.containsAll(all)) &&
      (none.isNil || !s.containsAny(none)) &&
      (any.isNil || s.containsAny(any))
  inline def isRw(id: ComponentId): Boolean = rw.aContains(id)

export Query.fromBuilder
object Query:
  given fromBuilder: Conversion[QueryBuilder, Query] = (qb: QueryBuilder) =>
    val all = qb._rw concat qb._ro
    Query(
      if (all.isEmpty) Signature.Nil else all.toSignature, 
      qb._none, 
      qb._any, 
      qb._rw.map(_.typeId))
