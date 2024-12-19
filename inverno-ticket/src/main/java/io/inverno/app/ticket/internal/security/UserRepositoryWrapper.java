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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.redis.RedisClient;
import io.inverno.mod.security.authentication.user.RedisUserRepository;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserRepository;
import io.inverno.mod.security.identity.PersonIdentity;
import java.util.function.Supplier;

/**
 * <p>
 * The user repository managing application users and used to authenticate users.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Wrapper @Bean( name = "userRepository" )
public class UserRepositoryWrapper implements Supplier<UserRepository<PersonIdentity, User<PersonIdentity>>> {

	private final RedisClient<String, String> redisClient;
	private final ObjectMapper mapper;

	public UserRepositoryWrapper(RedisClient<String, String> redisClient, ObjectMapper mapper) {
		this.redisClient = redisClient;
		this.mapper = mapper;
	}

	@Override
	public UserRepository<PersonIdentity, User<PersonIdentity>> get() {
		return new RedisUserRepository<>(this.redisClient, this.mapper);
	}
}
