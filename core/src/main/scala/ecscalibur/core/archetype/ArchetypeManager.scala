package ecscalibur.core.archetype

import ecscalibur.core.archetype.archetypes.Archetype
import ecscalibur.core.components.*
import ecscalibur.core.entity.Entity
import ecsutil.array.aFilterNot
import scala.collection.immutable.ArraySeq

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
  def addEntity(e: Entity, components: Component*): Unit

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
  def addComponents(e: Entity, components: Component*): Boolean

  /** Removes the given Components from an existing Entity. It will not be executed if none of the
    * given components are part of the given Entity.
    *
    * This operation is very slow: the Entity and its existing Components must be removed from the
    * Archetype in which they are stored and moved to the correct Archetype, which implies that a
    * new Fragment might also be instantiated.
    *
    * @param e
    *   the Entity from which the given components must be removed
    * @param compTypes
    *   the Components to be removed
    * @throws IllegalArgumentException
    *   if the given Entity is not handled by this manager or if the given component sequence is
    *   empty.
    * @return
    *   true if this operation has been successfully executed, false otherwise.
    */
  def removeComponents(e: Entity, compTypes: ComponentType*): Boolean

  /** Adds and removes the given components in one single operation.
    *
    * @param e
    *   the Entity to manipulate
    * @param toAdd
    *   the Components to add
    * @param toRemove
    *   the types of the Components to remove
    * @return
    *   true if this operation has been successfully executed, false otherwise.
    */
  def addRemove(e: Entity)(toAdd: Component*)(toRemove: ComponentType*): Boolean

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
  def iterate(isSelected: Signature => Boolean)(f: (Entity, Array[Component]) => Unit): Unit

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

  override def addEntity(e: Entity, components: Component*): Unit =
    require(
      !archetypesByEntity.contains(e),
      "Attempted to add an already existing entity."
    )
    newEntityToArchetype(e, components*)

  private inline def newEntityToArchetype(e: Entity, components: Component*): Unit =
    val entitySignature: Signature = Signature(components.map(~_)*)
    val desiredArchetype = archetypesBySignature.getOrElseUpdate(
      entitySignature, {
        val newArchetype = Archetype(entitySignature)
        archetypesBySignature += entitySignature -> newArchetype
        archetypes = archetypes :+ newArchetype
        newArchetype
      }
    )
    desiredArchetype.add(e, components*)
    archetypesByEntity += e -> desiredArchetype

  override def addComponents(e: Entity, components: Component*): Boolean =
    ensureEntityIsValid(e)
    val currentArch = archetypesByEntity(e)
    if components.isEmpty || currentArch.signature.containsAll(Signature(components*)) then
      return false
    val existing = currentArch.remove(e)
    move(e, existing concat components*)

  override def removeComponents(e: Entity, compTypes: ComponentType*): Boolean =
    ensureEntityIsValid(e)
    val currentArch = archetypesByEntity(e)
    if compTypes.isEmpty || !currentArch.signature.containsAny(Signature(compTypes*)) then
      return false
    val entityComps = currentArch.remove(e)
    move(e, filterOutExistingComponents(entityComps)(compTypes*)*)

  override def addRemove(e: Entity)(toAdd: Component*)(toRemove: ComponentType*): Boolean =
    ensureEntityIsValid(e)
    val currentArch = archetypesByEntity(e)
    if (toAdd.isEmpty && toRemove.isEmpty) ||
      (currentArch.signature
        .containsAll(Signature(toAdd*)) && !currentArch.signature.containsAny(Signature(toRemove*)))
    then return false
    val entityComps = currentArch.remove(e)
    val remainingComponents = filterOutExistingComponents(entityComps)(toRemove*)
    move(e, remainingComponents concat toAdd*)

  private inline def move(e: Entity, c: Component*): Boolean =
    var res = false
    archetypesByEntity -= e
    if c.nonEmpty then
      newEntityToArchetype(e, c*)
      res = true
    res

  private inline def filterOutExistingComponents(toBeFiltered: Array[Component])(
      toFilterOut: WithType*
  ): Seq[Component] =
    ArraySeq.unsafeWrapArray(toBeFiltered.aFilterNot(c => toFilterOut.exists(_.typeId == c.typeId)))

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
      f: (Entity, Array[Component]) => Unit
  ): Unit =
    for a <- archetypes do if isSelected(a.signature) then a.iterate(f)

  private inline def ensureEntityIsValid(e: Entity): Unit =
    require(archetypesByEntity.contains(e), "Given entity does not exist.")
