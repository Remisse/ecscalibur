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

/** A collection of [[Archetype]]s.
  */
trait ArchetypeManager:
  /** Adds the given Entity and its Components to the correct Archetype.
    *
    * @param e
    *   the Entity to be added
    * @param components
    *   the given Entity's Components
    */
  def addEntity(e: Entity, components: CSeq[Component]): Unit

  /** Adds the given Components to an existing Entity. It will not be executed if all of the given
    * components are already part of the given Entity.
    *
    * This operation is very slow: the Entity and its existing Components must be removed from the
    * Archetype in which they are stored and moved to the correct Archetype, which implies that a
    * new Fragment might also be instantiated.
    *
    * @param e
    *   the Entity to which the given components must be added
    * @param components
    *   the Components to be added
    * @return
    *   true if this operation has been successfully executed, false otherwise.
    */
  def addComponents(e: Entity, components: CSeq[Component]): Boolean

  /** Removes the given Components from an existing Entity. It will not be executed if none of the
    * given components are part of the given Entity.
    *
    * This operation is very slow: the Entity and its existing Components must be removed from the
    * Archetype in which they are stored and moved to the correct Archetype, which implies that a
    * new Fragment might also be instantiated.
    *
    * @param e
    *   the Entity from which the given components must be removed
    * @param components
    *   the Components to be removed
    * @return
    *   true if this operation has been successfully executed, false otherwise.
    */
  def removeComponents(e: Entity, compTypes: ComponentType*): Boolean

  /** Deletes an existing Entity.
    *
    * @param e
    *   the Entity to be deleted
    */
  def delete(e: Entity): Unit

  /** Iterates over all Entities across all Archetypes which satisfy the given predicate and selects
    * all Components whose ComponentIds are part of the given Signature.
    *
    * @param isSelected
    *   the predicate on which all Archetypes are to be tested
    * @param allIds
    *   Components that must be selected
    * @param f
    *   function to be executed on all selected Entities
    */
  def iterate(isSelected: Signature => Boolean, allIds: Signature)(
      f: (Entity, CSeq[Component], Archetype) => Unit
  ): Unit

object ArchetypeManager:
  /** Creates a new instance of [[ArchetypeManager]].
    *
    * @return
    *   a new ArchetypeManager instance.
    */
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
