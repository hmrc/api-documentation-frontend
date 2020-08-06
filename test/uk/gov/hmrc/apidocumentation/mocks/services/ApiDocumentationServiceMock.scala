/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.mocks.services

import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.apidocumentation.models._
import uk.gov.hmrc.apidocumentation.models.apispecification.ApiSpecification
import uk.gov.hmrc.apidocumentation.services.DocumentationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future.{failed, successful}

trait ApiDocumentationServiceMock extends MockitoSugar {
  val documentationService = mock[DocumentationService]

  def theDocumentationServiceWillFetchRaml(ramlAndSchemas: RamlAndSchemas) = {
    when(documentationService.fetchRAML(any(), any(), any())).thenReturn(successful(ramlAndSchemas))
  }

  def theDocumentationServiceWillFailWhenFetchingRaml(exception: Throwable) = {
    when(documentationService.fetchRAML(any(), any(), any())).thenReturn(failed(exception))
  }

  def theDocumentationServiceWillFetchApiSpecification(apiSpecification: ApiSpecification)(implicit hc: HeaderCarrier) = {
    when(documentationService.fetchApiSpecification(any(), any(), any())(any())).thenReturn(successful(apiSpecification))
  }

}
