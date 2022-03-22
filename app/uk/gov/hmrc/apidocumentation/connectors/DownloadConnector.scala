/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apidocumentation.connectors

import akka.NotUsed
import akka.stream.scaladsl.Source

import javax.inject.{Inject, Singleton}
import play.api.http.HttpEntity
import play.api.http.Status._
import play.api.libs.ws._
import play.api.mvc._
import play.api.mvc.Results._
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
/**
 * Object Store Client imports.
 */
import uk.gov.hmrc.objectstore.client.Path.File
import uk.gov.hmrc.objectstore.client.play._
import uk.gov.hmrc.objectstore.client.play.Implicits._
import akka.util.ByteString

@Singleton
class DownloadConnector @Inject()(ws: WSClient, appConfig: ApplicationConfig,
                                  objectStoreClient: PlayObjectStoreClient)(implicit ec: ExecutionContext) extends ApplicationLogger{

  private lazy val serviceBaseUrl = appConfig.apiPlatformMicroserviceBaseUrl

  private def buildRequest(resourceUrl: String): WSRequest = ws.url(resourceUrl).withRequestTimeout(10.seconds)

  private def makeRequest(serviceName: String, version: String, resource: String): Future[WSResponse] = {
    buildRequest(s"$serviceBaseUrl/combined-api-definitions/$serviceName/$version/documentation/$resource").withMethod("GET").stream()
  }

  def fetch(serviceName: String, version: String, resource: String): Future[Result] = {

    serviceName match {

      case "gatekeeperemail" =>
        implicit val hc = HeaderCarrier()
        logger.info(s"******* In gatekeeperemail service code")
        objectStoreClient.getObject[Source[ByteString, NotUsed]](File(s"/gatekeeper-email/$version/$resource"), "gatekeeper-email")
          .map {
            case Some(objectSource) =>
              /* Content MD5, Length and Type information can be forwarded on. */
              Ok.streamed(
                objectSource.content,
                contentLength = Some(objectSource.metadata.contentLength),
                contentType   = Some(objectSource.metadata.contentType)
              )
                .withHeaders("Content-MD5" -> objectSource.metadata.contentMd5.value)
            case None =>
              logger.info(s"Can't find the file requested $resource")
              NotFound
          }

      case _ =>
        makeRequest(serviceName, version, resource).map { response =>
          if(response.status == OK) {
            val contentType = response.headers.get("Content-Type").flatMap(_.headOption)
              .getOrElse("application/octet-stream")

            response.headers.get("Content-Length") match {
              case Some(Seq(length)) =>
                Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.toLong), Some(contentType)))
              case _ =>
                Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, None, Some(contentType)))
            }
          }
          else if(response.status == NOT_FOUND) {
            throw new NotFoundException(s"$resource not found for $serviceName $version")
          }
          else {
            throw new InternalServerException(s"Error (status ${response.status}) downloading $resource for $serviceName $version")
          }
        }
    }

  }
}

