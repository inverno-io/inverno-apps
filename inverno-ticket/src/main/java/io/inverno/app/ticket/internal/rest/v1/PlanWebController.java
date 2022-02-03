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

import io.inverno.app.ticket.internal.PlanService;
import io.inverno.app.ticket.internal.model.Plan;
import io.inverno.app.ticket.internal.model.Ticket;
import io.inverno.app.ticket.internal.rest.DtoMapper;
import io.inverno.app.ticket.internal.rest.v1.dto.PlanDto;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.annotation.Body;
import io.inverno.mod.web.annotation.FormParam;
import io.inverno.mod.web.annotation.PathParam;
import io.inverno.mod.web.annotation.QueryParam;
import io.inverno.mod.web.annotation.WebController;
import io.inverno.mod.web.annotation.WebRoute;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Plan related REST endpoints.
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
	 * 
	 * @param plan
	 * @param exchange
	 * @return 
	 */
	@WebRoute( method = Method.POST, consumes = MediaTypes.APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON )
	public Mono<PlanDto> createPlan(@Body PlanDto plan, WebExchange<?> exchange) {
		plan.setId(null);
		return this.planDtoMapper.toDomain(plan)
			.flatMap(this.planService::savePlan)
			.doOnNext(savedPlan -> {
				exchange.response().headers(headers -> headers
					.status(Status.CREATED)
					.add(Headers.NAME_LOCATION, exchange.request().getPathBuilder().segment(savedPlan.getId().toString()).buildPath())
				);
			})
			.flatMap(this.planDtoMapper::toDto);
	}
	
	/**
	 * 
	 * @return 
	 */
	@WebRoute( method = Method.GET, produces = MediaTypes.APPLICATION_JSON )
	public Flux<PlanDto> listPlans() {
		return this.planService.listPlans()
			.doOnNext(plan -> plan.setTickets(null)) // We don't want to return tickets when listing plans
			.flatMap(this.planDtoMapper::toDto);
	}
	
	/**
	 * 
	 * @param planId
	 * @param statuses
	 * @return 
	 */
	@WebRoute( path = { "/{planId}" }, method = Method.GET, produces = MediaTypes.APPLICATION_JSON )
	public Mono<PlanDto> getPlan(@PathParam long planId, @QueryParam Optional<List<Ticket.Status>> statuses) {
		return statuses.map(s -> this.planService.getPlan(planId, s)).orElse(this.planService.getPlan(planId)).flatMap(this.planDtoMapper::toDto).switchIfEmpty(Mono.error(() -> new NotFoundException()));
	}
	
	/**
	 * 
	 * @param planId
	 * @param plan
	 * @return 
	 */
	@WebRoute( path = { "/{planId}" }, method = Method.PUT, consumes = MediaTypes.APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON )
	public Mono<PlanDto> updatePlan(@PathParam long planId, @Body PlanDto plan) {
		plan.setId(planId);
		return this.planDtoMapper.toDomain(plan).flatMap(this.planService::savePlan).flatMap(this.planDtoMapper::toDto);
	}
	
	/**
	 * 
	 * @param planId
	 * @return 
	 */
	@WebRoute( path = { "/{planId}" }, method = Method.DELETE, produces = MediaTypes.APPLICATION_JSON )
	public Mono<PlanDto> deletePlan(@PathParam long planId) {
		return this.planService.removePlan(planId).flatMap(this.planDtoMapper::toDto).switchIfEmpty(Mono.error(() -> new NotFoundException()));
	}
	
	/**
	 * 
	 * @param planId
	 * @param ticketId
	 * @param referenceTicketId
	 * @return 
	 */
	@WebRoute( path = { "/{planId}/ticket" }, method = Method.POST, consumes= MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED )
	public Mono<Void> pushTicket(@PathParam long planId, @FormParam long ticketId, @FormParam Optional<Long> referenceTicketId) {
		return referenceTicketId
			.map(refTicketId -> this.planService.insertTicketBefore(planId, ticketId, refTicketId))
			.orElse(this.planService.addTicket(planId, ticketId));
	}
	
	/**
	 * 
	 * @param planId
	 * @param ticketId
	 * @return 
	 */
	@WebRoute( path = { "/{planId}/ticket/{ticketId}" }, method = Method.DELETE, produces = MediaTypes.TEXT_PLAIN )
	public Mono<Long> removeTicket(@PathParam long planId, @PathParam long ticketId) {
		return this.planService.removeTicket(planId, ticketId);
	}
}
