package ecscalibur.util

import ecscalibur.core.component.{ComponentId, Component}
import ecscalibur.util.spark.SizeEstimator

object sizeof:
  private var cache: Map[ComponentId, Long] = Map.empty

  /**
   * Wrapper method around [[SizeEstimator.estimate]]. Caches the results
   * for faster lookups.
   * 
   * Incorrectly reports sizes greater than 4900 bytes for classes
   * declared within test classes.
   */
  def sizeOf(c: Component): Long = 
    if (cache.contains(~c)) cache(~c)
    else
      val size = SizeEstimator.estimate(c)
      cache = cache + (~c -> size)
      size
