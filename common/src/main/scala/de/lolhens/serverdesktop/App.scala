package de.lolhens.serverdesktop

import io.circe.Codec
import io.circe.generic.semiauto._

case class App(id: String, title: String)

object App {
  implicit val codec: Codec[App] = deriveCodec
}
