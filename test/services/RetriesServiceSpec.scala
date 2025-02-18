/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import cats.implicits.catsStdInstancesForFuture
import config.RetryConfig
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import retry.alleycats.instances.threadSleepFuture
import retry.retryingOnFailures

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetriesServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures {

  "when provided with the RetriesServiceImpl" - {

    val sut = new RetriesServiceImpl()

    "with 3 retries, 0.1s delay and 10s cumulative delay should retry 3 times for 4 invocations total" in {

      val policy = sut.createRetryPolicy(RetryConfig(3, 100.milliseconds, 10.seconds))

      var counted = 0
      Await.result(
        retryingOnFailures[Unit][Future](
          policy,
          (_: Unit) => Future.successful(false),
          (_: Unit, _) => Future.unit
        ) {
          Future.successful(counted += 1)
        },
        10.seconds
      )

      counted mustBe 4
    }

    "with 3 retries, 0.2s delay and 0.1s cumulative delay should retry 0 times for 1 invocation total" in {

      val policy = sut.createRetryPolicy(RetryConfig(3, 200.milliseconds, 100.milliseconds))

      var counted = 0
      Await.result(
        retryingOnFailures[Unit][Future](
          policy,
          (_: Unit) => Future.successful(false),
          (_: Unit, _) => Future.unit
        ) {
          Future.successful(counted += 1)
        },
        1.seconds
      )

      counted mustBe 1
    }

    "with 3 retries, 0.16s delay and 0.3s cumulative delay should retry 1 times for 2 invocations total" in {

      val policy = sut.createRetryPolicy(RetryConfig(3, 160.milliseconds, 300.milliseconds))

      var counted = 0
      Await.result(
        retryingOnFailures[Unit][Future](
          policy,
          (_: Unit) => Future.successful(false),
          (_: Unit, _) => Future.unit
        ) {
          Future.successful(counted += 1)
        },
        1.seconds
      )

      counted mustBe 2
    }

  }

}
