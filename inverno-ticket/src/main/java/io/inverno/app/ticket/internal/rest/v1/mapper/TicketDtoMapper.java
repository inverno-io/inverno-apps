/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.app.ticket.internal.rest.v1.mapper;

import io.inverno.app.ticket.internal.model.Ticket;
import io.inverno.app.ticket.internal.rest.DtoMapper;
import io.inverno.app.ticket.internal.rest.v1.dto.TicketDto;
import io.inverno.core.annotation.Bean;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class TicketDtoMapper implements DtoMapper<TicketDto, Ticket> {

	@Override
	public Mono<TicketDto> toDto(Ticket domain) {
		return Mono.fromSupplier(() -> {
			TicketDto dto = new TicketDto();

			dto.setId(domain.getId());
			dto.setType(domain.getType());
			dto.setStatus(domain.getStatus());
			dto.setTitle(domain.getTitle());
			dto.setSummary(domain.getSummary());
			dto.setDescription(domain.getDescription());
			dto.setCreationDateTime(domain.getCreationDateTime());

			return dto;
		});
	}

	@Override
	public Mono<Ticket> toDomain(TicketDto dto) {
		return Mono.fromSupplier(() -> {
			Ticket domain = new Ticket();

			domain.setId(dto.getId());
			domain.setType(dto.getType());
			domain.setStatus(dto.getStatus());
			domain.setTitle(dto.getTitle());
			domain.setSummary(dto.getSummary());
			domain.setDescription(dto.getDescription());
			domain.setCreationDateTime(dto.getCreationDateTime());

			return domain;
		});
	}
}
