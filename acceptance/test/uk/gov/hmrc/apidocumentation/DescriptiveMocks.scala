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

package uk.gov.hmrc.apidocumentation

trait DescriptiveMocks extends Stubs {

  def condition(message: String): Unit

  def apiServicesIsDeployed(): Unit = {
    condition("All the API services are deployed")
    developerIsSignedIn()
    fetchAll()
    fetchAllXmlApis()
  }

  def helloWorldIsDeployed(serviceName: String, version: String): Unit = {
    condition(s"$serviceName is deployed with version $version")
    developerIsSignedIn()
    fetchDefinition(serviceName)
  }

  def apiDocumentationTestServiceIsDeployed(serviceName: String, version: String): Unit = {
    condition(s"$serviceName is deployed with version $version")
    developerIsSignedIn()
    fetchDefinition(serviceName)
  }

  def apiDocumentationTestServiceVersionsIsDeployed(): Unit = {
    val versions = List("0.1", "0.2", "0.3", "0.4", "1.0", "1.1", "1.2", "1.3", "1.5", "2.0")
    versions.foreach { version =>
      apiDocumentationTestServiceIsDeployed("api-documentation-test-service", version)
    }
  }
}
