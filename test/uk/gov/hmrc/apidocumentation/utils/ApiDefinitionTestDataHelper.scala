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

package uk.gov.hmrc.apidocumentation.utils

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{
  ApiAccess,
  ApiCategory,
  ApiDefinition,
  ApiStatus,
  ApiVersion,
  ExtendedAPIDefinition => NotUsedExtendedApiDef,
  HttpMethod,
  ServiceName
}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

import uk.gov.hmrc.apidocumentation.models._

trait ApiDefinitionTestDataHelper {

  def apiDefinition(name: String, versions: Seq[ApiVersion] = Seq(apiVersion("1.0", ApiStatus.STABLE)), categories: List[ApiCategory] = List.empty) = {
    ApiDefinition(ServiceName(name), name, name, name, ApiContext("hello"), versions.map(version => version.versionNbr -> version).toMap, categories = categories)
  }

  implicit class ApiAccessModifier(val inner: APIAccess) {

    def asPublic: APIAccess = {
      inner.copy(`type` = APIAccessType.PUBLIC)
    }

    def asPrivate: APIAccess = {
      inner.copy(`type` = APIAccessType.PRIVATE)
    }

    def asTrial: APIAccess = {
      inner.copy(isTrial = Some(true))
    }

    def notTrial: APIAccess = {
      inner.copy(isTrial = Some(false))
    }
  }

  def apiAvailability() = {
    APIAvailability(
      endpointsEnabled = true,
      access = APIAccess(
        `type` = APIAccessType.PUBLIC,
        whitelistedApplicationIds = Some(Seq.empty),
        isTrial = Some(false)
      ),
      loggedIn = false,
      authorised = false
    )
  }

  implicit class ApiAvailabilityModifier(val inner: APIAvailability) {

    def asPublic: APIAvailability =
      inner.copy(access = inner.access.asPublic)

    def asPrivate: APIAvailability =
      inner.copy(access = inner.access.asPrivate)

    def asTrial: APIAvailability =
      inner.copy(access = inner.access.asTrial)

    def notTrial: APIAvailability =
      inner.copy(access = inner.access.notTrial)

    def asAuthorised: APIAvailability =
      inner.copy(authorised = true)

    def notAuthorised: APIAvailability =
      inner.copy(authorised = false)

    def asLoggedIn: APIAvailability =
      inner.copy(loggedIn = true)

    def notLoggedIn: APIAvailability =
      inner.copy(loggedIn = false)

    def withAccess(altAccess: APIAccess): APIAvailability =
      inner.copy(access = altAccess)

    def endpointsDisabled: APIAvailability =
      inner.copy(endpointsEnabled = false)
  }

  def someApiAvailability() = {
    Some(
      apiAvailability()
    )
  }

  implicit class SomeApiAvailabilityModifier(val inner: Option[APIAvailability]) {

    def asPublic: Option[APIAvailability] =
      inner.map(_.asPublic)

    def asPrivate: Option[APIAvailability] =
      inner.map(_.asPrivate)

    def asTrial: Option[APIAvailability] =
      inner.map(_.asTrial)

    def notTrial: Option[APIAvailability] =
      inner.map(_.notTrial)

    def asAuthorised: Option[APIAvailability] =
      inner.map(_.asAuthorised)

    def notAuthorised: Option[APIAvailability] =
      inner.map(_.notAuthorised)

    def asLoggedIn: Option[APIAvailability] =
      inner.map(_.asLoggedIn)

    def notLoggedIn: Option[APIAvailability] =
      inner.map(_.notLoggedIn)

    def withAccess(altAccess: APIAccess): Option[APIAvailability] =
      inner.map(_.withAccess(altAccess))

    def endpointsDisabled: Option[APIAvailability] =
      inner.map(_.endpointsDisabled)
  }

  def endpoint(endpointName: String = "Hello World", url: String = "/world"): ExtendedEndpoint = {
    ExtendedEndpoint(endpointName, url, HttpMethod.GET, None)
  }

  implicit class EndpointModifier(val inner: ExtendedEndpoint) {

    def asPost: ExtendedEndpoint =
      inner.copy(method = HttpMethod.POST)
  }

  def apiVersion(version: String = "1.0", status: ApiStatus = ApiStatus.STABLE, access: ApiAccess = ApiAccess.PUBLIC): ApiVersion = {
    ApiVersion(
      ApiVersionNbr(version),
      status,
      access,
      List()
    )
  }

  implicit class ApiVersionModifier(val inner: ApiVersion) {

    def asAlpha: ApiVersion =
      inner.copy(status = ApiStatus.ALPHA)

    def asBeta: ApiVersion =
      inner.copy(status = ApiStatus.BETA)

    def asStable: ApiVersion =
      inner.copy(status = ApiStatus.STABLE)

    def asDeprecated: ApiVersion =
      inner.copy(status = ApiStatus.DEPRECATED)

    def asRETIRED: ApiVersion =
      inner.copy(status = ApiStatus.RETIRED)

    def asPublic: ApiVersion =
      inner.copy(access = inner.access)

    def asPrivate: ApiVersion =
      inner.copy(access = ApiAccess.Private(false))

    def asTrial: ApiVersion =
      inner.copy(access = ApiAccess.Private(true))

    def notTrial: ApiVersion =
      inner.copy(access = ApiAccess.Private(false))

    def withAccess(altAccess: ApiAccess): ApiVersion =
      inner.copy(access = altAccess)

  }

  def extendedApiDefinition(name: String) = {
    ExtendedAPIDefinition(
      name,
      name,
      name,
      name,
      requiresTrust = false,
      isTestSupport = false,
      Seq(
        ExtendedAPIVersion(
          version = "1.0",
          status = APIStatus.STABLE,
          endpoints = Seq(
            ExtendedEndpoint("Today's Date", "/today", HttpMethod.GET, None),
            ExtendedEndpoint("Yesterday's Date", "/yesterday", HttpMethod.GET, None)
          ),
          productionAvailability = someApiAvailability(),
          sandboxAvailability = None
        )
      )
    )
  }

}
