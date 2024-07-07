package ecscalibur.core

object context:
  /**
    * Container of secondary data related to the [[World]]'s execution.
    */
  trait MetaContext:
    /**
      * Amount of seconds passed since the last [[World]] loop.
      *
      * @return
      */
    def deltaTime: Float

    private[ecscalibur] def setDeltaTime(dt: Float): Unit

  object MetaContext:
    /**
      * Creates a new MetaContext instance.
      *
      * @return
      * a new MetaContext instance.
      */
    def apply(): MetaContext = new MetaContext:
      var _deltaTime: Float = 0.0

      override def deltaTime: Float = _deltaTime
      override def setDeltaTime(dt: Float) =
        require(dt >= 0.0, s"Given delta time is not a positive number ($dt)")
        _deltaTime = dt
