package de.lolhens.serverdesktop

import cats.effect.{Blocker, ExitCode}
import cats.syntax.semigroupk._
import monix.eval.{Task, TaskApp}
import monix.execution.Scheduler
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.task._
import org.http4s.implicits._
import org.http4s.scalatags._
import org.http4s.server.Router
import org.http4s.server.staticcontent.WebjarService.WebjarAsset
import org.http4s.server.staticcontent.{ResourceServiceBuilder, WebjarServiceBuilder}

object Server extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] =
    Task.deferAction { implicit scheduler =>
      BlazeServerBuilder[Task](scheduler)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(service.orNotFound)
        .resource
        .use(_ => Task.never)
    }

  lazy val resourceScheduler: Scheduler = Scheduler.io(name = "http4s-resources")
  lazy val blocker: Blocker = Blocker.liftExecutionContext(resourceScheduler)

  def webjarUri(asset: WebjarAsset) =
    s"assets/${asset.library}/${asset.version}/${asset.asset}"

  lazy val service: HttpRoutes[Task] = Router(
    "/assets" -> {
      (WebjarServiceBuilder[Task](blocker).toRoutes: HttpRoutes[Task]) <+>
        ResourceServiceBuilder[Task]("/assets", blocker).toRoutes
    },

    "/" -> {
      HttpRoutes.of {
        case request@GET -> Root =>
          Ok(MainPage())
      }
    }
  )
}
