package ecscalibur.core.archetype

import ecscalibur.core.Entity
import ecscalibur.core.archetype.Archetypes.Archetype
import ecscalibur.core.component.{Component, ComponentId, ComponentType, CSeq}
import ecscalibur.util.array.*

import CSeq.Extensions.*
import scala.collection.mutable

trait ArchetypeManager:
  def addEntity(e: Entity, components: CSeq): Unit
  def addComponents(e: Entity, components: CSeq): Unit
  def removeComponents(e: Entity, compTypes: ComponentType*): Unit
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

  override def addComponents(e: Entity, components: CSeq): Unit =
    ensureEntityIsValid(e)
    require(components.underlying.nonEmpty, "Component list is empty.")
    val currentSignature = signaturesByEntity(e)
    require(
      !components.underlying.aExists(c => currentSignature.underlying.aContains(~c)),
      "Given entity already has one or more of the given components."
    )
    val existing: CSeq = archetypes(signaturesByEntity(e)).remove(e)
    signaturesByEntity.remove(e)
    newEntityToArchetype(e, CSeq(existing.underlying concat components.underlying))

  override def removeComponents(e: Entity, compTypes: ComponentType*): Unit =
    ensureEntityIsValid(e)
    require(
      signaturesByEntity(e).containsAll(Signature(compTypes*)),
      "Given component types are not part of the given entity's signature."
    )
    val entityComps: CSeq = archetypes(signaturesByEntity(e)).remove(e)
    signaturesByEntity.remove(e)
    val filteredComps = CSeq(entityComps.underlying.filterNot(c => compTypes.map(~_).contains(~c)))
    newEntityToArchetype(e, filteredComps)

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
