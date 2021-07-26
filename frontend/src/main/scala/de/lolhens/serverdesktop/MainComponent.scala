package de.lolhens.serverdesktop

import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

object MainComponent {
  case class Props()

  class Backend($: BackendScope[Props, Unit]) {
    def render: VdomElement = {
      val s = $.state.runNow()
      <.div(
        ^.cls := "container my-5",
        <.div("Hello World")
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
