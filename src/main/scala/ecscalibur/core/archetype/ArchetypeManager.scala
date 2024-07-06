package ecscalibur.core.archetype

import ecscalibur.core.CSeq
import ecscalibur.core.Entity
import ecscalibur.core.archetype.archetypes.Archetype
import ecscalibur.core.component.Component
import ecscalibur.core.component.ComponentId
import ecscalibur.core.component.ComponentType
import ecscalibur.util.array._

import scala.collection.mutable

import CSeq._

trait ArchetypeManager:
  def addEntity(e: Entity, components: CSeq[Component]): Unit
  def addComponents(e: Entity, components: CSeq[Component]): Boolean
  def removeComponents(e: Entity, compTypes: ComponentType*): Boolean
  def delete(e: Entity): Unit
  def iterate(isSelected: Signature => Boolean, allIds: Signature)(
      f: (Entity, CSeq[Component], Archetype) => Unit
  ): Unit

object ArchetypeManager:
  def apply(): ArchetypeManager = ArchetypeManagerImpl()

private final class ArchetypeManagerImpl extends ArchetypeManager:
  private var archetypeBuffer: Vector[Archetype] = Vector.empty
  private val archetypes: mutable.Map[Signature, Archetype] = mutable.HashMap.empty
  private val signaturesByEntity: mutable.Map[Entity, Signature] = mutable.HashMap.empty

  override inline def addEntity(e: Entity, components: CSeq[Component]): Unit =
    require(
      !signaturesByEntity.contains(e),
      "Attempted to add an already existing entity."
    )
    newEntityToArchetype(e, components)

  private inline def newEntityToArchetype(e: Entity, components: CSeq[Component]): Unit =
    val entitySignature: Signature = Signature(components.map(~_).toArray)
    if !archetypes.contains(entitySignature) then
      val newArchetype = Archetype(entitySignature)
      archetypes += entitySignature -> newArchetype
      archetypeBuffer = archetypeBuffer :+ newArchetype
    archetypes(entitySignature).add(e, components)
    signaturesByEntity.update(e, entitySignature)

  override def addComponents(e: Entity, components: CSeq[Component]): Boolean =
    ensureEntityIsValid(e)
    require(components.nonEmpty, "Component list is empty.")
    val existing: CSeq[Component] = archetypes(signaturesByEntity(e)).remove(e)
    if (filterOutExistingComponents(existing, components.map(~_).toArray).isEmpty)
      return false
    signaturesByEntity -= e
    newEntityToArchetype(e, existing concat components)
    true

  override def removeComponents(e: Entity, compTypes: ComponentType*): Boolean =
    ensureEntityIsValid(e)
    if (!signaturesByEntity(e).containsAny(Signature(compTypes*)))
      return false
    val entityComps: CSeq[Component] = archetypes(signaturesByEntity(e)).remove(e)
    signaturesByEntity -= e
    val filtered = filterOutExistingComponents(entityComps, compTypes.toArray.aMap(~_))
    if (filtered.nonEmpty) newEntityToArchetype(e, filtered)
    true

  private inline def filterOutExistingComponents(
      toBeFiltered: CSeq[Component],
      ids: Array[ComponentId]
  ): CSeq[Component] =
    toBeFiltered.filterNot(c => ids.aContains(~c))

  override def delete(e: Entity): Unit =
    ensureEntityIsValid(e)
    archetypes(signaturesByEntity(e)).softRemove(e)
    signaturesByEntity -= e

  override def iterate(isSelected: Signature => Boolean, allIds: Signature)(
      f: (Entity, CSeq[Component], Archetype) => Unit
  ): Unit =
    archetypeBuffer foreach:
      case a if isSelected(a.signature) => a.iterate(allIds)(f)
      case _                            => ()

  private inline def ensureEntityIsValid(e: Entity): Unit =
    require(signaturesByEntity.contains(e), "Given entity does not exist.")
