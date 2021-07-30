package de.lolhens.serverdesktop

import cats.effect.{IO, SyncIO}
import japgolly.scalajs.react.ReactCatsEffect._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import org.scalajs.dom.window
import scodec.bits.ByteVector

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
    def start: SyncIO[Unit] = SyncIO {
      val props = $.props.unsafeRunSync()
      //val state = $.state.runNow()

      Backend.status(props.app.id).attempt.flatMap {
        case Right(status) =>
          $.modState(_.copy(status = Some(status))).to[IO]

        case Left(exception) =>
          //exception.printStackTrace()
          throw exception
      }.unsafeRunAndForget()(runtime)

      //if (state.iconBytes.isEmpty)
      Backend.icon(props.app.id).attempt.flatMap {
        case Right(iconBytes) =>
          $.modState(_.copy(iconBytes = Some(iconBytes))).to[IO]

        case Left(exception) =>
          //exception.printStackTrace()
          throw exception
      }.unsafeRunAndForget()(runtime)
    }

    def render: VdomElement = {
      val props = $.props.unsafeRunSync()
      val state = $.state.unsafeRunSync()

      <.div(
        ^.cls := "card app",
        ^.onClick --> SyncIO {
          window.location.href = props.app.url
        },
        <.div(
          ^.cls := "card-body",
          <.h5(^.cls := "card-title",
            <.div(
              ^.position := "relative",
              <.div(
                ^.cls := "status app-status",
                ^.position := "absolute",
                ^.right := "0",
                ^.top := "2px",
                ^.height := "12px",
                ^.width := "12px",
                ^.borderRadius := "50%",
                ^.backgroundColor := (state.status match {
                  case None => "lightgray"
                  case Some(true) => "limegreen"
                  case Some(false) => "red"
                }),
                ^.onClick ==> (e => SyncIO {
                  e.stopPropagation()
                  println("Status")
                })
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
      .componentDidMount(_.backend.start)
      .build

}
