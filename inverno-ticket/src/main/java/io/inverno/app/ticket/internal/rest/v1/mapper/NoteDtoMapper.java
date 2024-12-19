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

import io.inverno.app.ticket.internal.model.Note;
import io.inverno.app.ticket.internal.rest.DtoMapper;
import io.inverno.app.ticket.internal.rest.v1.dto.NoteDto;
import io.inverno.core.annotation.Bean;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Note DTO mapper.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
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
