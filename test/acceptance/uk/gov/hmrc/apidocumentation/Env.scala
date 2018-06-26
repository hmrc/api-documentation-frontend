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

import cucumber.api.scala.{EN, ScalaDsl}
import org.openqa.selenium._
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxProfile}
import org.scalatest.Matchers

import scala.util.Try

trait Env extends ScalaDsl with EN with Matchers {

  val port = 9680
  var host: String = _

  val hostIs = System.getProperty("env", "local").toLowerCase
  hostIs match {
    case "local" => host = s"http://localhost:$port"
  }

  lazy val driver: WebDriver = createWebDriver
  lazy val createWebDriver: WebDriver = {
    val targetBrowser = System.getProperty("browser", "chrome-local").toLowerCase
    targetBrowser match {
      case "chrome-local" => createChromeDriver()
      case "firefox-local" => createFirefoxDriver()
      case _ => throw new IllegalArgumentException(s"target browser $targetBrowser not recognised")
    }
  }

  def createChromeDriver(): WebDriver = {
    val driver = new ChromeDriver()
    driver
  }

  def createFirefoxDriver(): WebDriver = {
    val profile = new FirefoxProfile
    profile.setAcceptUntrustedCertificates(true)
    new FirefoxDriver(profile)
  }

  sys addShutdownHook {
    Try(driver.quit())
  }
}

object Env extends Env
