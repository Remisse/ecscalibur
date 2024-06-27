package ecscalibur.util

import ecscalibur.core.component.{ComponentId, Component}
import ecscalibur.util.spark.SizeEstimator

object sizeof:
  private var cache: Map[ComponentId, Long] = Map.empty

  def sizeOf(c: Component): Long = 
    if (cache.contains(~c)) cache(~c)
    else
      val size = SizeEstimator.estimate(c)
      cache = cache + (~c -> size)
      size
