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

package uk.gov.hmrc.apidocumentation.models

import scala.collection.immutable.ListMap
import scala.io.Source

import play.api.Configuration
import play.api.libs.json._
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._

import uk.gov.hmrc.apidocumentation.controllers.routes
import uk.gov.hmrc.apidocumentation.models.APIDefinitionLabel._

trait Documentation {

  val name: String
  val context: String
  val categories: Option[Seq[ApiCategory]]
  val label: DocumentationLabel

  def documentationUrl: String

  def mappedCategories(catMap: Map[String, Seq[ApiCategory]] = APICategory.categoryMap): Seq[ApiCategory] = categories match {
    case Some(x) if (x.nonEmpty) => x
    case _                       => catMap.getOrElse(name, Seq(ApiCategory.OTHER))
  }

  lazy val isRestOrXmlApi = label == REST_API || label == XML_API

  lazy val nameAsId = name.toLowerCase().replaceAll(" ", "-").replaceAll("[^a-z0-9-]", "")
}

object Documentation {

  def groupedByCategory(
      apiDefinitions: Seq[ApiDefinition],
      xmlDefinitions: Seq[XmlApiDocumentation],
      serviceGuides: Seq[ServiceGuide],
      roadMaps: Seq[RoadMap],
      catMap: Map[String, Seq[ApiCategory]] = APICategory.categoryMap
    ): ListMap[ApiCategory, Seq[Documentation]] = {
    val categorised: Map[ApiCategory, Seq[Documentation]] =
      (apiDefinitions.map(defi => WrappedApiDefinition(defi)) ++ xmlDefinitions ++ serviceGuides ++ roadMaps).foldLeft(Map(): Map[ApiCategory, Seq[Documentation]]) {
        (groupings, apiDefinition) =>
          groupings ++ apiDefinition.mappedCategories(catMap).map(cat => (cat, groupings.getOrElse(cat, Nil) :+ apiDefinition)).toMap
      }.filter(_._2.exists(_.isRestOrXmlApi))

    ListMap(categorised.toSeq.sortBy(_._1): _*)
  }
}

case class XmlApiDocumentation(name: String, context: String, description: String, categories: Option[Seq[ApiCategory]] = None)
    extends Documentation {

  val label: DocumentationLabel = XML_API

  def documentationUrl: String = routes.ApiDocumentationController.renderXmlApiDocumentation(name).url
}

object XmlApiDocumentation {
  implicit val format = Json.format[XmlApiDocumentation]

}

case class ServiceGuide(name: String, context: String, categories: Option[Seq[ApiCategory]] = None)
    extends Documentation {

  val label: DocumentationLabel = SERVICE_GUIDE

  def documentationUrl: String = context
}

object ServiceGuide {
  implicit val format = Json.format[ServiceGuide]

  def serviceGuides: Seq[ServiceGuide] =
    Json.parse(Source.fromInputStream(getClass.getResourceAsStream("/service_guides.json")).mkString).as[Seq[ServiceGuide]]
}

case class RoadMap(name: String, context: String, categories: Option[Seq[ApiCategory]] = None)
    extends Documentation {

  val label: DocumentationLabel = ROADMAP

  def documentationUrl: String = context
}

object RoadMap {
  implicit val format = Json.format[RoadMap]

  def roadMaps: Seq[RoadMap] =
    Json.parse(Source.fromInputStream(getClass.getResourceAsStream("/roadmap.json")).mkString).as[Seq[RoadMap]]
}

case class APIAccess(`type`: ApiAccessType, whitelistedApplicationIds: Option[Seq[String]], isTrial: Option[Boolean] = None)

object APIAccess {

  def apply(accessType: ApiAccessType): APIAccess = {
    APIAccess(accessType, Some(Seq.empty), Some(false))
  }

  def build(config: Option[Configuration]): APIAccess = APIAccess(
    `type` = ApiAccessType.PRIVATE,
    whitelistedApplicationIds = config.flatMap(_.getOptional[Seq[String]]("whitelistedApplicationIds")).orElse(Some(Seq.empty)),
    isTrial = None
  )
}

case class WrappedApiDefinition(definition: ApiDefinition) extends Documentation {
  override val name: String                         = definition.name
  override val context: String                      = definition.context.value
  override val categories: Option[Seq[ApiCategory]] = Some(definition.categories)
  override val label: DocumentationLabel            = if (definition.isTestSupport) TEST_SUPPORT_API else REST_API

  lazy val defaultVersion: ApiVersion = definition
    .versionsAsList
    .sorted(WrappedApiDefinition.statusVersionOrdering)
    .head

  override def documentationUrl: String = routes.ApiDocumentationController.renderApiDocumentation(definition.serviceName, defaultVersion.versionNbr, None).url
}

object WrappedApiDefinition {
  val statusVersionOrdering: Ordering[ApiVersion] = Ordering.by[ApiVersion, ApiStatus](_.status)(ApiStatus.orderingByPriority).reverse.orElseBy(_.versionNbr).reverse
}

case class DocumentationCategory(apiCategory: ApiCategory) {
  val filter = apiCategory.toString.toLowerCase.replaceAll("_", "-").replaceAll("vat-mtd", "vat")
}

object DocumentationCategory {

  def fromFilter(filter: String): Option[ApiCategory] = {
    ApiCategory.values.map(cat => DocumentationCategory(cat)).find(cat => cat.filter == filter).map(_.apiCategory)
  }
}

case class VersionVisibility(privacy: ApiAccessType, loggedIn: Boolean, authorised: Boolean, isTrial: Boolean = false)

object VersionVisibility {

  def apply(extendedApiVersion: ExtendedApiVersion): Option[VersionVisibility] = {

    def isLoggedIn(production: ApiAvailability, sandbox: ApiAvailability) = {
      production.loggedIn || sandbox.loggedIn
    }

    def isTrial(access: ApiAccess): Boolean = {
      access == ApiAccess.Private(true)
    }

    def isInTrial(production: ApiAvailability, sandbox: ApiAvailability): Boolean = (production.access, sandbox.access) match {
      case (ApiAccess.Private(true), _) | (_, ApiAccess.Private(true)) => true
      case _                                                           => false
    }

    (extendedApiVersion.productionAvailability, extendedApiVersion.sandboxAvailability) match {
      case (Some(prod), None)          => Some(VersionVisibility(prod.access.accessType, prod.loggedIn, prod.authorised, isTrial(prod.access)))
      case (None, Some(sandbox))       => Some(VersionVisibility(sandbox.access.accessType, sandbox.loggedIn, sandbox.authorised, isTrial(sandbox.access)))
      case (Some(prod), Some(sandbox)) =>
        Some(VersionVisibility(sandbox.access.accessType, isLoggedIn(prod, sandbox), sandbox.authorised, isInTrial(prod, sandbox)))
      case _                           => None
    }
  }
}

case class ServiceDetails(serviceName: String, serviceUrl: String)

case class ErrorResponse(code: Option[String] = None, message: Option[String] = None)

object DocsVisibility extends Enumeration {

  type DocsVisibility = Value
  val VISIBLE, OVERVIEW_ONLY, NOT_VISIBLE = Value
}
