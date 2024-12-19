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
package io.inverno.app.ticket.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.ZonedDateTime;
import reactor.core.publisher.Flux;

/**
 * <p>
 * A plan represents an ordered list of {@link Ticket} that forms a set of intended actions to achieve a goal.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@JsonInclude(Include.NON_NULL)
public class Plan {
	
	private Long id;
	private String title;
	private String summary;
	private String description;
	private ZonedDateTime creationDateTime;
	@JsonIgnore
	private Flux<Ticket> tickets;

	public Plan() {
	}

	public Plan(Long id, String title, String summary, String description, ZonedDateTime creationDateTime, Flux<Ticket> tickets) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.summary = summary;
		this.creationDateTime = creationDateTime;
		this.tickets = tickets;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ZonedDateTime getCreationDateTime() {
		return creationDateTime;
	}

	public void setCreationDateTime(ZonedDateTime creationDateTime) {
		this.creationDateTime = creationDateTime;
	}
	
	public Flux<Ticket> getTickets() {
		return tickets;
	}

	public void setTickets(Flux<Ticket> tickets) {
		this.tickets = tickets;
	}
}
