/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.apidocumentation.models.apispecification.ApiSpecification
import uk.gov.hmrc.apidocumentation.services.DocumentationService

import scala.concurrent.Future.successful
import org.mockito.ArgumentMatchersSugar
import org.mockito.MockitoSugar

trait ApiDocumentationServiceMock extends MockitoSugar with ArgumentMatchersSugar {
  val documentationService = mock[DocumentationService]

  def theDocumentationServiceWillFetchApiSpecification(apiSpecification: ApiSpecification) = {
    when(documentationService.fetchApiSpecification(*, *, *)(*)).thenReturn(successful(apiSpecification))
  }

}
