package ecscalibur.core

import entity.Entity
import components.{Component, ComponentType}

/** Handles operations that can cause performance-heavy structural changes to a [[World]] instance
  * and its internal state.
  */
trait Mutator:
  /** Schedules a [[DeferredRequest]] to be processed on the next [[World]] loop. Until
    * then, the [[World]]'s state will not be affected.
    *
    * @param q
    *   the request to be processed by this Mutator
    * @return
    *   true if the request has been accepted for processing, false otherwise
    */
  infix def defer(q: DeferredRequest): Boolean

  /** Processes the given [[ImmediateRequest]] immediately.
    *
    * @param q
    *   the request to be executed by this Mutator
    * @return
    *   true if the request has been processed successfully, false otherwise
    */
  infix def doImmediately(q: ImmediateRequest): Boolean

/** Class of requests to be executed immediately.
  */
enum ImmediateRequest:
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

  /** Request to update the reference to the given Component type for the given Entity.
    *
    * @param e
    *   the Entity for which the given Component must be updated
    * @param c
    *   the Component to update
    * @throws IllegalArgumentException
    *   if the given Entity does not exist
    */
  case update(e: Entity, c: Component)

/** Class of requests to be buffered and then processed at the start of a new [[World]] loop.
  */
enum DeferredRequest:
  /** Request to create a new Entity with the given components.
    *
    * @param components
    *   components of the new Entity
    */
  case createEntity(components: Component*)

  /** Request to delete an existing Entity.
    *
    * @param e
    *   entity to be deleted
    */
  case deleteEntity(e: Entity)

  /** Request to add a Component to the given Entity. This request can fail, either because the
    * entity already has the same component or because the entity was deleted before this 
    * operation could be executed
    *
    * @param e
    *   entity to which the Component will be added
    * @param component
    *   component to be added
    */
  case addComponent(e: Entity, component: Component)

  /** Request to remove a Component from the given Entity. This request can fail, either because the
    * entity does not have this component or because the entity was deleted before this operation 
    * could be executed
    *
    * @param e
    *   entity from which the Component will be removed
    * @param cType
    *   component to be removed
    */
  case removeComponent(e: Entity, cType: ComponentType)
