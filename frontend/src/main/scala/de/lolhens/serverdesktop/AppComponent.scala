package de.lolhens.serverdesktop

import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

object AppComponent {
  case class Props(app: App)

  class Backend($: BackendScope[Props, Unit]) {
    def render: VdomElement = {
      val props = $.props.runNow()
      <.div(
        ^.cls := "card",
        <.div(
          ^.cls := "card-body",
          <.h5(^.cls := "card-title", props.app.title),
          <.h6(^.cls := "card-subtitle mb-2 text-muted", "Card subtitle"),
          <.p(^.cls := "card-text", "Description"),
        )
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
