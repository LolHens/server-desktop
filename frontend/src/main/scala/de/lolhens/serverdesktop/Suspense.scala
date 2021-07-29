package de.lolhens.serverdesktop

import cats.data.{EitherT, OptionT}
import cats.syntax.option._
import japgolly.scalajs.react.CatsReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.{BackendScope, Unmounted}
import japgolly.scalajs.react.vdom.html_<^.VdomNode

// The builtin react Suspense has some restrictions
object Suspense {

  case class Props(fallback: () => VdomNode,
                   asyncBody: AsyncCallback[VdomNode])

  case class State(body: VdomNode,
                   asyncBody: Option[AsyncCallback[VdomNode]])

  object State {
    def fromProps(props: Props): State = {
      props.asyncBody.sync.runNow() match {
        case Right(sync) =>
          State(sync, none)

        case Left(async) =>
          State(props.fallback(), async.some)
      }
    }
  }

  class Backend($: BackendScope[Props, State]) {
    def shouldUpdate(nextProps: Props, nextState: State): CallbackTo[Boolean] = {
      (for {
        props <- EitherT.right[Boolean]($.props)
        _ <- {
          (for {
            _ <- EitherT.cond[CallbackTo](nextProps != props, (), true)
            _ <- EitherT.right($.modState(_ => State.fromProps(nextProps)))
            _ <- EitherT.leftT[CallbackTo, Unit](false)
          } yield ())
            .recover {
              case true => ()
            }
        }
        state <- EitherT.right[Boolean]($.state)
      } yield
        nextState != state)
        .merge
    }

    private def waitForAsyncBody: Callback = {
      (for {
        state <- OptionT.liftF($.state)
        async <- OptionT.fromOption[CallbackTo](state.asyncBody)
        _ <- OptionT.liftF(async.flatMapSync { body =>
          $.modState(_.copy(body = body, asyncBody = none))
        }.toCallback)
      } yield ())
        .value.map(_ => ())
    }

    def start: Callback = waitForAsyncBody

    def update: Callback = waitForAsyncBody

    def render(props: Props, state: State): VdomNode = state.body
  }

  private val Component =
    ScalaComponent.builder[Props]
      .initialStateFromProps(State.fromProps)
      .renderBackend[Backend]
      .shouldComponentUpdate(e => e.backend.shouldUpdate(e.nextProps, e.nextState))
      .componentDidMount(_.backend.start)
      .componentDidUpdate(_.backend.update)
      .build

  def apply[A](fallback: => VdomNode,
               asyncBody: AsyncCallback[A])
              (implicit ev: A => VdomNode): Unmounted[Props, State, Backend] = {
    Component(Props(() => fallback, asyncBody.map(ev)))
  }
}
