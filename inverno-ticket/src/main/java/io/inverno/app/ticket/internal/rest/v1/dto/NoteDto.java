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

/**
 * <p>
 * Note Data Transfer Object.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class NoteDto {
	
	private long ticketId;
	private Integer Index;
	private String title;
	private String content;
	
	/**
	 * 
	 */
	public NoteDto() {
	}

	/**
	 * 
	 * @param ticketId
	 * @param Index
	 * @param title
	 * @param content 
	 */
	public NoteDto(int ticketId, Integer Index, String title, String content) {
		this.ticketId = ticketId;
		this.Index = Index;
		this.title = title;
		this.content = content;
	}

	/**
	 * 
	 * @return 
	 */
	public long getTicketId() {
		return ticketId;
	}

	/**
	 * 
	 * @param ticketId 
	 */
	public void setTicketId(long ticketId) {
		this.ticketId = ticketId;
	}
	
	/**
	 * 
	 * @return 
	 */
	public Integer getIndex() {
		return Index;
	}

	/**
	 * 
	 * @param Index 
	 */
	public void setIndex(Integer Index) {
		this.Index = Index;
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
	public String getContent() {
		return content;
	}

	/**
	 * 
	 * @param content 
	 */
	public void setContent(String content) {
		this.content = content;
	}
}
