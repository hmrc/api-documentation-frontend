/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.models

object APIDefinitionLabel extends Enumeration {
  type DocumentationLabel = Value

  protected case class Val(displayName: String, modifier: String) extends super.Val
  implicit def valueToAPIDefinitionLabelVal(x: Value): Val = x.asInstanceOf[Val]

  val ROADMAP = Val("Roadmap", "roadmap")
  val SERVICE_GUIDE = Val("Service Guide", "service-guide")
  val REST_API = Val("REST API", "rest")
  val TEST_SUPPORT_API = Val("Test Support API", "test")
  val XML_API = Val("XML API", "xml")
}
