/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.app.ticket.internal.rest.v1.mapper;

import io.inverno.app.ticket.internal.model.Plan;
import io.inverno.app.ticket.internal.model.Ticket;
import io.inverno.app.ticket.internal.rest.DtoMapper;
import io.inverno.app.ticket.internal.rest.v1.dto.PlanDto;
import io.inverno.app.ticket.internal.rest.v1.dto.TicketDto;
import io.inverno.core.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 *
 * @author jkuhn
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class PlanDtoMapper implements DtoMapper<PlanDto, Plan> {

	private final DtoMapper<TicketDto, Ticket> ticketDtoMapper;
	
	public PlanDtoMapper(DtoMapper<TicketDto, Ticket> ticketDtoMapper) {
		this.ticketDtoMapper = ticketDtoMapper;
	}
	
	@Override
	public Mono<PlanDto> toDto(Plan domain) {
		return Optional.ofNullable(domain.getTickets()).orElse(Flux.empty())
			.flatMap(this.ticketDtoMapper::toDto)
			.collectList()
			.map(tickets -> {
				PlanDto dto = new PlanDto();

				dto.setId(domain.getId());
				dto.setTitle(domain.getTitle());
				dto.setSummary(domain.getSummary());
				dto.setDescription(domain.getDescription());
				dto.setCreationDateTime(domain.getCreationDateTime());
				dto.setTickets(tickets);

				return dto;
			});
	}

	@Override
	public Mono<Plan> toDomain(PlanDto dto) {
		return Mono.fromSupplier(() -> {
			Plan plan = new Plan();
			
			plan.setId(dto.getId());
			plan.setTitle(dto.getTitle());
			plan.setDescription(dto.getDescription());
			plan.setSummary(dto.getSummary());
			plan.setCreationDateTime(dto.getCreationDateTime());
			if(dto.getTickets() != null) {
				plan.setTickets(Flux.fromIterable(dto.getTickets()).flatMap(this.ticketDtoMapper::toDomain));
			}
			
			return plan;
		});
	}
	
}
