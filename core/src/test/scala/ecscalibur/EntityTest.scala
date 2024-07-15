package ecscalibur

import ecscalibur.core.*

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

class EntityTest extends AnyFlatSpec with should.Matchers:
  inline val defaultId = 0

  "Two Entities" should "be equal only if they share the same ID" in:
    val entity = Entity(defaultId)
    entity should equal(entity)
    entity shouldNot equal(Entity(defaultId + 1))
    entity shouldNot equal(Object())
