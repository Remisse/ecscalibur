package ecscalibur.core.archetype

import ecscalibur.core.component.ComponentType

inline def all(types: ComponentType*): QueryBuilder = QueryBuilder(_all = Signature(types*))
inline def none(types: ComponentType*): QueryBuilder = QueryBuilder(_none = Signature(types*))
inline def any(types: ComponentType*): QueryBuilder = QueryBuilder(_any = Signature(types*))

class QueryBuilder(
    private[core] var _all: Signature = Signature.nil,
    private[core] var _none: Signature = Signature.nil,
    private[core] var _any: Signature = Signature.nil
):
  infix def all(types: ComponentType*): QueryBuilder =
    ensureFieldIsEmpty(_all, "all")
    _all = Signature(types*)
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
    require(field.isNil, s"Called $methodName multiple times.")

final class Query(
    private val all: Signature,
    private val none: Signature,
    private val any: Signature
):
  import ecscalibur.core.component.ComponentId

  inline def filterIds(id: ComponentId): Boolean = all.isNil || all.underlying.contains(id)
  inline def matches(s: Signature): Boolean =
    (all.isNil || s.containsAll(all)) &&
      (none.isNil || !s.containsAny(none)) &&
      (any.isNil || s.containsAny(any))

export Query.fromBuilder
object Query:
  given fromBuilder: Conversion[QueryBuilder, Query] = (qb: QueryBuilder) =>
    Query(qb._all, qb._none, qb._any)
