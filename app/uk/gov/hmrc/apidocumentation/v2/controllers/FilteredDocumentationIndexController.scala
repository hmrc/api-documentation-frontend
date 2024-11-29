/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.v2.controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiCategory, ApiDefinition}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.controllers.{DocumentationCrumb, HeaderNavigation, HomeCrumb, routes}
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services.{ApiDefinitionService, LoggedInUserService, NavigationService, XmlServicesService}
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger
import uk.gov.hmrc.apidocumentation.v2.models._
import uk.gov.hmrc.apidocumentation.v2.views.html.FilteredIndexView

@Singleton
class FilteredDocumentationIndexController @Inject() (
    loggedInUserService: LoggedInUserService,
    val navigationService: NavigationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    apiDefinitionService: ApiDefinitionService,
    xmlServicesService: XmlServicesService,
    indexView: FilteredIndexView
  )(implicit val appConfig: ApplicationConfig,
    val ec: ExecutionContext
  ) extends FrontendController(mcc)
    with HeaderNavigation with ApplicationLogger with DocumentationCrumb with HomeCrumb {

  // lazy val v2HomeCrumb =
  //    Crumb("Home", uk.gov.hmrc.apidocumentation.v2.controllers.routes.FilteredDocumentationIndexController.apiListIndexPage(List.empty, List.empty).url)
  private lazy val apiDocCrumb = Crumb(
    "API Documentation",
    uk.gov.hmrc.apidocumentation.v2.controllers.routes.FilteredDocumentationIndexController.apiListIndexPage(List.empty, List.empty).url
  )

  private lazy val restApiDescriptionOverrides: Seq[RestApiDescriptionOverride] = RestApiDescriptionOverride.descriptionOverrides

  private def getRestApiDescriptionOverride(identifier: String): Option[RestApiDescriptionOverride] = {
    restApiDescriptionOverrides.find(x => x.identifier.value == identifier)
  }

  private def filterApiDocumentation(documents: Seq[ApiDocumentation], categoryFilters: List[ApiCategory], documentationTypeFilter: List[DocumentationTypeFilter])
      : Seq[ApiDocumentation] = {

    def filterByCategory(documents: Seq[ApiDocumentation]): Seq[ApiDocumentation] = {
      categoryFilters.flatMap(filter => documents.filter(api => api.categories.contains(filter))).distinct
    }

    def filterByDocType(documents: Seq[ApiDocumentation]): Seq[ApiDocumentation] = {
      documentationTypeFilter.flatMap(filter => documents.filter(api => filter == DocumentationTypeFilter.byLabel(api.label))).distinct
    }

    (documents, categoryFilters, documentationTypeFilter) match {
      case (Nil, Nil, Nil) => Nil
      case (_, Nil, Nil)   => documents
      case (_, _, Nil)     => filterByCategory(documents).sortBy(_.name)
      case (_, Nil, _)     => filterByDocType(documents).sortBy(_.name)
      case (_, _, _)       => filterByDocType(filterByCategory(documents)).sortBy(_.name)
    }

  }

  def apiListIndexPage(docTypeFilters: List[DocumentationTypeFilter], categoryFilters: List[ApiCategory]): Action[AnyContent] = headerNavigation { implicit request => navLinks =>
    def pageAttributes(title: String = "API Documentation") =
      PageAttributes(title, breadcrumbs = Breadcrumbs(apiDocCrumb, homeCrumb), headerLinks = navLinks, sidebarLinks = navigationService.sidebarNavigation())

    (for {
      userId              <- extractDeveloperIdentifier(loggedInUserService.fetchLoggedInUser())
      apis                <- apiDefinitionService.fetchAllDefinitions(userId)
      xmlApis             <- xmlServicesService.fetchAllXmlApis()
      restDocuments        = apis.map(apiDefinitionToRestDocumentation)
      xmlDocuments         = xmlApis.map(xmlApiToXmlDocumentation)
      allApiDocumentations = (restDocuments ++ xmlDocuments ++ RoadMapDocumentation.roadMaps ++ ServiceGuideDocumentation.serviceGuides).sortBy(_.name)
      filteredDocuments    = filterApiDocumentation(allApiDocumentations, categoryFilters, docTypeFilters)
    } yield Ok(indexView(pageAttributes(), filteredDocuments, docTypeFilters, categoryFilters))) recoverWith {
      case e: Throwable =>
        logger.error("Could not load API Documentation service", e)
        errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
    }

  }

  private def extractDeveloperIdentifier(f: Future[Option[Developer]]): Future[Option[DeveloperIdentifier]] = {
    f.map(o =>
      o.map(d => UuidIdentifier(d.userId))
    )
  }

  private def xmlApiToXmlDocumentation(api: XmlApiDocumentation) = {
    val identifier = DocumentIdentifier(api.name)
    val url        = routes.ApiDocumentationController.renderXmlApiDocumentation(identifier.value, useV2 = Some(true)).url
    XmlDocumentation.fromXmlDocumentation(identifier, api, url)
  }

  private def apiDefinitionToRestDocumentation(api: ApiDefinition) = {
    val defaultVersionNbr: ApiVersionNbr = api
      .versionsAsList
      .sorted(WrappedApiDefinition.statusVersionOrdering)
      .head.versionNbr
    val url: String                      = routes.ApiDocumentationController.renderApiDocumentation(api.serviceName, defaultVersionNbr, useV2 = Some(true)).url
    RestDocumentation.fromApiDefinition(api, url, defaultVersionNbr, getRestApiDescriptionOverride(api.serviceName.value))
  }

}
