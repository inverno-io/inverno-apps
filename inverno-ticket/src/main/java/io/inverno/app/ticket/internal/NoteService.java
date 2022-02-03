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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.app.ticket.internal.model.Note;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.redis.RedisTransactionalClient;
import java.io.UncheckedIOException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Ticket note service is used to create/read/update/delete Ticket {@link Note} in Redis data store.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean
public class NoteService {
	
	public static final String REDIS_KEY_TICKET_NOTES = TicketService.REDIS_KEY_TICKET + ":Notes";

	private final RedisTransactionalClient<String, String> redisClient;
		
	private final ObjectMapper mapper;
	
	/**
	 * 
	 * @param redisClient
	 * @param mapper 
	 */
	public NoteService(RedisTransactionalClient<String, String> redisClient, ObjectMapper mapper) {
		this.redisClient = redisClient;
		this.mapper = mapper;
	}
	
	/**
	 * 
	 * @param note
	 * @return 
	 */
	public Mono<Note> saveTicketNote(Note note) {
		if(note.getIndex() != null) {
			try {
				return this.redisClient
					.lset(String.format(REDIS_KEY_TICKET_NOTES, note.getTicketId()), note.getIndex(), this.mapper.writeValueAsString(note))
					.onErrorResume(t -> Mono.empty())
					.map(reply -> note);
			} 
			catch (JsonProcessingException ex) {
				throw new UncheckedIOException(ex);
			}
		}
		else {
			try {
				return this.redisClient
					.rpush(String.format(REDIS_KEY_TICKET_NOTES, note.getTicketId()), this.mapper.writeValueAsString(note))
					.map(length -> {
						note.setIndex(length.intValue() - 1);
						return note;
					});
			}
			catch (JsonProcessingException ex) {
				throw new UncheckedIOException(ex);
			}
		}
	}
	
	/**
	 * 
	 * @param ticketId
	 * @return 
	 */
	public Flux<Note> listTicketNotes(long ticketId) {
		return this.redisClient
			.lrange(String.format(REDIS_KEY_TICKET_NOTES, ticketId), 0, -1)
			.index()
			.map(tuple -> {
				try {
					Note note = this.mapper.readValue(tuple.getT2(), Note.class);
					note.setIndex(tuple.getT1().intValue());
					return note;
				} 
				catch (JsonProcessingException ex) {
					throw new UncheckedIOException(ex);
				}
			});
	}
	
	/**
	 * 
	 * @param ticketId
	 * @param noteIndex
	 * @return 
	 */
	public Mono<Note> getTicketNote(long ticketId, int noteIndex) {
		return this.redisClient
			.lindex(String.format(REDIS_KEY_TICKET_NOTES, ticketId), noteIndex)
			.map(result -> {
				try {
					Note note = this.mapper.readValue(result, Note.class);
					note.setIndex(noteIndex);
					return note;
				} 
				catch (JsonProcessingException ex) {
					throw new UncheckedIOException(ex);
				}
			});
	}
	
	/**
	 * 
	 * @param ticketId
	 * @param noteIndex
	 * @return 
	 */
	public Mono<Note> removeTicketNote(long ticketId, int noteIndex) {
		String ticketNotesKey = String.format(REDIS_KEY_TICKET_NOTES, ticketId);
		return Mono.from(this.redisClient.connection(operations -> {
			return operations.lindex(ticketNotesKey, noteIndex)
				.flatMap(result -> {
					try {
						Note note = this.mapper.readValue(result, Note.class);
						return operations.lrem(ticketNotesKey, 0, result).thenReturn(note);
					} 
					catch (JsonProcessingException ex) {
						throw new UncheckedIOException(ex);
					}
				});
		}));
	}
}
