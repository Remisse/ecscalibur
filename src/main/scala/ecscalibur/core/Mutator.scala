package ecscalibur.core

import ecscalibur.core.component.Component
import ecscalibur.core.component.ComponentType

trait Mutator:
  infix def defer(q: SystemRequest | EntityRequest): Boolean
  infix def isSystemRunning(name: String): Boolean
  infix def isSystemPaused(name: String): Boolean

enum SystemRequest:
  case pause(systemName: String)
  case resume(systemName: String)

enum EntityRequest:
  case create(components: CSeq[Component])
  case delete(e: Entity)
  case addComponent(e: Entity, component: Component, orElse: () => Unit = () => ())
  case removeComponent(e: Entity, cType: ComponentType, orElse: () => Unit = () => ())
