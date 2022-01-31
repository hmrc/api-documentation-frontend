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

package uk.gov.hmrc.apidocumentation.services

import javax.inject.{Inject, Singleton}
import play.api.cache._
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import uk.gov.hmrc.apidocumentation.connectors.ApiPlatformMicroserviceConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apidocumentation.models.apispecification.ApiSpecification
import uk.gov.hmrc.apidocumentation.models.TestEndpoint
import uk.gov.hmrc.apidocumentation.models.apispecification.Resource
import uk.gov.hmrc.apidocumentation.models.apispecification.ResourceGroup
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger
import scala.concurrent.Future

@Singleton
class DocumentationService @Inject()( appConfig: ApplicationConfig,
                                      cache: AsyncCacheApi,
                                      apm: ApiPlatformMicroserviceConnector
                                      )
                                      (implicit ec: ExecutionContext) extends ApplicationLogger {
  val defaultExpiration = 1.hour

  def fetchApiSpecification(serviceName: String, version: String, cacheBuster: Boolean)(implicit hc: HeaderCarrier): Future[ApiSpecification] = {
    val key = serviceName+":"+version
    if (cacheBuster) cache.remove(key)

    val spec = cache.getOrElseUpdate(key, defaultExpiration) {
      logger.info(s"****** Specification Cache miss for $key")
      apm.fetchApiSpecification(serviceName,version)(hc)
    }
    spec.onComplete(t => t.fold(e => { cache.remove(key); throw e }, identity ))
    spec
  }

  def buildTestEndpoints(service: String, version: String)(implicit hc: HeaderCarrier): Future[Seq[TestEndpoint]] = {
    fetchApiSpecification(service, version, cacheBuster = true).map(buildResources)
  }

  private def buildResources(specification: ApiSpecification): List[TestEndpoint] = {
    def buildResource(r: Resource): List[TestEndpoint] = {
      val nested: List[TestEndpoint] = r.children.flatMap(cr => buildResource(cr))
      r.methods match {
        case Nil => nested
        case ms => 
          val myMethods: List[String] = ms.map(m => m.method.toUpperCase).sorted
          TestEndpoint(s"{service-url}${r.resourcePath}", myMethods:_*) +: nested
      }
    }

    def buildResourceGrp(g: ResourceGroup): List[TestEndpoint] = {
      g.resources.flatMap(buildResource)
    }

    specification.resourceGroups.flatMap(buildResourceGrp)
  }

  // private def buildResources(resources: Seq[Resource]): Seq[TestEndpoint] = {
  //   resources.flatMap { res =>
  //     val nested = buildResources(res.resources().asScala)
  //     res.methods.asScala.headOption match {
  //       case Some(_) =>
  //         val methods = res.methods.asScala.map(_.method.toUpperCase).sorted
  //         val endpoint = TestEndpoint(s"{service-url}${res.resourcePath}", methods:_*)
  //         endpoint +: nested
  //       case _ => nested
  //     }
  //   }
  // }
}
