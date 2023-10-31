/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import play.api.http.HttpEntity
import play.api.http.Status._
import play.api.libs.ws._
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr
import uk.gov.hmrc.http.InternalServerException

import uk.gov.hmrc.apidocumentation.config.ApplicationConfig

@Singleton
class DownloadConnector @Inject() (ws: WSClient, appConfig: ApplicationConfig)(implicit ec: ExecutionContext) {

  private lazy val serviceBaseUrl = appConfig.apiPlatformMicroserviceBaseUrl

  private def buildRequest(resourceUrl: String): WSRequest = ws.url(resourceUrl).withRequestTimeout(10.seconds)

  private def makeRequest(serviceName: ServiceName, version: ApiVersionNbr, resource: String): Future[WSResponse] = {
    buildRequest(s"$serviceBaseUrl/combined-api-definitions/$serviceName/$version/documentation/$resource").withMethod("GET").stream()
  }

  def fetch(serviceName: ServiceName, version: ApiVersionNbr, resource: String): Future[Option[Result]] = {
    makeRequest(serviceName, version, resource).map { response =>
      if (response.status == OK) {
        val contentType = response.headers.get("Content-Type").flatMap(_.headOption)
          .getOrElse("application/octet-stream")

        response.headers.get("Content-Length") match {
          case Some(Seq(length)) =>
            Some(Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, Some(length.toLong), Some(contentType))))
          case _                 =>
            Some(Ok.sendEntity(HttpEntity.Streamed(response.bodyAsSource, None, Some(contentType))))
        }
      } else if (response.status == NOT_FOUND) {
        None
      } else {
        throw new InternalServerException(s"Error (status ${response.status}) downloading $resource for $serviceName $version")
      }
    }
  }
}
