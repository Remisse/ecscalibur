package ecsutil

/** Measures the time required to run a function and puts the current thread to
  * sleep if such a time falls under a specified frame time.
  */
trait FramePacer:
  type DeltaSeconds = Float

  /** Executes `f` and returns the amount of time that was required for `f` to complete.
    *
    * If the calculated time falls under the specified frame rate cap, it will put the current
    * thread to sleep for `frameTime - elapsedTime` seconds and return `frameTime` as the time
    * delta, where `frameTime` is equal to `1 / frameCap`.
    *
    * @param f
    *   the function whose execution time must be measured
    * @return
    *   the amount of time that was required for `f` to complete
    */
  def pace(f: => Unit): DeltaSeconds

object FramePacer:
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

    private val frameTime: Option[Long] = cap match
      case 0 => None
      case _ => Some((1e9 / cap).toLong)

    import java.lang.System.nanoTime
    import java.time.Duration

    override def pace(f: => Unit): DeltaSeconds =
      val start = nanoTime()
      f
      var newDtNanos: Long = 0
      val elapsed = nanoTime() - start
      frameTime match
        case Some(ft) =>
          val overhead = ft - elapsed
          if overhead > 0 then 
            Thread.sleep(Duration.ofNanos(overhead))
          newDtNanos = ft
        case None => newDtNanos = elapsed
      (newDtNanos * 1e-9).toFloat
