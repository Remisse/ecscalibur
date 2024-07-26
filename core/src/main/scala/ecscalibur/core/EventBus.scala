package ecscalibur.core

import scala.reflect.ClassTag
import ecscalibur.util.tpe.id0K

export events.*

object events:
  import ecscalibur.core.components.*

  /** Base trait for event components. Classes extending this should be annotated with [[component]]
    * and define a companion object extending [[EventType]].
    */
  trait Event extends Component

  /** Base trait for the companion objects of event components.
    */
  trait EventType extends ComponentType

  /** Handler of events emitted by entities.
    */
  trait EventBus:
    /** Emits an [[Event]] linked to the specified entity. Will make this EventBus execute all
      * callbacks associated to the type of the specified event.
      *
      * @param entity
      *   the entity the event is linked to
      * @param event
      *   the event to emit
      */
    def emit(entity: Entity, event: Event): Unit

    /** Subscribes the listener identified by the specified name to the event type given as type
      * parameter. Everytime an event of the same type is emitted, this EventBus will execute the
      * given callback.
      *
      * @param listenerName
      *   the name of the listener to be subscribed
      * @param callback
      *   the lambda function to call when an event of type E is emitted
      * @tparam E
      *   the type of event to subscribe to
      */
    def subscribe[E <: Event: ClassTag](listenerName: String)(callback: (Entity, E) => Unit): Unit

    /** Removes the listener identified by the given name from the subscribers of the event type
      * given as type parameter. Emitting events of type `eventType` will not trigger the specified
      * listener's callback method anymore.
      *
      * @param eventType
      *   the type of event to unsubscribe from
      * @param listenerName
      *   the name of the listener to be unsubscribed
      */
    infix def unsubscribe(listenerName: String, eventType: EventType): Unit

  private[ecscalibur] object EventBus:
    def apply(): EventBus = new EventBus:
      import scala.collection.mutable

      private type ListenerName = String
      private type EventId = ComponentId

      private val listenersByEvent
          : mutable.Map[EventId, Vector[(ListenerName, (Entity, Event) => Unit)]] =
        mutable.Map.empty

      override def emit(entity: Entity, event: Event): Unit =
        if listenersByEvent.contains(event.typeId) then
          for elem <- listenersByEvent(event.typeId) do elem._2(entity, event)

      override def subscribe[E <: Event: ClassTag](listenerName: String)(
          callback: (Entity, E) => Unit
      ): Unit =
        val eventId = id0K[E]
        if !listenersByEvent.contains(eventId) then listenersByEvent += eventId -> Vector.empty
        if !listenersByEvent(eventId).exists(_._1 == listenerName) then
          listenersByEvent.update(
            eventId,
            listenersByEvent(eventId) :+ listenerName -> callback
              .asInstanceOf[(Entity, Event) => Unit]
          )

      override def unsubscribe(listenerName: String, eventType: EventType): Unit =
        if listenersByEvent.contains(eventType.typeId) then
          listenersByEvent.update(
            eventType.typeId,
            listenersByEvent(eventType.typeId).filterNot(_._1 == listenerName)
          )
