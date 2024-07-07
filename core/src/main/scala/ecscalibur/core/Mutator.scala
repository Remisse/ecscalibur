package ecscalibur.core

import ecscalibur.core.component.Component
import ecscalibur.core.component.ComponentType

/** Handles operations that can cause performance-heavy structural changes to a [[World]]
  * instance and its internal state.
  */
trait Mutator:
  /** Schedules a request for structural changes for execution on the next [[World]] loop. Until
    * then, the [[World]]'s state will not be affected.
    *
    * @param q
    *   the request to be executed by this Mutator
    * @return
    *   true if the request has been accepted for execution, false otherwise
    */
  infix def defer(q: SystemRequest | EntityRequest): Boolean

  /** Checks whether the [[System]] identified by the given name is currently running.
    *
    * @param name
    *   name of the system to check
    * @return
    *   true if the system is running, false otherwise
    */
  infix def isSystemRunning(name: String): Boolean

  /** Checks whether the [[System]] identified by the given name is currently paused.
    *
    * @param name
    *   name of the system to check
    * @return
    *   true if the system is paused, false otherwise
    */
  infix def isSystemPaused(name: String): Boolean

/** Class of requests related to the execution of a [[System]].
  */
enum SystemRequest:
  /** Request to pause the system identified by the given name.
    *
    * @param systemName
    *   name of the system to be paused
    */
  case pause(systemName: String)

  /** Request to resume the system identified by the given name.
    *
    * @param systemName
    *   name of the system to be resumed
    */
  case resume(systemName: String)

/** Class of requests related to an [[Entity]] and its [[Component]]s.
  */
enum EntityRequest:
  /** Request to create a new Entity with the given components.
    *
    * @param components
    *   components of the new Entity
    */
  case create(components: CSeq[Component])

  /** Request to delete an existing Entity.
    *
    * @param e
    *   entity to be deleted
    */
  case delete(e: Entity)

  /** Request to add a Component to the given Entity. If this request fails (either because the
    * entity already has the same component or the entity is deleted before this operation can be
    * executed), the 'orElse' function will be executed instead.
    *
    * @param e
    *   entity to which the Component will be added
    * @param component
    *   component to be added
    * @param orElse
    *   callback that will be executed if this request fails
    */
  case addComponent(e: Entity, component: Component, orElse: () => Unit = () => ())

  /** Request to remove a Component from the given Entity. If this request fails (either because the
    * entity does not have this component or the entity is deleted before this operation can be
    * executed), the 'orElse' function will be executed instead.
    *
    * @param e
    *   entity from which the Component will be removed
    * @param cType
    *   component to be removed
    * @param orElse
    *   callback that will be executed if this request fails
    */
  case removeComponent(e: Entity, cType: ComponentType, orElse: () => Unit = () => ())
