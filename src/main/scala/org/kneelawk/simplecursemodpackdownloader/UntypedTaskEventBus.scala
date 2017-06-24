package org.kneelawk.simplecursemodpackdownloader

import scala.collection.mutable.HashMap
import scala.collection.mutable.Set
import scala.collection.mutable.MultiMap
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
 * Seperate classes for each set of events.
 * Requires macros for non-tedious implementation.
 */
class UntypedTaskEventBus(task: UntypedTaskEventBus => Unit) {
  val listeners = new HashMap[Class[_], Set[EventListener[_]]] with MultiMap[Class[_], EventListener[_]]

  def onEvent[EventType](listener: EventType => Unit)(implicit ct: ClassTag[EventType]) {
    listeners.addBinding(ct.runtimeClass, new EventListener[EventType](listener))
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