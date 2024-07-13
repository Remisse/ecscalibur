package ecsdemo

import ecsdemo.components.events.*
import ecscalibur.core.entity.Entity

object view:
  trait View:
    def handleStoppedEvent(e: Entity): Unit

    def handleResumedEvent(e: Entity): Unit

    def handleChangedVelocityEvent(e: Entity, ev: ChangedVelocityEvent): Unit

    def handleChangedColorEvent(e: Entity, ev: ChangedColorEvent): Unit

  object View:
    def empty(): View = new View:
      override def handleChangedColorEvent(e: Entity, ev: ChangedColorEvent): Unit = ()
      override def handleChangedVelocityEvent(e: Entity, ev: ChangedVelocityEvent): Unit = ()
      override def handleResumedEvent(e: Entity): Unit = ()
      override def handleStoppedEvent(e: Entity): Unit = ()

    def terminal(): View = new View:
      override def handleStoppedEvent(e: Entity): Unit =
        println(format(e, "has stopped"))

      override def handleResumedEvent(e: Entity): Unit =
        println(format(e, "has resumed its movement"))

      override def handleChangedVelocityEvent(e: Entity, ev: ChangedVelocityEvent): Unit =
        println(format(e, s"has changed its velocity to ${ev.newVelocity}"))

      override def handleChangedColorEvent(e: Entity, ev: ChangedColorEvent): Unit =
        println(format(e, s"has changed its color to ${ev.newColor}"))

      private inline def format(e: Entity, message: String): String = s"$e\t$message."
