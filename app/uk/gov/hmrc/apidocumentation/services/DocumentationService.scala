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

package uk.gov.hmrc.apidocumentation.services

import javax.inject.Inject

import org.raml.v2.api.model.v10.resources.Resource
import play.api.cache._
import uk.gov.hmrc.apidocumentation.connectors.APIDocumentationConnector
import uk.gov.hmrc.apidocumentation.models.{RamlAndSchemas, TestEndpoint, _}
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class DocumentationService @Inject()(apiDefinitionConnector: APIDocumentationConnector,
                                     cache: CacheApi, ramlLoader: RamlLoader, schemaService: SchemaService) {

  val defaultExpiration = 1.hour

  def fetchAPIs(email: Option[String])(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    val apiDefinitions = email match {
      case Some(e) => apiDefinitionConnector.fetchByEmail(e)
      case None => apiDefinitionConnector.fetchAll()
    }
    apiDefinitions map filterDefinitions
  }

  def fetchExtendedApiDefinition(serviceName: String, email: Option[String] = None)(implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {
    val apiDefinition = email match {
      case Some(e) => apiDefinitionConnector.fetchExtendedDefinitionByServiceNameAndEmail(serviceName, e)
      case None => apiDefinitionConnector.fetchExtendedDefinitionByServiceName(serviceName)
    }

    apiDefinition.map(api => if(api.requiresTrust) None else Some(api))
  }

  def filterDefinitions(apis: Seq[APIDefinition]): Seq[APIDefinition] = {
    apis.filter(api => !apiRequiresTrust(api) && api.hasActiveVersions)
  }

  private def apiRequiresTrust(api: APIDefinition): Boolean = {
    api.requiresTrust match {
      case Some(true) => true
      case _ => false
    }
  }

  def fetchRAML(serviceName: String, version: String, cacheBuster: Boolean)(implicit hc: HeaderCarrier): Future[RamlAndSchemas] = {
      val url = s"${apiDefinitionConnector.serviceBaseUrl}/apis/$serviceName/$version/documentation/application.raml"
      fetchRAML(url, cacheBuster)
  }

  def fetchRAML(url: String, cacheBuster: Boolean): Future[RamlAndSchemas] = {
    if (cacheBuster) cache.remove(url)

    Future {
      blocking {
        cache.getOrElse[Try[RamlAndSchemas]](url, defaultExpiration) {
          ramlLoader.load(url).map(raml => {
            val schemaBasePath =  s"${url.take(url.lastIndexOf('/'))}/schemas"
            RamlAndSchemas(raml, schemaService.loadSchemas(schemaBasePath, raml))
          })
        }
      }
    } flatMap {
      case Success(api) => Future.successful(api)
      case Failure(e) => {
        cache.remove(url)
        Future.failed(e)
      }
    }
  }

  def buildTestEndpoints(service: String, version: String)(implicit hc: HeaderCarrier) = {
    fetchRAML(service, version, true).map { ramlAndSchemas =>
      buildResources(ramlAndSchemas.raml.resources.toSeq)
    }
  }

  private def buildResources(resources: Seq[Resource]): Seq[TestEndpoint] = {
    resources.flatMap { res =>
      val nested = buildResources(res.resources())
      res.methods.headOption match {
        case Some(_) => {
          val methods = res.methods.map(_.method.toUpperCase).sorted
          val endpoint = TestEndpoint(s"{service-url}${res.resourcePath}", methods:_*)

          endpoint +: nested
        }
        case _ => nested
      }
    }
  }
}
