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
package io.inverno.app.ticket.internal.rest.v1;

import io.inverno.app.ticket.internal.NoteService;
import io.inverno.app.ticket.internal.TicketService;
import io.inverno.app.ticket.internal.model.Note;
import io.inverno.app.ticket.internal.model.Ticket;
import io.inverno.app.ticket.internal.rest.DtoMapper;
import io.inverno.app.ticket.internal.rest.v1.dto.NoteDto;
import io.inverno.app.ticket.internal.rest.v1.dto.TicketDto;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.annotation.Body;
import io.inverno.mod.web.annotation.PathParam;
import io.inverno.mod.web.annotation.QueryParam;
import io.inverno.mod.web.annotation.WebController;
import io.inverno.mod.web.annotation.WebRoute;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Ticket related REST endpoints.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
@WebController( path = "/api/v1/ticket" )
public class TicketWebController {

	private final TicketService ticketService;
	private final NoteService noteService;
	
	private final DtoMapper<TicketDto, Ticket> ticketDtoMapper;
	private final DtoMapper<NoteDto, Note> noteDtoMapper;
	
	/**
	 * 
	 * @param ticketService
	 * @param noteService
	 * @param ticketDtoMapper
	 * @param noteDtoMapper 
	 */
	public TicketWebController(TicketService ticketService, NoteService noteService, DtoMapper<TicketDto, Ticket> ticketDtoMapper, DtoMapper<NoteDto, Note> noteDtoMapper) {
		this.ticketService = ticketService;
		this.noteService = noteService;
		this.ticketDtoMapper = ticketDtoMapper;
		this.noteDtoMapper = noteDtoMapper;
	}
	
	/**
	 * 
	 * @param ticket
	 * @param exchange
	 * @return 
	 */
	@WebRoute( method = Method.POST, consumes = MediaTypes.APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON )
	public Mono<TicketDto> createTicket(@Body TicketDto ticket, WebExchange<?> exchange) {
		ticket.setId(null);
		return this.ticketDtoMapper.toDomain(ticket)
			.flatMap(this.ticketService::saveTicket)
			.doOnNext(savedTicket -> {
				exchange.response().headers(headers -> headers
					.status(Status.CREATED)
					.add(Headers.NAME_LOCATION, exchange.request().getPathBuilder().segment(savedTicket.getId().toString()).buildPath())
				);
			})
			.flatMap(this.ticketDtoMapper::toDto);
	}
	
	/**
	 * 
	 * @param statuses
	 * @return 
	 */
	@WebRoute( method = Method.GET, produces = MediaTypes.APPLICATION_JSON )
	public Flux<TicketDto> listTickets(@QueryParam Optional<List<Ticket.Status>> statuses) {
		return statuses.map(this.ticketService::listTickets).orElse(this.ticketService.listTickets()).flatMap(this.ticketDtoMapper::toDto);
	}
	
	/**
	 * 
	 * @param ticketId
	 * @return 
	 */
	@WebRoute( path = { "/{ticketId}" }, method = Method.GET, produces = MediaTypes.APPLICATION_JSON )
	public Mono<TicketDto> getTicket(@PathParam long ticketId) {
		return this.ticketService.getTicket(ticketId).flatMap(this.ticketDtoMapper::toDto).switchIfEmpty(Mono.error(() -> new NotFoundException()));
	}
	
	/**
	 * 
	 * @param ticketId
	 * @param ticket
	 * @return 
	 */
	@WebRoute( path = { "/{ticketId}" }, method = Method.PUT, consumes = MediaTypes.APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON )
	public Mono<TicketDto> updateTicket(@PathParam long ticketId, @Body TicketDto ticket) {
		ticket.setId(ticketId);
		return this.ticketDtoMapper.toDomain(ticket).flatMap(this.ticketService::saveTicket).flatMap(this.ticketDtoMapper::toDto);
	}
	
	/**
	 * 
	 * @param ticketId
	 * @param status
	 * @return 
	 */
	@WebRoute( path = { "/{ticketId}/status" }, method = Method.POST, consumes = MediaTypes.TEXT_PLAIN, produces = MediaTypes.APPLICATION_JSON)
	public Mono<TicketDto> updateTicketStatus(@PathParam long ticketId, @Body Ticket.Status status) {
		return this.ticketService.updateTicketStatus(ticketId, status).flatMap(this.ticketDtoMapper::toDto);
	}
	
	/**
	 * 
	 * @param ticketId
	 * @return 
	 */
	@WebRoute( path = { "/{ticketId}" }, method = Method.DELETE, produces = MediaTypes.APPLICATION_JSON )
	public Mono<TicketDto> deleteTicket(@PathParam long ticketId) {
		return this.ticketService.removeTicket(ticketId).flatMap(this.ticketDtoMapper::toDto);
	}
	
	/**
	 * 
	 * @param ticketId
	 * @param note
	 * @param exchange
	 * @return 
	 */
	@WebRoute( path = { "/{ticketId}/note" }, method = Method.POST, consumes = MediaTypes.APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON )
	public Mono<NoteDto> createTicketNote(@PathParam long ticketId, @Body NoteDto note, WebExchange<?> exchange) {
		note.setTicketId(ticketId);
		return this.noteDtoMapper.toDomain(note)
			.flatMap(this.noteService::saveTicketNote)
			.doOnNext(savedNote -> {
				exchange.response().headers(headers -> headers
					.status(Status.CREATED)
					.add(Headers.NAME_LOCATION, exchange.request().getPathBuilder().segment(savedNote.getIndex().toString()).buildPath())
				);
			})
			.flatMap(this.noteDtoMapper::toDto);
	}
	
	/**
	 * 
	 * @param ticketId
	 * @return 
	 */
	@WebRoute( path = { "/{ticketId}/note" }, method = Method.GET, produces = MediaTypes.APPLICATION_JSON )
	public Flux<NoteDto> listTicketNotes(@PathParam long ticketId) {
		return this.noteService.listTicketNotes(ticketId).flatMap(this.noteDtoMapper::toDto);
	}
	
	/**
	 * 
	 * @param ticketId
	 * @param noteIndex
	 * @return 
	 */
	@WebRoute( path = { "/{ticketId}/note/{noteIndex}" }, method = Method.GET, produces = MediaTypes.APPLICATION_JSON )
	public Mono<NoteDto> getTicketNote(@PathParam long ticketId, @PathParam int noteIndex) {
		return this.noteService.getTicketNote(ticketId, noteIndex).flatMap(this.noteDtoMapper::toDto).switchIfEmpty(Mono.error(() -> new NotFoundException()));
	}
	
	/**
	 * 
	 * @param ticketId
	 * @param noteIndex
	 * @param note
	 * @return 
	 */
	@WebRoute( path = { "/{ticketId}/note/{noteIndex}" }, method = Method.PUT, consumes = MediaTypes.APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON )
	public Mono<NoteDto> updateTicketNote(@PathParam long ticketId, @PathParam int noteIndex, @Body NoteDto note) {
		note.setTicketId(ticketId);
		note.setIndex(noteIndex);
		return this.noteDtoMapper.toDomain(note).flatMap(this.noteService::saveTicketNote).flatMap(this.noteDtoMapper::toDto);
	}
	
	/**
	 * 
	 * @param ticketId
	 * @param noteIndex
	 * @return 
	 */
	@WebRoute( path = { "/{ticketId}/note/{noteIndex}" }, method = Method.DELETE, produces = MediaTypes.APPLICATION_JSON )
	public Mono<NoteDto> deleteTicketNote(@PathParam long ticketId, @PathParam int noteIndex) {
		return this.noteService.removeTicketNote(ticketId, noteIndex).flatMap(this.noteDtoMapper::toDto);
	}
}
