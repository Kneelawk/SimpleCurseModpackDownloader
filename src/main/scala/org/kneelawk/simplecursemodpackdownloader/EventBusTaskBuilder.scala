package org.kneelawk.simplecursemodpackdownloader

trait EventBusTaskBuilder extends TaskBuilder {
  def getBus: TaskBuilderEventBus
}