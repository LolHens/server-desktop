package de.lolhens.serverdesktop

import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

object AppTilesComponent {
  case class Props(apps: Seq[App])

  class Backend($: BackendScope[Props, Unit]) {
    def render: VdomElement = {
      val props = $.props.runNow()

      def tiles(id: String, apps: Seq[App]): VdomElement = <.div(
        ^.key := id,
        ^.id := id,
        ^.cls := "flex-fill d-flex flex-row flex-wrap",
        apps.map { app =>
          <.div(
            ^.key := app.id.string,
            ^.cls := "p-2 app-tile",
            AppComponent.Component(AppComponent.Props(app))
          )
        }.toVdomArray
      )

      <.div(
        ^.cls := "d-flex flex-column", {
          val (apps, webservices) = props.apps.partition(!_.webservice)
          tiles("apps", apps) +: {
            if (webservices.isEmpty) Seq.empty
            else Seq(
              <.h4(
                ^.key := "webservices-label",
                ^.id := "webservices-label",
                ^.cls := "align-self-center mt-4",
                "Webservices"
              ),
              tiles("webservices", webservices)
            )
          }
        }.toVdomArray
      )
    }
  }

  val Component =
    ScalaComponent.builder[Props]
      .backend(new Backend(_))
      .render(_.backend.render)
      .build
}
