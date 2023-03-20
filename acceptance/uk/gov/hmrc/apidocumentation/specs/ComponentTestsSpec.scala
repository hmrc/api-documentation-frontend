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

package uk.gov.hmrc.apidocumentation.specs

import uk.gov.hmrc.apidocumentation.BaseSpec
import org.scalatest._
import uk.gov.hmrc.apidocumentation.DescriptiveMocks

trait ComponentTestsSpec extends GivenWhenThen { baseSpec: BaseSpec =>

  object Given extends DescriptiveMocks {
    override def condition(message: String): Unit = {
      Given(message: String)
    }
  }

  object When extends DescriptiveMocks {
    override def condition(message: String): Unit = {
      When(message: String)
    }
  }

  object And extends DescriptiveMocks {
    override def condition(message: String): Unit = {
      And(message: String)
    }
  }
}
