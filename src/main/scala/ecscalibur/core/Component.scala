package ecscalibur.core

import ecscalibur.annotation.component
import ecscalibur.core.Components.*
import scala.reflect.ClassTag
import ecscalibur.id.IdGenerator

object Components:
  /**
    * Type representing unique component IDs.
    */
  opaque type ComponentId = Int
  val noId: ComponentId = -1

  trait Component:
    val id: ComponentId = noId
    def unary_~ = id
