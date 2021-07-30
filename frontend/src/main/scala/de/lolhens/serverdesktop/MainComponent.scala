package de.lolhens.serverdesktop

import cats.effect.IO
import japgolly.scalajs.react.ReactCatsEffect._
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.ScalaComponent.BackendScope
import japgolly.scalajs.react.internal.CoreGeneral.ReactEventFromInput
import japgolly.scalajs.react.util.EffectCatsEffect._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

object MainComponent {
  case class Props()

  case class State(
                    apps: Seq[App],
                    filter: String
                  )

  object State {
    val empty: State = State(Seq.empty, "")
  }

  class Backend($: BackendScope[Props, State]) {
    Backend.apps().attempt.flatMap {
      case Right(apps) =>
        $.modState(_.copy(apps = apps)).to[IO]

      case Left(exception) =>
        exception.printStackTrace()
        throw exception
    }.unsafeRunAndForget()(runtime)

    def render: VdomElement = {
      val state = $.state.unsafeRunSync()

      <.div(
        ^.cls := "container my-4 d-flex flex-column",
        <.h1(
          ^.id := "settings",
          ^.position := "relative",
          <.i(
            ^.cls := "bi bi-gear bi-select",
            ^.position := "absolute",
            ^.right := "0",
            ^.top := "0.8rem",
            ^.onClick --> IO {
              println("Settings")
            }
          )
        ),
        <.input(
          ^.id := "search",
          ^.cls := "align-self-center form-control form-control-lg mb-4",
          ^.tpe := "text",
          ^.placeholder := "Search...",
          ^.onChange ==> { e: ReactEventFromInput =>
            val value = e.target.value
            $.modState(_.copy(filter = value))
          }
        ),
        AppTilesComponent.Component(AppTilesComponent.Props(
          state.apps.filter(_.title.toLowerCase.contains(state.filter.toLowerCase))
        ))
      )
    }
  }

  val Component =
    ScalaComponent.builder[Props]
      .initialState(State.empty)
      .backend(new Backend(_))
      .render(_.backend.render)
      .build
}
