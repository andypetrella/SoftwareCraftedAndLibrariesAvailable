package scala.yajug

import concurrent.duration._
import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import com.typesafe.config._

class Server {
  val config = ConfigFactory.load()
  val actorSystem = ActorSystem("yajug", config.getConfig("server").withFallback(config))

  actorSystem.actorOf(Props[VotedActor], "voted")
  actorSystem.actorOf(Props[VotingActor], "vote")
}

class VotedActor extends Actor {
  implicit val t = Timeout(5 seconds)

  val dir = {
    new java.io.File("/tmp/voters/").mkdirs()
    new java.io.File("/tmp/voters/")
  }

  var voters:List[(String, String)] = List.empty
  def receive = {
    case (country:String, id:String) =>
      voters = (country, id)::voters
      if (voters.size % 5 == 0) {
        //drop to file
        val f = new java.io.File(dir, "data"+".csv")
        val w = new java.io.FileWriter(f, true)
        w.write(voters.map{ case (c, i) => s"$c,$i" }.mkString("\n"))
        w.close()
        voters = List.empty
      }
      sender ! "ok"
  }
}

class VotingActor extends Actor {
  implicit val t = Timeout(5 seconds)
  val dir = {
    new java.io.File("/tmp/votes/").mkdirs()
    new java.io.File("/tmp/votes/")
  }
  var votes:List[(String, Traversable[String])] = List.empty
  def receive = {
    case (p:String, ones:Traversable[String] /*arf erasure*/) =>
      votes = (p, ones)::votes
      if (votes.size % 5 == 0) {
        //drop to file
        val f = new java.io.File(dir, "data"+".csv")
        val w = new java.io.FileWriter(f, true)
        w.write(
          votes.flatMap{ case (p, xs) =>
            xs.map{ one => s"$p,$one" }
          }.mkString("\n")
        )
        w.close()
        votes = List.empty
      }
      sender ? "ok"
  }
}