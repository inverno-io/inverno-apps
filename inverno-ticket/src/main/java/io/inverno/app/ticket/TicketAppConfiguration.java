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
package io.inverno.app.ticket;

import io.inverno.core.annotation.NestedBean;
import io.inverno.mod.boot.BootConfiguration;
import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.redis.lettuce.LettuceRedisClientConfiguration;
import io.inverno.mod.web.WebConfiguration;
import java.net.URI;

/**
 * <p>
 * Inverno's Ticket showcase application configuration.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Configuration
public interface TicketAppConfiguration {
	
	/**
	 * <p>
	 * The boot module configuration.
	 * </p>
	 * 
	 * @return the boot module configuration
	 */
	@NestedBean
	BootConfiguration boot();
	
	/**
	 * <p>
	 * The web module configuration.
	 * </p>
	 * 
	 * @return the web module configuration
	 */
	@NestedBean
	WebConfiguration web();
	
	/**
	 * <p>
	 * The Redis Lettuce module configuration.
	 * </p>
	 * 
	 * @return the Redis Lettuce module configuration
	 */
	@NestedBean
	LettuceRedisClientConfiguration redis();
	
	/**
	 * <p>
	 * The Web root location where to look for static resources.
	 * </p>
	 * 
	 * @return the Web root location
	 */
	default URI web_root() {
		return URI.create("module://" + TicketAppConfiguration.class.getModule().getName() + "/static");
	}
}
