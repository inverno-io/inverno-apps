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
package io.inverno.app.ticket.internal;

import io.inverno.app.ticket.TicketAppConfiguration;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.OpenApiRoutesConfigurer;
import io.inverno.mod.web.server.StaticHandler;
import io.inverno.mod.web.server.WebJarsRoutesConfigurer;
import io.inverno.mod.web.server.WebRoutable;
import io.inverno.mod.web.server.WebRoutesConfigurer;

/**
 * <p>
 * Web routes configurer used to configure routes to static resources: OpenAPI generated specifications, WebJars, Web root.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean(visibility = Bean.Visibility.PRIVATE)
public class StaticWebRoutesConfigurer implements WebRoutesConfigurer<ExchangeContext> {

	private final TicketAppConfiguration configuration;
	
	private final ResourceService resourceService;
	
	private final Resource homeResource;
	
	/**
	 * 
	 * @param configuration
	 * @param resourceService 
	 */
	public StaticWebRoutesConfigurer(TicketAppConfiguration configuration, ResourceService resourceService) {
		this.configuration = configuration;
		this.resourceService = resourceService;
		this.homeResource = this.resourceService.getResource(this.configuration.web_root()).resolve("index.html");
	}
	
	/**
	 * 
	 * @param routes
	 */
	@Override
	public void configure(WebRoutable<ExchangeContext, ?> routes) {
		routes
			// OpenAPI specifications
			.configureRoutes(new OpenApiRoutesConfigurer<>(this.resourceService, true))
			// WebJars
			.configureRoutes(new WebJarsRoutesConfigurer<>(this.resourceService))
			// Static resources: html, javascript, css, images...
			.route()
				.path("/static/{path:.*}", true)
				.method(Method.GET)
				.handler(new StaticHandler<>(this.resourceService.getResource(this.configuration.web_root())))
			// Welcome page
			.route()
				.path("/", true)
				.method(Method.GET)
				.handler(exchange -> exchange.response().body().resource().value(this.homeResource));

	}
}
