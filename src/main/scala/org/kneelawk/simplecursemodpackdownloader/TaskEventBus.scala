package org.kneelawk.simplecursemodpackdownloader

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
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
 * 
 * Question: Is there a way to build this class so it relies on compile time checks rather than run time ones
 * without using macros? HList type parameter? How would I check that a registered event listener was registering
 * for one of the specified events?
 * 
 * Should I use a list of event classes as a constructor arg, or should I have a type parameter that expects a tuple?
 */
abstract class TaskEventBus(task: TaskEventBus => Unit, val eventClasses: List[Class[_]]) {
  private val listeners = new HashMap[Class[_], Set[EventListener[_]]]
  eventClasses.foreach(listeners.put(_, new HashSet[EventListener[_]]))

  def register[EventType: ClassTag](listener: EventType => Unit) {
    val cls = implicitly[ClassTag[EventType]].runtimeClass
    var registered = false
    for (key <- listeners.keys; if cls.isAssignableFrom(key)) {
      listeners(key) += new EventListener[EventType](listener)
      registered = true
    }
    if (!registered)
      throw new UnsupportedOperationException(cls.getName + " is neither one of this event bus's event types nor is it a super class of one")
  }

  def sendEvent(event: AnyRef) {
    val eventClass = event.getClass
    
    // This set could be used for sorting listener priorities in the future
    val foundListeners = new HashSet[EventListener[_]]
    var foundClasses = false
    for (key <- listeners.keys; if key.isAssignableFrom(eventClass)) {
      for (listener <- listeners(key)) {
        foundListeners += listener
      }
      foundClasses = true
    }
    if (!foundClasses)
      throw new UnsupportedOperationException(event.getClass.getName + " is too generic an event for this event bus")
    foundListeners.foreach(_(event))
  }

  def startTask = task(this)
}

class EventListener[EventType: ClassTag](listener: EventType => Unit) {
  def apply(event: AnyRef) {
    listener(event.asInstanceOf[EventType])
  }
}