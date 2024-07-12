package oopdemo

import oopdemo.extenders.Extender
import demoutil.transform.Vector2

object objects:
  type DeltaTime = Float

  trait Updatable:
    def onUpdate(using DeltaTime): Unit

  trait SceneObject(final val name: String) extends Updatable:
    var position: Vector2 = Vector2.zero
    val repeatedAction: RepeatedAction
    private var extensions: Set[Updatable] = Set.empty

    final override def onUpdate(using DeltaTime): Unit =
      for e <- extensions do e.onUpdate
      update()

    protected def update()(using DeltaTime): Unit = ()

    final inline def addExtender(e: Extender): Unit =
      extensions = extensions + e

    override def toString: String = name 
