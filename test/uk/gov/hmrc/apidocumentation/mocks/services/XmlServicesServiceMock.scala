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

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.apidocumentation.models.XmlApiDocumentation
import uk.gov.hmrc.apidocumentation.services.XmlServicesService

import scala.concurrent.Future.{failed, successful}

trait XmlServicesServiceMock extends MockitoSugar with ArgumentMatchersSugar{
  val xmlApi1: XmlApiDocumentation = XmlApiDocumentation(
    name = "xml api 1",
    context = "xml api context",
    description = "xml api description",
    categories = None
  )

  val xmlApi2: XmlApiDocumentation = xmlApi1.copy(name = "xml api 2")
  val xmlApis = Seq(xmlApi1, xmlApi2)

  val vatXmlApi = XmlApiDocumentation(name = "VAT and EC Sales List Online",
    context = "/government/collections/vat-and-ec-sales-list-online-support-for-software-developers",
    description = "Technical specifications for software developers working with the VAT and EC Sales List Online service. This API is not part of the Making Tax Digital initiative."
  )


  lazy val xmlServicesService: XmlServicesService = mock[XmlServicesService]

  def fetchXmlApiReturnsApi() = {
    when(xmlServicesService.fetchXmlApi(*)(*)).thenReturn(successful(Some(xmlApi1)))
  }

  def fetchXmlApiReturnsNone() = {
    when(xmlServicesService.fetchXmlApi(*)(*)).thenReturn(successful(None))
  }

  def fetchAllXmlApisReturnsApis() = {
    when(xmlServicesService.fetchAllXmlApis()(*)).thenReturn(successful(xmlApis))
  }

  def fetchAllXmlApisReturnsVatApi() = {
    when(xmlServicesService.fetchAllXmlApis()(*)).thenReturn(successful(Seq(vatXmlApi)))
  }

  def fetchAllXmlApisReturnsEmptySeq() = {
    when(xmlServicesService.fetchAllXmlApis()(*)).thenReturn(successful(Seq.empty))
  }

  def fetchAllXmlApisFails(exception: Throwable) = {
    when(xmlServicesService.fetchAllXmlApis()(*)).thenReturn(failed(exception))
  }
}
