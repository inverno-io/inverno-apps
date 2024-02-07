/*
 * Copyright 2024 Jeremy KUHN
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

export default Vue.defineComponent({
	name: 'SplitPanel',
	props: {
		percent: {
			type: Number
		},
		expand: {
			type: String
		}
	},
	setup(props) {
		const percent = Vue.ref(props.percent);
		const gridTemplate = Vue.computed(() => {
			if(props.expand === 'left' || props.expand === 'right') {
				return `none / 1fr`;
			}
			return `none / ${percent.value}% auto 1fr`;
		});

		const isActive = Vue.ref(false);
		const userSelect = Vue.computed(() => isActive.value ? 'none' : 'auto');

		const splitPanelRef = Vue.ref();
		const splitterRef = Vue.ref();

		let dragOffset = 0;

		const onSplitterTouchDown = (event) => {
			dragOffset = 0;
			onSplitterDown();
		};

		const onSplitterMouseDown = (event) => {
			dragOffset = event.offsetX;
			onSplitterDown();
		};

		const onSplitterDown = () => {
			isActive.value = true;
			addSplitterBodyListeners();
		};

		const onSplitterBodyTouchMove = (event) => {
			if(isActive.value) {
				calculateSplitterPercent(event.touches[0]);
			}
		};

		const onSplitterBodyMouseMove = (event) => {
			if(event.buttons && event.buttons === 0) {
				isActive.value = false;
				removeSplitterBodyListeners();
			}
			if(isActive.value) {
				calculateSplitterPercent(event);
			}
		};

		const onSplitterBodyUp = (event) => {
			isActive.value = false;
			removeSplitterBodyListeners();
		};

		const addSplitterBodyListeners = () => {
			document.body.addEventListener('mousemove', onSplitterBodyMouseMove);
			document.body.addEventListener('touchmove', onSplitterBodyTouchMove);
			document.body.addEventListener('touchend', onSplitterBodyUp, { once: true });
			document.body.addEventListener('mouseup', onSplitterBodyUp, { once: true });
		};

		const removeSplitterBodyListeners = () => {
			document.body.removeEventListener('touchmove', onSplitterBodyTouchMove);
			document.body.removeEventListener('mousemove', onSplitterBodyMouseMove);
		};

		const calculateSplitterPercent = (event) => {
			let offset = dragOffset;
			let target = splitPanelRef.value;
			let newPercent = 0;

			offset += target.offsetLeft;
			while (target.offsetParent) {
				target = target.offsetParent;
				offset += target.offsetLeft;
			}
			newPercent = Math.floor( ((event.pageX - offset) / (splitPanelRef.value.offsetWidth)) * 10000 ) / 100;

			if (newPercent > 0 && newPercent < (100 - (Math.ceil((splitterRef.value.offsetWidth / splitPanelRef.value.offsetWidth) * 10000) / 100))) {
				percent.value = newPercent;
			}
		};

		return {
			percent,
			gridTemplate,
			userSelect,
			splitPanelRef,
			splitterRef,
			onSplitterTouchDown,
			onSplitterMouseDown
		};
	},
	template: `
		<div class="split-panel" ref="splitPanelRef" :style="{ userSelect, gridTemplate }">
			<slot name="left" v-if="expand !== 'right'"></slot>
			<div class="split-panel-splitter" ref="splitterRef" v-on:mousedown="onSplitterMouseDown" v-on:touchstart.passive="onSplitterTouchDown" v-if="!expand"></div>
			<slot name="right" v-if="expand !== 'left'"></slot>
		</div>`
});
