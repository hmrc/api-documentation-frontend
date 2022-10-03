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

package uk.gov.hmrc.apidocumentation

import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium._
import scala.util.{Properties, Try}
import uk.gov.hmrc.webdriver.SingletonDriver

trait Env {

  lazy val driver: WebDriver = createWebDriver()

  val port = 6001

  Runtime.getRuntime addShutdownHook new Thread {
    override def run() {
      shutdown()
    }
  }

  private def shutdown() = {
    Try(driver.close())
    Try(driver.quit())
  }

  private def browser = Properties.propOrElse("browser","chrome")

  private def remoteChromeOptions = {
    val browserOptions: ChromeOptions = new ChromeOptions()
    browserOptions.addArguments("--headless")
    browserOptions.addArguments("--proxy-server='direct://'")
    browserOptions.addArguments("--proxy-bypass-list=*")
    Some(browserOptions)
  }

  private def createWebDriver(): WebDriver = {
    val options = browser match {
      case "remote-chrome" => remoteChromeOptions
      case _ => None
    }
    val driver = SingletonDriver.getInstance(options)
    driver.manage().deleteAllCookies()
    driver.manage().window().setSize(new Dimension(1280, 720))
    driver
  }
}

object Env extends Env
