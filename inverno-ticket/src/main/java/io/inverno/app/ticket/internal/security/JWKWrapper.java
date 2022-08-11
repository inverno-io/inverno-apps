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
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.InMemoryJWKStore;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwk.JWKStore;

import java.util.function.Supplier;

/**
 * <p>
 * The JWK used in the {@link SecurityController} to create and verify JWS tokens.
 * </p>
 *
 * <p>
 * A new key is generated each time the application is started.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Wrapper @Bean( name = "jwk")
public class JWKWrapper implements Supplier<JWK> {

	/**
	 * The JWK service.
	 */
	private final JWKService jwkService;

	/**
	 * <p>
	 * Creates the JWK wrapper.
	 * </p>
	 *
	 * @param jwkService
	 */
	public JWKWrapper(JWKService jwkService) {
		this.jwkService = jwkService;
	}

	@Override
	public JWK get() {
		return this.jwkService.oct().generator()
			.keyId("tkt")
			.algorithm(OCTAlgorithm.HS512.getAlgorithm())
			.generate()
			.map(JWK::trust)
			.flatMap(jwk -> jwkService.store().set(jwk).thenReturn(jwk))
			.block();
	}

	/**
	 * <p>
	 * The in-memory JWK store used to store the JWK so it can be easily resolved by id in the JOSE module.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 */
	@Wrapper @Bean
	public static class JWKStoreWrapper implements Supplier<JWKStore> {

		@Override
		public JWKStore get() {
			return new InMemoryJWKStore();
		}
	}
}
