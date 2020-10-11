package org.bitcoins.esplora.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.HttpResponse
import akka.util.ByteString
import org.bitcoins.core.util.BitcoinSLogger
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}

import scala.concurrent.{ExecutionContext, Future}

abstract class EsploraClient extends BitcoinSLogger {

  implicit protected val system: ActorSystem
  implicit protected val executor: ExecutionContext = system.getDispatcher

  def baseUrl: String

  def makeGetRequest[T](methodName: String, param: String)(implicit
      reader: Reads[T]): Future[T] = {

    val url = baseUrl ++ s"/$methodName/$param"

    Http()
      .singleRequest(Get(url))
      .flatMap(request => executeRequest(url, request))
  }

  def makePostRequest[T](methodName: String, param: String)(implicit
      reader: Reads[T]): Future[T] = {

    val url = baseUrl ++ s"/$methodName"

    Http()
      .singleRequest(Post(url, param))
      .flatMap(request => executeRequest(url, request))
  }

  protected def executeRequest[T](url: String, response: HttpResponse)(implicit
      reader: Reads[T]): Future[T] = {

    response.entity.dataBytes
      .runFold(ByteString.empty)(_ ++ _)
      .map(payload => payload.decodeString(ByteString.UTF_8))
      .map { str =>
        val json = Json.parse(str)
        json.validate[T] match {
          case JsSuccess(value, _) => value
          case _: JsError =>
            throw new RuntimeException(
              s"Error executing call on $url, got $str")
        }
      }
  }
}
