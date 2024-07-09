package ecsutil

/** Tracks the time elapsed between each call to [[FramePacer.pace]] and puts the current thread to
  * sleep if such time falls under a specified frame time.
  */
trait FramePacer:
  type DeltaSeconds = Float

  /** Returns the amount of time elapsed since the last call to this method.
    *
    * If called for the first time, it will return 0.
    *
    * If the calculated time falls under the specified frame rate cap, it will put the current
    * thread to sleep for `frameTime - elapsedTime` seconds and return `frameTime` as the time
    * delta, where `frameTime` is equal to `1 / frameCap`.
    *
    * @return
    *   the amount of time elapsed since the last call.
    */
  def pace(): DeltaSeconds

object FramePacer:
  private inline val Uninitialized = -1

  /** Creates a new [[FramePacer]] instance with the given frame rate limit. A limit equal to 0
    * means that no limit should be enforced.
    *
    * @param cap
    *   the frame rate limit that this FramePacer should enforce on the current thread.
    * @return
    *   a new FramePacer instance
    */
  def apply(cap: Int = 0): FramePacer = new FramePacer:
    require(cap >= 0, "Frame cap must be a non-negative number.")

    private var lastUpdateTime: Long = Uninitialized
    private val frameTime: Option[Long] = cap match
      case 0 => None
      case _ => Some(((1.0 / cap) * 1e+9).toLong)

    import java.lang.System.nanoTime
    import java.time.Duration

    override def pace(): DeltaSeconds =
      val time = nanoTime()
      var newDtNanos: Long = 0
      if lastUpdateTime != Uninitialized then
        val elapsed = time - lastUpdateTime
        frameTime match
          case Some(ft) =>
            val overhead = math.max(ft - elapsed, 0)
            if overhead > 0 then Thread.sleep(Duration.ofNanos(overhead))
            newDtNanos = ft
          case None => newDtNanos = elapsed
      lastUpdateTime = time
      (newDtNanos * 1e-9).toFloat
