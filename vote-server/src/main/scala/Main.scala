package scala.yajug

object Main extends App {
  def m:Unit = println("""
    Usage:
    |  1) Server
    |  2) Generator
    """.stripMargin)
  def r:Unit = scala.Console.readInt() match {
    case 1 => new Server
    case 2 => new Generator
    case _ => run
  }
  def run:Unit = {
    m
    r
  }
  run
}