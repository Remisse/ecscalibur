package ecscalibur.core.archetype

import ecscalibur.core.{CSeq, Entity}
import ecscalibur.core.archetype.Archetypes.Archetype
import ecscalibur.core.component.{Component, ComponentId, ComponentType}
import CSeq.*
import ecscalibur.util.array.*

import scala.collection.mutable

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

private class ArchetypeManagerImpl extends ArchetypeManager:
  private val archetypes: mutable.Map[Signature, Archetype] = mutable.HashMap.empty
  private val signaturesByEntity: mutable.Map[Entity, Signature] = mutable.HashMap.empty

  override inline def addEntity(e: Entity, components: CSeq[Component]): Unit =
    require(
      !signaturesByEntity.contains(e),
      "Attempted to add an already existing entity."
    )
    newEntityToArchetype(e, components)

  private inline def newEntityToArchetype(e: Entity, components: CSeq[Component]): Unit =
    val entitySignature: Signature = components.map(~_).toSignature
    if !archetypes.contains(entitySignature) then
      archetypes += entitySignature -> Archetype(entitySignature)
    archetypes(entitySignature).add(e, components)
    signaturesByEntity.update(e, entitySignature)

  override def addComponents(e: Entity, components: CSeq[Component]): Boolean =
    ensureEntityIsValid(e)
    require(components.nonEmpty, "Component list is empty.")
    val existing: CSeq[Component] = archetypes(signaturesByEntity(e)).remove(e)
    if (filterOutExistingComponents(existing, components.map(~_)).isEmpty)
      return false
    signaturesByEntity.remove(e)
    newEntityToArchetype(e, existing concat components)
    true

  override def removeComponents(e: Entity, compTypes: ComponentType*): Boolean =
    ensureEntityIsValid(e)
    if (!signaturesByEntity(e).containsAny(Signature(compTypes*)))
      return false
    val entityComps: CSeq[Component] = archetypes(signaturesByEntity(e)).remove(e)
    signaturesByEntity.remove(e)
    val filtered = filterOutExistingComponents(entityComps, compTypes.toArray.aMap(~_))
    if (filtered.nonEmpty) newEntityToArchetype(e, filtered)
    true

  private inline def filterOutExistingComponents(
      toBeFiltered: CSeq[Component],
      ids: Array[ComponentId]
  ): CSeq[Component] =
    CSeq[Component](toBeFiltered.filterNot(c => ids.aContains(~c)))

  override def delete(e: Entity): Unit =
    ensureEntityIsValid(e)
    archetypes(signaturesByEntity(e)).softRemove(e)
    val _ = signaturesByEntity.remove(e)

  override def iterate(isSelected: Signature => Boolean, allIds: Signature)(
      f: (Entity, CSeq[Component], Archetype) => Unit
  ): Unit =
    archetypes foreach:
      case (s, arch) if isSelected(s) => arch.iterate(allIds)(f)
      case _                          => ()

  private inline def ensureEntityIsValid(e: Entity): Unit =
    require(signaturesByEntity.contains(e), "Given entity does not exist.")
