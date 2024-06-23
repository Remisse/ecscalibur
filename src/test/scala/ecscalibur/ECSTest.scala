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

  import ecscalibur.core.{Component, ComponentType}
  import Annotations.component

  @component
  class Comp1 extends Component
  object Comp1 extends ComponentType

  it should "be able to have components" in:
    given world: World = World()
    val entity = world.spawn
    entity += Comp1()
    entity has Comp1 shouldBe true

  it should "not have multiple occurrences of the same component type" in:
    given world: World = World()
    val entity = world.spawn
    entity += Comp1()
    an[IllegalArgumentException] should be thrownBy (entity += Comp1())

  it should "not have a component that was never explicitly added to it" in:
    given world: World = World()
    val entity = world.spawn
    entity has Comp1 shouldBe false

  @component
  class Comp2 extends Component
  object Comp2 extends ComponentType

  @component
  class Comp3 extends Component
  object Comp3 extends ComponentType

  it should "be able to correctly remove its components" in:
    given world: World = World()
    val entity = world.spawn
    entity += Comp1()
      += Comp2()
      += Comp3()
    entity -= Comp1
      -= Comp3
    entity.has(Comp1, Comp2, Comp3) shouldBe false
    !(entity has Comp1) && (entity has Comp2) && !(entity has Comp3) shouldBe true

  it should "throw when attemping to remove a non-existing component" in:
    given world: World = World()
    val entity = world.spawn
    an[IllegalArgumentException] should be thrownBy (entity -= Comp1)
