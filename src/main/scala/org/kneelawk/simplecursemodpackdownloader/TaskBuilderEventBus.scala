package org.kneelawk.simplecursemodpackdownloader

trait TaskBuilderEventBus extends EventBus {
  def builder: EventBusTaskBuilder
}