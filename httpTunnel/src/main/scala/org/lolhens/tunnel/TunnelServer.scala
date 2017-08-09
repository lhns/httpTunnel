package org.lolhens.tunnel

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{get, handleWebSocketMessages, path, reject, _}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Tcp

import scala.io.StdIn

object TunnelServer extends Tunnel {
  def main(args: Array[String]): Unit = {
    val route: Route =
      get {
        path(Remaining) { p =>
          parseSocketAddress(if (p.endsWith("/")) p.dropRight(1) else p).map { socketAddress =>
            println(socketAddress.getHostString + ":" + socketAddress.getPort)

            handleWebSocketMessages(
              messageToByteString
                .via(Tcp().outgoingConnection(socketAddress.getHostString, socketAddress.getPort).mapError {
                  case e =>
                    println(e)
                    e
                })
                .via(byteStringToMessage)
            )
          }.getOrElse(reject)
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

    println(s"Server online at http://0.0.0.0:8080/\nPress RETURN to stop...")
    StdIn.readLine()

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
