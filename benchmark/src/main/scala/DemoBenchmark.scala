package benchmark

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

import scala.compiletime.uninitialized

import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
class DemoBenchmark:
  private var ecsWorker300: Worker = uninitialized
  private var ecsWorker3000: Worker = uninitialized
  private var ecsWorker30000: Worker = uninitialized
  private var ecsWorker300000: Worker = uninitialized
  private var oopWorker300: Worker = uninitialized
  private var oopWorker3000: Worker = uninitialized
  private var oopWorker30000: Worker = uninitialized
  private var oopWorker300000: Worker = uninitialized

  import ecscalibur.core.*

  @Setup
  def setup(blackhole: Blackhole): Unit =
    inline val entitiesCount300 = 100
    inline val entitiesCount3000 = 1000
    inline val entitiesCount30000 = 10000
    inline val entitiesCount300000 = 100000

    inline val interval = Float.MinPositiveValue
    inline val iterations = 100

    def ecsBase(count: Int): Worker = new Worker:
      private given view: ecsdemo.view.View = ecsdemo.view.View.empty()
      private val controller = ecsdemo.controller.Controller()
      private given world: World = World()
      private val model = ecsdemo.model.Model(interval)
      for _ <- 0 until count do model.bindEntitiesTo(world)
      model.bindSystemsTo(world)
      controller bindListenersTo world

      override def work: Unit =
        blackhole.consume:
          world loop iterations.times

    def oopBase(count: Int): Worker = new Worker:
      private val model = oopdemo.model.Model(interval)
      private val objects = (for _ <- 0 until count yield model.objects).flatten
      private val view: oopdemo.View = oopdemo.View.empty()
      private val controller = oopdemo.controller.Controller(iterationsPerSecond = 0, maxIterations = iterations)
      for o <- objects do view.bind(o)

      override def work: Unit =
        blackhole.consume:
          controller.loop: deltaTime =>
            given oopdemo.objects.DeltaTime = deltaTime
            view.onUpdate
            for o <- objects do o.onUpdate

    ecsWorker300 = ecsBase(entitiesCount300)
    ecsWorker3000 = ecsBase(entitiesCount3000)
    ecsWorker30000 = ecsBase(entitiesCount30000)
    ecsWorker300000 = ecsBase(entitiesCount300000)
    oopWorker300 = oopBase(entitiesCount300)
    oopWorker3000 = oopBase(entitiesCount3000)
    oopWorker30000 = oopBase(entitiesCount30000)
    oopWorker300000 = oopBase(entitiesCount300000)

  @Benchmark
  def ecs300Entities(): Unit = ecsWorker300.work
  @Benchmark
  def ecs3000Entities(): Unit = ecsWorker3000.work
  @Benchmark
  def ecs30000Entities(): Unit = ecsWorker30000.work
  // @Benchmark
  // def ecs300000Entities(): Unit = ecsWorker300000.work
  @Benchmark
  def oop300Entities(): Unit = oopWorker300.work
  @Benchmark
  def oop3000Entities(): Unit = oopWorker3000.work
  @Benchmark
  def oop30000Entities(): Unit = oopWorker30000.work
  // @Benchmark
  // def oop300000Entities(): Unit = oopWorker300000.work
