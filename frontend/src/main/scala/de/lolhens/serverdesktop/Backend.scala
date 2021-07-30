package de.lolhens.serverdesktop

import cats.effect.IO
import io.circe.syntax._
import io.circe.{Json, parser}
import japgolly.scalajs.react.extra.Ajax
import scodec.bits.ByteVector

import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.util.chaining.scalaUtilChainingOps

object Backend {
  private def request(
                       method: String,
                       url: String,
                       json: Option[Json]
                     ): IO[ByteVector] =
    Ajax(method, url)
      .setRequestContentTypeJsonUtf8
      .and(_.responseType = "arraybuffer")
      .pipe(e => json match {
        case Some(json) => e.send(json.noSpaces)
        case None => e.send
      })
      .asAsyncCallback
      .map { xhr =>
        xhr.status match {
          case 200 => ByteVector.view(TypedArrayBuffer.wrap(xhr.response.asInstanceOf[ArrayBuffer]))
          case status => throw new RuntimeException(s"Http Request returned status code $status!")
        }
      }


  private def jsonRequest(
                           method: String,
                           url: String,
                           json: Option[Json]
                         ): IO[Json] =
    request(method, url, json).map(bytes =>
      bytes.decodeUtf8.toTry.flatMap(parser.parse(_).toTry).get
    )

  def apps(): IO[Seq[App]] =
    jsonRequest("GET", "/api/apps", None).map(_.as[Seq[App]].toTry.get)

  def status(appId: AppId): IO[Boolean] =
    jsonRequest("POST", "/api/app/status", Some(appId.asJson)).map(_.as[Boolean].toTry.get)

  def icon(appId: AppId): IO[ByteVector] =
    request("POST", "/api/app/icon", Some(appId.asJson))
}
