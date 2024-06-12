package ecscalibur

object Statics:
  trait UniqueId:
    val id: Int

  object UniqueId:
    private var currentAvailableId = 0
    def newId: Int =
      val retVal = currentAvailableId
      currentAvailableId = currentAvailableId + 1
      retVal
  
  abstract class ComponentType extends UniqueId:
    import Statics.UniqueId.newId
    val id: Int = newId
  
  case class Position(var x: Float, var y: Float)
  object Position extends ComponentType

  case class Velocity(var dx: Float, var dy: Float)
  object Velocity extends ComponentType
