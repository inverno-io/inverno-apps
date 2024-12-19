/*
 * Copyright 2022 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * <p>
 * Inverno's Ticket showcase application module.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 * @version 1.0.0
 */
@io.inverno.core.annotation.Module
module io.inverno.app.ticket {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.redis.lettuce;
    requires io.inverno.mod.web.server;
	requires io.inverno.mod.security.http;
	requires io.inverno.mod.security.jose;

    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.layout.template.json;

    exports io.inverno.app.ticket.internal.model to com.fasterxml.jackson.databind;
    exports io.inverno.app.ticket.internal.rest.v1.dto to com.fasterxml.jackson.databind;
}
