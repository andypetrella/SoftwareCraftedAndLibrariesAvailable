package scala.yajug

import concurrent._
import concurrent.duration._
import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import com.typesafe.config._

class Generator {
  val config = ConfigFactory.load()
  val actorSystem = ActorSystem("client", config.getConfig("client").withFallback(config))

  val VotedActor = actorSystem.actorSelection("akka.tcp://yajug@127.0.0.1:5225/user/voted")
  val VotingActor = actorSystem.actorSelection("akka.tcp://yajug@127.0.0.1:5225/user/vote")

  val chosenOnes = Map(
    "X" -> ('A' to 'Z').take(5).map(_.toString).toList,
    "Y" -> ('A' to 'Z').drop(5).take(5).map(_.toString).toList,
    "Z" -> ('A' to 'Z').drop(10).take(5).map(_.toString).toList
  )

  val countries = List(
    "BE" -> "Belgium",
    "DE" -> "Germany",
    "FR" -> "France",
    "LU" -> "Luxemburg"
  )
  import scala.util.Random._
  import ExecutionContext.Implicits.global
  def pick[A](xs:Seq[A]):A = xs(nextInt(xs.length))
  for { i <- 1 to 1000 } {
    for { j <- 1 to 10} {
      future {
        for { k <- 1 to 1000 } {
          val country = countries(nextInt(countries.length))._1
          val id = (1 to 10).map(_ => nextPrintableChar).filter(x => x != '"' && x != '\'' && x != ',').mkString("")
          val p = pick(chosenOnes.keys.toList)
          val n = pick(1 to chosenOnes(p).length)
          val ones = (1 to n).map { _ => pick(chosenOnes(p)) }.toSeq
          VotedActor ! (country, id)
          VotingActor ! (p, ones)
        }
        println(s"End publishing batch of 1000 #$i-$j")
      }
    }
    Thread.sleep(60 * 1000)
  }


}