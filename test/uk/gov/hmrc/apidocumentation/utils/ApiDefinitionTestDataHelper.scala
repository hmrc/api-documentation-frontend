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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

trait ApiDefinitionTestDataHelper {

  def apiDefinition(name: String, versions: Seq[ApiVersion] = Seq(apiVersion("1.0", ApiStatus.STABLE)), categories: List[ApiCategory] = List.empty) = {
    ApiDefinition(ServiceName(name), name, name, name, ApiContext("hello"), versions.map(version => version.versionNbr -> version).toMap, categories = categories)
  }

  def apiAvailability() = {
    ApiAvailability(
      endpointsEnabled = true,
      access = ApiAccess.PUBLIC,
      loggedIn = false,
      authorised = false
    )
  }

  implicit class ApiAvailabilityModifier(val inner: ApiAvailability) {

    def asPublic: ApiAvailability =
      inner.copy(access = ApiAccess.PUBLIC)

    def asPrivate: ApiAvailability =
      inner.copy(access = ApiAccess.Private(false))

    def asTrial: ApiAvailability =
      inner.copy(access = ApiAccess.Private(true))

    def notTrial: ApiAvailability =
      inner.copy(access = ApiAccess.Private(false))

    def asAuthorised: ApiAvailability =
      inner.copy(authorised = true)

    def notAuthorised: ApiAvailability =
      inner.copy(authorised = false)

    def asLoggedIn: ApiAvailability =
      inner.copy(loggedIn = true)

    def notLoggedIn: ApiAvailability =
      inner.copy(loggedIn = false)

    def withAccess(altAccess: ApiAccess): ApiAvailability =
      inner.copy(access = altAccess)

    def endpointsDisabled: ApiAvailability =
      inner.copy(endpointsEnabled = false)
  }

  def someApiAvailability() = {
    Some(
      apiAvailability()
    )
  }

  implicit class SomeApiAvailabilityModifier(val inner: Option[ApiAvailability]) {

    def asPublic: Option[ApiAvailability] =
      inner.map(_.asPublic)

    def asPrivate: Option[ApiAvailability] =
      inner.map(_.asPrivate)

    def asTrial: Option[ApiAvailability] =
      inner.map(_.asTrial)

    def notTrial: Option[ApiAvailability] =
      inner.map(_.notTrial)

    def asAuthorised: Option[ApiAvailability] =
      inner.map(_.asAuthorised)

    def notAuthorised: Option[ApiAvailability] =
      inner.map(_.notAuthorised)

    def asLoggedIn: Option[ApiAvailability] =
      inner.map(_.asLoggedIn)

    def notLoggedIn: Option[ApiAvailability] =
      inner.map(_.notLoggedIn)

    def withAccess(altAccess: ApiAccess): Option[ApiAvailability] =
      inner.map(_.withAccess(altAccess))

    def endpointsDisabled: Option[ApiAvailability] =
      inner.map(_.endpointsDisabled)
  }

  def endpoint(endpointName: String = "Hello World", url: String = "/world"): Endpoint = {
    Endpoint(endpointName, url, HttpMethod.GET, AuthType.APPLICATION)
  }

  implicit class EndpointModifier(val inner: Endpoint) {

    def asPost: Endpoint =
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
    ExtendedApiDefinition(
      ServiceName(name),
      serviceBaseUrl = name,
      name = name,
      description = name,
      context = ApiContext(name),
      versions = List(
        ExtendedApiVersion(
          version = ApiVersionNbr("1.0"),
          status = ApiStatus.STABLE,
          endpoints = List(
            Endpoint(endpointName = "Today's Date", uriPattern = "/today", method = HttpMethod.GET, authType = AuthType.APPLICATION),
            Endpoint(endpointName = "Yesterday's Date", uriPattern = "/yesterday", method = HttpMethod.GET, authType = AuthType.NONE)
          ),
          productionAvailability = someApiAvailability(),
          sandboxAvailability = None
        )
      ),
      isTestSupport = false,
      lastPublishedAt = None,
      categories = List(ApiCategory.OTHER)
    )
  }

}
