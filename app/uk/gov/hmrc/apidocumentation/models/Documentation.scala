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
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr

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

object APIAccessType extends Enumeration {
  type APIAccessType = Value
  val PRIVATE, PUBLIC = Value
}

case class APIAccess(`type`: APIAccessType.Value, whitelistedApplicationIds: Option[Seq[String]], isTrial: Option[Boolean] = None)

object APIAccess {

  def apply(accessType: APIAccessType.Value): APIAccess = {
    APIAccess(accessType, Some(Seq.empty), Some(false))
  }

  def build(config: Option[Configuration]): APIAccess = APIAccess(
    `type` = APIAccessType.PRIVATE,
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
    .sorted(ApiVersionSorting.statusVersionOrdering)
    .head

  override def documentationUrl: String = routes.ApiDocumentationController.renderApiDocumentation(definition.serviceName.value, defaultVersion.versionNbr.value, None).url
}

object ApiVersionSorting {

  def priorityOf(apiStatus: ApiStatus): Int = {
    apiStatus match {
      case ApiStatus.STABLE     => 1
      case ApiStatus.BETA       => 2
      case ApiStatus.ALPHA      => 3
      case ApiStatus.DEPRECATED => 4
      case ApiStatus.RETIRED    => 5
    }
  }

  implicit val statusOrdering: Ordering[ApiStatus]                         = Ordering.by[ApiStatus, Int](priorityOf)
  implicit val statusVersionOrdering: Ordering[ApiVersion]                 = Ordering.by[ApiVersion, ApiStatus](_.status).reverse.orElseBy(_.versionNbr).reverse
  implicit val statusExtendedVersionOrdering: Ordering[ExtendedAPIVersion] = Ordering.by[ExtendedAPIVersion, ApiStatus](_.status).reverse.orElseBy(_.version).reverse

}

case class DocumentationCategory(apiCategory: ApiCategory) {
  val filter = apiCategory.toString.toLowerCase.replaceAll("_", "-").replaceAll("vat-mtd", "vat")
}

object DocumentationCategory {

  def fromFilter(filter: String): Option[ApiCategory] = {
    ApiCategory.values.map(cat => DocumentationCategory(cat)).find(cat => cat.filter == filter).map(_.apiCategory)
  }
}

case class ExtendedAPIDefinition(
    serviceName: String,
    name: String,
    description: String,
    context: String,
    requiresTrust: Boolean,
    isTestSupport: Boolean,
    versions: Seq[ExtendedAPIVersion]
  ) {

  def userAccessibleApiDefinition = {
    def isAccessible(availability: Option[APIAvailability]) =
      availability.fold(false)(avail => avail.authorised || avail.access.isTrial.contains(true)) // scalastyle:ignore

    copy(versions = versions.filter(v => isAccessible(v.productionAvailability) || isAccessible(v.sandboxAvailability)))
  }

  lazy val sortedVersions             = versions.sortBy(_.version).reverse
  lazy val sortedActiveVersions       = sortedVersions.filterNot(_.status == ApiStatus.RETIRED)
  lazy val statusSortedVersions       = versions.sorted(ApiVersionSorting.statusExtendedVersionOrdering)
  lazy val statusSortedActiveVersions = statusSortedVersions.filterNot(_.status == ApiStatus.RETIRED)
  lazy val defaultVersion             = statusSortedActiveVersions.headOption
}

case class ExtendedAPIVersion(
    version: ApiVersionNbr,
    status: ApiStatus,
    endpoints: Seq[Endpoint],
    productionAvailability: Option[APIAvailability],
    sandboxAvailability: Option[APIAvailability]
  ) {

  val displayedStatus = {
    val accessIndicator = VersionVisibility(this) match {
      case Some(VersionVisibility(APIAccessType.PRIVATE, _, _, _)) => "Private "
      case _                                                       => ""
    }
    s"${accessIndicator}${status.displayText}"
  }
}

case class APIAvailability(endpointsEnabled: Boolean, access: APIAccess, loggedIn: Boolean, authorised: Boolean)

case class VersionVisibility(privacy: APIAccessType.APIAccessType, loggedIn: Boolean, authorised: Boolean, isTrial: Option[Boolean] = None)

object VersionVisibility {

  def apply(extendedApiVersion: ExtendedAPIVersion): Option[VersionVisibility] = {

    def isLoggedIn(production: APIAvailability, sandbox: APIAvailability) = {
      production.loggedIn || sandbox.loggedIn
    }

    def isInTrial(production: APIAvailability, sandbox: APIAvailability) = (production.access.isTrial, sandbox.access.isTrial) match {
      case (Some(true), _) | (_, Some(true)) => Some(true)
      case _                                 => None
    }

    (extendedApiVersion.productionAvailability, extendedApiVersion.sandboxAvailability) match {
      case (Some(prod), None)          => Some(VersionVisibility(prod.access.`type`, prod.loggedIn, prod.authorised, prod.access.isTrial))
      case (None, Some(sandbox))       => Some(VersionVisibility(sandbox.access.`type`, sandbox.loggedIn, sandbox.authorised, sandbox.access.isTrial))
      case (Some(prod), Some(sandbox)) =>
        Some(VersionVisibility(sandbox.access.`type`, isLoggedIn(prod, sandbox), sandbox.authorised, isInTrial(prod, sandbox)))
      case _                           => None
    }
  }
}

object APIStatus extends Enumeration {

  type APIStatus = Value
  val ALPHA, BETA, PROTOTYPED, PUBLISHED, STABLE, DEPRECATED, RETIRED = Value

  def priorityOf(apiStatus: APIStatus): Int = {
    apiStatus match {
      case STABLE | PUBLISHED => 5
      case BETA | PROTOTYPED  => 4
      case ALPHA              => 3
      case DEPRECATED         => 2
      case RETIRED            => 1
    }
  }
}

case class ServiceDetails(serviceName: String, serviceUrl: String)

case class ErrorResponse(code: Option[String] = None, message: Option[String] = None)

object DocsVisibility extends Enumeration {

  type DocsVisibility = Value
  val VISIBLE, OVERVIEW_ONLY, NOT_VISIBLE = Value
}
