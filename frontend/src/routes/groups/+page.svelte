<script>
	import { isAdmin, isAuthenticated } from '../../stores/authentication';
	import { onMount, onDestroy } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import groups from '../../stores/groups';
	import Modal from '../../lib/Modal.svelte';
	import refreshPage from '../../stores/refreshPage';
	import { goto } from '$app/navigation';
	import { browser } from '$app/env';
	import urlparameters from '../../stores/urlparameters';
	import deleteSVG from '../../icons/delete.svg';
	import editSVG from '../../icons/edit.svg';
	import addSVG from '../../icons/add.svg';
	import groupSVG from '../../icons/groups.svg';
	import topicsSVG from '../../icons/topics.svg';
	import appsSVG from '../../icons/apps.svg';
	import pageforwardSVG from '../../icons/pageforward.svg';
	import pagebackwardsSVG from '../../icons/pagebackwards.svg';
	import pagefirstSVG from '../../icons/pagefirst.svg';
	import pagelastSVG from '../../icons/pagelast.svg';
	import headerTitle from '../../stores/headerTitle';
	import detailView from '../../stores/detailView';
	import errorMessages from '$lib/errorMessages.json';
	import renderAvatar from '../../stores/renderAvatar';
	import groupContext from '../../stores/groupContext';
	import permissionBadges from '../../stores/permissionBadges';
	import singleGroupCheck from '../../stores/singleGroupCheck';

	export let data;
	export let errors;

	$: if ($groupContext === 'clear') {
		groupContext.set();
		singleGroupCheck.set();
		reloadAllGroups();
	}

	// Redirects the User to the Login screen if not authenticated
	$: if (browser) {
		setTimeout(() => {
			if (!$isAuthenticated) goto(`/`, true);
		}, waitTime);
	}

	// Locks the background scroll when modal is open
	$: if (browser && (addGroupVisible || deleteGroupVisible || errorMessageVisible)) {
		document.body.classList.add('modal-open');
	} else if (browser && !(addGroupVisible || deleteGroupVisible || errorMessageVisible)) {
		document.body.classList.remove('modal-open');
	}

	// checkboxes selection
	$: if ($groups?.length === groupsRowsSelected?.length) {
		groupsRowsSelectedTrue = false;
		groupsAllRowsSelectedTrue = true;
	} else if (groupsRowsSelected?.length > 0) {
		groupsRowsSelectedTrue = true;
	} else {
		groupsAllRowsSelectedTrue = false;
	}

	// Messages
	let deleteToolip;
	let deleteMouseEnter = false;

	let addTooltip;
	let addMouseEnter = false;

	let activateToolip = 'Select this group as context';
	let activateMouseEnter = new Array($groups?.length).fill(false);

	let createUserMouseEnter, createTopicMouseEnter, createApplicationMouseEnter;

	// Promises
	let promise;

	// Constants
	const returnKey = 13;
	const waitTime = 1000;
	const searchStringLength = 3;

	// Error Handling
	let errorMsg, errorObject;

	// Search
	let searchString;
	let timer;

	// Modals
	let errorMessageVisible = false;
	let addGroupVisible = false;
	let deleteGroupVisible = false;
	let editGroupVisible = false;

	// Tables
	let groupsRowsSelected = [];
	let groupsRowsSelectedTrue = false;
	let groupsAllRowsSelectedTrue = false;

	// Pagination
	let groupsPerPage = 10;
	let groupsTotalPages, groupsTotalSize;
	let groupsCurrentPage = 0;

	// Selection
	let selectedGroupId;
	let selectedGroupName;

	// Tooltip
	let isGroupAdminMouseEnter = false;
	let isTopicAdminMouseEnter = false;
	let isApplicationAdminMouseEnter = false;

	// Search Feature
	$: if (searchString?.trim().length >= searchStringLength) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchGroups(searchString.trim());
		}, waitTime);
	}

	$: if (searchString?.trim().length < searchStringLength) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			reloadAllGroups();
		}, waitTime);
	}

	onMount(async () => {
		headerTitle.set('Groups');
		detailView.set();

		promise = await reloadAllGroups();

		setTimeout(() => renderAvatar.set(true), 40);
	});

	onDestroy(() => renderAvatar.set(false));

	const searchGroups = async (searchStr) => {
		const res = await httpAdapter.get(`/groups?page=0&size=${groupsPerPage}&filter=${searchStr}`);
		if (res.data.content) {
			groups.set(res.data.content);
		} else {
			groups.set([]);
		}
		groupsTotalPages = res.data.totalPages;
		if (res.data.totalSize !== undefined) groupsTotalSize = res.data.totalSize;
		groupsCurrentPage = 0;
	};

	const errorMessage = (errMsg, errObj) => {
		errorMsg = errMsg;
		errorObject = errObj;
		errorMessageVisible = true;
	};

	const errorMessageClear = () => {
		errorMessageVisible = false;
		errorMsg = '';
		errorObject = '';
	};

	const reloadAllGroups = async (page = 0) => {
		try {
			let res;
			if (searchString && searchString.length >= searchStringLength) {
				res = await httpAdapter.get(
					`/groups?page=${page}&size=${groupsPerPage}&filter=${searchString}`
				);
			} else {
				res = await httpAdapter.get(`/groups?page=${page}&size=${groupsPerPage}`);
			}
			if (res.data) {
				groupsTotalPages = res.data.totalPages;
				groupsTotalSize = res.data.totalSize;
			}
			groups.set(res.data.content);
			groupsTotalPages = res.data.totalPages;
			groupsCurrentPage = page;
		} catch (err) {
			userValidityCheck.set(true);

			errorMessage('Error Loading Groups', err.message);
		}
	};

	const addGroup = async (newGroupName) => {
		await httpAdapter
			.post(`/groups/save/`, {
				name: newGroupName
			})
			.catch((err) => {
				addGroupVisible = false;
				const decodedError = decodeError(Object.create(...err.response.data));
				errorMessage('Error Adding Group', errorMessages[decodedError.category][decodedError.code]);
			});

		searchString = '';
		addGroupVisible = false;
		groupsCurrentPage = 0;

		await reloadAllGroups();
	};

	const editGroupName = async (groupId, groupNewName) => {
		await httpAdapter
			.post(`/groups/save/`, {
				id: groupId,
				name: groupNewName
			})
			.catch((err) => {
				errorMessage('Error Editing Group Name', err.message);
			});

		await reloadAllGroups();

		editGroupVisible = false;
	};

	const deleteSelectedGroups = async () => {
		try {
			groupContext.set();
			for (const group of groupsRowsSelected) {
				await httpAdapter.post(`/groups/delete/${group.id}`);
			}
		} catch (err) {
			deleteGroupVisible = false;
			errorMessage('Error Deleting Group', err.response.data);
		}
	};

	const deselectAllGroupsCheckboxes = () => {
		groupsAllRowsSelectedTrue = false;
		groupsRowsSelectedTrue = false;
		groupsRowsSelected = [];
		let checkboxes = document.querySelectorAll('.groups-checkbox');
		checkboxes.forEach((checkbox) => (checkbox.checked = false));
	};

	const numberOfSelectedCheckboxes = () => {
		let checkboxes = document.querySelectorAll('.groups-checkbox');
		checkboxes = Array.from(checkboxes);
		return checkboxes.filter((checkbox) => checkbox.checked === true).length;
	};

	const decodeError = (errorObject) => {
		errorObject = errorObject.code.replaceAll('-', '_');
		const cat = errorObject.substring(0, errorObject.indexOf('.'));
		const code = errorObject.substring(errorObject.indexOf('.') + 1, errorObject.length);
		return { category: cat, code: code };
	};
</script>

<svelte:head>
	<title>Groups | DDS Permissions Manager</title>
	<meta name="description" content="DDS Permissions Manager Groups" />
</svelte:head>

{#key $refreshPage}
	{#if $isAuthenticated}
		{#await promise then _}
			{#if errorMessageVisible}
				<Modal
					title={errorMsg}
					errorMsg={true}
					errorDescription={errorObject}
					closeModalText={'Close'}
					on:cancel={() => {
						errorMessageVisible = false;
						errorMessageClear();
					}}
				/>
			{/if}

			{#if deleteGroupVisible}
				<Modal
					title="Delete {groupsRowsSelected.length > 1 ? 'Groups' : 'Group'}"
					actionDeleteGroups={true}
					on:deleteGroups={async () => {
						await deleteSelectedGroups();
						reloadAllGroups();

						deselectAllGroupsCheckboxes();
						deleteGroupVisible = false;
					}}
					on:cancel={() => {
						if (groupsRowsSelected?.length === 1 && numberOfSelectedCheckboxes() === 0)
							groupsRowsSelected = [];

						deleteGroupVisible = false;
					}}
				/>
			{/if}

			{#if addGroupVisible}
				<Modal
					title="Add New Group"
					actionAddGroup={true}
					groupNewName={true}
					on:addGroup={(e) => addGroup(e.detail.newGroupName)}
					on:cancel={() => (addGroupVisible = false)}
				/>
			{/if}

			{#if editGroupVisible}
				<Modal
					title="Edit Group"
					actionEditGroup={true}
					groupCurrentName={selectedGroupName}
					groupNewName={true}
					groupId={selectedGroupId}
					on:addGroup={(e) => {
						editGroupName(e.detail.groupId, e.detail.newGroupName);
					}}
					on:cancel={() => (editGroupVisible = false)}
				/>
			{/if}

			<div class="content">
				<h1 data-cy="groups">Groups</h1>

				<form class="searchbox">
					<input
						data-cy="search-groups-table"
						class="searchbox"
						type="search"
						placeholder="Search"
						bind:value={searchString}
						on:blur={() => {
							searchString = searchString?.trim();
						}}
						on:keydown={(event) => {
							if (event.which === returnKey) {
								document.activeElement.blur();
								searchString = searchString?.trim();
							}
						}}
					/>
				</form>

				{#if searchString?.length > 0}
					<button
						class="button-blue"
						style="cursor: pointer; width: 4rem; height: 2.1rem"
						on:click={() => (searchString = '')}>Clear</button
					>
				{/if}

				<img
					src={deleteSVG}
					alt="options"
					class="dot"
					class:button-disabled={!$isAdmin || groupsRowsSelected.length === 0}
					style="margin-left: 0.5rem"
					on:click={() => {
						if (groupsRowsSelected.length > 0) deleteGroupVisible = true;
					}}
					on:keydown={(event) => {
						if (event.which === returnKey) {
							if (groupsRowsSelected.length > 0) deleteGroupVisible = true;
						}
					}}
					on:mouseenter={() => {
						deleteMouseEnter = true;
						if ($isAdmin) {
							deleteToolip = 'Select groups to delete';
							if (groupsRowsSelected.length === 0) {
								const tooltip = document.querySelector('#delete-groups');
								setTimeout(() => {
									if (deleteMouseEnter) {
										tooltip.classList.remove('tooltip-hidden');
										tooltip.classList.add('tooltip');
										if ($groupContext === null || $groupContext === undefined)
											tooltip.setAttribute('style', 'margin-left:13.2rem; margin-top: -1.8rem');
										else tooltip.setAttribute('style', 'margin-left:32rem; margin-top: -1.8rem');
									}
								}, 1000);
							}
						} else {
							deleteToolip = 'Super User permission required';
							const tooltip = document.querySelector('#delete-groups');
							setTimeout(() => {
								if (deleteMouseEnter) {
									tooltip.classList.remove('tooltip-hidden');
									tooltip.classList.add('tooltip');
									tooltip.setAttribute('style', 'margin-left:9.2rem; margin-top: -1.8rem');
								}
							}, 1000);
						}
					}}
					on:mouseleave={() => {
						deleteMouseEnter = false;

						if (groupsRowsSelected.length === 0) {
							const tooltip = document.querySelector('#delete-groups');
							setTimeout(() => {
								if (!deleteMouseEnter) {
									tooltip.classList.add('tooltip-hidden');
									tooltip.classList.remove('tooltip');
								}
							}, 1000);
						}
					}}
				/>
				<span
					id="delete-groups"
					class="tooltip-hidden"
					style="margin-left: 33.2rem; margin-top: -1.8rem"
					>{deleteToolip}
				</span>
				<img
					data-cy="add-group"
					src={addSVG}
					alt="options"
					class="dot"
					class:button-disabled={!$isAdmin}
					on:click={() => {
						if ($isAdmin) addGroupVisible = true;
					}}
					on:keydown={(event) => {
						if (event.which === returnKey) {
							addGroupVisible = true;
						}
					}}
					on:mouseenter={() => {
						addMouseEnter = true;
						if (!$isAdmin) {
							addTooltip = 'Super User permission required';
							const tooltip = document.querySelector('#add-applications');
							setTimeout(() => {
								if (addMouseEnter) {
									tooltip.classList.remove('tooltip-hidden');
									tooltip.classList.add('tooltip');
								}
							}, waitTime);
						}
					}}
					on:mouseleave={() => {
						addMouseEnter = false;
						const tooltip = document.querySelector('#add-applications');
						setTimeout(() => {
							if (!addMouseEnter) {
								tooltip.classList.add('tooltip-hidden');
								tooltip.classList.remove('tooltip');
							}
						}, waitTime);
					}}
				/>
				<span
					id="add-applications"
					class="tooltip-hidden"
					style="margin-left: 7rem; margin-top: -1.8rem"
					>{addTooltip}
				</span>

				{#if $groups}
					{#if $groups.length > 0}
						<table
							class="main"
							data-cy="groups-table"
							style="margin-top: 0.5rem; min-width: 33rem; width:max-content"
						>
							<thead>
								<tr style="border-top: 1px solid black; border-bottom: 2px solid">
									{#if $isAdmin}
										<td>
											<input
												tabindex="-1"
												type="checkbox"
												class="groups-checkbox"
												style="margin-right: 0.5rem; vertical-align: middle;"
												bind:indeterminate={groupsRowsSelectedTrue}
												on:click={(e) => {
													if (e.target.checked) {
														groupsRowsSelected = $groups;
														groupsRowsSelectedTrue = false;
														groupsAllRowsSelectedTrue = true;
													} else {
														groupsAllRowsSelectedTrue = false;
														groupsRowsSelectedTrue = false;
														groupsRowsSelected = [];
													}
												}}
												checked={groupsAllRowsSelectedTrue}
											/>
										</td>
									{/if}
									<td style="width: 5rem; text-align:center">Activate</td>
									<td style="min-width: 7rem">Group</td>
									{#if $groupContext?.id}
										<td style="text-align:center">Permissions</td>
										<td style="text-align:center">Create</td>
									{/if}
									<td style="width: 4rem; text-align:center">Users</td>
									<td style="width: 4rem; text-align:center">Topics</td>
									<td style="width: 4rem; text-align:right; padding-right: 1rem">Applications</td>
								</tr>
							</thead>
							<tbody>
								{#each $groups as group, i}
									<tr>
										{#if $isAdmin}
											<td style="width: 2rem">
												<input
													tabindex="-1"
													type="checkbox"
													class="groups-checkbox"
													style="vertical-align: middle;"
													checked={groupsAllRowsSelectedTrue}
													on:change={(e) => {
														if (e.target.checked === true) {
															groupsRowsSelected.push(group);
															// reactive statement
															groupsRowsSelected = groupsRowsSelected;
															groupsRowsSelectedTrue = true;
														} else {
															groupsRowsSelected = groupsRowsSelected.filter(
																(selection) => selection !== group
															);
															if (groupsRowsSelected.length === 0) {
																groupsRowsSelectedTrue = false;
															}
														}
													}}
												/>
											</td>
										{/if}

										<td style="text-align: center; cursor: pointer">
											<img
												data-cy="activate-group-context"
												src={groupSVG}
												alt="select group"
												width="27rem"
												height="27rem"
												style="vertical-align: middle;cursor: pointer"
												class:context-selected={group.name === $groupContext?.name}
												class:context-deselected={group.name !== $groupContext?.name}
												on:click={() => {
													if (!$groupContext) groupContext.set(group);
													else groupContext.set('clear');
												}}
												on:mouseenter={() => {
													activateMouseEnter[i] = true;
													const tooltip = document.querySelector(`#activate-groups${i}`);
													setTimeout(() => {
														if (activateMouseEnter[i]) {
															tooltip.classList.remove('tooltip-hidden');
															tooltip.classList.add('tooltip');
														}
													}, waitTime);
												}}
												on:mouseleave={() => {
													activateMouseEnter[i] = false;
													const tooltip = document.querySelector(`#activate-groups${i}`);
													setTimeout(() => {
														if (!activateMouseEnter[i]) {
															tooltip.classList.add('tooltip-hidden');
															tooltip.classList.remove('tooltip');
														}
													}, waitTime);
												}}
											/>
										</td>
										<td
											style="width: max-content"
											class:highlighted={group.name === $groupContext?.name}
										>
											{group.name}
										</td>

										{#if $groupContext?.name && group.name === $groupContext.name}
											<td>
												<div
													class:permission-badges-table={$groupContext?.id}
													class:permission-badges-table-hidden={!$groupContext?.id}
													style="display:inline-flex; vertical-align:middle; justify-content: center"
												>
													<img
														src={groupSVG}
														alt="Group Admin"
														width="23rem"
														height="23rem"
														style="margin-right: 0.4rem"
														class:permission-badges-green={$permissionBadges?.isGroupAdminInContext ||
															$isAdmin}
														class:permission-badges-grey={!$permissionBadges?.isGroupAdminInContext &&
															!$isAdmin}
														on:mouseenter={() => {
															isGroupAdminMouseEnter = true;
															const tooltip = document.querySelector('#is-group-admin-groups');
															setTimeout(() => {
																if (isGroupAdminMouseEnter) {
																	tooltip.classList.remove('tooltip-hidden');
																	tooltip.classList.add('tooltip');
																}
															}, 1000);
														}}
														on:mouseleave={() => {
															isGroupAdminMouseEnter = false;
															const tooltip = document.querySelector('#is-group-admin-groups');
															setTimeout(() => {
																if (!isGroupAdminMouseEnter) {
																	tooltip.classList.add('tooltip-hidden');
																	tooltip.classList.remove('tooltip');
																}
															}, 1000);
														}}
													/>

													<span
														id="is-group-admin-groups"
														class="tooltip-hidden"
														style="margin-top: 1.8rem; margin-left: -4rem"
														>{$permissionBadges?.isGroupAdminToolip}
													</span>

													<img
														src={topicsSVG}
														alt="Topic Admin"
														width="23rem"
														height="23rem"
														style="margin-right: 0.2rem"
														class:permission-badges-green={$permissionBadges?.isTopicAdminInContext ||
															$isAdmin}
														class:permission-badges-grey={!$permissionBadges?.isTopicAdminInContext &&
															!$isAdmin}
														on:mouseenter={() => {
															isTopicAdminMouseEnter = true;
															const tooltip = document.querySelector('#is-topic-admin-groups');
															setTimeout(() => {
																if (isTopicAdminMouseEnter) {
																	tooltip.classList.remove('tooltip-hidden');
																	tooltip.classList.add('tooltip');
																}
															}, 1000);
														}}
														on:mouseleave={() => {
															isTopicAdminMouseEnter = false;
															const tooltip = document.querySelector('#is-topic-admin-groups');
															setTimeout(() => {
																if (!isTopicAdminMouseEnter) {
																	tooltip.classList.add('tooltip-hidden');
																	tooltip.classList.remove('tooltip');
																}
															}, 1000);
														}}
													/>

													<span
														id="is-topic-admin-groups"
														class="tooltip-hidden"
														style="margin-top: 1.8rem"
														>{$permissionBadges?.isTopicAdminTooltip}
													</span>

													<img
														src={appsSVG}
														alt="Application Admin"
														width="23rem"
														height="23rem"
														class:permission-badges-green={$permissionBadges?.isApplicationAdminInContext ||
															$isAdmin}
														class:permission-badges-grey={!$permissionBadges?.isApplicationAdminInContext &&
															!$isAdmin}
														on:mouseenter={() => {
															isApplicationAdminMouseEnter = true;
															const tooltip = document.querySelector(
																'#is-application-admin-groups'
															);
															setTimeout(() => {
																if (isApplicationAdminMouseEnter) {
																	tooltip.classList.remove('tooltip-hidden');
																	tooltip.classList.add('tooltip');
																}
															}, 1000);
														}}
														on:mouseleave={() => {
															isApplicationAdminMouseEnter = false;
															const tooltip = document.querySelector(
																'#is-application-admin-groups'
															);
															setTimeout(() => {
																if (!isApplicationAdminMouseEnter) {
																	tooltip.classList.add('tooltip-hidden');
																	tooltip.classList.remove('tooltip');
																}
															}, 1000);
														}}
													/>

													<span
														id="is-application-admin-groups"
														class="tooltip-hidden"
														style="margin-top: 1.8rem; margin-left: 3rem"
														>{$permissionBadges?.isApplicationAdminTooltip}
													</span>
												</div>
											</td>
										{:else if $groupContext}
											<td /><td />
										{/if}

										{#if $groupContext?.name && group.name === $groupContext.name}
											<td>
												<div
													class:permission-badges-table={$groupContext?.id}
													class:permission-badges-table-hidden={!$groupContext?.id}
													style="display:inline-flex; vertical-align:middle; justify-content: center"
												>
													<img
														src={groupSVG}
														alt="create new user"
														width="23rem"
														height="23rem"
														style="margin-right: 0.4rem"
														class:permission-badges-blue={$permissionBadges?.isGroupAdminInContext ||
															$isAdmin}
														class:permission-badges-grey={!$permissionBadges?.isGroupAdminInContext &&
															!$isAdmin}
														disabled={!$permissionBadges?.isGroupAdminInContext && !$isAdmin}
														on:mouseenter={() => {
															createUserMouseEnter = true;
															const tooltip = document.querySelector('#user-create');
															setTimeout(() => {
																if (createUserMouseEnter) {
																	tooltip.classList.remove('tooltip-hidden');
																	tooltip.classList.add('tooltip');
																}
															}, 1000);
														}}
														on:mouseleave={() => {
															createUserMouseEnter = false;
															const tooltip = document.querySelector('#user-create');
															setTimeout(() => {
																if (!createUserMouseEnter) {
																	tooltip.classList.add('tooltip-hidden');
																	tooltip.classList.remove('tooltip');
																}
															}, 1000);
														}}
														on:click={() => {
															if ($permissionBadges?.isGroupAdminInContext || $isAdmin) {
																urlparameters.set('create');
																goto(`/users`, true);
															}
														}}
													/>

													<span
														id="user-create"
														class="tooltip-hidden"
														style="margin-top: 1.8rem; margin-left: -3rem"
													>
														{#if $permissionBadges?.isGroupAdminInContext || $isAdmin}
															Create a new User in {group.name}
														{:else if !$permissionBadges?.isGroupAdminInContext && !$isAdmin}
															Group Admin permission required
														{/if}
													</span>

													<img
														src={topicsSVG}
														alt="create new topic"
														width="23rem"
														height="23rem"
														style="margin-right: 0.2rem"
														class:permission-badges-blue={$permissionBadges?.isTopicAdminInContext ||
															$isAdmin}
														class:permission-badges-grey={!$permissionBadges?.isTopicAdminInContext &&
															!$isAdmin}
														disabled={!$permissionBadges?.isTopicAdminInContext && !$isAdmin}
														on:mouseenter={() => {
															createTopicMouseEnter = true;
															const tooltip = document.querySelector('#topic-create');
															setTimeout(() => {
																if (createTopicMouseEnter) {
																	tooltip.classList.remove('tooltip-hidden');
																	tooltip.classList.add('tooltip');
																}
															}, 1000);
														}}
														on:mouseleave={() => {
															createTopicMouseEnter = false;
															const tooltip = document.querySelector('#topic-create');
															setTimeout(() => {
																if (!createTopicMouseEnter) {
																	tooltip.classList.add('tooltip-hidden');
																	tooltip.classList.remove('tooltip');
																}
															}, 1000);
														}}
														on:click={() => {
															if ($permissionBadges?.isTopicAdminInContext || $isAdmin) {
																urlparameters.set('create');
																goto(`/topics`, true);
															}
														}}
													/>

													<span id="topic-create" class="tooltip-hidden" style="margin-top: 1.8rem"
														>{#if $permissionBadges?.isTopicAdminInContext || $isAdmin}
															Create a new Topic in {group.name}
														{:else if !$permissionBadges?.isTopicAdminInContext && !$isAdmin}
															Topic Admin permission required
														{/if}
													</span>

													<img
														src={appsSVG}
														alt="create application"
														width="23rem"
														height="23rem"
														class:permission-badges-blue={$permissionBadges?.isApplicationAdminInContext ||
															$isAdmin}
														class:permission-badges-grey={!$permissionBadges?.isApplicationAdminInContext &&
															!$isAdmin}
														disabled={!$permissionBadges?.isApplicationAdminInContext && !$isAdmin}
														on:mouseenter={() => {
															createApplicationMouseEnter = true;
															const tooltip = document.querySelector('#application-create');
															setTimeout(() => {
																if (createApplicationMouseEnter) {
																	tooltip.classList.remove('tooltip-hidden');
																	tooltip.classList.add('tooltip');
																}
															}, 1000);
														}}
														on:mouseleave={() => {
															createApplicationMouseEnter = false;
															const tooltip = document.querySelector('#application-create');
															setTimeout(() => {
																if (!createApplicationMouseEnter) {
																	tooltip.classList.add('tooltip-hidden');
																	tooltip.classList.remove('tooltip');
																}
															}, 1000);
														}}
														on:click={() => {
															if ($permissionBadges?.isApplicationAdminInContext || $isAdmin) {
																urlparameters.set('create');
																goto(`/applications`, true);
															}
														}}
													/>

													<span
														id="application-create"
														class="tooltip-hidden"
														style="margin-top: 1.8rem; margin-left: 3rem"
														>{#if $permissionBadges?.isApplicationAdminInContext || $isAdmin}
															Create a new Application in {group.name}
														{:else if !$permissionBadges?.isApplicationcAdminInContext && !$isAdmin}
															Application Admin permission required
														{/if}
													</span>
												</div>
											</td>
										{/if}

										<td style="width: max-content">
											<center>
												<a
													tabindex="-1"
													style="vertical-align: middle"
													href="/users"
													on:click={() => groupContext.set(group)}>{group.membershipCount}</a
												>
											</center>
										</td>

										<td style="width: max-content">
											<center>
												<a
													tabindex="-1"
													style="vertical-align: middle"
													href="/topics"
													on:click={() => groupContext.set(group)}>{group.topicCount}</a
												>
											</center>
										</td>

										<td style="width: max-content">
											<center>
												<a
													tabindex="-1"
													style="vertical-align: middle"
													href="/applications"
													on:click={() => groupContext.set(group)}>{group.applicationCount}</a
												>
											</center>
										</td>

										<span
											id="activate-groups{i}"
											class="tooltip-hidden"
											class:activate-no-context={!$groupContext?.id}
											class:activate-context={$groupContext?.id}
											>{activateToolip}
										</span>

										{#if $isAdmin}
											<td style="cursor: pointer; text-align: right; padding-right: 0.25rem">
												<img
													data-cy="edit-group-icon"
													src={editSVG}
													alt="edit group"
													style="cursor: pointer; vertical-align: -0.25rem"
													height="17rem"
													width="17rem"
													on:click={() => {
														editGroupVisible = true;
														selectedGroupId = group.id;
														selectedGroupName = group.name;
													}}
												/>
											</td>

											<td style="cursor: pointer; text-align: right; padding-right: 0.25rem">
												<img
													data-cy="delete-group-icon"
													src={deleteSVG}
													alt="delete group"
													style="cursor: pointer; vertical-align: -0.5rem"
													height="27rem"
													on:click={() => {
														if (!groupsRowsSelected.some((grp) => grp === group))
															groupsRowsSelected.push(group);
														deleteGroupVisible = true;
													}}
												/>
											</td>
										{/if}
									</tr>
								{/each}
							</tbody>
						</table>
					{/if}
				{:else}
					<p>
						No Groups Found.&nbsp;<span class="link" on:click={() => (addGroupVisible = true)}>
							Click here
						</span>
						to create a new Group.
					</p>
				{/if}
			</div>

			<div class="pagination">
				<span>Rows per page</span>
				<select
					tabindex="-1"
					on:change={(e) => {
						groupsPerPage = e.target.value;
						reloadAllGroups();
					}}
					name="RowsPerPage"
				>
					<option value="10">10</option>
					<option value="25">25</option>
					<option value="50">50</option>
					<option value="75">75</option>
					<option value="100">100&nbsp;</option>
				</select>
				<span style="margin: 0 2rem 0 2rem">
					{#if groupsTotalSize > 0}
						{1 + groupsCurrentPage * groupsPerPage}
					{:else}
						0
					{/if}
					- {Math.min(groupsPerPage * (groupsCurrentPage + 1), groupsTotalSize)} of
					{groupsTotalSize}
				</span>
				<img
					src={pagefirstSVG}
					alt="first page"
					class="pagination-image"
					class:disabled-img={groupsCurrentPage === 0}
					on:click={() => {
						deselectAllGroupsCheckboxes();
						if (groupsCurrentPage > 0) {
							groupsCurrentPage = 0;
							reloadAllGroups();
						}
					}}
				/>
				<img
					src={pagebackwardsSVG}
					alt="previous page"
					class="pagination-image"
					class:disabled-img={groupsCurrentPage === 0}
					on:click={() => {
						deselectAllGroupsCheckboxes();
						if (groupsCurrentPage > 0) {
							groupsCurrentPage--;
							reloadAllGroups(groupsCurrentPage);
						}
					}}
				/>
				<img
					src={pageforwardSVG}
					alt="next page"
					class="pagination-image"
					class:disabled-img={groupsCurrentPage + 1 === groupsTotalPages ||
						$groups?.length === undefined}
					on:click={() => {
						deselectAllGroupsCheckboxes();
						if (groupsCurrentPage + 1 < groupsTotalPages) {
							groupsCurrentPage++;
							reloadAllGroups(groupsCurrentPage);
						}
					}}
				/>
				<img
					src={pagelastSVG}
					alt="last page"
					class="pagination-image"
					class:disabled-img={groupsCurrentPage + 1 === groupsTotalPages ||
						$groups?.length === undefined}
					on:click={() => {
						deselectAllGroupsCheckboxes();
						if (groupsCurrentPage < groupsTotalPages) {
							groupsCurrentPage = groupsTotalPages - 1;
							reloadAllGroups(groupsCurrentPage);
						}
					}}
				/>
			</div>
			<p style="margin-top: 8rem">© 2022 Unity Foundation. All rights reserved.</p>
		{/await}
	{/if}
{/key}

<style>
	.content {
		width: fit-content;
		min-width: 32rem;
		margin-right: 1rem;
	}

	.dot {
		float: right;
	}

	.activate-context {
		margin-left: -48.24rem;
		margin-top: -1rem;
	}

	.activate-no-context {
		margin-left: -34.5rem;
		margin-top: -1rem;
	}

	tr {
		line-height: 2.2rem;
	}

	p {
		font-size: large;
	}
</style>
