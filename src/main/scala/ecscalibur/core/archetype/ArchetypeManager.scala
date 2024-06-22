package ecscalibur.core.archetype

import ecscalibur.core.Entities.Entity
import ecscalibur.core.Components.{Component, ComponentType}
import ecscalibur.core.archetype.Archetypes.Archetype
import Signatures.Signature
import Signature.Extensions.*
import ecscalibur.core.archetype.Queries.*
import ecscalibur.core.Components.CSeqs.CSeq

object ArchetypeManagers:
  trait ArchetypeManager:
    def addEntity(e: Entity, components: CSeq): Unit
    def addComponents(e: Entity, components: CSeq): Unit
    def removeComponents(e: Entity, compTypes: ComponentType*): Unit
    def delete(e: Entity): Unit
    def iterateReading(query: Query)(f: (Entity, CSeq) => Unit): Unit
    def iterateWriting(query: Query)(f: (Entity, CSeq) => CSeq): Unit

  object ArchetypeManager:
    def apply(): ArchetypeManager = ArchetypeManagerImpl()

  private class ArchetypeManagerImpl extends ArchetypeManager:
    import scala.collection.mutable
    import ecscalibur.core.Components.ComponentId

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
        !components.underlying.exists(c => currentSignature.underlying.contains(~c)),
        "Given entity already has one or more of the given components."
      )
      val existing: CSeq = archetypes(signaturesByEntity(e)).remove(e)
      signaturesByEntity.remove(e)
      newEntityToArchetype(e, CSeq(existing.underlying concat components.underlying))

    override def removeComponents(e: Entity, compTypes: ComponentType*): Unit =
      ensureEntityIsValid(e)
      require(
        Signature(compTypes*) isPartOf signaturesByEntity(e),
        "Given component types are not part of the given entity's signature."
      )
      val entityComps: CSeq = archetypes(signaturesByEntity(e)).remove(e)
      signaturesByEntity.remove(e)
      newEntityToArchetype(e, CSeq(entityComps.underlying.filterNot(compTypes.contains)))

    override def delete(e: Entity): Unit =
      ensureEntityIsValid(e)
      archetypes(signaturesByEntity(e)).softRemove(e)
      val _ = signaturesByEntity.remove(e)

    override def iterateReading(query: Query)(f: (Entity, CSeq) => Unit): Unit =
      archetypes foreach:
        case (s, arch) if query.matches(s) => arch.readAll(query.filterIds, f)
        case _                          => ()

    override def iterateWriting(query: Query)(f: (Entity, CSeq) => CSeq): Unit =
      archetypes foreach:
        case (s, arch) if query.matches(s) => arch.writeAll(query.filterIds, f)
        case _                          => ()

    private inline def ensureEntityIsValid(e: Entity): Unit =
      require(signaturesByEntity.contains(e), "Given entity does not exist.")
