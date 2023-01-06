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
import scala.util.Try

import play.api.Configuration
import play.api.libs.json._

import uk.gov.hmrc.apidocumentation.controllers.routes
import uk.gov.hmrc.apidocumentation.models.APICategory._
import uk.gov.hmrc.apidocumentation.models.APIDefinitionLabel._
import uk.gov.hmrc.apidocumentation.models.APIStatus.APIStatus
import uk.gov.hmrc.apidocumentation.models.HttpMethod.HttpMethod
import uk.gov.hmrc.apidocumentation.models.JsonFormatters._

trait Documentation {

  val name: String
  val context: String
  val categories: Option[Seq[APICategory]]
  val label: DocumentationLabel

  def documentationUrl: String

  def mappedCategories(catMap: Map[String, Seq[APICategory]] = categoryMap): Seq[APICategory] = categories match {
    case Some(x) if (x.nonEmpty) => x
    case _                       => catMap.getOrElse(name, Seq(OTHER))
  }

  lazy val isRestOrXmlApi = label == REST_API || label == XML_API

}

object Documentation {

  def groupedByCategory(
      apiDefinitions: Seq[APIDefinition],
      xmlDefinitions: Seq[XmlApiDocumentation],
      serviceGuides: Seq[ServiceGuide],
      roadMaps: Seq[RoadMap],
      catMap: Map[String, Seq[APICategory]] = categoryMap
    ): ListMap[APICategory, Seq[Documentation]] = {
    val categorised: Map[APICategory, Seq[Documentation]] =
      (apiDefinitions ++ xmlDefinitions ++ serviceGuides ++ roadMaps).foldLeft(Map(): Map[APICategory, Seq[Documentation]]) {
        (groupings, apiDefinition) =>
          groupings ++ apiDefinition.mappedCategories(catMap).map(cat => (cat, groupings.getOrElse(cat, Nil) :+ apiDefinition)).toMap
      }.filter(_._2.exists(_.isRestOrXmlApi))

    ListMap(categorised.toSeq.sortBy(_._1): _*)
  }
}

case class XmlApiDocumentation(name: String, context: String, description: String, categories: Option[Seq[APICategory]] = None)
    extends Documentation {

  val label: DocumentationLabel = XML_API

  def documentationUrl: String = routes.ApiDocumentationController.renderXmlApiDocumentation(name).url
}

object XmlApiDocumentation {
  implicit val format = Json.format[XmlApiDocumentation]

}

case class ServiceGuide(name: String, context: String, categories: Option[Seq[APICategory]] = None)
    extends Documentation {

  val label: DocumentationLabel = SERVICE_GUIDE

  def documentationUrl: String = context
}

object ServiceGuide {
  implicit val format = Json.format[ServiceGuide]

  def serviceGuides: Seq[ServiceGuide] =
    Json.parse(Source.fromInputStream(getClass.getResourceAsStream("/service_guides.json")).mkString).as[Seq[ServiceGuide]]
}

case class RoadMap(name: String, context: String, categories: Option[Seq[APICategory]] = None)
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

case class APIDefinition(
    serviceName: String,
    name: String,
    description: String,
    context: String,
    requiresTrust: Option[Boolean],
    isTestSupport: Option[Boolean],
    versions: Seq[APIVersion],
    categories: Option[Seq[APICategory]] = None
  ) extends Documentation {

  require(versions.nonEmpty, s"API versions must not be empty! serviceName=$serviceName")

  // TODO - should this be context based on non-unique names
  def isIn(definitions: Seq[APIDefinition]): Boolean = {
    definitions.find(_.name == name).isDefined
  }

  lazy val retiredVersions            = versions.filter(_.status == APIStatus.RETIRED)
  lazy val sortedVersions             = versions.sortWith(APIDefinition.versionSorter)
  lazy val sortedActiveVersions       = sortedVersions.filterNot(v => v.status == APIStatus.RETIRED)
  lazy val statusSortedVersions       = versions.sortWith(APIDefinition.statusAndVersionSorter)
  lazy val statusSortedActiveVersions = statusSortedVersions.filterNot(v => v.status == APIStatus.RETIRED)
  lazy val defaultVersion             = statusSortedActiveVersions.headOption
  lazy val hasActiveVersions          = statusSortedActiveVersions.nonEmpty
  val label: DocumentationLabel       = if (isTestSupport.contains(true)) TEST_SUPPORT_API else REST_API

  def documentationUrl: String = routes.ApiDocumentationController.renderApiDocumentation(serviceName, defaultVersion.get.version, None).url
}

object APIDefinition {

  private def versionSorter(v1: APIVersion, v2: APIVersion) = {
    val v1Parts = Try(v1.version.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val v2Parts = Try(v2.version.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val pairs   = v1Parts.zip(v2Parts)

    val firstUnequalPair = pairs.find { case (one, two) => one != two }
    firstUnequalPair.fold(v1.version.length > v2.version.length) { case (a, b) => a > b }
  }

  def statusAndVersionSorter(v1: APIVersion, v2: APIVersion) = {
    val v1Status = APIStatus.priorityOf(v1.status)
    val v2Status = APIStatus.priorityOf(v2.status)
    v1Status match {
      case `v2Status` => versionSorter(v1, v2)
      case _          => v1Status > v2Status
    }
  }

  private val nonNumericOrPeriodRegex = "[^\\d^.]*"
  private val fallback                = Array(1, 0, 0)

}

case class APIVersion(
    version: String,
    access: Option[APIAccess],
    status: APIStatus,
    endpoints: Seq[Endpoint]
  ) {
  val accessType = access.fold(APIAccessType.PUBLIC)(_.`type`)

  val displayedStatus = {
    val accessIndicator = accessType match {
      case APIAccessType.PRIVATE => "Private "
      case _                     => ""
    }
    s"${accessIndicator}${APIStatus.description(status)}"
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
      availability.fold(false)(avail => avail.authorised || avail.access.isTrial.contains(true))

    copy(versions = versions.filter(v => isAccessible(v.productionAvailability) || isAccessible(v.sandboxAvailability)))
  }

  lazy val retiredVersions            = versions.filter(_.status == APIStatus.RETIRED)
  lazy val sortedVersions             = versions.sortWith(ExtendedAPIDefinition.versionSorter)
  lazy val sortedActiveVersions       = sortedVersions.filterNot(v => v.status == APIStatus.RETIRED)
  lazy val statusSortedVersions       = versions.sortWith(ExtendedAPIDefinition.statusAndVersionSorter)
  lazy val statusSortedActiveVersions = statusSortedVersions.filterNot(v => v.status == APIStatus.RETIRED)
  lazy val defaultVersion             = statusSortedActiveVersions.headOption
  lazy val hasActiveVersions          = statusSortedActiveVersions.nonEmpty
}

object ExtendedAPIDefinition {

  private def versionSorter(v1: ExtendedAPIVersion, v2: ExtendedAPIVersion) = {
    val v1Parts = Try(v1.version.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val v2Parts = Try(v2.version.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val pairs   = v1Parts.zip(v2Parts)

    val firstUnequalPair = pairs.find { case (one, two) => one != two }
    firstUnequalPair.fold(v1.version.length > v2.version.length) { case (a, b) => a > b }
  }

  def statusAndVersionSorter(v1: ExtendedAPIVersion, v2: ExtendedAPIVersion) = {
    val v1Status = APIStatus.priorityOf(v1.status)
    val v2Status = APIStatus.priorityOf(v2.status)
    v1Status match {
      case `v2Status` => versionSorter(v1, v2)
      case _          => v1Status > v2Status
    }
  }

  private val nonNumericOrPeriodRegex = "[^\\d^.]*"
  private val fallback                = Array(1, 0, 0)
}

case class ExtendedAPIVersion(
    version: String,
    status: APIStatus,
    endpoints: Seq[Endpoint],
    productionAvailability: Option[APIAvailability],
    sandboxAvailability: Option[APIAvailability]
  ) {

  def visibility: Option[VersionVisibility] = {

    def isLoggedIn(production: APIAvailability, sandbox: APIAvailability) = {
      production.loggedIn || sandbox.loggedIn
    }

    def isInTrial(production: APIAvailability, sandbox: APIAvailability) = (production.access.isTrial, sandbox.access.isTrial) match {
      case (Some(true), _) | (_, Some(true)) => Some(true)
      case _                                 => None
    }

    (productionAvailability, sandboxAvailability) match {
      case (Some(prod), None)          => Some(VersionVisibility(prod.access.`type`, prod.loggedIn, prod.authorised, prod.access.isTrial))
      case (None, Some(sandbox))       => Some(VersionVisibility(sandbox.access.`type`, sandbox.loggedIn, sandbox.authorised, sandbox.access.isTrial))
      case (Some(prod), Some(sandbox)) =>
        Some(VersionVisibility(sandbox.access.`type`, isLoggedIn(prod, sandbox), sandbox.authorised, isInTrial(prod, sandbox)))
      case _                           => None
    }
  }

  val displayedStatus = {
    val accessIndicator = visibility match {
      case Some(VersionVisibility(APIAccessType.PRIVATE, _, _, _)) => "Private "
      case _                                                       => ""
    }
    s"${accessIndicator}${APIStatus.description(status)}"
  }
}

case class APIAvailability(endpointsEnabled: Boolean, access: APIAccess, loggedIn: Boolean, authorised: Boolean)

case class VersionVisibility(privacy: APIAccessType.APIAccessType, loggedIn: Boolean, authorised: Boolean, isTrial: Option[Boolean] = None)

case class Endpoint(
    endpointName: String,
    uriPattern: String,
    method: HttpMethod,
    queryParameters: Option[Seq[Parameter]] = None
  ) {

  def decoratedUriPattern = {
    queryParameters.getOrElse(Seq()).isEmpty match {
      case true  => uriPattern
      case false => {
        val queryString = queryParameters
          .getOrElse(Seq())
          .filter(_.required)
          .map(parameter => s"${parameter.name}={${parameter.name}}")
          .mkString("&")
        queryString.isEmpty match {
          case true  => uriPattern
          case false => s"$uriPattern?$queryString"
        }
      }
    }
  }

}

object HttpMethod extends Enumeration {
  type HttpMethod = Value
  val GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD = Value
}

case class Parameter(name: String, required: Boolean = false)

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

  def description(apiStatus: APIStatus) = {
    apiStatus match {
      case APIStatus.ALPHA                        => "Alpha"
      case APIStatus.BETA | APIStatus.PROTOTYPED  => "Beta"
      case APIStatus.STABLE | APIStatus.PUBLISHED => "Stable"
      case APIStatus.DEPRECATED                   => "Deprecated"
      case APIStatus.RETIRED                      => "Retired"
    }
  }

}

case class ServiceDetails(serviceName: String, serviceUrl: String)

case class RawDocumentationContent(contentType: String, content: String)

case class ErrorResponse(code: Option[String] = None, message: Option[String] = None)

object DocsVisibility extends Enumeration {

  type DocsVisibility = Value
  val VISIBLE, OVERVIEW_ONLY, NOT_VISIBLE = Value
}
