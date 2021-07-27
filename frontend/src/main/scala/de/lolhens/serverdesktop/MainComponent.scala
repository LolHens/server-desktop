package de.lolhens.serverdesktop

import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

object MainComponent {
  case class Props()

  private val itemsPerRow = 4
  private val apps: Seq[App] = (0 until 20).map { i =>
    App(id = i.toString, title = s"My App $i")
  }

  class Backend($: BackendScope[Props, Unit]) {
    def render: VdomElement = {
      val s = $.state.runNow()
      <.div(
        ^.cls := "container my-5 d-flex flex-row flex-wrap",
        apps.map { app =>
          <.div(
            ^.key := app.id,
            ^.cls := "pe-3 pb-3",
            ^.width := s"${100 / itemsPerRow}%",
            AppComponent.Component(AppComponent.Props(app))
          )
        }.toVdomArray
      )
    }
  }

  val Component =
    ScalaComponent.builder[Props]
      //.initialState(())
      .backend(new Backend(_))
      .render(_.backend.render)
      .build
}
