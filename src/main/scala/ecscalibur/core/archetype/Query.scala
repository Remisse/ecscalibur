package ecscalibur.core.archetype

export Queries.*
export Queries.Query.fromBuilder
object Queries:
  import ecscalibur.core.Components.ComponentType

  inline def all(types: ComponentType*): QueryBuilder = QueryBuilder(_all = types.toArray)
  inline def none(types: ComponentType*): QueryBuilder = QueryBuilder(_none = types.toArray)
  inline def any(types: ComponentType*): QueryBuilder = QueryBuilder(_any = types.toArray)

  class QueryBuilder(
    private[Queries] var _all: Array[ComponentType] = Array.empty,
    private[Queries] var _none: Array[ComponentType] = Array.empty,
    private[Queries] var _any: Array[ComponentType] = Array.empty
  ):
    infix def all(types: ComponentType*): QueryBuilder = 
      ensureFieldIsEmpty(_all, "all")
      _all = types.toArray
      this

    infix def none(types: ComponentType*): QueryBuilder = 
      ensureFieldIsEmpty(_none, "none")
      _none = types.toArray
      this

    infix def any(types: ComponentType*): QueryBuilder =
      ensureFieldIsEmpty(_any, "any")
      _any = types.toArray
      this

    private inline def ensureFieldIsEmpty(field: Array[ComponentType], fieldName: String) = 
      require(field.isEmpty, s"Called $fieldName multiple times.")

  final class Query(
      private val all: Array[ComponentType],
      private val none: Array[ComponentType],
      private val any: Array[ComponentType]
  ):
    import ecscalibur.core.Components.ComponentId

    private val allCache: Signature = if all.isEmpty then Signature.nil else all.toSignature
    private val noneCache: Signature = if none.isEmpty then Signature.nil else none.toSignature
    private val anyCache: Signature = if any.isEmpty then Signature.nil else any.toSignature
    private val allTypes = all.map(_.typeId)

    inline def testAll(id: ComponentId): Boolean = allTypes.isEmpty || allTypes.contains(id)
    inline def test(s: Signature): Boolean =
      (all.isEmpty || allCache.isPartOf(s)) && (none.isEmpty || !noneCache.isPartOf(s)) && 
      (any.isEmpty || s.containsAny(anyCache))
  
  object Query:
    given fromBuilder: Conversion[QueryBuilder, Query] = (qb: QueryBuilder) => Query(qb._all, qb._none, qb._any)
