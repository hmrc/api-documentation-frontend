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
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiCategory
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import uk.gov.hmrc.apidocumentation.config.ApplicationConfig
import uk.gov.hmrc.apidocumentation.controllers.{DocumentationCrumb, HeaderNavigation, HomeCrumb}
import uk.gov.hmrc.apidocumentation.models.{Breadcrumbs, PageAttributes}
import uk.gov.hmrc.apidocumentation.services.{ApiDefinitionService, NavigationService, XmlServicesService}
import uk.gov.hmrc.apidocumentation.util.ApplicationLogger
import uk.gov.hmrc.apidocumentation.v2.models._
import uk.gov.hmrc.apidocumentation.views.html.v2.IndexViewV2

@Singleton
class V2DocumentationController @Inject() (
    val navigationService: NavigationService,
    mcc: MessagesControllerComponents,
    apiDefinitionService: ApiDefinitionService,
    xmlServicesService: XmlServicesService,
    indexView: IndexViewV2
  )(implicit val appConfig: ApplicationConfig,
    val ec: ExecutionContext
  ) extends FrontendController(mcc)
    with HeaderNavigation with ApplicationLogger with DocumentationCrumb with HomeCrumb {

  private lazy val restApiDescriptionOverrides: Seq[RestApiDescriptionOverride] = RestApiDescriptionOverride.descriptionOverrides

  private def getRestApiDescriptionOverride(identifier: String): Option[RestApiDescriptionOverride] = {
    restApiDescriptionOverrides.find(x => x.identifier.value == identifier)
  }

  private def filterApiDocumentation(documents: Seq[ApiDocumentation], categoryFilters: List[ApiCategory], documentationTypeFilter: List[DocumentationTypeFilter]) : Seq[ApiDocumentation] = {
   def filterByCategory(documents: Seq[ApiDocumentation]): Seq[ApiDocumentation] = {
     categoryFilters.flatMap(filter => documents.filter(api => api.categories.contains(filter))).distinct
   }

    def filterByDocType(documents: Seq[ApiDocumentation]): Seq[ApiDocumentation] = {
      documentationTypeFilter.flatMap(filter => documents.filter(api => filter == DocumentationTypeFilter.byLabel(api.label))).distinct
    }
    (documents, categoryFilters, documentationTypeFilter) match {
      case (Nil , Nil, Nil) => Nil
      case (_, Nil, Nil) => documents
      case (_, _, Nil) => filterByCategory(documents).sortBy(_.name)
      case (_, Nil, _) => filterByDocType(documents).sortBy(_.name)
      case (_, _, _) => filterByDocType(filterByCategory(documents)).sortBy(_.name)
    }

  }

  def start(docTypeFilters: List[DocumentationTypeFilter], categoryFilters: List[ApiCategory]): Action[AnyContent] = headerNavigation { implicit request =>navLinks =>

    def pageAttributes(title: String = "API Documentation") =
      PageAttributes(title, breadcrumbs = Breadcrumbs(documentationCrumb, homeCrumb), headerLinks = navLinks, sidebarLinks = navigationService.sidebarNavigation())

    val documentsF: Future[Seq[ApiDocumentation]] = for {
      apis          <- apiDefinitionService.fetchAllDefinitions()
      xmlApis       <- xmlServicesService.fetchAllXmlApis()
      roadMaps       = RoadMapDocumentation.roadMaps
      serviceGuides  = ServiceGuideDocumentation.serviceGuides
      apiDocuments   = apis.map(api => RestDocumentation.fromApiDefinition(api, getRestApiDescriptionOverride(api.serviceName.value)))
      xmlDocuments   = xmlApis.map(x => XmlDocumentation.fromXmlDocumentation(x))
    } yield (apiDocuments ++ xmlDocuments ++ roadMaps ++ serviceGuides).sortBy(_.name)

    documentsF.map(apiDocumentations => {
      val filteredApiDocumentations = filterApiDocumentation(apiDocumentations, categoryFilters, docTypeFilters)
       Ok(indexView(pageAttributes(), filteredApiDocumentations, docTypeFilters, categoryFilters))
    })
  }

}
