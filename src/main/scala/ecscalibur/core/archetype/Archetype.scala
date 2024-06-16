package ecscalibur.core.archetype

import ecscalibur.core.Components.ComponentType
import scala.quoted.*
import scala.collection.immutable.ArraySeq

object Archetypes:
  trait Archetype:
    def hasSignature(types: ComponentType*): Boolean
    def contains(types: ComponentType*): Boolean

  object Archetype:
    def apply(types: ComponentType*): Archetype =
      require(!types.isEmpty, "Given signature is empty.")
      val distinct = types.distinct
      require(distinct.length == types.length, "Given signature has duplicate component types.")
      ArchetypeImpl(distinct)

    private class ArchetypeImpl(types: Seq[ComponentType]) extends Archetype:
      val signature: Array[ComponentType] = types.toArray.sorted
      override inline def hasSignature(types: ComponentType*): Boolean =
        signature.sameElements(types.toArray.sorted)

      override inline def contains(types: ComponentType*): Boolean =
        signature.containsSlice(types.toArray.sorted)