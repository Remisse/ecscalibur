package oopdemo

import oopdemo.objects.DeltaTime
import oopdemo.extenders.Extender

trait RepeatedAction(val intervalSeconds: Float) extends Extender:
  private var currentTime: Float = 0f 
  private var observers: Set[Observer] = Set.empty

  def execute(): Unit

  def makeEvent(): Event

  final inline def addObserver(o: Observer): Unit = observers = observers + o

  final override def onUpdate(using dt: DeltaTime): Unit =
    currentTime += dt
    if currentTime >= intervalSeconds then 
      executeTemplate()
      currentTime = 0f

  private inline def executeTemplate(): Unit =
    execute()
    for o <- observers do o.signal(makeEvent())
