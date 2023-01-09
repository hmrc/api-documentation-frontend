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

import uk.gov.hmrc.apidocumentation.WebPage
import org.openqa.selenium.{By, WebElement}

import scala.util.Try

case class Link(href: String, text: String)

trait Section extends WebPage {

  def sectionQuery: Query

  def text = section.get.text

  def SectionNotDisplayedException = new NoSuchElementException("Section not displayed: " + sectionQuery)

  def section: Option[Element] = find(sectionQuery)

  def displayed = section.fold(false)(_.isDisplayed)

  override def toString: String = s"Section(${sectionQuery.toString})"

  def find(by: By): Option[WebElement] = section.flatMap(s => Try(s.underlying.findElement(by)).toOption)

  def elementToLink(element: Option[WebElement]): Option[Link] =
    element match {
      case Some(e) => Some(Link(e.getAttribute("href"), e.getText))
      case None => None
    }

  def findLink(id: String): Option[Link] = elementToLink(find(By.id(id)))

}
