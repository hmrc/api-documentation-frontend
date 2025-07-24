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

package uk.gov.hmrc.apidocumentation.controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiCategory, ApiDefinition}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import uk.gov.hmrc.apidocumentation.ErrorHandler
import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.services.{ApiDefinitionService, LoggedInUserService, NavigationService, XmlServicesService}
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger
import uk.gov.hmrc.apidocumentation.views.html.documentationList.FilteredIndexView

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

  private lazy val apiDocCrumb = Crumb(
    "API Documentation",
    uk.gov.hmrc.apidocumentation.controllers.routes.FilteredDocumentationIndexController.apiListIndexPage(List.empty, List.empty).url
  )

  private lazy val restApiDescriptionOverrides: Seq[RestApiDescriptionOverride] = RestApiDescriptionOverride.descriptionOverrides

  private def getRestApiDescriptionOverride(identifier: String): Option[RestApiDescriptionOverride] = {
    restApiDescriptionOverrides.find(x => x.identifier.value == identifier)
  }

  private def filterApiDocumentation(documents: Seq[ApiDocumentation], categoryFilters: List[ApiCategory], documentationTypeFilter: List[DocumentationTypeFilter], maybeSearchTerm: Option[String])
      : Seq[ApiDocumentation] = {

    def filterByCategory(documents: Seq[ApiDocumentation]): Seq[ApiDocumentation] = {
      println(s"******* In filterByCategory categoryFilters:$categoryFilters")
      categoryFilters.flatMap(filter => documents.filter(api => api.categories.contains(filter))).distinct
    }

    def filterByDocType(documents: Seq[ApiDocumentation]): Seq[ApiDocumentation] = {
      println(s"******* In filterByDocType documentationTypeFilter:$documentationTypeFilter")
      documentationTypeFilter.flatMap(filter => documents.filter(api => filter == DocumentationTypeFilter.byLabel(api.label))).distinct
    }

    def filterBySearchTerm(documents: Seq[ApiDocumentation], searchTerm: String): Seq[ApiDocumentation] = {
      println(s"******* In filterBySearchTerm searchTerm:$searchTerm")
      documents.filter(api => api.name.toLowerCase.contains(searchTerm.toLowerCase) | api.description.toLowerCase.contains(searchTerm.toLowerCase))
    }

    (documents, categoryFilters, documentationTypeFilter, maybeSearchTerm) match {
      case (Nil, Nil, Nil, None) => Nil
      case (_, Nil, Nil, None)   => documents
      case (_, _, Nil, None)     => filterByCategory(documents).sortBy(_.name)
      case (_, Nil, _, None)     => filterByDocType(documents).sortBy(_.name)
      case (_, _, _, None)       => filterByDocType(filterByCategory(documents)).sortBy(_.name)
      case (_, Nil, Nil, Some(search))     => filterBySearchTerm(documents, search).sortBy(_.name)
      case (_, _, Nil, Some(search))     => filterBySearchTerm(filterByCategory(documents).sortBy(_.name), search)
      case (_, Nil, _, Some(search))     => filterBySearchTerm(filterByDocType(documents).sortBy(_.name), search)
      case (_, _, _, Some(search))       => filterBySearchTerm(filterByDocType(filterByCategory(documents)), search).sortBy(_.name)
    }

  }

  def apiListIndexPage(docTypeFilters: List[DocumentationTypeFilter], categoryFilters: List[ApiCategory], searchTerm: Option[String] = None): Action[AnyContent] = headerNavigation { implicit request => navLinks =>
    def pageAttributes(title: String = "API Documentation") = {
      PageAttributes(title, breadcrumbs = Breadcrumbs(apiDocCrumb, homeCrumb), headerLinks = navLinks, sidebarLinks = navigationService.sidebarNavigation())
    }
    (for {
      userId              <- extractDeveloperIdentifier(loggedInUserService.fetchLoggedInUser())
      apis                <- apiDefinitionService.fetchAllDefinitions(userId)
      xmlApis             <- xmlServicesService.fetchAllXmlApis()
      restDocuments        = apis.map(apiDefinitionToRestDocumentation)
      xmlDocuments         = xmlApis.map(xmlApiToXmlDocumentation)
      allApiDocumentations = (restDocuments ++ xmlDocuments ++ RoadMapDocumentation.roadMaps ++ ServiceGuideDocumentation.serviceGuides).sortBy(_.name)
      filteredDocuments    = filterApiDocumentation(allApiDocumentations, categoryFilters, docTypeFilters, searchTerm)
    } yield Ok(indexView(pageAttributes(), filteredDocuments, docTypeFilters, categoryFilters, searchTerm))) recoverWith {
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
    val url        = routes.ApiDocumentationController.renderXmlApiDocumentation(identifier.value).url
    XmlDocumentation.fromXmlDocumentation(identifier, api, url)
  }

  private def apiDefinitionToRestDocumentation(api: ApiDefinition) = {
    val defaultVersionNbr: ApiVersionNbr = api
      .versionsAsList
      .sorted(WrappedApiDefinition.statusVersionOrdering)
      .head.versionNbr
    val url: String                      = routes.ApiDocumentationController.renderApiDocumentation(api.serviceName, defaultVersionNbr).url
    RestDocumentation.fromApiDefinition(api, url, defaultVersionNbr, getRestApiDescriptionOverride(api.serviceName.value))
  }

}
