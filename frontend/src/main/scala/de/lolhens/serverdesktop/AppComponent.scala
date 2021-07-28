package de.lolhens.serverdesktop

import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import org.scalajs.dom.window
import scodec.bits.ByteVector

import scala.util.{Failure, Success}

object AppComponent {
  case class Props(app: App)

  case class State(
                    status: Option[Boolean],
                    iconBytes: Option[ByteVector]
                  )

  object State {
    val empty: State = State(None, None)
  }

  private def dataUrl(bytes: ByteVector, contentType: String): String =
    s"data:$contentType;base64,${bytes.toBase64}"

  class Backend($: BackendScope[Props, State]) {
    Backend.status($.props.runNow().app.id).completeWith {
      case Success(status) => $.modState(_.copy(status = Some(status)))
      case Failure(exception) =>
        exception.printStackTrace()
        throw exception
    }.runNow()

    Backend.icon($.props.runNow().app.id).completeWith {
      case Success(iconBytes) => $.modState(_.copy(iconBytes = Some(iconBytes)))
      case Failure(exception) =>
        exception.printStackTrace()
        throw exception
    }.runNow()

    def render: VdomElement = {
      val props = $.props.runNow()
      val state = $.state.runNow()

      <.div(
        ^.cls := "card",
        ^.cursor := "pointer",
        ^.boxShadow := "2px 2px 8px rgb(0, 0, 0, 20%)",
        ^.onClick --> Callback {
          window.location.href = props.app.url
        },
        <.div(
          ^.cls := "card-body",
          <.h5(^.cls := "card-title",
            <.div(
              ^.position := "relative",
              <.div(
                ^.cls := "status",
                ^.position := "absolute",
                ^.right := "0",
                ^.top := "2px",
                ^.height := "12px",
                ^.width := "12px",
                ^.backgroundColor := (state.status match {
                  case None => "lightgray"
                  case Some(true) => "limegreen"
                  case Some(false) => "red"
                }),
                ^.borderRadius := "50%",
                ^.boxShadow := "1.5px 1.5px 4px rgb(0, 0, 0, 40%)",
                ^.onClick --> Callback {
                  println("Status")
                }
              )
            ),
            state.iconBytes match {
              case Some(iconBytes) =>
                <.img(^.cls := "mb-1 me-2", ^.height := "1.2em", ^.src := dataUrl(iconBytes, "image/png"))

              case None =>
                VdomArray.empty()
            },
            props.app.title
          ),
          <.h6(^.cls := "card-subtitle mb-2 text-muted", props.app.url),
          <.p(^.cls := "card-text", props.app.description),
        )
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
