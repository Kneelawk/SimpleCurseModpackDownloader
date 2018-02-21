package org.kneelawk.simplecursemodpackdownloader.task

import org.kneelawk.simplecursemodpackdownloader.event.EventBus

class TaskEventBus[Builder <: TaskBuilder](b: Builder) extends EventBus {
  def builder: Builder = b
}