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

package uk.gov.hmrc.apidocumentation.views

import java.util.Locale

import org.scalatest.{Matchers, OptionValues, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.WsScalaTestClient
import play.api.i18n._
import play.api.mvc.{AnyContent, Request}

trait CommonViewSpec extends WordSpec with Matchers with OptionValues with WsScalaTestClient with MockitoSugar with GuiceOneAppPerSuite {
  implicit val messagesProvider: MessagesProvider = MessagesImpl(Lang(Locale.ENGLISH), new DefaultMessagesApi())
  implicit val request = mock[Request[AnyContent]]
}
