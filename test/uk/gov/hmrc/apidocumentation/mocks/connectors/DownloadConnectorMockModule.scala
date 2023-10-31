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

package uk.gov.hmrc.apidocumentation.mocks.connectors

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ServiceName
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr

import uk.gov.hmrc.apidocumentation.connectors.DownloadConnector

trait DownloadConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait AbstractDownloadConnectorMock {
    def aMock: DownloadConnector

    object Fetch {

      def returnsNoneIfNotFound() = {
        when(aMock.fetch(*[ServiceName], *[ApiVersionNbr], *)).thenReturn(successful(None))
      }
    }
  }

  object DownloadConnectorMock extends AbstractDownloadConnectorMock {
    val aMock = mock[DownloadConnector]
  }
}
