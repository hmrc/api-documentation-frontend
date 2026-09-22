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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.*
import uk.gov.hmrc.apiplatform.modules.common.domain.models.*

trait ApiDefinitionTestDataHelper {

  def apiDefinition(name: String, versions: Seq[ApiVersion] = Seq(apiVersion("1.0", ApiStatus.Stable)), categories: List[ApiCategory] = List.empty) = {
    ApiDefinition(
      ServiceName(name),
      ApiDefinition.ServiceBaseUrl(s"/name"),
      ApiDefinition.Name(name),
      ApiDefinition.Description(name),
      ApiContext("hello"),
      versions.map(version => (version.versionNbr -> version)).toMap,
      false,
      None,
      categories = categories
    )
  }

  def apiAvailability() = {
    ApiAvailability(
      endpointsEnabled = true,
      access = ApiAccessType.Public,
      loggedIn = false,
      authorised = false
    )
  }

  implicit class ApiAvailabilityModifier(val inner: ApiAvailability) {

    def asPublic: ApiAvailability =
      inner.copy(access = ApiAccessType.Public)

    def asPrivate: ApiAvailability =
      inner.copy(access = ApiAccessType.Internal)

    def asTrial: ApiAvailability =
      inner.copy(access = ApiAccessType.Controlled)

    def notTrial: ApiAvailability =
      inner.copy(access = ApiAccessType.Internal)

    def asAuthorised: ApiAvailability =
      inner.copy(authorised = true)

    def notAuthorised: ApiAvailability =
      inner.copy(authorised = false)

    def asLoggedIn: ApiAvailability =
      inner.copy(loggedIn = true)

    def notLoggedIn: ApiAvailability =
      inner.copy(loggedIn = false)

    def withAccess(altAccess: ApiAccessType): ApiAvailability =
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

    def withAccess(altAccess: ApiAccessType): Option[ApiAvailability] =
      inner.map(_.withAccess(altAccess))

    def endpointsDisabled: Option[ApiAvailability] =
      inner.map(_.endpointsDisabled)
  }

  def endpoint(endpointName: String = "Hello World", url: String = "/world"): Endpoint = {
    Endpoint(Endpoint.UriPattern(url), Endpoint.Name(endpointName), HttpMethod.Get, AuthType.Application, ResourceThrottlingTier.Unlimited, None, Nil)
  }

  implicit class EndpointModifier(val inner: Endpoint) {

    def asPost: Endpoint =
      inner.copy(method = HttpMethod.Post)
  }

  def apiVersion(version: String = "1.0", status: ApiStatus = ApiStatus.Stable, access: ApiAccessType = ApiAccessType.Public): ApiVersion = {
    ApiVersion(
      ApiVersionNbr(version),
      status,
      access,
      List(),
      true,
      None,
      ApiVersionSource.OAS
    )
  }

  implicit class ApiVersionModifier(val inner: ApiVersion) {

    def asAlpha: ApiVersion =
      inner.copy(status = ApiStatus.Alpha)

    def asBeta: ApiVersion =
      inner.copy(status = ApiStatus.Beta)

    def asStable: ApiVersion =
      inner.copy(status = ApiStatus.Stable)

    def asDeprecated: ApiVersion =
      inner.copy(status = ApiStatus.Deprecated)

    def asRETIRED: ApiVersion =
      inner.copy(status = ApiStatus.Retired)

    def asPublic: ApiVersion =
      inner.copy(access = inner.access)

    def asPrivate: ApiVersion =
      inner.copy(access = ApiAccessType.Internal)

    def asTrial: ApiVersion =
      inner.copy(access = ApiAccessType.Controlled)

    def notTrial: ApiVersion =
      inner.copy(access = ApiAccessType.Internal)

    def withAccess(altAccess: ApiAccessType): ApiVersion =
      inner.copy(access = altAccess)

  }

  def extendedApiDefinition(name: String) = {
    ExtendedApiDefinition(
      ServiceName(name),
      serviceBaseUrl = ApiDefinition.ServiceBaseUrl(name),
      name = ApiDefinition.Name(name),
      description = ApiDefinition.Description(name),
      context = ApiContext(name),
      versions = List(
        ExtendedApiVersion(
          version = ApiVersionNbr("1.0"),
          status = ApiStatus.Stable,
          endpoints = List(
            Endpoint(
              endpointName = Endpoint.Name("Today's Date"),
              uriPattern = Endpoint.UriPattern("/today"),
              method = HttpMethod.Get,
              authType = AuthType.Application,
              ResourceThrottlingTier.Unlimited,
              None,
              Nil
            ),
            Endpoint(
              endpointName = Endpoint.Name("Yesterday's Date"),
              uriPattern = Endpoint.UriPattern("/yesterday"),
              method = HttpMethod.Get,
              authType = AuthType.None,
              ResourceThrottlingTier.Unlimited,
              None,
              Nil
            )
          ),
          productionAvailability = someApiAvailability(),
          sandboxAvailability = None
        )
      ),
      isTestSupport = false,
      lastPublishedAt = None,
      categories = List(ApiCategory.Other)
    )
  }

}
