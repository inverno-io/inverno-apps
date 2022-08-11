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
package io.inverno.app.ticket.internal.rest.v1;

import io.inverno.app.ticket.internal.model.Plan;
import io.inverno.app.ticket.internal.model.Ticket;
import io.inverno.app.ticket.internal.rest.DtoMapper;
import io.inverno.app.ticket.internal.rest.v1.dto.PlanDto;
import io.inverno.app.ticket.internal.service.PlanService;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ForbiddenException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.annotation.Body;
import io.inverno.mod.web.annotation.FormParam;
import io.inverno.mod.web.annotation.PathParam;
import io.inverno.mod.web.annotation.QueryParam;
import io.inverno.mod.web.annotation.WebController;
import io.inverno.mod.web.annotation.WebRoute;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Create, update and delete Plans and links tickets to Plans.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
@WebController( path = "/api/v1/plan" )
public class PlanWebController {

	private final PlanService planService;
	private final DtoMapper<PlanDto, Plan> planDtoMapper;

	/**
	 *
	 * @param planService
	 * @param planDtoMapper
	 */
	public PlanWebController(PlanService planService, DtoMapper<PlanDto, Plan> planDtoMapper) {
		this.planService = planService;
		this.planDtoMapper = planDtoMapper;
	}

	/**
	 * Create a new plan.
	 *
	 * @param plan     the plan to create
	 * @param exchange
	 *
	 * @return {@inverno.web.status 201} the created plan
	 */
	@WebRoute( method = Method.POST, consumes = MediaTypes.APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON )
	public Mono<PlanDto> createPlan(@Body PlanDto plan, WebExchange<? extends SecurityContext<? extends PersonIdentity, ? extends RoleBasedAccessController>> exchange) {
		return exchange.context().getAccessController()
			.orElseThrow(() -> new ForbiddenException("Missing access controller"))
			.hasRole("admin")
			.flatMap(hasRole -> {
				if(!hasRole) {
					throw new ForbiddenException();
				}
				plan.setId(null);
				return this.planDtoMapper.toDomain(plan)
					.flatMap(this.planService::savePlan)
					.doOnNext(savedPlan ->
						exchange.response().headers(headers -> headers
							.status(Status.CREATED)
							.add(Headers.NAME_LOCATION, exchange.request().getPathBuilder().segment(savedPlan.getId().toString()).buildPath())
						)
					)
					.flatMap(this.planDtoMapper::toDto);
			});
	}

	/**
	 * List plans.
	 *
	 * @return the list of plans
	 */
	@WebRoute( method = Method.GET, produces = MediaTypes.APPLICATION_JSON )
	public Flux<PlanDto> listPlans() {
		return this.planService.listPlans()
			.doOnNext(plan -> plan.setTickets(null)) // We don't want to return tickets when listing plans
			.flatMap(this.planDtoMapper::toDto);
	}

	/**
	 * Get a plan with its associated tickets filtered by status.
	 *
	 * @param planId   the id of the plan to get
	 * @param statuses the statuses of the tickets to include, if not specified include all tickets
	 *
	 * @return a plan
	 * @throws NotFoundException if there's no ticket with the specified id
	 */
	@WebRoute( path = "/{planId}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON )
	public Mono<PlanDto> getPlan(@PathParam long planId, @QueryParam Optional<List<Ticket.Status>> statuses) {
		return statuses.map(s -> this.planService.getPlan(planId, s)).orElse(this.planService.getPlan(planId))
			.flatMap(this.planDtoMapper::toDto)
			.switchIfEmpty(Mono.error(() -> new NotFoundException()));
	}

	/**
	 * Update a plan.
	 *
	 * @param planId the id of the plan to update
	 * @param plan   the updated plan
	 * @param securityContext
	 *
	 * @return the updated plan
	 */
	@WebRoute( path = "/{planId}", method = Method.PUT, consumes = MediaTypes.APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON )
	public Mono<PlanDto> updatePlan(@PathParam long planId, @Body PlanDto plan, SecurityContext<? extends PersonIdentity, ? extends RoleBasedAccessController> securityContext) {
		return securityContext.getAccessController()
			.orElseThrow(() -> new ForbiddenException("Missing access controller"))
			.hasRole("admin")
			.flatMap(hasRole -> {
				if (!hasRole) {
					throw new ForbiddenException();
				}
				plan.setId(planId);
				return this.planDtoMapper.toDomain(plan)
					.flatMap(this.planService::savePlan)
					.flatMap(this.planDtoMapper::toDto)
					.switchIfEmpty(Mono.error(() -> new NotFoundException()));
			});
	}

	/**
	 * Delete a plan.
	 *
	 * @param planId the id of the plan to delete
	 * @param securityContext
	 *
	 * @return the deleted plan
	 * @throws NotFoundException if there's no ticket with the specified id
	 */
	@WebRoute( path = "/{planId}", method = Method.DELETE, produces = MediaTypes.APPLICATION_JSON )
	public Mono<PlanDto> deletePlan(@PathParam long planId, SecurityContext<? extends PersonIdentity, ? extends RoleBasedAccessController> securityContext) {
		return securityContext.getAccessController()
			.orElseThrow(() -> new ForbiddenException("Missing access controller"))
			.hasRole("admin")
			.flatMap(hasRole -> {
				if (!hasRole) {
					throw new ForbiddenException();
				}
				return this.planService.removePlan(planId)
					.flatMap(this.planDtoMapper::toDto)
					.switchIfEmpty(Mono.error(() -> new NotFoundException()));
			});
	}

	/**
	 * Add a ticket to a plan.
	 *
	 * @param planId            the id of the plan
	 * @param ticketId          the id of the ticket to add
	 * @param referenceTicketId the id of the reference ticket before which the ticket must be added, if not specified add the ticket at the end of the list
	 *
	 * @return
	 */
	@WebRoute( path = "/{planId}/ticket", method = Method.POST, consumes= MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED )
	public Mono<Void> pushTicket(@PathParam long planId, @FormParam long ticketId, @FormParam Optional<Long> referenceTicketId) {
		return referenceTicketId
			.map(refTicketId -> this.planService.insertTicketBefore(planId, ticketId, refTicketId))
			.orElse(this.planService.addTicket(planId, ticketId));
	}

	/**
	 * Remove a ticket from a plan.
	 *
	 * @param planId   the id of the plan
	 * @param ticketId the id of the ticket to remove
	 *
	 * @return 1 if the ticket was removed, 0 if the ticket wasn't associated to the plan
	 */
	@WebRoute( path = "/{planId}/ticket/{ticketId}", method = Method.DELETE, produces = MediaTypes.TEXT_PLAIN )
	public Mono<Long> removeTicket(@PathParam long planId, @PathParam long ticketId) {
		return this.planService.removeTicket(planId, ticketId);
	}
}
