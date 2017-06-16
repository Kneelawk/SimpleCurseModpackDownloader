package org.kneelawk.simplecursemodpackdownloader.console

import java.io.Console
import java.io.PrintStream
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader

trait ConsoleInterface {
  def prompt(fmt: String, args: Object*): String

  def promptHiddenInput(fmt: String, args: Object*): String
}

class DefaultConsoleInterface(console: Console) extends ConsoleInterface {
  override def prompt(fmt: String, args: Object*) = console.readLine(fmt, args)

  override def promptHiddenInput(fmt: String, args: Object*) = new String(console.readPassword(fmt, args))
}

class EclipseConsoleInterface(out: PrintStream, in: InputStream) extends ConsoleInterface {
  val reader = new BufferedReader(new InputStreamReader(in))
  
  override def prompt(fmt: String, args: Object*) = {
    out.printf(fmt, args)
    reader.readLine()
  }

  override def promptHiddenInput(fmt: String, args: Object*) = {
    out.printf(fmt, args)
    reader.readLine()
  }
}

object ConsoleInterfaceFactory {
  def getConsoleInterface: ConsoleInterface = {
    val console = System.console()
    if (console != null) {
      return new DefaultConsoleInterface(console)
    } else if (System.in != null) {
      return new EclipseConsoleInterface(System.out, System.in)
    } else {
      return null
    }
  }
}