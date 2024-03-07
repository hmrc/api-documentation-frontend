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

import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}

import uk.gov.hmrc.apidocumentation.specs.ComponentTestsSpec

trait TableDrivenPropertyMocks extends TableDrivenPropertyChecks { cs: ComponentTestsSpec =>

  def helloWorldVersionsIsDeployed(versionTable: TableFor1[String] = Table("Versions", "1.0", "1.2")): Unit = {
    forAll(versionTable) { version =>
      And.helloWorldIsDeployed("api-example-microservice", version)
    }
  }
}
