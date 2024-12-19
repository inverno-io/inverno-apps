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
import io.inverno.mod.http.server.HttpAccessLogsInterceptor;
import io.inverno.mod.web.server.ErrorWebRouteInterceptor;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.OpenApiRoutesConfigurer;
import io.inverno.mod.web.server.StaticHandler;
import io.inverno.mod.web.server.WebJarsRoutesConfigurer;
import io.inverno.mod.web.server.WebRouteInterceptor;
import io.inverno.mod.web.server.WebRouter;
import io.inverno.mod.web.server.WhiteLabelErrorRoutesConfigurer;

/**
 * <p>
 * Web server configurer used to configure access and error logging, error routes and static resources routes: OpenAPI specifications, WebJars, Web root.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean(visibility = Bean.Visibility.PRIVATE)
public class WebServerConfigurer implements WebRouteInterceptor.Configurer<ExchangeContext>, WebRouter.Configurer<ExchangeContext>, ErrorWebRouteInterceptor.Configurer<ExchangeContext>, ErrorWebRouter.Configurer<ExchangeContext> {

	private final TicketAppConfiguration configuration;

	private final ResourceService resourceService;

	private final Resource homeResource;

	public WebServerConfigurer(TicketAppConfiguration configuration, ResourceService resourceService) {
		this.configuration = configuration;
		this.resourceService = resourceService;
		this.homeResource = this.resourceService.getResource(this.configuration.web_root()).resolve("index.html");
	}

	@Override
	public WebRouteInterceptor<ExchangeContext> configure(WebRouteInterceptor<ExchangeContext> interceptors) {
		return interceptors
			.intercept()
				.interceptor(new HttpAccessLogsInterceptor<>());
	}

	@Override
	public void configure(WebRouter<ExchangeContext> routes) {
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


	@Override
	public ErrorWebRouteInterceptor<ExchangeContext> configure(ErrorWebRouteInterceptor<ExchangeContext> errorInterceptors) {
		return errorInterceptors
			.interceptError()
				.interceptor(new HttpAccessLogsInterceptor<>());
	}

	@Override
	public void configure(ErrorWebRouter<ExchangeContext> errorRoutes) {
		errorRoutes
			.configureErrorRoutes(new WhiteLabelErrorRoutesConfigurer<>());
	}
}
