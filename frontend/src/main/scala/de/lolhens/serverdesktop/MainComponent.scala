package de.lolhens.serverdesktop

import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

object MainComponent {
  case class Props()

  private val itemsPerRow = 4

  class Backend($: BackendScope[Props, Unit]) {
    def render: VdomElement = {
      val s = $.state.runNow()
      <.div(
        ^.cls := "container my-5 d-flex flex-row flex-wrap",
        (0 until 20).map { i =>
          <.div(
            ^.key := s"$i",
            ^.cls := "pe-3 pb-3",
            ^.width := s"${100 / itemsPerRow}%",
            AppComponent.Component(AppComponent.Props(App(s"My App $i")))
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
