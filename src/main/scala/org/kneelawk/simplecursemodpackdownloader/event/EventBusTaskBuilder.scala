package org.kneelawk.simplecursemodpackdownloader.event

import org.kneelawk.simplecursemodpackdownloader.TaskBuilder

trait EventBusTaskBuilder extends TaskBuilder {
  def bus: TaskBuilderEventBus
}