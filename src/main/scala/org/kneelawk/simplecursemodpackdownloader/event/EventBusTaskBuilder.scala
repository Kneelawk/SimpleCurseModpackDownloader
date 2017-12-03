package org.kneelawk.simplecursemodpackdownloader.event

import org.kneelawk.simplecursemodpackdownloader.task.TaskBuilder

trait EventBusTaskBuilder extends TaskBuilder {
  def bus: TaskBuilderEventBus
}