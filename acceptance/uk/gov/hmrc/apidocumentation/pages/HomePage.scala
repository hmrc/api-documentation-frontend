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

import org.scalatestplus.selenium.WebBrowser
import uk.gov.hmrc.apidocumentation.{Env, WebPage}

object HomePage extends WebPage {

  override val url = s"http://localhost:${Env.port}/api-documentation"

  override def isCurrentPage: Boolean = find("RESTful-APIs-Title").fold(false)(_.text == "Create tax software and apps using HMRC APIs")

  def applicationName = WebBrowser.className("hmrc-header__service-name").element.text

  def selectApidoc(): Unit = {
    val apiDocLInk = find(linkText("API documentation")).get
    click on apiDocLInk
  }

}
