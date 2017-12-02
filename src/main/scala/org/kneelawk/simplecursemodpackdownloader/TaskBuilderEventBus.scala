package org.kneelawk.simplecursemodpackdownloader

trait TaskBuilderEventBus extends EventBus {
  def getBuilder: EventBusTaskBuilder
}