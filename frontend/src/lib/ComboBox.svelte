<script>
	import { tick, onMount } from 'svelte';
	import { httpAdapter } from '../appconfig';
	import { isAdmin } from '../stores/authentication';
	import groupAdminGroups from '../stores/groupAdminGroups';
	import topicAdminTopics from '../stores/topicAdminTopics';
	import applicationAdminApplications from '../stores/applicationAdminApplications';
	import { inview } from 'svelte-inview';
	import { createEventDispatcher } from 'svelte';

	export let actionAddApplication = false;

	const dispatch = createEventDispatcher();

	let status = 'blur';
	let comboboxfilter;
	let list;
	let selected = -1;
	let previousLength;

	// Constants
	const groupsDropdownSuggestion = 7;
	const searchStringLength = 3;
	const waitTime = 500;

	// SearchBox
	let searchGroups = '';
	let searchGroupResults;
	let searchGroupActive = false;
	let groupResultPage = 0;
	let hasMoreGroups = true;
	let stopSearchingGroups = false;
	let timer;
	let singleGroup;
	let selectedGroup;

	// If the user does not select any of the available groups, we clear the filter on blur
	$: if (status === 'blur' && !selectedGroup) searchGroups = '';

	// Search Groups Feature
	$: if (
		searchGroups?.trim().length >= searchStringLength &&
		searchGroupActive &&
		!stopSearchingGroups
	) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchGroup(searchGroups.trim());
			stopSearchingGroups = true;
		}, waitTime);
	}

	// Clear the selection on blur
	$: {
		if (status === 'blur' && selected !== -1) {
			selected = -1;
		}
	}

	onMount(async () => {
		if (actionAddApplication) document.querySelector('#combobox-1').placeholder = 'Group **';
		await singleGroupCheck();
	});

	const searchGroup = async (searchGroupStr) => {
		let res;
		if ($isAdmin) {
			res = await httpAdapter.get(
				`/groups?page=${groupResultPage}&size=${groupsDropdownSuggestion}&filter=${searchGroupStr}`
			);
		} else if ($groupAdminGroups?.length > 0 && actionAddUser && !$isAdmin) {
			res = await httpAdapter.get(`/groups/search/${searchGroupStr}?role=GROUP_ADMIN`);
		} else if ($topicAdminTopics?.length > 0 && actionAddTopic && !$isAdmin) {
			res = await httpAdapter.get(`/groups/search/${searchGroupStr}?role=TOPIC_ADMIN`);
		} else if ($applicationAdminApplications?.length > 0 && actionAddApplication && !$isAdmin) {
			res = await httpAdapter.get(`/groups/search/${searchGroupStr}?role=APPLICATION_ADMIN`);
		}

		if (res.data.content) previousLength = res.data.content.length;

		// For all cases
		if (res.data && res.data?.content?.length < groupsDropdownSuggestion) {
			hasMoreGroups = false;
		}

		if (
			res.data?.content?.length > 0 &&
			JSON.stringify(searchGroupResults) !== JSON.stringify(res.data.content)
		) {
			if (searchGroupResults) searchGroupResults = [...searchGroupResults, ...res.data.content];
			else searchGroupResults = [...res.data.content];
		}

		if (searchGroupResults.length === 0) {
			searchGroupResults = [{ name: 'No Results' }];
		}
	};

	async function handleKeyDown(e) {
		switch (e.key) {
			case 'Escape':
				comboboxfilter.blur();
				break;
			case 'Enter':
				selectedGroup = searchGroupResults[selected];
				searchGroups = searchGroupResults[selected].name;

				dispatch('selected-group', selectedGroup.id);

				comboboxfilter.blur();
				break;
			case 'ArrowUp':
				if (selected === -1 || selected === 0) {
					selected = searchGroupResults.length - 1;
				} else {
					selected--;
				}
				list.querySelector(`#listbox-1-option-${selected}`).scrollIntoView(false);

				break;
			case 'ArrowDown':
				if (selected !== -1) {
					selected = (selected + 1) % searchGroupResults.length;
				} else {
					selected = 0;
				}
				list.querySelector(`#listbox-1-option-${selected}`).scrollIntoView(false);

				break;
		}

		// Resume group search
		if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp' && e.key !== 'Enter' && e.key !== 'Escape') {
			searchGroupResults = [];
			stopSearchingGroups = false;
			hasMoreGroups = true;
			groupResultPage = 0;
		}
	}

	const loadMoreResultsGroups = (e) => {
		if (e.detail.inView && hasMoreGroups) {
			groupResultPage++;
			searchGroup(searchGroups);
		}
	};

	const options = {
		rootMargin: '20px',
		unobserveOnEnter: true
	};

	const singleGroupCheck = async () => {
		const res = await httpAdapter.get(
			`/groups?page=${groupResultPage}&size=${groupsDropdownSuggestion}`
		);
		if (res.data.content?.length === 1) {
			singleGroup = res.data.content;
			searchGroupActive = false;
			searchGroups = singleGroup[0].name;
		}
	};
</script>

<div class="container">
	<div
		class="wrapper"
		aria-expanded={status === 'focus'}
		aria-owns="listbox-1"
		aria-haspopup="listbox"
	>
		<input
			data-cy="group-input"
			placeholder="Group *"
			role="combobox"
			id="combobox-1"
			aria-autocomplete="list"
			aria-controls="listbox-1"
			aria-activedescendant={selected !== -1 ? `listbox-1-option-${selected}` : null}
			bind:this={comboboxfilter}
			on:keydown={handleKeyDown}
			required
			bind:value={searchGroups}
			type="text"
			on:focus={() => {
				status = 'focus';
				searchGroupResults = [];
				searchGroupActive = true;
				selectedGroup = '';
			}}
			on:click={() => {
				searchGroupResults = [];
				groupResultPage = 0;
				hasMoreGroups = true;
				searchGroupActive = true;
				selectedGroup = '';
				stopSearchingGroups = false;
			}}
			on:blur={() => (status = 'blur')}
		/>

		<div>
			{#if searchGroupResults}
				{#if status === 'focus' && searchGroupResults.length > 0}
					<ul bind:this={list} id="listbox-1" role="listbox" tabindex={-1}>
						{#each searchGroupResults as group, i (group)}
							<li
								id="listbox-1-option-{i}"
								role="option"
								aria-setsize={searchGroupResults.length}
								aria-posinset={i + 1}
								aria-selected={selected === i}
								class:selected={selected === i}
								on:mousedown={() => {
									searchGroupActive = false;
									selectedGroup = group;
									searchGroups = group.name;

									dispatch('selected-group', selectedGroup.id);

									comboboxfilter.blur();
									selected = -1;
								}}
							>
								{group.name}
							</li>
						{/each}
						<div use:inview={{ options }} on:change={loadMoreResultsGroups} />
					</ul>
				{/if}
			{/if}
		</div>
	</div>
</div>

<style>
	* {
		box-sizing: border-box;
	}
	.container {
		width: 250px;
		position: relative;
	}

	input {
		width: 14rem;
		background: transparent url('../icons/expand.svg') no-repeat right;
		background-size: 10%;
		border-width: 1px;
		margin: 1.5rem 0 1rem 0;
		height: 2.1rem;
	}

	ul {
		top: 3.6rem;
		width: 14rem;
		max-height: 12.5rem;
		overflow-y: auto;
		padding-left: 0;
		list-style: none;
		position: absolute;
		display: block;
		border: 1px solid #000;
		margin-top: -0.1rem;
		background-color: white;
		z-index: 5000;
	}

	li {
		padding: 5px 10px;
	}

	li:hover,
	.selected {
		background-color: #efefef;
	}
</style>
