package ecscalibur.core

object context:
  trait MetaContext:
    def deltaTime: Float
    private[ecscalibur] def setDeltaTime(dt: Float): Unit

  object MetaContext:
    def apply(): MetaContext = new MetaContext:
      var _deltaTime: Float = 0.0

      override def deltaTime: Float = _deltaTime
      override def setDeltaTime(dt: Float) =
        require(dt >= 0.0, s"Given delta time is not a positive number ($dt)")
        _deltaTime = dt
