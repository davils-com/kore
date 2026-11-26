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

module com.davils.kore.core {
    requires transitive kotlin.stdlib;
    requires transitive kotlinx.coroutines.core;

    exports com.davils.kore.core;
    exports com.davils.kore.core.annotation;
    exports com.davils.kore.core.architecture.data;
    exports com.davils.kore.core.architecture.domain;
    exports com.davils.kore.core.architecture.mapper;
    exports com.davils.kore.core.arguments;
    exports com.davils.kore.core.arguments.conversion;
    exports com.davils.kore.core.arguments.declaration;
    exports com.davils.kore.core.arguments.report;
    exports com.davils.kore.core.budget;
    exports com.davils.kore.core.collection;
    exports com.davils.kore.core.concurrency;
    exports com.davils.kore.core.dsl;
    exports com.davils.kore.core.failure;
    exports com.davils.kore.core.identity;
    exports com.davils.kore.core.identity.generator;
    exports com.davils.kore.core.naming;
    exports com.davils.kore.core.outcome;
    exports com.davils.kore.core.platform;
    exports com.davils.kore.core.principal;
    exports com.davils.kore.core.random;
    exports com.davils.kore.core.size;
    exports com.davils.kore.core.text;
    exports com.davils.kore.core.text.diagnostic;
    exports com.davils.kore.core.text.encoding;
    exports com.davils.kore.core.uri;
    exports com.davils.kore.core.validation;
    exports com.davils.kore.core.validation.report;
    exports com.davils.kore.core.validation.violation;
    exports com.davils.kore.core.version;
    exports com.davils.kore.core.version.interval;
}
