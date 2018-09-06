/*
 * Copyright 2018 HM Revenue & Customs
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

package acceptance.uk.gov.hmrc.apidocumentation

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.openqa.selenium.WebDriver
import org.scalatest._
import org.scalatestplus.play.guice.{GuiceFakeApplicationFactory, GuiceOneServerPerSuite}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}

trait BaseSpec extends FeatureSpec with BeforeAndAfterEach with BeforeAndAfterAll with Matchers with NavigationSugar
  with GuiceOneServerPerSuite with GuiceFakeApplicationFactory {

  override lazy val port = Env.port
  val stubPort = 11111
  val stubHost = "localhost"

  implicit val webDriver: WebDriver = Env.driver

  var wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .configure("run.mode" -> "Stub")
      .in(Mode.Prod)
      .build()
  }

  override def beforeAll() = {
    println("Starting WM")
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterAll() = {
    println("Stopping WM")
    wireMockServer.stop()
  }

  override def beforeEach() = {
    println(s"Wiremock Running - ${wireMockServer.isRunning}, on port ${wireMockServer.port()}")
    webDriver.manage().deleteAllCookies()
    WireMock.reset()
  }
}
