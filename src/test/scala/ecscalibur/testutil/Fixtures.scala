package ecscalibur.core

import ecscalibur.core.component.CSeq
import CSeq.Extensions.*
import ecscalibur.core.archetype.ArchetypeManager
import ecscalibur.core.Entity
import ecscalibur.core.component.Component
import ecscalibur.core.archetype.Signature
import ecscalibur.core.archetype.Archetypes.Aggregate
import ecscalibur.core.archetype.Archetypes.Archetype.DefaultFragmentSizeBytes
import ecscalibur.core.archetype.Archetypes.Fragment

object fixtures:
  import ecscalibur.testutil.testclasses.Value
  class IterateNFixture(nEntities: Int = 100, extraComponents: CSeq):
    require(nEntities > 0)

    val archManager = ArchetypeManager()
    val testValue = Value(10)
    private val entities: Vector[Entity] = (0 until nEntities).map(Entity(_)).toVector
    private val values: Vector[Value] = (0 until nEntities).map(Value(_)).toVector
    private var sum = 0

    for (e, idx) <- entities.zipWithIndex do
      archManager.addEntity(e, CSeq(values(idx) +: extraComponents.underlying))

    def onIterationStart(v: Value) = sum += v.x
    def isSuccess = sum == values.map(_.x).sum + nEntities * testValue.x

  private[fixtures] abstract class ArchetypeFixture(components: Component*)(nEntities: Int):
    val componentIds = components.map(_.typeId).toArray
    val entities = (0 until nEntities).map(Entity(_)).toVector
    val nextEntity = Entity(entities.length)

  class StandardArchetypeFixture(components: Component*)(
      nEntities: Int = 100,
      fragmentSize: Long = DefaultFragmentSizeBytes
  ) extends ArchetypeFixture(components*)(nEntities):
    val archetype = Aggregate(Signature(componentIds))(fragmentSize)
    for e <- entities do archetype.add(e, CSeq(components*))

  class StandardFragmentFixture(components: Component*)(
      nEntities: Int = 100,
      maxEntities: Int = 100
  ) extends ArchetypeFixture(components*)(nEntities):
    require(nEntities <= maxEntities)
    val fragment = Fragment(Signature(componentIds), maxEntities)
    for e <- entities do fragment.add(e, CSeq(components*))
