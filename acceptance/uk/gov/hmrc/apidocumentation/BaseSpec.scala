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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.featurespec.AnyFeatureSpec
import uk.gov.hmrc.apidocumentation.common.MyTestServerFactory
import org.scalatestplus.play.guice.GuiceOneServerPerTest
import org.scalatest.concurrent.Eventually
import uk.gov.hmrc.selenium.webdriver.{Browser, Driver, ScreenshotOnFailure}
import play.api.test.TestServerFactory

trait BaseSpec extends AnyFeatureSpec
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with GuiceOneServerPerTest
  with Eventually
  with Browser
  with ScreenshotOnFailure {

  val stubPort = 11111
  val stubHost = "localhost"

  override protected def testServerFactory: TestServerFactory = MyTestServerFactory

  var wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.developer-frontend.host" -> stubHost,
        "microservice.services.developer-frontend.port" -> stubPort,
        "microservice.services.api-platform-microservice.host" -> stubHost,
        "microservice.services.api-platform-microservice.port" -> stubPort,
        "microservice.services.third-party-developer.host" -> stubHost,
        "microservice.services.third-party-developer.port" -> stubPort,
        "microservice.services.api-platform-xml-services.host" -> stubHost,
        "microservice.services.api-platform-xml-services.port" -> stubPort
      )
      .in(Mode.Prod)
      .build()
  }

  override def beforeAll() = {
    super.beforeAll()
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterAll() = {
    wireMockServer.stop()
    super.afterAll()
  }

  override def beforeEach() = {
    super.beforeEach()
    startBrowser()
    Driver.instance.manage().deleteAllCookies()
    WireMock.reset()
  }

  override def afterEach() = {
    quitBrowser()
    super.afterEach()
  }
}
