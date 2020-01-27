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

package acceptance.uk.gov.hmrc.apidocumentation.pages

import acceptance.uk.gov.hmrc.apidocumentation.{Env, WebPage}

object TutorialsPage extends WebPage {

  override val url = s"http://localhost:${Env.port}/api-documentation/docs/tutorials"

  override def isCurrentPage: Boolean = find(className("page-header")).fold(false)(_.text == "Tutorials")

  def findLinkByText(text: String) = find(linkText(text)) match {
    case Some(e) => Some(elementToLink(e))
    case None => None
  }

  def findLinksByText(text: String): List[Link] = findAll(linkText(text)).toList.map(elementToLink)

  def findElementsByXpath(xpathQuery: String) = findAll(xpath(xpathQuery)).toList

  def elementToLink(element: Element) = Link(element.attribute("href").get, element.text)

}