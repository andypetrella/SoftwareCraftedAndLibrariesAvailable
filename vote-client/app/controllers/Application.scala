package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import scala.concurrent._

object Application extends Controller {
  case class Voter(country:String, id:String)
  val newVoter = Form(
    mapping(
    "country" -> text,
    "id" -> text
    )(Voter.apply)(Voter.unapply)
  )

  val countries = Map(
    "BE" -> "Belgium",
    "DE" -> "Germany",
    "FR" -> "France",
    "LU" -> "Luxemburg"
  )

  def index = Action {
    Ok(views.html.index("Vote for me", countries, newVoter))
  }

  case class One(p:String, name:String)
  val chosenOnes = Map(
    "X" -> ('A' to 'Z').take(5).map(x=>One("X", x.toString)).toSeq,
    "Y" -> ('A' to 'Z').drop(5).take(5).map(x=>One("Y", x.toString)).toSeq,
    "Z" -> ('A' to 'Z').drop(10).take(5).map(x=>One("Z", x.toString)).toSeq
  )
  def voteUI = Action { implicit request =>
    newVoter.bindFromRequest.fold(
      success = {
        case Voter(c, id) => Ok(views.html.ui(chosenOnes)).withSession("country" -> c, "id" -> id)
      },
      hasErrors = badForm => BadRequest("Missing info")
    )
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  import play.api.Play.current
  import play.api.libs.concurrent.Akka
  import akka.pattern.ask
  val VotedActor = Akka.system.actorSelection("akka.tcp://yajug@127.0.0.1:5225/user/voted")
  val VotingActor = Akka.system.actorSelection("akka.tcp://yajug@127.0.0.1:5225/user/vote")

  def vote = Action.async { request =>
    //fetch vote information
    val body = request.body.asFormUrlEncoded // Option[Map[String, Seq[String]]]

    val pO  = body.flatMap(_.get("p"))       // Option[Seq[String]]
                  .flatMap(_.headOption)     // Option[Seq[String]]

    val onesO = body.flatMap(_.get("ones"))  // Option[Seq[String]]
                    .flatMap{ ones =>
                      pO.map(p => (p, ones))
                    } // Option[(String, Seq[String])]
                    .map { case (p, xs) =>
                      xs.map { n =>
                        One(p, n)
                      }
                    }

    val dataO = for {
      country <- request.session.get("country")
      id      <- request.session.get("id")
      p       <- pO
      ones    <- onesO
    } yield (country, id, p, ones)

    dataO map { case (country, id, p, ones) =>
      // for comprehension to chain publishing of both info
      import akka.util.Timeout
      import concurrent.duration._
      implicit val t = Timeout(5 seconds)
      for {
        voted <- VotedActor  ? (country, id)
        vote  <- VotingActor ? (p, ones.map(one => one.name))
      } yield {
        // return the async chain ACK
        Ok("Your vote has been accepted")
      }
    } getOrElse {
      future { BadRequest("missing data") }
    }
  }

}