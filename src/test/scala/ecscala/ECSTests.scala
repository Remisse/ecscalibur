package ecscala

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

class ECSTests extends AnyFlatSpec with should.Matchers:
  import ecscalibur.Worlds.*

  "An entity" should "be created succesfully" in:
    given world: World = World()
    val entity: Entity = world.spawn
    world.exists(entity) shouldBe true

  import ecscalibur.Components.Component
  class Comp1 extends Component
  class Comp2 extends Component
  it should "be able to have components" in:
    given world: World = World()
    val entity = world.spawn
    entity += Comp1()
    entity.has[Comp1] shouldBe true
    an [IllegalStateException] should be thrownBy (entity += Comp1())
    entity.has[Comp2] shouldBe false

  class Comp3 extends Component
  it should "be able to correctly remove its components" in:
    given world: World = World()
    val entity = world.spawn
    entity += Comp1() += Comp2() += Comp3()
    entity.remove[Comp1]
    (!entity.has[Comp1] && entity.has[Comp2] && entity.has[Comp3]) shouldBe true
