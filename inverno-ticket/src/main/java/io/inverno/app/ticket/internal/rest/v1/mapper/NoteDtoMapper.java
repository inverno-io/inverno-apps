/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.app.ticket.internal.rest.v1.mapper;

import io.inverno.app.ticket.internal.model.Note;
import io.inverno.app.ticket.internal.rest.DtoMapper;
import io.inverno.app.ticket.internal.rest.v1.dto.NoteDto;
import io.inverno.core.annotation.Bean;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
@Bean
public class NoteDtoMapper implements DtoMapper<NoteDto, Note> {

	@Override
	public Mono<NoteDto> toDto(Note domain) {
		return Mono.fromSupplier(() -> {
			NoteDto dto = new NoteDto();
		
			dto.setTicketId(domain.getTicketId());
			dto.setIndex(domain.getIndex());
			dto.setTitle(domain.getTitle());
			dto.setContent(domain.getContent());

			return dto;
		});
	}

	@Override
	public Mono<Note> toDomain(NoteDto dto) {
		return Mono.fromSupplier(() -> {
			Note domain = new Note();

			domain.setTicketId(dto.getTicketId());
			domain.setIndex(dto.getIndex());
			domain.setTitle(dto.getTitle());
			domain.setContent(dto.getContent());

			return domain;
		});
	}
	
}
