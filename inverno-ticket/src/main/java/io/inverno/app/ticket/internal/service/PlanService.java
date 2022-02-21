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
package io.inverno.app.ticket.internal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.app.ticket.TicketApp;
import static io.inverno.app.ticket.internal.service.TicketService.REDIS_KEY_TICKET_STATUS;
import io.inverno.app.ticket.internal.exception.PlanAlreadyExistsException;
import io.inverno.app.ticket.internal.exception.TicketException;
import io.inverno.app.ticket.internal.exception.TicketNotFoundInPlanException;
import io.inverno.app.ticket.internal.model.Plan;
import io.inverno.app.ticket.internal.model.Ticket;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.redis.RedisTransactionalClient;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Plan service is used to create/read/update/delete {@link Plan} in Redis data store.
 * </p>
 * 
 * <p>
 * It also exposes methods to link tickets to plans.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean
public class PlanService {

	public static final String REDIS_KEY_PLAN = TicketApp.REDIS_KEY + ":Plan:%d";
	private static final String REDIS_KEY_PLAN_PATTERN = TicketApp.REDIS_KEY + ":Plan:*";
	private static final String REDIS_KEY_PLAN_REGEX = TicketApp.REDIS_KEY + ":Plan:[0-9]*";
	
	public static final String REDIS_KEY_PLAN_SEQ = TicketApp.REDIS_KEY + ":Plan:SEQ";
	
	public static final String REDIS_KEY_PLAN_TICKETS = TicketApp.REDIS_KEY + ":Plan:%d:Tickets";
	
	private final RedisTransactionalClient<String, String> redisClient;
	
	private final ObjectMapper mapper;
	
	private final TicketService ticketService;

	/**
	 * 
	 * @param redisClient
	 * @param mapper
	 * @param ticketService 
	 */
	public PlanService(RedisTransactionalClient<String, String> redisClient, ObjectMapper mapper, TicketService ticketService) {
		this.redisClient = redisClient;
		this.mapper = mapper;
		this.ticketService = ticketService;
	}
	
	/**
	 * 
	 * @param plan
	 * @return 
	 */
	public Mono<Plan> savePlan(Plan plan) {
		if(plan.getId() != null) {
			try {
				// Try to update
				return this.redisClient
					.set()
					.xx()
					.build(String.format(REDIS_KEY_PLAN, plan.getId()), this.mapper.writeValueAsString(plan))
					.map(result -> {
						if(!result.equals("OK")) {
							// should always be OK
							throw new IllegalStateException("Received unexpected result: " + result);
						}
						return plan;
					});
			} 
			catch (JsonProcessingException ex) {
				throw new UncheckedIOException(ex);
			}
		}
		else {
			// Get a new sequence then save
			return Mono.from(this.redisClient.connection(operations -> {
				return operations
					.incr(REDIS_KEY_PLAN_SEQ)
					.flatMap(planId -> {
						plan.setId(planId);
						plan.setCreationDateTime(ZonedDateTime.now(ZoneOffset.UTC));
						try {
							return operations
								.set()
								.nx()
								.build(String.format(REDIS_KEY_PLAN, planId), this.mapper.writeValueAsString(plan))
								.map(result -> {
									if(!result.equals("OK")) {
										// should always be OK
										throw new IllegalStateException("Received unexpected result: " + result);
									}
									return plan;
								})
								.switchIfEmpty(Mono.error(() -> new PlanAlreadyExistsException(planId)));
						} 
						catch (JsonProcessingException ex) {
							throw new UncheckedIOException(ex);
						}
					});
			}));
		}
	}
	
	/**
	 * 
	 * @param planId
	 * @param ticketId
	 * @return 
	 */
	public Mono<Void> addTicket(long planId, long ticketId) {
		String sTicketId = Long.toString(ticketId);
		return this.redisClient.multi(operations -> Flux.just(
			operations.lrem(String.format(REDIS_KEY_PLAN_TICKETS, planId), 0, sTicketId),
			operations.rpush(String.format(REDIS_KEY_PLAN_TICKETS, planId), sTicketId)
		))
		.flatMap(transactionResult -> {
			if(transactionResult.wasDiscarded()) {
				throw new TicketException("Error while removing plan: transaction was discarded");
			}
			return Mono.empty();
		});
	}

	/**
	 * 
	 * @param planId
	 * @param ticketId
	 * @param referenceTicketId
	 * @return 
	 */
	public Mono<Void> insertTicketBefore(long planId, long ticketId, long referenceTicketId) {
		String sTicketId = Long.toString(ticketId);
		String sReferenceTicketId = Long.toString(referenceTicketId);
		return this.redisClient.multi(operations -> Flux.just(
			operations.lrem(String.format(REDIS_KEY_PLAN_TICKETS, planId), 0, sTicketId),
			operations.linsert(String.format(REDIS_KEY_PLAN_TICKETS, planId), true, sReferenceTicketId, sTicketId)
				.doOnNext(result -> {
					if(result == -1) {
						throw new TicketNotFoundInPlanException(planId, referenceTicketId);
					}
				})
		))
		.flatMap(transactionResult -> {
			if(transactionResult.wasDiscarded()) {
				throw new TicketException("Error while inserting ticket " + ticketId + "into plan " + planId + ": transaction was discarded");
			}
			return Mono.empty();
		});
	}
	
	/**
	 * 
	 * @param planId
	 * @param ticketId
	 * @return 
	 */
	public Mono<Long> removeTicket(long planId, long ticketId) {
		String sTicketId = Long.toString(ticketId);
		return this.redisClient
			.lrem(String.format(REDIS_KEY_PLAN_TICKETS, planId), 0, sTicketId)
			.flatMap(count -> {
				if(count == 0) {
					return Mono.empty();
				}
				else {
					return Mono.just(ticketId);
				}
			});
	}
	
	/**
	 * 
	 * @return 
	 */
	public Flux<Plan> listPlans() {
		return Flux.from(this.redisClient.connection(operations -> operations
			.scan()
			.pattern(REDIS_KEY_PLAN_PATTERN)
			.build("0")
			.expand(result -> {
				if(result.isFinished()) {
					return Mono.empty();
				}
				return operations.scan()
					.pattern(REDIS_KEY_PLAN_PATTERN)
					.build(result.getCursor());
			})
			.flatMapIterable(result -> result.getKeys())
			.filter(key -> key.matches(REDIS_KEY_PLAN_REGEX))
			.collectList()
			.flatMapMany(planIds -> {
				if(planIds.isEmpty()) {
					return Flux.empty();
				}
				return operations.mget(keys -> planIds.forEach(keys::key));
			})
			.mapNotNull(opt -> opt
				.getValue()
				.map(result -> {
					try {
						Plan plan = this.mapper.readValue(result, Plan.class);
						plan.setTickets(this.redisClient
							.lrange(String.format(REDIS_KEY_PLAN_TICKETS, plan.getId()), 0, -1)
							.map(id -> Long.parseLong(id))
							.collectList()
							.flatMapMany(this.ticketService::getTickets)
						);
						return plan;
					} 
					catch (JsonProcessingException ex) {
						throw new UncheckedIOException(ex);
					}
				})
				.orElse(null)
			)
		));
	}
	
	/**
	 * 
	 * @param planId
	 * @return 
	 */
	public Mono<Plan> getPlan(long planId) {
		return this.getPlan(planId, List.of(Ticket.Status.OPEN, Ticket.Status.STUDIED, Ticket.Status.IN_PROGRESS, Ticket.Status.DONE, Ticket.Status.REJECTED));
	}
	
	/**
	 * 
	 * @param planId
	 * @param statuses
	 * @return 
	 */
	public Mono<Plan> getPlan(long planId, List<Ticket.Status> statuses) {
		return this.redisClient.get(String.format(REDIS_KEY_PLAN, planId))
			.map(result -> {
				try {
					Plan plan = this.mapper.readValue(result, Plan.class);
					plan.setTickets(this.getPlanTickets(planId, statuses));
					return plan;
				} 
				catch (JsonProcessingException ex) {
					throw new UncheckedIOException(ex);
				}
			});
	}
	
	/**
	 * 
	 * @param planId
	 * @param statuses
	 * @return 
	 */
	private Flux<Ticket> getPlanTickets(long planId, List<Ticket.Status> statuses) {
		if(statuses == null || statuses.isEmpty()) {
			return Flux.empty();
		}
		return Flux.from(this.redisClient.connection(operations -> operations
			.sunion(keys -> statuses.forEach(status -> keys.key(String.format(REDIS_KEY_TICKET_STATUS, status))))
			.collectList()
			.filter(ticketIds -> !ticketIds.isEmpty())
			.flatMapMany(ticketIds -> operations.lrange(String.format(REDIS_KEY_PLAN_TICKETS, planId), 0, -1)
				.filter(id -> ticketIds.contains(id))
				.map(id -> Long.parseLong(id))
				.collectList()
				.flatMapMany(this.ticketService::getTickets)
			)
		));
	}
	
	/**
	 * 
	 * @param planId
	 * @return 
	 */
	public Mono<Plan> removePlan(long planId) {
		String planKey = String.format(REDIS_KEY_PLAN, planId);
		return this.redisClient
			.exists(planKey)
			.flatMap(count -> {
				if(count == 0) {
					return Mono.empty();
				}
				return this.redisClient
					.multi(operations -> Flux.just(
						operations.del(String.format(REDIS_KEY_PLAN_TICKETS, planId)),
						operations.getdel(String.format(REDIS_KEY_PLAN, planId))
					))
					.map(transactionResult -> {
						if(transactionResult.wasDiscarded()) {
							throw new TicketException("Error while removing plan: transaction was discarded");
						}
						try {
							return this.mapper.readValue(transactionResult.<String>get(1), Plan.class);
						}
						catch (IOException ex) {
							throw new UncheckedIOException(ex);
						}
					});
			});
	}
}
