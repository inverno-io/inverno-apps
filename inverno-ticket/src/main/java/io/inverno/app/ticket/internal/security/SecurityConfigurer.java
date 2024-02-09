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
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ForbiddenException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.ldap.LDAPClient;
import io.inverno.mod.security.accesscontrol.GroupsRoleBasedAccessControllerResolver;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
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
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jws.JWSAuthentication;
import io.inverno.mod.security.jose.jws.JWSAuthenticator;
import io.inverno.mod.security.jose.jws.JWSService;
import io.inverno.mod.security.ldap.authentication.LDAPAuthentication;
import io.inverno.mod.security.ldap.authentication.LDAPAuthenticator;
import io.inverno.mod.security.ldap.identity.LDAPIdentity;
import io.inverno.mod.security.ldap.identity.LDAPIdentityResolver;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.ErrorWebRouterConfigurer;
import io.inverno.mod.web.server.WebInterceptable;
import io.inverno.mod.web.server.WebInterceptorsConfigurer;
import io.inverno.mod.web.server.WebRoutable;
import io.inverno.mod.web.server.WebRoutesConfigurer;
import io.inverno.mod.web.server.annotation.WebRoute;
import io.inverno.mod.web.server.annotation.WebRoutes;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * <p>
 * The Web configurer used to configure application security.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@WebRoutes({
	@WebRoute(path = { "/login" }, method = { Method.GET }),
	@WebRoute(path = { "/login" }, method = { Method.POST }),
	@WebRoute(path = { "/logout" }, method = { Method.GET }, produces = { "application/json" }),
})
@Bean( visibility = Bean.Visibility.PRIVATE )
public class SecurityConfigurer implements WebRoutesConfigurer<SecurityContext<LDAPIdentity, RoleBasedAccessController>>, WebInterceptorsConfigurer<InterceptingSecurityContext<LDAPIdentity, RoleBasedAccessController>>, ErrorWebRouterConfigurer<ExchangeContext> {

	/**
	 * The LDAP client.
	 */
	private final LDAPClient ldapClient;

	/**
	 * The JWS service
	 */
	private final JWSService jwsService;

	/**
	 * <p>
	 * Creates the security configurer.
	 * </p>
	 *
	 * @param ldapClient
	 * @param jwsService
	 */
	public SecurityConfigurer(LDAPClient ldapClient, JWSService jwsService) {
		this.ldapClient = ldapClient;
		this.jwsService = jwsService;
	}

	@Override
	public void configure(WebRoutable<SecurityContext<LDAPIdentity, RoleBasedAccessController>, ?> routes) {
		routes
			.route()
				.path("/login")
				.method(Method.GET)
				.handler(new FormLoginPageHandler<>())
			.route()
				.path("/login")
				.method(Method.POST)
				.handler(new LoginActionHandler<>(
					new FormCredentialsExtractor(),
					new LDAPAuthenticator(this.ldapClient, "dc=inverno,dc=io")
						.failOnDenied()
						.flatMap(authentication -> this.jwsService.builder(LDAPAuthentication.class)
							.header(header -> header
								.keyId("tkt")
								.algorithm(OCTAlgorithm.HS512.getAlgorithm())
							)
							.payload(authentication)
							.build(MediaTypes.APPLICATION_JSON)
							.map(JWSAuthentication::new)
						),
					LoginSuccessHandler.of(
						new CookieTokenLoginSuccessHandler<>(),
						new RedirectLoginSuccessHandler<>()
					),
					new RedirectLoginFailureHandler<>()
				))
			.route()
				.path("/logout")
				.produces(MediaTypes.APPLICATION_JSON)
				.handler(new LogoutActionHandler<>(
					authentication -> Mono.empty(),
					LogoutSuccessHandler.of(
						new CookieTokenLogoutSuccessHandler<>(),
						new RedirectLogoutSuccessHandler<>()
					)
				));
	}

	@Override
	public void configure(WebInterceptable<InterceptingSecurityContext<LDAPIdentity, RoleBasedAccessController>, ?> interceptors) {
		interceptors
			.intercept()
				.path("/")
				.path("/api/**")
				.path("/static/**")
				.path("/webjars/**")
				.path("/open-api/**")
				.path("/logout")
				.interceptors(List.of(
					SecurityInterceptor.of(
						new CookieTokenCredentialsExtractor(),
						new JWSAuthenticator<>(
							this.jwsService,
							LDAPAuthentication.class
						)
						.failOnDenied()
						.map(jwsAuthentication -> jwsAuthentication.getJws().getPayload()),
						new LDAPIdentityResolver(this.ldapClient),
						new GroupsRoleBasedAccessControllerResolver()
					),
					AccessControlInterceptor.authenticated()
				))
				.intercept()
					.path("/open-api/**")
					.interceptor(AccessControlInterceptor.verify(securityContext -> securityContext.getAccessController()
						.orElseThrow(() -> new ForbiddenException("Missing access controller"))
						.hasRole("developer")
					));
	}

	@Override
	public void configure(ErrorWebRouter<ExchangeContext> errorRouter) {
		errorRouter
			.intercept()
			.path("/")
			.error(UnauthorizedException.class)
			.interceptor(new FormAuthenticationErrorInterceptor<>())
			.applyInterceptors(); // We must apply interceptors to intercept white labels error routes which are already defined
	}
}
