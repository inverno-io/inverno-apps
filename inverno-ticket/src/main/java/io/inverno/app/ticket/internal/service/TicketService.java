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
import io.inverno.app.ticket.internal.exception.TicketException;
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
 * Ticket service is used to create/read/update/delete {@link Ticket} in Redis data store.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean
public class TicketService {

	public static final String REDIS_KEY_TICKET = TicketApp.REDIS_KEY + ":Ticket:%d";
	
	public static final String REDIS_KEY_TICKET_SEQ = TicketApp.REDIS_KEY + ":Ticket:SEQ";
	
	public static final String REDIS_KEY_TICKET_STATUS = TicketApp.REDIS_KEY + ":Ticket:%s";
	public static final String REDIS_KEY_TICKET_OPEN = String.format(REDIS_KEY_TICKET_STATUS, Ticket.Status.OPEN);
	public static final String REDIS_KEY_TICKET_STUDIED = String.format(REDIS_KEY_TICKET_STATUS, Ticket.Status.STUDIED);
	public static final String REDIS_KEY_TICKET_IN_PROGRESS = String.format(REDIS_KEY_TICKET_STATUS, Ticket.Status.IN_PROGRESS);
	public static final String REDIS_KEY_TICKET_DONE = String.format(REDIS_KEY_TICKET_STATUS, Ticket.Status.DONE);
	public static final String REDIS_KEY_TICKET_REJECTED = String.format(REDIS_KEY_TICKET_STATUS, Ticket.Status.REJECTED);
	
	private final RedisTransactionalClient<String, String> redisClient;
	
	private final ObjectMapper mapper;
	
	/**
	 * 
	 * @param redisClient
	 * @param mapper 
	 */
	public TicketService(RedisTransactionalClient<String, String> redisClient, ObjectMapper mapper) {
		this.redisClient = redisClient;
		this.mapper = mapper;
	}
	
	/**
	 * 
	 * @param ticket
	 * @return 
	 */
	public Mono<Ticket> saveTicket(Ticket ticket) {
		if(ticket.getId() != null) {
			// Try to update
			return Mono.from(this.redisClient.connection(operations -> {
				try {
					return operations
						.setGet()
						.xx()
						.build(String.format(REDIS_KEY_TICKET, ticket.getId()), this.mapper.writeValueAsString(ticket))
						.flatMap(result -> {
							try {
								Ticket oldTicket = this.mapper.readValue(result, Ticket.class);
								if(!oldTicket.getStatus().equals(ticket.getStatus())) {
									return operations.smove(String.format(REDIS_KEY_TICKET_STATUS, oldTicket.getStatus()), String.format(REDIS_KEY_TICKET_STATUS, ticket.getStatus()), Long.toString(ticket.getId())).thenReturn(ticket);
								}
								else {
									return Mono.just(ticket);
								}
							}
							catch (JsonProcessingException ex) {
								throw new UncheckedIOException(ex);
							}
						});
				} 
				catch (JsonProcessingException ex) {
					throw new UncheckedIOException(ex);
				}
			}));
		}
		else {
			return this.redisClient
				.incr(REDIS_KEY_TICKET_SEQ)
				.flatMap(ticketId -> {
					ticket.setCreationDateTime(ZonedDateTime.now(ZoneOffset.UTC));
					ticket.setStatus(Ticket.Status.OPEN);
					ticket.setId(ticketId);
					return this.redisClient.multi(operations -> {
						try {
							return Flux.just(
									operations
										.set()
										.nx()
										.build(String.format(REDIS_KEY_TICKET, ticketId), this.mapper.writeValueAsString(ticket)),
									operations
										.sadd(REDIS_KEY_TICKET_OPEN, Long.toString(ticketId))
							);
						}
						catch (JsonProcessingException ex) {
							throw new UncheckedIOException(ex);
						}
					});
				})
				.map(transactionResult -> {
					if(transactionResult.wasDiscarded()) {
						throw new TicketException("Error while creating ticket: transaction was discarded");
					}
					return ticket;
				});
		}
	}
	
	/**
	 * 
	 * @param ticketId
	 * @param status
	 * @return 
	 */
	public Mono<Ticket> updateTicketStatus(long ticketId, Ticket.Status status) {
		return Mono.from(this.redisClient.connection(operations -> operations
			.get(String.format(REDIS_KEY_TICKET, ticketId))
			.flatMap(result -> {
				try {
					Ticket ticket = this.mapper.readValue(result, Ticket.class);
					Ticket.Status oldStatus = ticket.getStatus();
					if(status.equals(oldStatus)) {
						return Mono.empty();
					}
					ticket.setStatus(status);
					return Mono.when(
						operations.set().xx().build(String.format(REDIS_KEY_TICKET, ticket.getId()), this.mapper.writeValueAsString(ticket)),
						operations.smove(String.format(REDIS_KEY_TICKET_STATUS, oldStatus), String.format(REDIS_KEY_TICKET_STATUS, status), Long.toString(ticket.getId()))
					).thenReturn(ticket);
				} 
				catch (JsonProcessingException ex) {
					throw new UncheckedIOException(ex);
				}
			})
		));
	}
	
	/**
	 * 
	 * @return 
	 */
	public Flux<Ticket> listTickets() {
		return this.listTickets(List.of(Ticket.Status.OPEN, Ticket.Status.STUDIED, Ticket.Status.IN_PROGRESS, Ticket.Status.DONE, Ticket.Status.REJECTED));
	}
	
	/**
	 * 
	 * @param statuses
	 * @return 
	 */
	public Flux<Ticket> listTickets(List<Ticket.Status> statuses) {
		if(statuses == null || statuses.isEmpty()) {
			return Flux.empty();
		}
		return Flux.from(this.redisClient.connection(operations -> operations
			.sunion(keys -> statuses.forEach(status -> keys.key(String.format(REDIS_KEY_TICKET_STATUS, status))))
			.collectList()
			.filter(ticketIds -> !ticketIds.isEmpty())
			.flatMapMany(ticketIds -> operations.mget(keys -> ticketIds.forEach(ticketId -> keys.key(String.format(REDIS_KEY_TICKET, Long.parseLong(ticketId))))))
			.mapNotNull(opt -> opt
				.getValue()
				.map(result -> {
					try {
						return this.mapper.readValue(result, Ticket.class);
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
	 * @param ticketId
	 * @return 
	 */
	public Mono<Ticket> getTicket(long ticketId) {
		return this.redisClient
			.get(String.format(REDIS_KEY_TICKET, ticketId))
			.map(result -> {
				try {
					return this.mapper.readValue(result, Ticket.class);
				} 
				catch (JsonProcessingException ex) {
					throw new UncheckedIOException(ex);
				}
			});
	}
	
	/**
	 * 
	 * @param ticketIds
	 * @return 
	 */
	public Flux<Ticket> getTickets(List<Long> ticketIds) {
		if(ticketIds == null || ticketIds.isEmpty()) {
			return Flux.empty();
		}
		return this.redisClient
			.mget(keys -> ticketIds.forEach(ticketId -> keys.key(String.format(REDIS_KEY_TICKET, ticketId))))
			.mapNotNull(opt -> opt
				.getValue()
				.map(result -> {
					try {
						return this.mapper.readValue(result, Ticket.class);
					} 
					catch (JsonProcessingException ex) {
						throw new UncheckedIOException(ex);
					}
				})
				.orElse(null)
			);
	}
	
	/**
	 * 
	 * @param ticketId
	 * @return 
	 */
	public Mono<Ticket> removeTicket(long ticketId) {
		// TODO tickets are not removed from plan's ticket set
		String ticketKey = String.format(REDIS_KEY_TICKET, ticketId);
		return this.redisClient
			.exists(ticketKey)
			.flatMap(count -> {
				if(count == 0) {
					return Mono.empty();
				}
				String sTicketId = Long.toString(ticketId);
				return this.redisClient
					.multi(operations -> Flux.just(
						operations.srem(String.format(REDIS_KEY_TICKET_OPEN, ticketId), sTicketId),
						operations.srem(String.format(REDIS_KEY_TICKET_STUDIED, ticketId), sTicketId),
						operations.srem(String.format(REDIS_KEY_TICKET_IN_PROGRESS, ticketId), sTicketId),
						operations.srem(String.format(REDIS_KEY_TICKET_DONE, ticketId), sTicketId),
						operations.srem(String.format(REDIS_KEY_TICKET_REJECTED, ticketId), sTicketId),
						operations.del(String.format(NoteService.REDIS_KEY_TICKET_NOTES, ticketId)),
						operations.getdel(String.format(REDIS_KEY_TICKET, ticketId))
					))
					.map(transactionResult -> {
						if(transactionResult.wasDiscarded()) {
							throw new TicketException("Error while removing ticket: transaction was discarded");
						}
						try {
							return this.mapper.readValue(transactionResult.<String>get(6), Ticket.class);
						}
						catch (IOException ex) {
							throw new UncheckedIOException(ex);
						}
					});
			});
	}
}
