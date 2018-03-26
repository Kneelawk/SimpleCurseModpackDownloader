package org.kneelawk.simplecursemodpackdownloader.event

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.Set
import scala.reflect.runtime.{ universe => ru } // to work around annoying eclipse import organization bug
import scala.collection.mutable.MultiMap

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
 * 
 * Btw, now that we are working on an actual task system, EventBusses should only hand the event bus stuff.
 * 
 * Is there any way we can make EventBusses into a trait without using macros?
 * They would be really handy things to tack onto TaskBuilders.
 * 
 * Another thought, should EventBusses be their own objects? (not designed to be superclasses)
 * Especially, cause you might not want to hang onto a builder, but you might want to hang onto an EventBus.
 * Passing EventBusses around would mean that they would need to be typed.
 * Honestly, I'm not sure you'd want to hang onto an event buss either.
 * 
 * Looks like it's getting too tricky to specify the types an event bus can handle ahead of time.
 * I'm making event busses untyped.
 */
abstract class EventBus() {
  private val listeners = new HashMap[ru.Type, Set[EventListener[_]]] with MultiMap[ru.Type, EventListener[_]]

  def register[EventType: ru.TypeTag](listener: EventType => Unit): this.type = {
    val tpe = ru.typeOf[EventType]
    listeners.addBinding(tpe, new EventListener[EventType](listener))

    return this
  }

  def sendEvent[EventType <: AnyRef: ru.TypeTag](event: EventType) {
    val tpe = ru.typeOf[EventType]

    // This set could be used for sorting listener priorities in the future
    val foundListeners = new HashSet[EventListener[_]]
    for (key <- listeners.keys; if tpe <:< key) {
      for (listener <- listeners(key)) {
        foundListeners += listener
      }
    }
    foundListeners.foreach(_(event))
  }
}

class EventListener[EventType: ru.TypeTag](listener: EventType => Unit) {
  def apply(event: AnyRef) {
    listener(event.asInstanceOf[EventType])
  }
}