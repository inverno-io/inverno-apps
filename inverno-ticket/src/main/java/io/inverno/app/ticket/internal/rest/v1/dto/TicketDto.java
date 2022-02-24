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
package io.inverno.app.ticket.internal.rest.v1.dto;

import io.inverno.app.ticket.internal.model.Ticket;
import java.time.ZonedDateTime;

/**
 * <p>
 * Ticket Data Transfer Object.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class TicketDto {
	
	private Long id;
	private Ticket.Type type;
	private Ticket.Status status;
	private ZonedDateTime creationDateTime;
	private String title;
	private String summary;
	private String description;

	/**
	 * 
	 */
	public TicketDto() {
	}

	/**
	 * 
	 * @param id
	 * @param type
	 * @param status
	 * @param title
	 * @param summary
	 * @param description
	 * @param creationDateTime 
	 */
	public TicketDto(Long id, Ticket.Type type, Ticket.Status status, String title, String summary, String description, ZonedDateTime creationDateTime) {
		this.id = id;
		this.type = type;
		this.status = status;
		this.title = title;
		this.summary = summary;
		this.description = description;
		this.creationDateTime = creationDateTime;
	}

	/**
	 * 
	 * @return 
	 */
	public Long getId() {
		return id;
	}

	/**
	 * 
	 * @param id 
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * 
	 * @return 
	 */
	public Ticket.Type getType() {
		return type;
	}

	/**
	 * 
	 * @param type 
	 */
	public void setType(Ticket.Type type) {
		this.type = type;
	}

	/**
	 * 
	 * @return 
	 */
	public Ticket.Status getStatus() {
		return status;
	}

	/**
	 * 
	 * @param status 
	 */
	public void setStatus(Ticket.Status status) {
		this.status = status;
	}

	/**
	 * 
	 * @return 
	 */
	public ZonedDateTime getCreationDateTime() {
		return creationDateTime;
	}

	/**
	 * 
	 * @param creationDateTime 
	 */
	public void setCreationDateTime(ZonedDateTime creationDateTime) {
		this.creationDateTime = creationDateTime;
	}

	/**
	 * 
	 * @return 
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * 
	 * @param title 
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * 
	 * @return 
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * 
	 * @param summary 
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}
	
	/**
	 * 
	 * @return 
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * 
	 * @param description 
	 */
	public void setDescription(String description) {
		this.description = description;
	}
}
