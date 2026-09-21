/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.apidocumentation.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import uk.gov.hmrc.play.bootstrap.metrics.Metrics

sealed trait Timer {
  def stop(): Unit
}

trait ConnectorMetrics {
  def record[A](apiName: ApiName)(f: => Future[A])(using ExecutionContext): Future[A]
}

@Singleton
class ConnectorMetricsImpl @Inject() (metrics: Metrics) extends ConnectorMetrics {

  def record[A](apiName: ApiName)(f: => Future[A])(using ExecutionContext): Future[A] = {
    val timer = startTimer(apiName)

    f.andThen {
      case _ => timer.stop()
    }.andThen {
      case Success(_) => recordSuccess(apiName)
      case Failure(_) => recordFailure(apiName)
    }
  }

  private def recordFailure(apiName: ApiName): Unit =
    metrics.defaultRegistry.counter(apiName ++ "-failed-counter").inc()

  private def recordSuccess(apiName: ApiName): Unit =
    metrics.defaultRegistry.counter(apiName ++ "-success-counter").inc()

  private def startTimer(apiName: ApiName): Timer = {
    val context = metrics.defaultRegistry.timer(apiName ++ "-timer").time()

    new Timer {
      def stop(): Unit = context.stop()
    }
  }
}

@Singleton
class NoopConnectorMetrics extends ConnectorMetrics {
  def record[A](apiName: ApiName)(f: => Future[A])(using ExecutionContext): Future[A] = f
}
