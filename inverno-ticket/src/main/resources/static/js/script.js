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

import SplitPanel from './split-panel.js'

const API_BASE_URL = '/api/v1';
const API_PLAN_URL = API_BASE_URL + '/plan';
const API_TICKET_URL = API_BASE_URL + '/ticket';

const TicketApp = {
	setup() {
		/* Init */
		const init = () => {
			fetch(API_PLAN_URL,  {
				method: 'get'
			})
			.then(res => {
				if (!res.ok) {
					const error = new Error(res.statusText);
					error.json = res.json();
					throw error;
				}
				return res.json();
			})
			.then(json => {
				plans.value = json;
				if(json.length > 0) {
					selectPlan(json[0].id);
				}
			})
			.catch(err => {
				if (err.json) {
					return err.json.then(json => {
						alert(json.message);
					});
				} 
				else {
					alert(err.message);
				}
			});
		};
		
		Vue.onMounted(() => {
            init();
        });
		
		/* Plans */
		const plans = Vue.ref([]);

		const selectedPlan = Vue.ref(null);
		const expandedPlan = Vue.ref(false);
		
		const editedTitlePlan = Vue.ref(null);
		const editedSummaryPlan = Vue.ref(null);
		const editedDescriptionPlan = Vue.ref(null);
		
		const addToPlanTicketId = Vue.ref(null);
		const creatingPlan = Vue.ref(false);
		const createPlanTitle = Vue.ref(null);
		const createPlanSummary = Vue.ref(null);
		const createPlanDescription = Vue.ref(null);
		
		const ticketListSize = Vue.ref(20);
		
		const filteredTicketStatuses = Vue.ref(new Set([]));
		
		const createPlan = (plan) => {
			fetch(API_PLAN_URL, {
				method: 'post',
				headers: {
					'accept':'application/json',
					'content-type':'application/json'
				},
				body: JSON.stringify(plan, ["title", "summary", "description"])
			})
			.then(res => {
				if (!res.ok) {
					const error = new Error(res.statusText);
					error.json = res.json();
					throw error;
				}
				return res.json();
			})
			.then(json => {
				plans.value.push(json);
				selectPlan(json.id);
			})
			.catch(err => {
				if (err.json) {
					return err.json.then(json => {
						alert(json.error + '(' + json.status + '): ' + json.message);
					});
				} 
				else {
					alert(err.message);
				}
			});
		};
		
		const selectPlan = (id, statuses) => {
			let path = API_PLAN_URL + '/' + id;
			if(statuses && statuses.length > 0) {
				filteredTicketStatuses.value = new Set(statuses);
				path += '?statuses=' + statuses.join(",");
			}
			else {
				filteredTicketStatuses.value = new Set(["OPEN", "STUDIED", "IN_PROGRESS", "DONE", "REJECTED"]);
			}
			fetch(path, {
				method: 'get',
				headers: {
					'accept':'application/json'
				}
			})
			.then(res => {
				if (!res.ok) {
					const error = new Error(res.statusText);
					error.json = res.json();
					throw error;
				}
				return res.json();
			})
			.then(json => {
				selectedPlan.value = json;
				if(json.tickets.length > 0) {
					selectTicket(json.tickets[0]);
				}
				else {
					selectedTicket.value = null;
				}
			})
			.catch(err => {
				if (err.json) {
					return err.json.then(json => {
						alert(json.error + '(' + json.status + '): ' + json.message);
					});
				} 
				else {
					alert(err.message);
				}
			});
		};
		
		const updatePlan = (plan) => {
			fetch(API_PLAN_URL + '/' + plan.id, {
				method: 'put',
				headers: {
					'accept': 'application/json',
					'content-type': 'application/json'
				},
				body: JSON.stringify(plan, ["id", "title", "summary", "description"]) // `{"id":${plan.id},"title":${JSON.stringify(plan.title)},"summary":${JSON.stringify(plan.summary)},"description":${JSON.stringify(plan.description)}}`
			})
			.then(res => {
				if (!res.ok) {
					const error = new Error(res.statusText);
					error.json = res.json();
					throw error;
				}
				return res.json();
			})
			.then(json => {
				delete json.tickets;
				Object.assign(plan, json);
				for(let p of plans.value) {
					if(p.id === plan.id) {
						p.title = plan.title;
						p.summary = plan.summary;
						p.description = plan.description;
						break;
					}
				}
			})
			.catch(err => {
				if (err.json) {
					return err.json.then(json => {
						alert(json.error + '(' + json.status + '): ' + json.message);
					});
				} 
				else {
					alert(err.message);
				}
			});
		};
		
		const addTicketToPlan = (plan, ticketId) => {
			fetch(API_TICKET_URL + '/' + ticketId, {
				method: 'get',
				headers: {
					'accept':'application/json'
				}
			})
			.then(res => {
				if (!res.ok) {
					const error = new Error(res.statusText);
					error.json = res.json();
					throw error;
				}
				return res.json();
			})
			.then(json => {
				insertTicketToPlan(plan, json); 
			})
			.catch(err => {
				if (err.json) {
					return err.json.then(json => {
						alert(json.error + '(' + json.status + '): ' + json.message);
					});
				} 
				else {
					alert(err.message);
				}
			});
		};
		
		const insertTicketToPlan = (plan, ticket, refTicket) => {
			let body = 'ticketId=' + ticket.id;
			if(refTicket) {
				body += '&referenceTicketId=' + refTicket.id;
			}
			fetch(API_PLAN_URL + '/' + plan.id + '/ticket', {
				method: 'post',
				headers: {
					'accept':'application/json',
					'content-type':'application/x-www-form-urlencoded'
				},
				body: body
			})
			.then(res => {
				if (!res.ok) {
					const error = new Error(res.statusText);
					error.json = res.json();
					throw error;
				}
				else {
					if(plan.id === selectedPlan.value.id) {
						if(refTicket) {
							plan.tickets.splice(selectedPlan.value.tickets.indexOf(ticket), 1);
							plan.tickets.splice(selectedPlan.value.tickets.indexOf(refTicket), 0, ticket);
						}
						else {
							plan.tickets.push(ticket);
						}
						selectTicket(ticket);
					}
				}
			})
			.catch(err => {
				if (err.json) {
					return err.json.then(json => {
						alert(json.error + '(' + json.status + '): ' + json.message);
					});
				} 
				else {
					alert(err.message);
				}
			});
		};
		
		const removeTicketFromPlan = (plan, ticket) => {
			fetch(API_PLAN_URL + '/' + plan.id + '/' + ticket.id, {
				method: 'delete',
				headers: {
					'accept':'application/json'
				}
			})
			.then(res => {
				if (!res.ok) {
					const error = new Error(res.statusText);
					error.json = res.json();
					throw error;
				}
				else {
					plan.tickets.splice(plan.tickets.indexOf(ticket), 1);
				}
				return res.json();
			})
			.catch(err => {
				if (err.json) {
					return err.json.then(json => {
						alert(json.error + '(' + json.status + '): ' + json.message);
					});
				} 
				else {
					alert(err.message);
				}
			});
		};
		
		/* Tickets */
		const selectedTicket = Vue.ref(null);
		const expandedTicket = Vue.ref(false);
		const draggedTicket = Vue.ref(null);

		const editedTitleTicket = Vue.ref(null);
		const editedSummaryTicket = Vue.ref(null);
		const editedDescriptionTicket = Vue.ref(null);

		const creatingTicket = Vue.ref(false);
		const createTicketType = Vue.ref(null);
		const createTicketTitle = Vue.ref(null);
		const createTicketSummary = Vue.ref(null);
		const createTicketDescription = Vue.ref(null);
		
		const createTicket = (ticket) => {
			fetch(API_TICKET_URL, {
				method: 'post',
				headers: {
					'accept':'application/json',
					'content-type':'application/json'
				},
				body: JSON.stringify(ticket)
			})
			.then(res => {
				if (!res.ok) {
					const error = new Error(res.statusText);
					error.json = res.json();
					throw error;
				}
				return res.json();
			})
			.then(json => {
				// Add ticket to selected plan
				if(selectedPlan.value) {
					insertTicketToPlan(selectedPlan.value, json);
				}
			})
			.catch(err => {
				if (err.json) {
					return err.json.then(json => {
						alert(json.error + '(' + json.status + '): ' + json.message);
					});
				} 
				else {
					alert(err.message);
				}
			});
		};
		
		const selectTicket = (ticket) => {
			selectedTicket.value = ticket;
			selectedTicketNote.value = null;
			fetch(API_TICKET_URL + '/' + ticket.id + '/note', {
				method: 'get',
				headers: {
					'accept':'application/json'
				}
			})
			.then(res => {
				if (!res.ok) {
					const error = new Error(res.statusText);
					error.json = res.json();
					throw error;
				}
				return res.json();
			})
			.then(json => {
				selectedTicket.value.notes = json;
				if(json.length > 0) {
					selectTicketNote(json[0]);
				}
			})
			.catch(err => {
				if (err.json) {
					return err.json.then(json => {
						alert(json.error + '(' + json.status + '): ' + json.message);
					});
				} 
				else {
					alert(err.message);
				}
			});
		};
		
		const updateTicket = (ticket) => {
			fetch(API_TICKET_URL + '/' + ticket.id, {
				method: 'put',
				headers: {
					'accept': 'application/json',
					'content-type': 'application/json'
				},
				body: JSON.stringify(ticket, ["id", "type", "status", "creationDateTime", "title", "summary", "description"])
			})
			.then(res => {
				if (!res.ok) {
					const error = new Error(res.statusText);
					error.json = res.json();
					throw error;
				}
				return res.json();
			})
			.then(json => {
				Object.assign(ticket, json);
			})
			.catch(err => {
				if (err.json) {
					return err.json.then(json => {
						alert(json.error + '(' + json.status + '): ' + json.message);
					});
				} 
				else {
					alert(err.message);
				}
			});
		};
		
		const updateTicketStatus = (ticket, status) => {
			fetch(API_TICKET_URL + '/' + ticket.id + '/status', {
				method: 'post',
				headers: {
					'accept': 'application/json',
					'content-type':'text/plain'
				},
				body: status
			})
			.then(res => {
				if (!res.ok) {
					const error = new Error(res.statusText);
					error.json = res.json();
					throw error;
				}
				return res.json();
			})
			.then(json => {
				ticket.status = status;
			})
			.catch(err => {
				if (err.json) {
					return err.json.then(json => {
						alert(json.error + '(' + json.status + '): ' + json.message);
					});
				} 
				else {
					alert(err.message);
				}
			});
		};
		
		/* Notes */
		const selectedTicketNote = Vue.ref(null);
		const editedTitleTicketNote = Vue.ref(null);
		const editedContentTicketNote = Vue.ref(null);
		
		const createTicketNote = (note) => {
			fetch(API_TICKET_URL + '/' + note.ticketId + '/note', {
				method: 'post',
				headers: {
					'accept': 'application/json',
					'content-type':'application/json'
				},
				body: JSON.stringify(note, ["type", "title", "summary", "description"])
			})
			.then(res => {
				if (!res.ok) {
					const error = new Error(res.statusText);
					error.json = res.json();
					throw error;
				}
				return res.json();
			})
			.then(json => {
				selectedTicket.value.notes.push(json);
				selectTicketNote(json);
			})
			.catch(err => {
				if (err.json) {
					return err.json.then(json => {
						alert(json.error + '(' + json.status + '): ' + json.message);
					});
				} 
				else {
					alert(err.message);
				}
			});
		};
		
		const updateTicketNote = (note) => {
			fetch(API_TICKET_URL + '/' + note.ticketId + '/note/' + note.index, {
				method: 'put',
				headers: {
					'accept': 'application/json',
					'content-type':'application/json'
				},
				body: JSON.stringify(note,  ["ticketId", "index", "title", "content"])
			})
			.then(res => {
				if (!res.ok) {
					const error = new Error(res.statusText);
					error.json = res.json();
					throw error;
				}
				return res.json();
			})
			.then(json => {
				Object.assign(note, json);
			})
			.catch(err => {
				if (err.json) {
					return err.json.then(json => {
						alert(json.error + '(' + json.status + '): ' + json.message);
					});
				} 
				else {
					alert(err.message);
				}
			});
		};
		
		const selectTicketNote = (note) => {
			selectedTicketNote.value = note;
		};
		
		const deleteTicketNote = (note) => {
			if(confirm(`Are you sure you want to delete note '${note.title}'?`)) {
				fetch(API_TICKET_URL + '/' + note.ticketId + '/note/' + note.index, {
					method: 'delete',
					headers: {
						'accept': 'application/json'
					}
				})
				.then(res => {
					if (!res.ok) {
						const error = new Error(res.statusText);
						error.json = res.json();
						throw error;
					}
					return res.json();
				})
				.then(json => {
					selectedTicket.value.notes.splice(note.index, 1);
					if(selectedTicket.value.notes.length === 0) {
						selectedTicketNote.value = null;
					}
					else if(selectedTicketNote.value.index === note.index) {
						selectTicketNote(selectedTicket.value.notes[Math.min(note.index, selectedTicket.value.notes.length - 1)]);
					}
				})
				.catch(err => {
					if (err.json) {
						return err.json.then(json => {
							alert(json.error + '(' + json.status + '): ' + json.message);
						});
					} 
					else {
						alert(err.message);
					}
				});
			}
		};
		
		/* UI Callbacks */
		const onCreatePlan = () => {
		    createPlanTitle.value = createPlanSummary.value = createPlanDescription.value = null;
			document.getElementById('createPlanForm').classList.remove('was-validated');
			document.getElementById('createPlanForm').reset();
			creatingPlan.value = true;
		};

		const onSubmitCreatePlan = (evt) => {
			if (!evt.target.checkValidity()) {
                evt.preventDefault();
                evt.stopPropagation();
                evt.target.classList.add('was-validated');
                return;
            }
            evt.target.classList.remove('was-validated');
			bootstrap.Modal.getInstance(document.getElementById('createPlanModal')).hide();
			createPlan({"title": createPlanTitle.value, "summary": createPlanSummary.value, "description": createPlanDescription.value});
			evt.target.reset();
			creatingPlan.value = false;
		};

		const onAddTicketToPlan = () => {
		    addToPlanTicketId.value = null;
			document.getElementById('addTicketToPlanForm').classList.remove('was-validated');
		};

		const onSubmitAddTicketToPlan = (evt) => {
			if (!evt.target.checkValidity()) {
                evt.preventDefault();
                evt.stopPropagation();
                evt.target.classList.add('was-validated');
                return;
            }
            evt.target.classList.remove('was-validated');
			addTicketToPlan(selectedPlan.value, addToPlanTicketId.value);
			addToPlanTicketId.value = null;
			evt.target.reset();
		};

		const onCreateTicket = () => {
		    createTicketType.value = createTicketTitle.value = createTicketSummary.value = createTicketDescription.value = null;
			createTicketType.value = 'FEATURE';
			document.getElementById('createTicketForm').classList.remove('was-validated');
			document.getElementById('createTicketForm').reset();
			creatingTicket.value = true;
		};

		const onSubmitCreateTicket = (evt) => {
			if (!evt.target.checkValidity()) {
                evt.preventDefault();
                evt.stopPropagation();
                evt.target.classList.add('was-validated');
                return;
            }
            evt.target.classList.remove('was-validated');
			bootstrap.Modal.getInstance(document.getElementById('createTicketModal')).hide();
			createTicket({"type": createTicketType.value, "title": createTicketTitle.value, "summary": createTicketSummary.value, "description": createTicketDescription.value});
			evt.target.reset();
			creatingTicket.value = false;
		};
		
		const onToggleStatus = (status) => {
			if(filteredTicketStatuses.value.has(status)) {
				filteredTicketStatuses.value.delete(status);
			}
			else {
				filteredTicketStatuses.value.add(status);
			}
			selectPlan(selectedPlan.value.id, Array.from(filteredTicketStatuses.value));
		};

		return {
			init,
			plans,
			selectedPlan,
			expandedPlan,
			editedTitlePlan,
			editedSummaryPlan,
			editedDescriptionPlan,
			addToPlanTicketId,
			creatingPlan,
			createPlanTitle,
			createPlanSummary,
			createPlanDescription,
			ticketListSize,
			filteredTicketStatuses,
			createPlan,
			selectPlan,
			updatePlan,
			addTicketToPlan,
			insertTicketToPlan,
			removeTicketFromPlan,
			selectedTicket,
			expandedTicket,
			draggedTicket,
			editedTitleTicket,
			editedSummaryTicket,
			editedDescriptionTicket,
			creatingTicket,
			createTicketType,
			createTicketTitle,
			createTicketSummary,
			createTicketDescription,
			createTicket,
			selectTicket,
			updateTicket,
			updateTicketStatus,
			selectedTicketNote,
			editedTitleTicketNote,
			editedContentTicketNote,
			createTicketNote,
			updateTicketNote,
			selectTicketNote,
			deleteTicketNote,
			onCreatePlan,
			onSubmitCreatePlan,
			onAddTicketToPlan,
			onSubmitAddTicketToPlan,
			onCreateTicket,
			onSubmitCreateTicket,
			onToggleStatus
		};
	},
	computed: {
		renderedDescriptionPlan() {
			if(this.selectedPlan !== null && this.selectedPlan.description !== null) {
				let renderedNode = document.createElement('DIV');
				renderedNode.innerHTML = marked.parse(this.selectedPlan.description);
				renderedNode.querySelectorAll('pre code').forEach((block) => {
					hljs.highlightBlock(block);
				});
				return renderedNode.innerHTML;
			}
			return '';
		},
		renderedDescriptionTicket() {
			if(this.selectedPlan !== null && this.selectedTicket.description !== null) {
				let renderedNode = document.createElement('DIV');
				renderedNode.innerHTML = marked.parse(this.selectedTicket.description);
				renderedNode.querySelectorAll('pre code').forEach((block) => {
					hljs.highlightBlock(block);
				});
				return renderedNode.innerHTML;
			}
			return '';
		},
		renderedContentTicketNote() {
			if(this.selectedTicketNote !== null && this.selectedTicketNote.content !== null) {
				let renderedNode = document.createElement('DIV');
				renderedNode.innerHTML = marked.parse(this.selectedTicketNote.content);
				renderedNode.querySelectorAll('pre code').forEach((block) => {
					hljs.highlightBlock(block);
				});
				return renderedNode.innerHTML;
			}
			return '';
		}
	}
};

const TicketVueAPP = Vue.createApp(TicketApp)
    .component('split-panel', SplitPanel)
	.directive('focus', {
		mounted(el) {
			el.focus();
		}
	})
	.directive('scroll', {
		mounted(el, binding) {
			if(binding.value.scrollRatio === null) {
				binding.value.scrollRatio = 0;
			}
			el.scrollTo(0, binding.value.scrollRatio * el.scrollTopMax);
		},
		updated(el, binding) {
			if(binding.value.scrollRatio === null) {
				binding.value.scrollRatio = 0;
			}
			el.scrollTo(0, binding.value.scrollRatio * el.scrollTopMax);
		}
	})
	.directive('markdown', {
		mounted(el, binding) {
			if(binding.arg === "create") {
				if(el.mde) {
					el.mde.toTextArea();
					el.mde = null;
				}
				el.mde = new SimpleMDE({
					"element": el,
					"tabSize": 4,
					"autofocus": true,
					"status": false,
					"forceSync": true,
					"toolbar": false
				});
				el.mde.codemirror.on("change", function() {
					binding.value.save(el.mde.value());
				});
			}
			else if(binding.arg === "update") {
				el.mde = new SimpleMDE({
					"element": el,
					"tabSize": 4,
					"autofocus": true,
					"status": false,
					"forceSync": true,
					"toolbar": false
				});
				el.mde.codemirror.on("blur", function() {
					if(binding.value.scrollTarget) {
						binding.value.scrollTarget.scrollRatio = el.mde.codemirror.getScrollerElement().scrollTop / el.mde.codemirror.getScrollerElement().scrollTopMax;
					}
					binding.value.save(el.mde.value());
					el.mde.toTextArea();
				});
				if(binding.value.scrollTarget) {
					el.mde.codemirror.scrollTo(0, binding.value.scrollTarget.scrollRatio * el.mde.codemirror.getScrollerElement().scrollTopMax);
				}
			}
		},
		beforeUnmount(el, binding) {
			if(el.mde) {
				el.mde.toTextArea();
				el.mde = null;
			}
		}
	})
	.mount('#ticket-app');
