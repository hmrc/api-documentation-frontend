/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import play.api.http.HttpEntity
import play.api.http.Status._
import play.api.libs.ws._
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.http.{InternalServerException, NotFoundException}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.metrics.Metrics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class DownloadConnector @Inject()(ws: WSClient, metrics: Metrics) extends ServicesConfig {

  val serviceBaseUrl = baseUrl("api-documentation")

  private def buildRequest(resourceUrl: String): WSRequest = ws.url(resourceUrl)

  private def makeRequest(serviceName: String, version: String, resource: String): Future[StreamedResponse] = {
    buildRequest(s"$serviceBaseUrl/apis/$serviceName/$version/documentation/$resource").withMethod("GET").stream()
  }

  def fetch(serviceName: String, version: String, resource: String): Future[Result] = {

    makeRequest(serviceName, version, resource).map {
      case StreamedResponse(response, body) =>
        response.status match {
          case OK => {
            val contentType = response.headers.get("Content-Type").flatMap(_.headOption)
              .getOrElse("application/octet-stream")

            response.headers.get("Content-Length") match {
              case Some(Seq(length)) =>
                Ok.sendEntity(HttpEntity.Streamed(body, Some(length.toLong), Some(contentType)))
              case _ =>
                Ok.chunked(body).as(contentType)
            }
          }
          case NOT_FOUND => throw new NotFoundException(s"$resource not found for $serviceName $version")
          case _ => throw new InternalServerException(s"Error downloading $resource for $serviceName $version")
        }
    }
  }
}

