package ecscalibur.core.archetype

import ecscalibur.core.Entity
import ecscalibur.core.archetype.Archetypes.Archetype
import ecscalibur.core.component.{ComponentType, CSeq}
import ecscalibur.util.array.*

import CSeq.Extensions.*
import ecscalibur.core.component.Component
import scala.reflect.ClassTag
import ecscalibur.core.component.Rw
import izumi.reflect.Tag
import ecscalibur.core.component.{shallowId, deepId}

trait ArchetypeManager:
  def addEntity(e: Entity, components: CSeq): Unit
  def addComponents(e: Entity, components: CSeq): Unit
  def removeComponents(e: Entity, compTypes: ComponentType*): Unit
  def delete(e: Entity): Unit
  def iterate(query: Query)(f: (Entity, CSeq) => Unit): Unit
  def iterate[C0 <: Component: Tag](f: (Entity, C0) => Unit): Unit
  def iterate[C0 <: Component: Tag, C1 <: Component: Tag](f: (Entity, C0, C1) => Unit): Unit
  def iterate[C0 <: Component: Tag, C1 <: Component: Tag, C2 <: Component: Tag](
      f: (Entity, C0, C1, C2) => Unit
  ): Unit
  def iterate[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3) => Unit): Unit
  def iterate[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4) => Unit): Unit
  def iterate[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag,
      C5 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4, C5) => Unit): Unit
  def iterate[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag,
      C5 <: Component: Tag,
      C6 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4, C5, C6) => Unit): Unit

object ArchetypeManager:
  def apply(): ArchetypeManager = ArchetypeManagerImpl()

private class ArchetypeManagerImpl extends ArchetypeManager:
  import scala.collection.mutable
  import ecscalibur.core.component.ComponentId

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

  override def iterate(query: Query)(f: (Entity, CSeq) => Unit): Unit =
    archetypes foreach:
      case (s, arch) if query.matches(s) => arch.iterate(query.selectedIds, query.rwIds)(f)
      case _                             => ()

  override def iterate[C0 <: Component: Tag](f: (Entity, C0) => Unit): Unit =
    val wrapped = Array(shallowId[C0])
    val trueIds = Array(deepId[C0])
    iteratePartial(trueIds, wrapped): (e, components) =>
      f(e, findOfType[C0](trueIds(0))(components))

  override def iterate[C0 <: Component: Tag, C1 <: Component: Tag](
      f: (Entity, C0, C1) => Unit
  ): Unit =
    val wrapped = Array(shallowId[C0], shallowId[C1])
    val trueIds = Array(deepId[C0], deepId[C1])
    iteratePartial(trueIds, wrapped): (e, components) =>
      f(
        e,
        findOfType[C0](trueIds(0))(components),
        findOfType[C1](trueIds(1))(components)
      )

  override def iterate[C0 <: Component: Tag, C1 <: Component: Tag, C2 <: Component: Tag](
      f: (Entity, C0, C1, C2) => Unit
  ): Unit =
    val wrapped = Array(shallowId[C0], shallowId[C1], shallowId[C2])
    val trueIds = Array(deepId[C0], deepId[C1], deepId[C2])
    iteratePartial(trueIds, wrapped): (e, components) =>
      f(
        e,
        findOfType[C0](trueIds(0))(components),
        findOfType[C1](trueIds(1))(components),
        findOfType[C2](trueIds(2))(components)
      )

  override def iterate[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3) => Unit): Unit =
    val wrapped = Array(shallowId[C0], shallowId[C1], shallowId[C2], shallowId[C3])
    val trueIds = Array(deepId[C0], deepId[C1], deepId[C2], deepId[C3])
    iteratePartial(trueIds, wrapped): (e, components) =>
      f(
        e,
        findOfType[C0](trueIds(0))(components),
        findOfType[C1](trueIds(1))(components),
        findOfType[C2](trueIds(2))(components),
        findOfType[C3](trueIds(3))(components)
      )

  override def iterate[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4) => Unit): Unit =
    val wrapped =
      Array(shallowId[C0], shallowId[C1], shallowId[C2], shallowId[C3], shallowId[C4])
    val trueIds = Array(deepId[C0], deepId[C1], deepId[C2], deepId[C3], deepId[C4])
    iteratePartial(trueIds, wrapped): (e, components) =>
      f(
        e,
        findOfType[C0](trueIds(0))(components),
        findOfType[C1](trueIds(1))(components),
        findOfType[C2](trueIds(2))(components),
        findOfType[C3](trueIds(3))(components),
        findOfType[C4](trueIds(4))(components)
      )

  override def iterate[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag,
      C5 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4, C5) => Unit): Unit =
    val wrapped = Array(
      shallowId[C0],
      shallowId[C1],
      shallowId[C2],
      shallowId[C3],
      shallowId[C4],
      shallowId[C5]
    )
    val trueIds = Array(deepId[C0], deepId[C1], deepId[C2], deepId[C3], deepId[C4], deepId[C5])
    iteratePartial(trueIds, wrapped): (e, components) =>
      f(
        e,
        findOfType[C0](trueIds(0))(components),
        findOfType[C1](trueIds(1))(components),
        findOfType[C2](trueIds(2))(components),
        findOfType[C3](trueIds(3))(components),
        findOfType[C4](trueIds(4))(components),
        findOfType[C5](trueIds(5))(components)
      )

  override def iterate[
      C0 <: Component: Tag,
      C1 <: Component: Tag,
      C2 <: Component: Tag,
      C3 <: Component: Tag,
      C4 <: Component: Tag,
      C5 <: Component: Tag,
      C6 <: Component: Tag
  ](f: (Entity, C0, C1, C2, C3, C4, C5, C6) => Unit): Unit =
    val wrapped = Array(
      shallowId[C0],
      shallowId[C1],
      shallowId[C2],
      shallowId[C3],
      shallowId[C4],
      shallowId[C5],
      shallowId[C6]
    )
    val trueIds =
      Array(deepId[C0], deepId[C1], deepId[C2], deepId[C3], deepId[C4], deepId[C5], deepId[C6])
    iteratePartial(trueIds, wrapped): (e, components) =>
      f(
        e,
        findOfType[C0](trueIds(0))(components),
        findOfType[C1](trueIds(1))(components),
        findOfType[C2](trueIds(2))(components),
        findOfType[C3](trueIds(3))(components),
        findOfType[C4](trueIds(4))(components),
        findOfType[C5](trueIds(5))(components),
        findOfType[C6](trueIds(6))(components)
      )

  private inline def iteratePartial(selectedIds: Array[ComponentId], wrapped: Array[ComponentId])(inline f: (Entity, CSeq) => Unit): Unit =
    val selected = selectedIds.toSignature
    val rwIds = selectedIds.aFilterNot(wrapped.contains)
    // TODO Create a ComponentId wrapper that carries metadata such as 'isRw'.
    archetypes foreach:
      case (s, arch) if (selected isPartOf s) =>
        arch.iterate(selected, rwIds)((e, components) => f(e, components))
      case _ => ()

  private inline def findOfType[C <: Component](id: ComponentId)(components: CSeq): C =
    components.underlying.aFindUnsafe(_.typeId == id).asInstanceOf[C]

  private inline def ensureEntityIsValid(e: Entity): Unit =
    require(signaturesByEntity.contains(e), "Given entity does not exist.")
