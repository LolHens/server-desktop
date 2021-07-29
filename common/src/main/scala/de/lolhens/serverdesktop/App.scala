package de.lolhens.serverdesktop

import io.circe.generic.semiauto._
import io.circe.{Codec, Decoder, Encoder}

import java.util.UUID

case class AppId(string: String)

object AppId {
  def create(): AppId = AppId(UUID.randomUUID().toString)

  implicit val codec: Codec[AppId] = Codec.from(
    Decoder[String].map(AppId(_)),
    Encoder[String].contramap(_.string)
  )
}

case class App(
                id: AppId,
                title: String,
                url: String,
                description: String,
                webservice: Boolean,
              )

object App {
  implicit val codec: Codec[App] = deriveCodec
}
