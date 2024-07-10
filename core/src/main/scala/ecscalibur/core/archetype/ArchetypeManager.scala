package ecscalibur.core.archetype

import ecscalibur.core.archetype.archetypes.Archetype
import ecscalibur.core.components.*
import ecscalibur.core.entity.Entity
import ecsutil.CSeq
import ecsutil.array.*

import CSeq.*

/** A collection of [[Archetype]]s.
  */
private[ecscalibur] trait ArchetypeManager:
  /** Adds the given Entity and its Components to the correct Archetype.
    *
    * @param e
    *   the Entity to be added
    * @throws IllegalArgumentException
    *   if the given Entity is already handled by this manager or if the given component sequence is
    *   empty.
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
    * @throws IllegalArgumentException
    *   if the given Entity is not handled by this manager or if the given component sequence is
    *   empty.
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
    * @throws IllegalArgumentException
    *   if the given Entity is not handled by this manager or if the given component sequence is
    *   empty.
    * @return
    *   true if this operation has been successfully executed, false otherwise.
    */
  def removeComponents(e: Entity, compTypes: ComponentType*): Boolean

  /** Checks whether the given Entity has all of the given Components.
    *
    * @param e
    *   the Entity on which to perform the check
    * @param types
    *   the ComponentTypes of the Components the given Entity should have
    * @return
    *   true if the Entity has all of the specified Components, false otherwise
    */
  def hasComponents(e: Entity, types: ComponentType*): Boolean

  /** Deletes an existing Entity.
    *
    * @param e
    *   the Entity to be deleted
    * @throws IllegalArgumentException
    *   if the given Entity is not handled by this manager.
    */
  def delete(e: Entity): Unit

  /** Updates the reference to the given Component type for the given Entity.
    *
    * @param e
    *   the Entity for which the given Component must be updated
    * @param c
    *   the Component to update
    * @throws IllegalArgumentException
    *   if the given Entity is not handled by this ArchetypeManager
    */
  def update(e: Entity, c: Component): Unit

  /** Iterates over all Entities across all Archetypes which satisfy the given predicate.
    *
    * @param isSelected
    *   the predicate on which all Archetypes are to be tested
    * @param f
    *   function to be executed on all selected Entities
    */
  def iterate(isSelected: Signature => Boolean)(f: (Entity, CSeq[Component]) => Unit): Unit

object ArchetypeManager:
  /** Creates a new instance of [[ArchetypeManager]].
    *
    * @return
    *   a new ArchetypeManager instance.
    */
  def apply(): ArchetypeManager = ArchetypeManagerImpl()

private final class ArchetypeManagerImpl extends ArchetypeManager:
  import scala.collection.mutable

  private var archetypes: Vector[Archetype] = Vector.empty
  private val archetypesBySignature: mutable.Map[Signature, Archetype] = mutable.HashMap.empty
  private val archetypesByEntity: mutable.Map[Entity, Archetype] = mutable.HashMap.empty

  override inline def addEntity(e: Entity, components: CSeq[Component]): Unit =
    require(
      !archetypesByEntity.contains(e),
      "Attempted to add an already existing entity."
    )
    newEntityToArchetype(e, components)

  private inline def newEntityToArchetype(e: Entity, components: CSeq[Component]): Unit =
    val entitySignature: Signature = Signature(components.map(~_))
    val desiredArchetype = archetypesBySignature.getOrElseUpdate(
      entitySignature, {
        val newArchetype = Archetype(entitySignature)
        archetypesBySignature += entitySignature -> newArchetype
        archetypes = newArchetype +: archetypes
        newArchetype
      }
    )
    desiredArchetype.add(e, components)
    archetypesByEntity.update(e, desiredArchetype)

  override def addComponents(e: Entity, components: CSeq[Component]): Boolean =
    ensureEntityIsValid(e)
    require(components.nonEmpty, "Component list is empty.")
    val existing: CSeq[Component] = archetypesByEntity(e).remove(e)
    if filterOutExistingComponents(existing, components.map(~_)).isEmpty then return false
    archetypesByEntity -= e
    newEntityToArchetype(e, existing concat components)
    true

  override def removeComponents(e: Entity, compTypes: ComponentType*): Boolean =
    ensureEntityIsValid(e)
    if !archetypesByEntity(e).signature.containsAny(Signature(compTypes*)) then return false
    val entityComps: CSeq[Component] = archetypesByEntity(e).remove(e)
    archetypesByEntity -= e
    val filtered = filterOutExistingComponents(entityComps, CSeq(compTypes*).map(~_))
    if filtered.nonEmpty then newEntityToArchetype(e, filtered)
    true

  private inline def filterOutExistingComponents(
      toBeFiltered: CSeq[Component],
      ids: CSeq[ComponentId]
  ): CSeq[Component] =
    toBeFiltered.filterNot(c => ids.contains(~c))

  override def hasComponents(e: Entity, types: ComponentType*): Boolean =
    ensureEntityIsValid(e)
    archetypesByEntity(e).signature.containsAll(types*)

  override def delete(e: Entity): Unit =
    ensureEntityIsValid(e)
    archetypesByEntity(e).softRemove(e)
    archetypesByEntity -= e

  override def update(e: Entity, c: Component): Unit =
    ensureEntityIsValid(e)
    archetypesByEntity(e).update(e, c)

  override def iterate(isSelected: Signature => Boolean)(
      f: (Entity, CSeq[Component]) => Unit
  ): Unit =
    for a <- archetypes if isSelected(a.signature) do a.iterate(f)

  private inline def ensureEntityIsValid(e: Entity): Unit =
    require(archetypesByEntity.contains(e), "Given entity does not exist.")
