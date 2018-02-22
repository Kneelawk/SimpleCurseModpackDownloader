package org.kneelawk.simplecursemodpackdownloader.util

import java.util.concurrent.locks.Lock

object LockUtil {
  
  /**
   * Lock a lock before a scope and unlock it after.
   */
  def lock[R](lock: Lock)(func: => R): R = {
    lock.lock()
    try {
      func
    } finally {
      lock.unlock()
    }
  }
  
  /**
   * Try-lock a lock before a scope and unlock it after.
   */
  def tryLock[R](lock: Lock)(func: => R): Option[R] = {
    if (lock.tryLock()) {
      try {
        Some(func)
      } finally {
        lock.unlock()
      }
    } else None
  }
}