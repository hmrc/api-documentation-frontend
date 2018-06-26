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

package uk.gov.hmrc.apidocumentation.models

import uk.gov.hmrc.apidocumentation.models.APIStatus.APIStatus
import uk.gov.hmrc.apidocumentation.models.HttpMethod.HttpMethod

import scala.util.Try

object APIAccessType extends Enumeration {
  type APIAccessType = Value
  val PRIVATE, PUBLIC = Value
}

case class APIAccess(`type`: APIAccessType.Value)

case class APIDefinition(
                          serviceName: String,
                          name: String,
                          description: String,
                          context: String,
                          requiresTrust: Option[Boolean],
                          isTestSupport: Option[Boolean],
                          versions: Seq[APIVersion]) {

  require(versions.nonEmpty, s"API versions must not be empty! serviceName=${serviceName}")

  lazy val retiredVersions = versions.filter(_.status == APIStatus.RETIRED)
  lazy val sortedVersions = versions.sortWith(APIDefinition.versionSorter)
  lazy val sortedActiveVersions = sortedVersions.filterNot(v => v.status == APIStatus.RETIRED)
  lazy val statusSortedVersions = versions.sortWith(APIDefinition.statusAndVersionSorter)
  lazy val statusSortedActiveVersions = statusSortedVersions.filterNot(v => v.status == APIStatus.RETIRED)
  lazy val defaultVersion = statusSortedActiveVersions.headOption
  lazy val hasActiveVersions = statusSortedActiveVersions.nonEmpty
}

object APIDefinition {
  private def versionSorter(v1: APIVersion, v2: APIVersion) = {
    val v1Parts = Try(v1.version.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val v2Parts = Try(v2.version.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val pairs = v1Parts.zip(v2Parts)

    val firstUnequalPair = pairs.find { case (one, two) => one != two }
    firstUnequalPair.fold(v1.version.length > v2.version.length) { case (a, b) => a > b }
  }

  def statusAndVersionSorter(v1: APIVersion, v2: APIVersion) = {
    val v1Status = APIStatus.priorityOf(v1.status)
    val v2Status = APIStatus.priorityOf(v2.status)
    v1Status match {
      case `v2Status` => versionSorter(v1, v2)
      case _ => v1Status > v2Status
    }
  }

  private val nonNumericOrPeriodRegex = "[^\\d^.]*"
  private val fallback = Array(1, 0, 0)
}

case class APIVersion(
                       version: String,
                       access: Option[APIAccess],
                       status: APIStatus,
                       endpoints: Seq[Endpoint]) {

  val accessType = access.fold(APIAccessType.PUBLIC)(_.`type`)

  val displayedStatus = {
    val accessIndicator = accessType match {
      case APIAccessType.PRIVATE => "Private "
      case _ => ""
    }
    status match {
      case APIStatus.ALPHA => s"${accessIndicator}Alpha"
      case APIStatus.BETA | APIStatus.PROTOTYPED => s"${accessIndicator}Beta"
      case APIStatus.STABLE | APIStatus.PUBLISHED => s"${accessIndicator}Stable"
      case APIStatus.DEPRECATED => s"${accessIndicator}Deprecated"
      case APIStatus.RETIRED => s"${accessIndicator}Retired"
    }
  }
}

case class ExtendedAPIDefinition(serviceName: String,
                                 serviceBaseUrl: String,
                                 name: String,
                                 description: String,
                                 context: String,
                                 requiresTrust: Boolean,
                                 isTestSupport: Boolean,
                                 versions: Seq[ExtendedAPIVersion]) {

  def userAccessibleApiDefinition = {
    def isAccessible(availability: Option[APIAvailability]) = availability.fold(false)(_.authorised)

    copy(versions = versions.filter(v => isAccessible(v.productionAvailability) || isAccessible(v.sandboxAvailability)))
  }

  lazy val retiredVersions = versions.filter(_.status == APIStatus.RETIRED)
  lazy val sortedVersions = versions.sortWith(ExtendedAPIDefinition.versionSorter)
  lazy val sortedActiveVersions = sortedVersions.filterNot(v => v.status == APIStatus.RETIRED)
  lazy val statusSortedVersions = versions.sortWith(ExtendedAPIDefinition.statusAndVersionSorter)
  lazy val statusSortedActiveVersions = statusSortedVersions.filterNot(v => v.status == APIStatus.RETIRED)
  lazy val defaultVersion = statusSortedActiveVersions.headOption
  lazy val hasActiveVersions = statusSortedActiveVersions.nonEmpty
}

object ExtendedAPIDefinition {
  private def versionSorter(v1: ExtendedAPIVersion, v2: ExtendedAPIVersion) = {
    val v1Parts = Try(v1.version.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val v2Parts = Try(v2.version.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val pairs = v1Parts.zip(v2Parts)

    val firstUnequalPair = pairs.find { case (one, two) => one != two }
    firstUnequalPair.fold(v1.version.length > v2.version.length) { case (a, b) => a > b }
  }

  def statusAndVersionSorter(v1: ExtendedAPIVersion, v2: ExtendedAPIVersion) = {
    val v1Status = APIStatus.priorityOf(v1.status)
    val v2Status = APIStatus.priorityOf(v2.status)
    v1Status match {
      case `v2Status` => versionSorter(v1, v2)
      case _ => v1Status > v2Status
    }
  }

  private val nonNumericOrPeriodRegex = "[^\\d^.]*"
  private val fallback = Array(1, 0, 0)
}

case class ExtendedAPIVersion(version: String,
                              status: APIStatus,
                              endpoints: Seq[Endpoint],
                              productionAvailability: Option[APIAvailability],
                              sandboxAvailability: Option[APIAvailability]) {
  def visibility = {
    def highestAccess(production: APIAvailability, sandbox: APIAvailability) = {
      if(production.access.`type` == APIAccessType.PUBLIC) APIAccessType.PUBLIC
      else sandbox.access.`type`
    }
    def isLoggedIn(production: APIAvailability, sandbox: APIAvailability) = {
      production.loggedIn || sandbox.loggedIn
    }
    def isAuthorised(production: APIAvailability, sandbox: APIAvailability) = {
      production.authorised || sandbox.authorised
    }

    (productionAvailability, sandboxAvailability) match {
      case (Some(production), None) => Some(VersionVisibility(production.access.`type`, production.loggedIn, production.authorised))
      case (None, Some(sandbox)) => Some(VersionVisibility(sandbox.access.`type`, sandbox.loggedIn, sandbox.authorised))
      case (Some(production), Some(sandbox)) =>
        Some(VersionVisibility(highestAccess(production, sandbox), isLoggedIn(production, sandbox), isAuthorised(production, sandbox)))
      case _ => None
    }
  }

  val displayedStatus = {
    val accessIndicator = visibility match {
      case Some(VersionVisibility(APIAccessType.PRIVATE, _, _)) => "Private "
      case _ => ""
    }
    status match {
      case APIStatus.ALPHA => s"${accessIndicator}Alpha"
      case APIStatus.BETA | APIStatus.PROTOTYPED => s"${accessIndicator}Beta"
      case APIStatus.STABLE | APIStatus.PUBLISHED => s"${accessIndicator}Stable"
      case APIStatus.DEPRECATED => s"${accessIndicator}Deprecated"
      case APIStatus.RETIRED => s"${accessIndicator}Retired"
    }
  }
}

case class APIAvailability(endpointsEnabled: Boolean, access: APIAccess, loggedIn: Boolean, authorised: Boolean)

case class VersionVisibility(privacy: APIAccessType.APIAccessType, loggedIn: Boolean, authorised: Boolean)

case class Endpoint(
                     endpointName: String,
                     uriPattern: String,
                     method: HttpMethod,
                     queryParameters: Option[Seq[Parameter]] = None) {

  def decoratedUriPattern = {
    queryParameters.getOrElse(Seq()).isEmpty match {
      case true => uriPattern
      case false => {
        val queryString = queryParameters
          .getOrElse(Seq())
          .filter(_.required)
          .map(parameter => s"${parameter.name}={${parameter.name}}")
          .mkString("&")
        queryString.isEmpty match {
          case true => uriPattern
          case false => s"$uriPattern?$queryString"
        }
      }
    }
  }

}

object HttpMethod extends Enumeration {
  type HttpMethod = Value
  val GET, POST, PUT, DELETE, OPTIONS = Value
}

case class Parameter(name: String, required: Boolean = false)

object APIStatus extends Enumeration {

  type APIStatus = Value
  val ALPHA, BETA, PROTOTYPED, PUBLISHED, STABLE, DEPRECATED, RETIRED = Value

  def priorityOf(apiStatus: APIStatus): Int = {
    apiStatus match {
      case STABLE | PUBLISHED => 5
      case BETA | PROTOTYPED => 4
      case ALPHA => 3
      case DEPRECATED => 2
      case RETIRED => 1
    }
  }
}

case class ServiceDetails(serviceName: String, serviceUrl: String)

case class RawDocumentationContent(contentType: String, content: String)

case class ErrorResponse(code: Option[String] = None, message: Option[String] = None)