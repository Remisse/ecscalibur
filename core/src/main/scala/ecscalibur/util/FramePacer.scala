package ecscalibur.util

private[ecscalibur] trait FramePacer:
  def pace(): Float

private[ecscalibur] object FramePacer:
  private inline val Uninitialized = -1

  def apply(cap: Int = 0): FramePacer = new FramePacer:
    require(cap >= 0, "Frame cap must be a non-negative number.")

    private var lastUpdateTime: Long = Uninitialized
    private val frameTime: Option[Long] = cap match
      case 0 => None
      case _ => Some(((1.0 / cap) * 1e+9).toLong)

    import java.lang.System.nanoTime
    import java.time.Duration

    override def pace(): Float =
      val time = nanoTime()
      var newDtNanos: Long = 0
      if (lastUpdateTime != Uninitialized)
        val elapsed = time - lastUpdateTime
        frameTime match
          case Some(ft) =>
            val overhead = math.max(ft - elapsed, 0)
            if (overhead > 0) Thread.sleep(Duration.ofNanos(overhead))
            newDtNanos = elapsed + overhead
          case None => newDtNanos = elapsed
      lastUpdateTime = time
      (newDtNanos * 1e-9).toFloat
