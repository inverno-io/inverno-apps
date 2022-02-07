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
package io.inverno.app.ticket.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.inverno.app.ticket.internal.model.Plan;
import io.inverno.app.ticket.internal.model.Ticket;
import io.inverno.mod.redis.RedisTransactionalClient;
import io.inverno.mod.redis.lettuce.PoolRedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.support.AsyncConnectionPoolSupport;
import io.lettuce.core.support.BoundedAsyncPool;
import io.lettuce.core.support.BoundedPoolConfig;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.Assertions;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@EnabledIf( value = "isEnabled", disabledReason = "Failed to connect to test Redis database" )
public class PlanServiceTest {
	
	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}

	private static final ObjectMapper MAPPER;
	
	static {
		MAPPER = new ObjectMapper();
		MAPPER.registerModule(new JavaTimeModule());
		MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	}
	
	private static final io.lettuce.core.RedisClient REDIS_CLIENT = io.lettuce.core.RedisClient.create();
	
	private static PoolRedisClient<String, String, StatefulRedisConnection<String, String>> createClient() {
		
		BoundedAsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
			() -> REDIS_CLIENT.connectAsync(StringCodec.UTF8, RedisURI.create("redis://localhost:6379")), 
			BoundedPoolConfig.create()
		);
		return new PoolRedisClient<>(pool, String.class, String.class);
	}
	
	private static void flushAll() {
		REDIS_CLIENT.connect(RedisURI.create("redis://localhost:6379")).reactive().flushall().block();
	}
	
	public static boolean isEnabled() {
		try (StatefulRedisConnection<String, String> connection = REDIS_CLIENT.connect(RedisURI.create("redis://localhost:6379"))) {
			return true;
		}
		catch (RedisConnectionException e) {
			return false;
		}	
	}
	
	/**
	 * Test of savePlan method, of class PlanService.
	 */
	@Test
	public void testPlanService() throws JsonProcessingException {
		RedisTransactionalClient<String, String> client = createClient();
		TicketService ticketService = new TicketService(client, MAPPER);
		PlanService planService = new PlanService(client, MAPPER, ticketService);
		try {
			ZonedDateTime planCreationDateTime = ZonedDateTime.now(ZoneOffset.UTC);
			Plan savePlan = new Plan(null, "Test plan", "Test plan summary", "Test plan description", null, null);
			Plan savedPlan = planService.savePlan(savePlan).block();
			
			Assertions.assertEquals(1l, savedPlan.getId());
			Assertions.assertEquals("Test plan", savedPlan.getTitle());
			Assertions.assertEquals("Test plan summary", savedPlan.getSummary());
			Assertions.assertEquals("Test plan description", savedPlan.getDescription());
			Assertions.assertTrue(ChronoUnit.SECONDS.between(planCreationDateTime, savedPlan.getCreationDateTime()) < 1);
			
			Plan getPlan = planService.getPlan(1).block();
			
			Assertions.assertEquals(1l, getPlan.getId());
			Assertions.assertEquals("Test plan", getPlan.getTitle());
			Assertions.assertEquals("Test plan summary", getPlan.getSummary());
			Assertions.assertEquals("Test plan description", getPlan.getDescription());
			Assertions.assertTrue(ChronoUnit.SECONDS.between(planCreationDateTime, getPlan.getCreationDateTime()) < 1);
			
			Plan getPlanNotExist = planService.getPlan(2).block();
			
			Assertions.assertNull(getPlanNotExist);
			
			Plan updatePlan = new Plan(1l, "Test plan", "Test plan summary", "Test plan updated description", planCreationDateTime, null);
			Plan updatedPlan = planService.savePlan(updatePlan).block();
			
			Assertions.assertEquals(1l, updatedPlan.getId());
			Assertions.assertEquals("Test plan", updatedPlan.getTitle());
			Assertions.assertEquals("Test plan summary", updatedPlan.getSummary());
			Assertions.assertEquals("Test plan updated description", updatedPlan.getDescription());
			Assertions.assertTrue(ChronoUnit.SECONDS.between(planCreationDateTime, updatedPlan.getCreationDateTime()) < 1);
			
			ticketService.saveTicket(new Ticket(null, Ticket.Type.FEATURE, Ticket.Status.OPEN, "ticket 1", "Summary 1", "Description 1", null)).block();
			ticketService.saveTicket(new Ticket(null, Ticket.Type.FEATURE, Ticket.Status.OPEN, "ticket 2", "Summary 2", "Description 2", null)).block();
			ticketService.saveTicket(new Ticket(null, Ticket.Type.FEATURE, Ticket.Status.OPEN, "ticket 3", "Summary 3", "Description 3", null)).block();
			ticketService.saveTicket(new Ticket(null, Ticket.Type.FEATURE, Ticket.Status.OPEN, "ticket 4", "Summary 4", "Description 4", null)).block();
			
			planService.addTicket(1l, 1l).block();
			planService.addTicket(1l, 2l).block();
			planService.addTicket(1l, 3l).block();
			
			getPlan = planService.getPlan(1).block();
			
			List<Ticket> getPlanTickets = getPlan.getTickets().collectList().block();
			
			Assertions.assertEquals(3, getPlanTickets.size());
			
			Assertions.assertEquals(1l, getPlanTickets.get(0).getId());
			Assertions.assertEquals(2l, getPlanTickets.get(1).getId());
			Assertions.assertEquals(3l, getPlanTickets.get(2).getId());
			
			planService.insertTicketBefore(1l, 3l, 2l).block();
			planService.insertTicketBefore(1l, 4l, 1l).block();
			
			getPlanTickets = getPlan.getTickets().collectList().block();
			
			Assertions.assertEquals(4, getPlanTickets.size());
			
			Assertions.assertEquals(4l, getPlanTickets.get(0).getId());
			Assertions.assertEquals(1l, getPlanTickets.get(1).getId());
			Assertions.assertEquals(3l, getPlanTickets.get(2).getId());
			Assertions.assertEquals(2l, getPlanTickets.get(3).getId());
			
			ZonedDateTime planCreationDateTime2 = ZonedDateTime.now(ZoneOffset.UTC);
			Plan savePlan2 = new Plan(null, "Test plan 2", "Test plan 2 summary", "Test plan 2 description", planCreationDateTime2, null);
			Plan savedPlan2 = planService.savePlan(savePlan2).block();
			
			Assertions.assertEquals(2l, savedPlan2.getId());
			Assertions.assertEquals("Test plan 2", savedPlan2.getTitle());
			Assertions.assertEquals("Test plan 2 summary", savedPlan2.getSummary());
			Assertions.assertEquals("Test plan 2 description", savedPlan2.getDescription());
			Assertions.assertTrue(ChronoUnit.SECONDS.between(planCreationDateTime2, savedPlan2.getCreationDateTime()) < 1);
			
			List<Plan> listPlans = planService.listPlans().sort(Comparator.comparing(Plan::getId)).collectList().block();
			
			Assertions.assertEquals(2, listPlans.size());
			
			Assertions.assertEquals(1l, listPlans.get(0).getId());
			Assertions.assertEquals("Test plan", listPlans.get(0).getTitle());
			Assertions.assertEquals("Test plan summary", listPlans.get(0).getSummary());
			Assertions.assertEquals("Test plan updated description", listPlans.get(0).getDescription());
			Assertions.assertTrue(ChronoUnit.SECONDS.between(planCreationDateTime, listPlans.get(0).getCreationDateTime()) < 1);
			
			Assertions.assertEquals(2l, listPlans.get(1).getId());
			Assertions.assertEquals("Test plan 2", listPlans.get(1).getTitle());
			Assertions.assertEquals("Test plan 2 summary", listPlans.get(1).getSummary());
			Assertions.assertEquals("Test plan 2 description", listPlans.get(1).getDescription());
			Assertions.assertTrue(ChronoUnit.SECONDS.between(planCreationDateTime2, listPlans.get(1).getCreationDateTime()) < 1);
			
			planService.removePlan(1l).block();
			
			listPlans = planService.listPlans().collectList().block();
			
			Assertions.assertEquals(1, listPlans.size());
			
			Assertions.assertEquals(2l, listPlans.get(0).getId());
			Assertions.assertEquals("Test plan 2", listPlans.get(0).getTitle());
			Assertions.assertEquals("Test plan 2 summary", listPlans.get(0).getSummary());
			Assertions.assertEquals("Test plan 2 description", listPlans.get(0).getDescription());
			Assertions.assertTrue(ChronoUnit.SECONDS.between(planCreationDateTime2, listPlans.get(0).getCreationDateTime()) < 1);
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
}
