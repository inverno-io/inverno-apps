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
package io.inverno.app.ticket.internal.exception;

/**
 * <p>
 * Thrown when trying to insert a ticket in plan before a ticket which is not linked to the plan.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class TicketNotFoundInPlanException extends TicketException {

	private static final String MESSAGE_PATTERN = "Ticket %d was not found in plan %d";
	
	private final long planId;
	
	private final long ticketId;
	
	/**
	 * 
	 * @param planId
	 * @param ticketId 
	 */
	public TicketNotFoundInPlanException(long planId, long ticketId) {
		super(String.format(MESSAGE_PATTERN, ticketId, planId));
		this.planId = planId;
		this.ticketId = ticketId;
	}

	/**
	 * 
	 * @param planId
	 * @param ticketId
	 * @param cause 
	 */
	public TicketNotFoundInPlanException(long planId, long ticketId, Throwable cause) {
		super(String.format(MESSAGE_PATTERN, ticketId, planId), cause);
		this.planId = planId;
		this.ticketId = ticketId;
	}

	/**
	 * 
	 * @param planId
	 * @param ticketId
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace 
	 */
	public TicketNotFoundInPlanException(long planId, long ticketId, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(String.format(MESSAGE_PATTERN, ticketId, planId), cause, enableSuppression, writableStackTrace);
		this.planId = planId;
		this.ticketId = ticketId;
	}

	/**
	 * 
	 * @return 
	 */
	public long getPlanId() {
		return planId;
	}

	/**
	 * 
	 * @return 
	 */
	public long getTicketId() {
		return ticketId;
	}
}
