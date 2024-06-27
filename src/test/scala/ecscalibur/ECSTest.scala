package ecscalibur

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

class ECSTests extends AnyFlatSpec with should.Matchers:
  import ecscalibur.core.*

  "An entity" should "be created succesfully" in:
    given world: World = World()
    val entity: Entity = world.spawn
    world.isValid(entity) shouldBe true

  import ecscalibur.testutil.testclasses
  import testclasses.C1

  it should "be able to have components" in:
    given world: World = World()
    val entity = world.spawn
    entity += C1()
    entity has C1 shouldBe true

  it should "not have multiple occurrences of the same component type" in:
    given world: World = World()
    val entity = world.spawn
    entity += C1()
    an[IllegalArgumentException] should be thrownBy (entity += C1())

  it should "not have a component that was never explicitly added to it" in:
    given world: World = World()
    val entity = world.spawn
    entity has C1 shouldBe false

  import testclasses.{C2, C3}

  it should "be able to correctly remove its components" in:
    given world: World = World()
    val entity = world.spawn
    entity += C1()
      += C2()
      += C3()
    entity -= C1
      -= C3
    entity.has(C1, C2, C3) shouldBe false
    !(entity has C1) && (entity has C2) && !(entity has C3) shouldBe true

  it should "throw when attemping to remove a non-existing component" in:
    given world: World = World()
    val entity = world.spawn
    an[IllegalArgumentException] should be thrownBy (entity -= C1)
