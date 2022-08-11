/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/javascript.js to edit this template
 */

const API_SECURITY_URL = '/api/security';
const API_IDENTITY_URL = API_SECURITY_URL + '/identity';

const API_BASE_URL = '/api/v1';
const API_PLAN_URL = API_BASE_URL + '/plan';
const API_TICKET_URL = API_BASE_URL + '/ticket';

const LOGOUT_URL = '/logout';

const TicketApp = {
	setup() {
		/* Init */
		const init = () => {
			fetch(API_IDENTITY_URL, {
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
				identity.value = json;
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
		const identity = Vue.ref(null);
		const plans = Vue.ref([]);

		const selectedPlan = Vue.ref(null);
		const expandedPlan = Vue.ref(false);
		
		const editedTitlePlan = Vue.ref(null);
		const editedSummaryPlan = Vue.ref(null);
		const editedDescriptionPlan = Vue.ref(null);
		
		const addToPlanTicketId = Vue.ref(null);
		const createPlanTitle = Vue.ref(null);
		const createPlanSummary = Vue.ref(null);
		const createPlanDescription = Vue.ref(null);
		
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

		const onLogout = () => {
			if(confirm('Are you sure you want to logout?')) {
				fetch(LOGOUT_URL, {
					method: 'get',
					redirect: 'manual',
					headers: {
						'accept': 'application/json'
					}
				})
				.then(res => {
					if(res.type !== 'opaqueredirect') {
						const error = new Error(res.statusText);
						error.json = res.json();
						throw error;
					}
					window.location = '/login';
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

		return {
			init,
			identity,
			plans,
			selectedPlan,
			expandedPlan,
			editedTitlePlan,
			editedSummaryPlan,
			editedDescriptionPlan,
			addToPlanTicketId,
			createPlanTitle,
			createPlanSummary,
			createPlanDescription,
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
			onToggleStatus,
			onLogout
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
				let mde = new SimpleMDE({
					"element": el,
					"tabSize": 4,
					"autofocus": true,
					"status": false,
					"forceSync": true,
					"toolbar": false
				});
				mde.codemirror.on("change", function() {
					binding.value.save(mde.value());
				});
			}
			else if(binding.arg === "update") {
				let mde = new SimpleMDE({
					"element": el,
					"tabSize": 4,
					"autofocus": true,
					"status": false,
					"forceSync": true,
					"toolbar": false
				});
				mde.codemirror.on("blur", function() {
					if(binding.value.scrollTarget) {
						binding.value.scrollTarget.scrollRatio = mde.codemirror.getScrollerElement().scrollTop / mde.codemirror.getScrollerElement().scrollTopMax;
					}
					binding.value.save(mde.value());
					mde.toTextArea();
				});
				if(binding.value.scrollTarget) {
					mde.codemirror.scrollTo(0, binding.value.scrollTarget.scrollRatio * mde.codemirror.getScrollerElement().scrollTopMax);
				}
			}
		}
	})
	.mount('#ticket-app');