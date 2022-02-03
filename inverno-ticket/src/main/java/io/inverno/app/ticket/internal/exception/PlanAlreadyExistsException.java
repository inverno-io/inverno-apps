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
 * Thrown when trying to create a plan with the id of an existing plan.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class PlanAlreadyExistsException extends TicketException {

	private static final String MESSAGE_PATTERN = "Plan %d already exists";
	
	private final long planId;
	
	/**
	 * 
	 * @param planId 
	 */
	public PlanAlreadyExistsException(long planId) {
		super(String.format(MESSAGE_PATTERN, planId));
		this.planId = planId;
	}

	/**
	 * 
	 * @param planId
	 * @param cause 
	 */
	public PlanAlreadyExistsException(long planId, Throwable cause) {
		super(String.format(MESSAGE_PATTERN, planId), cause);
		this.planId = planId;
	}

	/**
	 * 
	 * @param planId
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace 
	 */
	public PlanAlreadyExistsException(long planId, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(String.format(MESSAGE_PATTERN, planId), cause, enableSuppression, writableStackTrace);
		this.planId = planId;
	}

	/**
	 * 
	 * @return 
	 */
	public long getPlanId() {
		return planId;
	}
}
