/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.services

import javax.inject.{Inject, Singleton}
import org.raml.v2.api.model.v10.resources.Resource
import play.api.cache._
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models.{RamlAndSchemas, TestEndpoint}
import uk.gov.hmrc.ramltools.loaders.RamlLoader

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, _}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import uk.gov.hmrc.apidocumentation.connectors.ApiPlatformMicroserviceConnector
import uk.gov.hmrc.http.HeaderCarrier
import play.api.Logger
import uk.gov.hmrc.apidocumentation.models.apispecification.ApiSpecification

object DocumentationService {
  def ramlUrl(serviceBaseUrl: String, serviceName: String, version: String): String =
    s"$serviceBaseUrl/combined-api-definitions/$serviceName/$version/documentation/application.raml"

  def schemasUrl(serviceBaseUrl: String, serviceName: String, version: String): String =
    s"$serviceBaseUrl/combined-api-definitions/$serviceName/$version/documentation/schemas"

}

@Singleton
class DocumentationService @Inject()( appConfig: ApplicationConfig,
                                      cache: CacheApi,
                                      apm: ApiPlatformMicroserviceConnector,
                                      ramlLoader: RamlLoader,
                                      schemaService: SchemaService)
                                      (implicit ec: ExecutionContext) {
  import DocumentationService.ramlUrl

  val defaultExpiration = 1.hour

  private lazy val serviceBaseUrl = appConfig.apiPlatformMicroserviceBaseUrl

  def fetchApiSpecification(serviceName: String, version: String, cacheBuster: Boolean)(implicit hc: HeaderCarrier): Future[ApiSpecification] = {
    val key = serviceName+":"+version
    if (cacheBuster) cache.remove(key)

    // TODO - This microservice call is not blocking any threads in this microservice.  Remove the future blocking
    Future {
      blocking {
        cache.getOrElse[Try[ApiSpecification]](key, defaultExpiration) {
          Logger.info(s"****** Specification Cache miss for $key")
          Try {
            Await.result(apm.fetchApiSpecification(serviceName,version)(hc), 30.seconds)
          }
        }
      }.fold(e => { cache.remove(key); throw e }, identity )
    }
  }

  def fetchRAML(serviceName: String, version: String, cacheBuster: Boolean): Future[RamlAndSchemas] = {
      val url = ramlUrl(serviceBaseUrl,serviceName,version)
      fetchRAML(url, cacheBuster)
  }

  def fetchRAML(url: String, cacheBuster: Boolean): Future[RamlAndSchemas] = {
    if (cacheBuster) cache.remove(url)

    Future {
      blocking {  // ramlLoader is blocking and synchronous
        cache.getOrElse[Try[RamlAndSchemas]](url, defaultExpiration) {
          ramlLoader.load(url).map(raml => {
            val schemaBasePath =  s"${url.take(url.lastIndexOf('/'))}/schemas"
            RamlAndSchemas(raml, schemaService.loadSchemas(schemaBasePath, raml))
          })
        }
      }
    } flatMap {
      case Success(api) => Future.successful(api)
      case Failure(e) =>
        cache.remove(url)
        Future.failed(e)
    }
  }

  def buildTestEndpoints(service: String, version: String): Future[Seq[TestEndpoint]] = {
    fetchRAML(service, version, cacheBuster = true).map { ramlAndSchemas =>
      buildResources(ramlAndSchemas.raml.resources.asScala.toSeq)
    }
  }

  private def buildResources(resources: Seq[Resource]): Seq[TestEndpoint] = {
    resources.flatMap { res =>
      val nested = buildResources(res.resources().asScala)
      res.methods.asScala.headOption match {
        case Some(_) =>
          val methods = res.methods.asScala.map(_.method.toUpperCase).sorted
          val endpoint = TestEndpoint(s"{service-url}${res.resourcePath}", methods:_*)

          endpoint +: nested

        case _ => nested
      }
    }
  }
}
