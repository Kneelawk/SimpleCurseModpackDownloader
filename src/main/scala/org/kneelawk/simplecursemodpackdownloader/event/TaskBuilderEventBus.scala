package org.kneelawk.simplecursemodpackdownloader.event

trait TaskBuilderEventBus extends EventBus {
  def builder: EventBusTaskBuilder
}