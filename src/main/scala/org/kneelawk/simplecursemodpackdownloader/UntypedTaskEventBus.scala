package org.kneelawk.simplecursemodpackdownloader

import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import scala.collection.mutable.Set
import scala.reflect.ClassTag

/*
 * QUESION: Should I try to make sure event busses are type-specified.
 * Type-specified event busses would be individual classes for each set of events.
 * Type-specified event busses would likely require macros for ease of creation.
 * 
 * Pros of type-specified event busses:
 * Event type safety.
 * You know what possible events can be fired.
 * 
 * Cons of type-specified event busses:
 * Separate classes for each set of events.
 * Requires macros for non-tedious implementation.
 * 
 * Other possible idea: Just have subclasses of this class that implement a
 * method checking if an event type is valid. An unsupported something exception
 * would be thrown for invalid event types.
 * 
 * Event polymorphism?
 * 
 * Another idea: Make abstract. Subclasses will pass a list of types of events that they will throw.
 * (or more correctly, a list of the lowest level event types they will let listeners register for,
 * and the highest level event types they can throw. (These are the class keys for listener holding maps.
 * (so no multimaps)))
 * Registering a listener will add it to all the sets for events its event type superclasses.
 * If a registering listener's event type isn't a superclass of any of the specified events, throw some exception.
 * You can actually throw events more specific than the ones you specified, just noone will be able to listen
 * for just those events. You will not be able to throw any events more generic than the ones you specified.
 * 
 * Use a base class for all events?
 * 
 * I don't think this would need a base class for all events. Trait events???
 */
class UntypedTaskEventBus(task: UntypedTaskEventBus => Unit) {
  val listeners = new HashMap[Class[_], Set[EventListener[_]]] with MultiMap[Class[_], EventListener[_]]

  def onEvent[EventType: ClassTag](listener: EventType => Unit) {
    listeners.addBinding(implicitly[ClassTag[EventType]].runtimeClass, new EventListener[EventType](listener))
  }

  def sendEvent(event: AnyRef) {
    val eventClass = event.getClass
    if (listeners.contains(eventClass)) {
      for (listener <- listeners.get(eventClass).get) {
        listener(event)
      }
    }
  }

  def startTask = task(this)
}

class EventListener[EventType: ClassTag](listener: EventType => Unit) {
  def apply(event: AnyRef) {
    listener(event.asInstanceOf[EventType])
  }
}