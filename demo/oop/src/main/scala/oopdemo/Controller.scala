package oopdemo

import ecsutil.FramePacer
import oopdemo.objects.DeltaTime

object controller:
  trait Controller:
    def loop(f: DeltaTime => Unit): Unit

  object Controller:
    def apply(iterationsPerSecond: Int, maxIterations: Int = 0): Controller = new Controller:
      val pacer = FramePacer(iterationsPerSecond)
      var dt: DeltaTime = 0f

      def loop(f: DeltaTime => Unit): Unit =
        maxIterations match
            case 0 => while true do dt = pacer.pace(f(dt))
            case _ => for _ <- (0 until maxIterations) do dt = pacer.pace(f(dt))
