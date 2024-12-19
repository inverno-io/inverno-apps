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
package io.inverno.app.ticket.internal.security;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Init;
import io.inverno.mod.base.reflect.Types;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.configuration.DefaultingStrategy;
import io.inverno.mod.configuration.source.RedisConfigurationSource;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ForbiddenException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.redis.RedisClient;
import io.inverno.mod.security.accesscontrol.ConfigurationSourcePermissionBasedAccessControllerResolver;
import io.inverno.mod.security.accesscontrol.PermissionBasedAccessController;
import io.inverno.mod.security.authentication.LoginCredentialsMatcher;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserAuthentication;
import io.inverno.mod.security.authentication.user.UserAuthenticator;
import io.inverno.mod.security.authentication.user.UserRepository;
import io.inverno.mod.security.http.AccessControlInterceptor;
import io.inverno.mod.security.http.SecurityInterceptor;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.http.form.FormAuthenticationErrorInterceptor;
import io.inverno.mod.security.http.form.FormCredentialsExtractor;
import io.inverno.mod.security.http.form.FormLoginPageHandler;
import io.inverno.mod.security.http.form.RedirectLoginFailureHandler;
import io.inverno.mod.security.http.form.RedirectLoginSuccessHandler;
import io.inverno.mod.security.http.form.RedirectLogoutSuccessHandler;
import io.inverno.mod.security.http.login.LoginActionHandler;
import io.inverno.mod.security.http.login.LoginSuccessHandler;
import io.inverno.mod.security.http.login.LogoutActionHandler;
import io.inverno.mod.security.http.login.LogoutSuccessHandler;
import io.inverno.mod.security.http.token.CookieTokenCredentialsExtractor;
import io.inverno.mod.security.http.token.CookieTokenLoginSuccessHandler;
import io.inverno.mod.security.http.token.CookieTokenLogoutSuccessHandler;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.security.identity.UserIdentityResolver;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jws.JWSAuthentication;
import io.inverno.mod.security.jose.jws.JWSAuthenticator;
import io.inverno.mod.security.jose.jws.JWSService;
import io.inverno.mod.web.server.ErrorWebRouteInterceptor;
import io.inverno.mod.web.server.WebRouteInterceptor;
import io.inverno.mod.web.server.WebRouter;
import io.inverno.mod.web.server.annotation.WebRoute;
import io.inverno.mod.web.server.annotation.WebRoutes;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Web server configurer used to configure application security.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
@WebRoutes({
	@WebRoute(path = { "/login" }, method = { Method.GET }),
	@WebRoute(path = { "/login" }, method = { Method.POST }),
	@WebRoute(path = { "/logout" }, method = { Method.GET }, produces = { "application/json" })
})
public class SecurityConfigurer implements WebRouteInterceptor.Configurer<InterceptingSecurityContext<PersonIdentity, PermissionBasedAccessController>>, WebRouter.Configurer<SecurityContext<PersonIdentity, PermissionBasedAccessController>>, ErrorWebRouteInterceptor.Configurer<ExchangeContext> {

	private final UserRepository<PersonIdentity, User<PersonIdentity>> userRepository;
	private final JWSService jwsService;
	private final RedisConfigurationSource permissionsSource;

	public SecurityConfigurer(UserRepository<PersonIdentity, User<PersonIdentity>> userRepository, JWSService jwsService, RedisClient<String, String> redisClient) {
		this.userRepository = userRepository;
		this.jwsService = jwsService;
		this.permissionsSource = new RedisConfigurationSource(redisClient).withDefaultingStrategy(DefaultingStrategy.wildcard());
		this.permissionsSource.setKeyPrefix("SEC");
	}

	@Init
	public void init() {
		this.permissionsSource.set("jsmith", "remove")
			.and()
			.set("jsmith", "*").withParameters("plan", "1")
			.execute()
			.blockLast();
	}

	@Override
	public WebRouteInterceptor<InterceptingSecurityContext<PersonIdentity, PermissionBasedAccessController>> configure(WebRouteInterceptor<InterceptingSecurityContext<PersonIdentity, PermissionBasedAccessController>> interceptors) {
		return interceptors
			.intercept()                                                                                  // 1
				.path("/")
				.path("/api/**")
				.path("/static/**")
				.path("/webjars/**")
				.path("/open-api/**")
				.path("/logout")
				.interceptors(List.of(
					SecurityInterceptor.of(                                                               // 2
						new CookieTokenCredentialsExtractor(),                                            // 3
						new JWSAuthenticator<UserAuthentication<PersonIdentity>>(                         // 4
							this.jwsService,
							Types.type(UserAuthentication.class).type(PersonIdentity.class).and().build()
						)
						.failOnDenied()                                                                   // 5
						.map(jwsAuthentication -> jwsAuthentication.getJws().getPayload()),               // 6
						new UserIdentityResolver<>(),
						new ConfigurationSourcePermissionBasedAccessControllerResolver(this.permissionsSource)
					),
					AccessControlInterceptor.authenticated()                                              // 7
				))
				.intercept()
					.path("/open-api/**")
					.interceptor(AccessControlInterceptor.verify(securityContext -> securityContext.getAccessController()
						.orElseThrow(() -> new ForbiddenException("Missing access controller"))
						.hasPermission("access-api")
					));
	}

	@Override
	public void configure(WebRouter<SecurityContext<PersonIdentity, PermissionBasedAccessController>> routes) {
		routes
			.route()                                                                                                                     // 1
				.path("/login")
				.method(Method.GET)
				.handler(new FormLoginPageHandler<>())
			.route()                                                                                                                     // 2
				.path("/login")
				.method(Method.POST)
				.handler(new LoginActionHandler<>(                                                                                       // 3
					new FormCredentialsExtractor(),                                                                                      // 4
					new UserAuthenticator<>(this.userRepository, new LoginCredentialsMatcher<>())                                        // 5
						.failOnDenied()                                                                                                  // 6
						.flatMap(authentication -> this.jwsService.<UserAuthentication<PersonIdentity>>builder(UserAuthentication.class) // 7
							.header(header -> header
								.keyId("tkt")
								.algorithm(OCTAlgorithm.HS512.getAlgorithm())
							)
							.payload(authentication)
							.build(MediaTypes.APPLICATION_JSON)
							.map(JWSAuthentication::new)
						),
					LoginSuccessHandler.of(                                                                                              // 8
						new CookieTokenLoginSuccessHandler<>(),
						new RedirectLoginSuccessHandler<>()
					),
					new RedirectLoginFailureHandler<>()                                                                                  // 9
				))
			.route()
				.path("/logout")
				.produce(MediaTypes.APPLICATION_JSON)
				.handler(new LogoutActionHandler<>(
					authentication -> Mono.empty(),
					LogoutSuccessHandler.of(
						new CookieTokenLogoutSuccessHandler<>(),
						new RedirectLogoutSuccessHandler<>()
					)
				));
	}

	@Override
	public ErrorWebRouteInterceptor<ExchangeContext> configure(ErrorWebRouteInterceptor<ExchangeContext> errorInterceptors) {
		return errorInterceptors
			.interceptError()
				.path("/")
				.error(UnauthorizedException.class)
				.interceptor(new FormAuthenticationErrorInterceptor<>());
	}
}
