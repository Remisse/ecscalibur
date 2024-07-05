package ecscalibur.core.archetype

import ecscalibur.core.Entity
import ecscalibur.core.archetype.Archetypes.Archetype
import ecscalibur.core.component.{Component, ComponentId, ComponentType, CSeq}
import ecscalibur.util.array.*

import CSeq.Extensions.*
import scala.collection.mutable

trait ArchetypeManager:
  def addEntity(e: Entity, components: CSeq): Unit
  def addComponents(e: Entity, components: CSeq): Boolean 
  def removeComponents(e: Entity, compTypes: ComponentType*): Boolean
  def delete(e: Entity): Unit
  def iterate(isSelected: Signature => Boolean, allIds: Signature)(f: (Entity, CSeq, Archetype) => Unit): Unit

object ArchetypeManager:
  def apply(): ArchetypeManager = ArchetypeManagerImpl()

private class ArchetypeManagerImpl extends ArchetypeManager:
  private val archetypes: mutable.Map[Signature, Archetype] = mutable.HashMap.empty
  private val signaturesByEntity: mutable.Map[Entity, Signature] = mutable.HashMap.empty

  override inline def addEntity(e: Entity, components: CSeq): Unit =
    require(
      !signaturesByEntity.contains(e),
      "Attempted to add an already existing entity."
    )
    newEntityToArchetype(e, components)

  private inline def newEntityToArchetype(e: Entity, components: CSeq): Unit =
    val entitySignature: Signature = components.toTypes.toSignature
    if !archetypes.contains(entitySignature) then
      archetypes += entitySignature -> Archetype(entitySignature)
    archetypes(entitySignature).add(e, components)
    signaturesByEntity.update(e, entitySignature)

  override def addComponents(e: Entity, components: CSeq): Boolean =
    ensureEntityIsValid(e)
    require(components.underlying.nonEmpty, "Component list is empty.")
    val existing: CSeq = archetypes(signaturesByEntity(e)).remove(e)
    if (filterOutExistingComponents(existing, components.toTypes).isEmpty) 
      return false
    signaturesByEntity.remove(e)
    newEntityToArchetype(e, CSeq(existing.underlying concat components.underlying))
    true

  override def removeComponents(e: Entity, compTypes: ComponentType*): Boolean =
    ensureEntityIsValid(e)
    if (!signaturesByEntity(e).containsAny(Signature(compTypes*))) 
      return false
    val entityComps: CSeq = archetypes(signaturesByEntity(e)).remove(e)
    signaturesByEntity.remove(e)
    val filtered = filterOutExistingComponents(entityComps, compTypes.toArray.aMap(~_))
    if (filtered.nonEmpty) newEntityToArchetype(e, filtered)
    true

  private inline def filterOutExistingComponents(toBeFiltered: CSeq, ids: Array[ComponentId]): CSeq =
    CSeq(toBeFiltered.underlying.aFilterNot(c => ids.aContains(~c)))

  override def delete(e: Entity): Unit =
    ensureEntityIsValid(e)
    archetypes(signaturesByEntity(e)).softRemove(e)
    val _ = signaturesByEntity.remove(e)

  override def iterate(isSelected: Signature => Boolean, allIds: Signature)(f: (Entity, CSeq, Archetype) => Unit): Unit =
    archetypes foreach:
      case (s, arch) if isSelected(s) => arch.iterate(allIds)(f)
      case _ => ()

  private inline def ensureEntityIsValid(e: Entity): Unit =
    require(signaturesByEntity.contains(e), "Given entity does not exist.")
