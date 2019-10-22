/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.apidocumentation.models.{APIDefinition, ExtendedAPIDefinition, ExtendedAPIVersion}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProxyAwareApiDefinitionService @Inject()(principal: PrincipalApiDefinitionService,
                                               subordinate: SubordinateApiDefinitionService
                                              )(implicit val ec: ExecutionContext)
                                              extends BaseApiDefinitionService {

  def fetchAllDefinitions(thirdPartyDeveloperEmail: Option[String])
                         (implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {

    val principalFuture = principal.fetchAllDefinitions(thirdPartyDeveloperEmail)
    val subordinateFuture = subordinate.fetchAllDefinitions(thirdPartyDeveloperEmail)
    mergeSeqsOfDefinitions(subordinateFuture,principalFuture) map filterDefinitions
  }

  private def mergeSeqsOfDefinitions(subordinateFuture: Future[Seq[APIDefinition]], principalFuture: Future[Seq[APIDefinition]]) = {
    for {
      subordinateDefinitions <- subordinateFuture
      principalDefinitions <- principalFuture
    } yield (subordinateDefinitions ++ principalDefinitions.filterNot(_.isIn(subordinateDefinitions))).sortBy(_.name)
  }

  def filterDefinitions(apis: Seq[APIDefinition]): Seq[APIDefinition] = {
    def apiRequiresTrust(api: APIDefinition): Boolean = {
      api.requiresTrust match {
        case Some(true) => true
        case _ => false
      }
    }

    apis.filter(api => !apiRequiresTrust(api) && api.hasActiveVersions)
  }

  def fetchExtendedDefinition(serviceName: String, thirdPartyDeveloperEmail: Option[String])
                        (implicit hc: HeaderCarrier): Future[Option[ExtendedAPIDefinition]] = {
    val principalFuture = principal.fetchExtendedDefinition(serviceName, thirdPartyDeveloperEmail)
    val subordinateFuture = subordinate.fetchExtendedDefinition(serviceName, thirdPartyDeveloperEmail)

    for {
      maybePrincipalDefinition <- principalFuture
      maybeRemoteSubordinate <- subordinateFuture
      combined = combine(maybePrincipalDefinition, maybeRemoteSubordinate)
    } yield combined.filterNot(_.requiresTrust)
  }

  private def combine(maybePrincipalDefinition: Option[ExtendedAPIDefinition], maybeSubordinateDefinition: Option[ExtendedAPIDefinition]) = {
    def findProductionDefinition(maybePrincipalDefinition: Option[ExtendedAPIDefinition], maybeSubordinateDefinition: Option[ExtendedAPIDefinition]) = {
      if (maybePrincipalDefinition.exists(_.versions.exists(_.productionAvailability.isDefined))) {
        maybePrincipalDefinition
      } else {
        maybeSubordinateDefinition
      }
    }

    def findSandboxDefinition(maybePrincipalDefinition: Option[ExtendedAPIDefinition], maybeSubordinateDefinition: Option[ExtendedAPIDefinition]) = {
      if (maybePrincipalDefinition.exists(_.versions.exists(_.sandboxAvailability.isDefined))) {
        maybePrincipalDefinition
      } else {
        maybeSubordinateDefinition
      }
    }

    def combineVersion(maybePrincipalVersion: Option[ExtendedAPIVersion], maybeSubordinateVersion: Option[ExtendedAPIVersion]) = {
      maybePrincipalVersion.fold(maybeSubordinateVersion) { productionVersion =>
        maybeSubordinateVersion.fold(maybePrincipalVersion) { sandboxVersion =>
          Some(sandboxVersion.copy(productionAvailability = productionVersion.productionAvailability))
        }
      }
    }

    def combineVersions(productionVersions: Seq[ExtendedAPIVersion], sandboxVersions: Seq[ExtendedAPIVersion]): Seq[ExtendedAPIVersion] = {
      val allVersions = (productionVersions.map(_.version) ++ sandboxVersions.map(_.version)).distinct.sorted
      allVersions.flatMap { version =>
        combineVersion(productionVersions.find(_.version == version), sandboxVersions.find(_.version == version))
      }
    }

    val maybeProductionDefinition = findProductionDefinition(maybePrincipalDefinition, maybeSubordinateDefinition)
    val maybeSandboxDefinition = findSandboxDefinition(maybePrincipalDefinition, maybeSubordinateDefinition)

    maybeProductionDefinition.fold(maybeSandboxDefinition) { productionDefinition =>
      maybeSandboxDefinition.fold(maybeProductionDefinition) { sandboxDefinition =>
        Some(sandboxDefinition.copy(versions = combineVersions(productionDefinition.versions, sandboxDefinition.versions)))
      }
    }
  }
}
