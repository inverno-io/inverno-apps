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
package io.inverno.app.ticket.internal.rest.v1.mapper;

import io.inverno.app.ticket.internal.model.Ticket;
import io.inverno.app.ticket.internal.rest.DtoMapper;
import io.inverno.app.ticket.internal.rest.v1.dto.TicketDto;
import io.inverno.core.annotation.Bean;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Ticket DTO mapper.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
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
