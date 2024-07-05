package ecscalibur.core

import ecscalibur.core.component.{Component, ComponentType, CSeq}

trait Mutator:
  infix def defer(q: SystemRequest | EntityRequest): Boolean
  infix def isSystemRunning(name: String): Boolean
  infix def isSystemPaused(name: String): Boolean

enum SystemRequest:
  case stop(systemName: String)
  case resume(systemName: String)

enum EntityRequest:
  case create(components: CSeq)
  case delete(e: Entity)
  case addComponent(e: Entity, component: Component, orElse: () => Unit = () => ())
  case removeComponent(e: Entity, cType: ComponentType, orElse: () => Unit = () => ())
