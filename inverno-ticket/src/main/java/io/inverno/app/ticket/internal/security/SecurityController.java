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
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.web.server.annotation.WebController;
import io.inverno.mod.web.server.annotation.WebRoute;

/**
 * <p>
 * Exposes user's identity.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
@WebController( path = "/api/security" )
public class SecurityController {

	/**
	 * Get the identity of the authenticated user.
	 *
	 * @param securityContext the security context
	 *
	 * @return the user's identity
	 */
	@WebRoute( path = "/identity", method = Method.GET, produces = MediaTypes.APPLICATION_JSON )
	public Identity identity(SecurityContext<? extends Identity, ? extends AccessController> securityContext) {
		return securityContext.getIdentity().orElseThrow(() -> new NotFoundException("Identify not found"));
	}
}
