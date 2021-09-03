package de.lolhens.serverdesktop

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.semigroupk._
import io.circe.Json
import io.circe.syntax._
import org.apache.commons.imaging.ImagingConstants.PARAM_KEY_FILENAME
import org.apache.commons.imaging.{ImageFormats, Imaging}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.middleware.FollowRedirect
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.jdkhttpclient.JdkHttpClient
import org.http4s.scalatags._
import org.http4s.server.Router
import org.http4s.server.staticcontent.WebjarService.WebjarAsset
import org.http4s.server.staticcontent.{ResourceServiceBuilder, WebjarServiceBuilder}
import org.http4s.{HttpRoutes, Uri}
import scodec.bits.ByteVector

import java.io.ByteArrayInputStream

object Server extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    applicationResource.use(_ => IO.never)

  private val applicationResource: Resource[IO, Unit] =
    for {
      client <- JdkHttpClient.simple[IO]
      ec <- Resource.eval(IO.executionContext)
      _ <- BlazeServerBuilder[IO](ec)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(service(FollowRedirect(99)(client)).orNotFound)
        .resource
    } yield ()

  def webjarUri(asset: WebjarAsset) =
    s"assets/${asset.library}/${asset.version}/${asset.asset}"

  private val apps: Seq[App] = Seq(
    App(id = AppId.create(), title = "Google", url = "https://www.google.de/", description = "Google Search Engine", webservice = false),
    App(id = AppId.create(), title = "Firefox", url = "https://www.mozilla.org/de/firefox/", description = "Mozilla Firefox Browser", webservice = false),
    App(id = AppId.create(), title = "GitHub", url = "https://github.com/", description = "Code Collaboration Platform", webservice = false),
    App(id = AppId.create(), title = "Hacker News", url = "https://news.ycombinator.com/", description = "Hacker News", webservice = false),
    App(id = AppId.create(), title = "YouTube", url = "https://www.youtube.com/", description = "YouTube", webservice = false),
    App(id = AppId.create(), title = "Webservice", url = "https://lolhens.de/", description = "Test Webservice", webservice = true),
  )
  /*(0 until 20).map { i =>
    App(id = AppId(i.toString), title = s"My App $i", "https://myapp.lolhens.de", "My App Description", "https://www.google.de/favicon.ico")
  }*/

  def extractFaviconPng(client: Client[IO], uri: Uri): IO[ByteVector] = {
    def extractPng(url: String): IO[ByteVector] = {
      if (url == "") IO.raiseError(new RuntimeException("Empty URL"))
      else for {
        bytes <- client.expect[Array[Byte]](Uri.unsafeFromString(url))
        ext = url.replaceAll(".*(\\.[^.])", "$1")
        image = Imaging.getBufferedImage(new ByteArrayInputStream(bytes), java.util.Map.of(PARAM_KEY_FILENAME, ext))
        png = Imaging.writeImageToBytes(image, ImageFormats.PNG, null)
      } yield
        ByteVector.view(png)
    }

    val IconR = "(<link.*?rel=\"icon\".*?>)".r.unanchored
    val AlternateIconR = "(<link.*?rel=\"alternate icon\".*?>)".r.unanchored
    val HrefR = "href=\"(.*?)\"".r.unanchored

    for {
      html <- client.expect[String](uri)
      iconUrl = html match {
        case IconR(HrefR(href)) => href
        case _ => ""
      }
      alternateIconUrl = html match {
        case AlternateIconR(HrefR(href)) => href
        case _ => ""
      }
      defaultIconUrl = (uri / "favicon.ico").renderString
      png <- extractPng(iconUrl)
        .handleErrorWith(_ => extractPng(alternateIconUrl))
        .handleErrorWith(_ => extractPng(defaultIconUrl))
    } yield
      png
  }

  def service(client: Client[IO]): HttpRoutes[IO] = Router(
    "/assets" -> {
      (WebjarServiceBuilder[IO].toRoutes: HttpRoutes[IO]) <+>
        ResourceServiceBuilder[IO]("/assets").toRoutes
    },

    "/api" -> HttpRoutes.of {
      case GET -> Root / "apps" =>
        Ok(apps.asJson)

      case request@POST -> Root / "app" / "status" =>
        for {
          appId <- request.as[Json].map(_.as[AppId].toTry.get)
          app = apps.find(_.id == appId).getOrElse(throw new RuntimeException("app not found!"))
          result <- client.expect[String](Uri.unsafeFromString(app.url)).attempt
          response <- Ok(result.isRight.asJson)
        } yield
          response

      case request@POST -> Root / "app" / "icon" =>
        for {
          appId <- request.as[Json].map(_.as[AppId].toTry.get)
          app = apps.find(_.id == appId).getOrElse(throw new RuntimeException("app not found!"))
          png <- extractFaviconPng(client, Uri.unsafeFromString(app.url))
          response <- Ok(png.toArray)
        } yield
          response
    },

    "/" -> HttpRoutes.of {
      case request@GET -> Root =>
        Ok(MainPage("Server Desktop"))
    }
  )
}
