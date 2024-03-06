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

package uk.gov.hmrc.apidocumentation.pages

import org.openqa.selenium.By

import uk.gov.hmrc.apidocumentation.{Env, WebPage}

object HomePage extends WebPage with HasApplicationName {

  val pageHeading = "Create tax software and apps using HMRC APIs"

  private val aHeading    = By.id("RESTful-APIs-Title")
  private val apiDocsLink = By.linkText("API documentation")

  override val url = s"http://localhost:${Env.port}/api-documentation"

  override def isCurrentPage(): Boolean = getText(aHeading) == "Create tax software and apps using HMRC APIs"

  def selectApidoc(): Unit = {
    click(apiDocsLink)
  }

}
