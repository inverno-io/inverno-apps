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

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.source.BootstrapConfigurationSource;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.identity.PersonIdentity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

/**
 * <p>
 * Inverno's Ticket showcase application entry point.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class TicketApp {

	private static final Logger LOGGER = LogManager.getLogger(TicketApp.class);

	public static final String PROFILE_PROPERTY_NAME = "profile";

	public static final String REDIS_KEY = "APP:Ticket";
	
	@Bean( name = "configurationSource")
	public static interface TicketAppConfigurationSource extends Supplier<ConfigurationSource<?, ?, ?>> {}

	@Bean( name = "configurationParameters")
	public static interface TicketAppConfigurationParameters extends Supplier<List<ConfigurationKey.Parameter>> {}

	public static void main(String[] args) throws IOException {
		final BootstrapConfigurationSource bootstrapConfigurationSource = new BootstrapConfigurationSource(TicketApp.class.getModule(), args);
		Ticket ticketApp = bootstrapConfigurationSource
			.get(PROFILE_PROPERTY_NAME)
			.execute()
			.single()
			.map(configurationQueryResult -> configurationQueryResult.getResult().flatMap(ConfigurationProperty::asString).orElse("default"))
			.map(profile -> {
				LOGGER.info(() -> "Active profile: " + profile);
				return Application.run(new Ticket.Builder()
						.setConfigurationSource(bootstrapConfigurationSource)
						.setConfigurationParameters(List.of(ConfigurationKey.Parameter.of(PROFILE_PROPERTY_NAME, profile)))
				);
			})
			.block();

		ticketApp.userRepository().getUser("jsmith")
			.switchIfEmpty(Mono.defer(() -> ticketApp.userRepository().createUser(User.of("jsmith")
				.password(new RawPassword("password"))
				.identity(new PersonIdentity("jsmith", "John", "Smith", "jsmith@inverno.io"))
				.groups("developer")
				.build())
			))
			.flatMap(user -> {
				if(!user.getGroups().contains("developer")) {
					return ticketApp.userRepository().addUserToGroups("jsmith",  "developer");
				}
				return Mono.just(user);
			})
			.block();

		ticketApp.userRepository().getUser("admin")
			.switchIfEmpty(Mono.defer(() -> ticketApp.userRepository().createUser(User.<PersonIdentity>of("admin")
					.password(new RawPassword("password"))
					.groups("admin")
					.build())
			))
			.flatMap(user -> {
				if(!user.getGroups().contains("admin")) {
					return ticketApp.userRepository().addUserToGroups("admin",  "admin");
				}
				return Mono.just(user);
			})
			.block();
	}
}
