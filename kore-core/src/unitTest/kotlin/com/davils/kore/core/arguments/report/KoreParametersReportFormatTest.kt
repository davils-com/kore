/*
 * Copyright 2026 Davils
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

package com.davils.kore.core.arguments.report

import com.davils.kore.core.arguments.fixture.NO_ENVIRONMENT
import com.davils.kore.core.arguments.fixture.lines
import com.davils.kore.core.arguments.report.fixture.Deployment
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class KoreParametersReportFormatTest : FunSpec({
    context("a failed load") {
        test("renders every problem in one readable report and keeps the listing apart from it") {
            val expected = lines(
                "Invalid program parameters (4 problems):",
                "",
                "  --moed         is not a known parameter - did you mean --mode?",
                "  --port         'eighty' is not a valid integer",
                "  --data-dir     is required but was not given, and APP_DATA_DIR is not set",
                "  --db-password  is required but was not given, and APP_DB_PASSWORD is not set",
            )
            val listing = lines(
                "Supported parameters:",
                "  --mode <Stage>          default: Production  env: APP_MODE",
                "  --port, -p <int>        The port to listen on.  required  env: APP_PORT",
                "  --data-dir <path>       required  env: APP_DATA_DIR",
                "  --db-password <string>  required  env: APP_DB_PASSWORD",
                "  --tag <string>          repeatable  env: APP_TAG",
                "  --dry-run               env: APP_DRY_RUN",
            )

            val failure = shouldThrow<KoreParameterException> {
                Deployment().load(listOf("--port=eighty", "--moed=production"), NO_ENVIRONMENT)
            }

            failure.message shouldBe expected
            failure.report.render() shouldBe expected
            failure.usage shouldBe listing
            failure.usage shouldBe Deployment().usage()
        }
    }
})
