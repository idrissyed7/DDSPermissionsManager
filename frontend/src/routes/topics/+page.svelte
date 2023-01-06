<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount, onDestroy } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import topics from '../../stores/groups';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import refreshPage from '../../stores/refreshPage';
	import Modal from '../../lib/Modal.svelte';
	import TopicDetails from './TopicDetails.svelte';
	import { goto } from '$app/navigation';
	import { browser } from '$app/env';
	import userValidityCheck from '../../stores/userValidityCheck';
	import headerTitle from '../../stores/headerTitle';
	import detailView from '../../stores/detailView';
	import deleteSVG from '../../icons/delete.svg';
	import detailSVG from '../../icons/detail.svg';
	import addSVG from '../../icons/add.svg';
	import pageforwardSVG from '../../icons/pageforward.svg';
	import pagebackwardsSVG from '../../icons/pagebackwards.svg';
	import pagefirstSVG from '../../icons/pagefirst.svg';
	import pagelastSVG from '../../icons/pagelast.svg';
	import errorMessages from '$lib/errorMessages.json';
	import groupContext from '../../stores/groupContext';
	import showSelectGroupContext from '../../stores/showSelectGroupContext';
	import singleGroupCheck from '../../stores/singleGroupCheck';
	import createItem from '../../stores/createItem';

	export let data;
	export let errors;

	// Group Context
	$: if ($groupContext?.id) reloadAllTopics();

	$: if ($groupContext === 'clear') {
		groupContext.set();
		singleGroupCheck.set();
		selectedGroup = '';
		reloadAllTopics();
	}

	// Permission Badges Create
	$: if ($createItem === 'topic') {
		createItem.set(false);
		addTopicVisible = true;
	}

	// Redirects the User to the Login screen if not authenticated
	$: if (browser) {
		setTimeout(() => {
			if (!$isAuthenticated) goto(`/`, true);
		}, waitTime);
	}

	// Locks the background scroll when modal is open
	$: if (browser && (addTopicVisible || deleteTopicVisible || errorMessageVisible)) {
		document.body.classList.add('modal-open');
	} else if (browser && !(addTopicVisible || deleteTopicVisible || errorMessageVisible)) {
		document.body.classList.remove('modal-open');
	}

	// checkboxes selection
	$: if ($topics?.length === topicsRowsSelected?.length) {
		topicsRowsSelectedTrue = false;
		topicsAllRowsSelectedTrue = true;
	} else if (topicsRowsSelected?.length > 0) {
		topicsRowsSelectedTrue = true;
	} else {
		topicsAllRowsSelectedTrue = false;
	}

	// Messages
	let deleteToolip;
	let deleteMouseEnter = false;

	let addTooltip;
	let addMouseEnter = false;

	// Promises
	let promise;

	// Constants
	const returnKey = 13;
	const waitTime = 1000;
	const searchStringLength = 3;

	// DropDowns
	let topicsDropDownVisible = false;
	let topicsDropDownMouseEnter = false;

	// Tables
	let topicsRowsSelected = [];
	let topicsRowsSelectedTrue = false;
	let topicsAllRowsSelectedTrue = false;
	let topicsPerPage = 10;

	// Search
	let searchString;
	let timer;

	// Authentication
	let isTopicAdmin = false;

	// Error Handling
	let errorMsg, errorObject;

	//Pagination
	let topicsTotalPages, topicsTotalSize;
	let topicsCurrentPage = 0;

	// Modals
	let errorMessageVisible = false;
	let topicsListVisible = true;
	let topicDetailVisible = false;
	let addTopicVisible = false;
	let deleteTopicVisible = false;

	// Selection
	let selectedTopicId;

	// Topic Creation
	let newTopicName = '';
	let searchGroups = '';
	let selectedGroup = '';
	let anyApplicationCanRead;
	let selectedApplicationList = [];

	// Topics Filter Feature
	$: if (searchString?.trim().length >= searchStringLength && $urlparameters === null) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchTopics(searchString.trim());
		}, waitTime);
	}

	$: if (searchString?.trim().length < searchStringLength) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			reloadAllTopics();
		}, waitTime);
	}

	// Return to List view
	$: if ($detailView === 'backToList') {
		headerTitle.set('Topics');
		reloadAllTopics();
		returnToTopicsList();
	}

	const reloadAllTopics = async (page = 0) => {
		try {
			let res;
			if (searchString && searchString.length >= searchStringLength) {
				res = await httpAdapter.get(
					`/topics?page=${page}&size=${topicsPerPage}&filter=${searchString}`
				);
			} else if ($groupContext) {
				res = await httpAdapter.get(
					`/topics?page=${page}&size=${topicsPerPage}&group=${$groupContext.id}`
				);
			} else {
				res = await httpAdapter.get(`/topics?page=${page}&size=${topicsPerPage}`);
			}
			if (res.data) {
				topicsTotalPages = res.data.totalPages;
				topicsTotalSize = res.data.totalSize;
			}
			topics.set(res.data.content);
			topicsCurrentPage = page;
		} catch (err) {
			userValidityCheck.set(true);

			errorMessage('Error Loading Topics', err.message);
		}
	};

	const initializeTopics = async () => {
		promise = await reloadAllTopics();

		if ($permissionsByGroup) {
			isTopicAdmin = $permissionsByGroup?.some(
				(groupPermission) => groupPermission.isTopicAdmin === true
			);
		}
	};

	initializeTopics();

	onMount(async () => {
		if ($urlparameters?.type === 'prepopulate') {
			searchString = $urlparameters.data;
		}

		detailView.set('first run');

		headerTitle.set('Topics');

		if ($urlparameters === 'create') {
			if ($isAdmin || isTopicAdmin) {
				addTopicVisible = true;
			} else {
				errorMessage('Only Topic Admins can add new Topics.', 'Contact your Admin.');
			}
		}
	});

	onDestroy(() => urlparameters.set([]));

	const searchTopics = async (searchStr) => {
		const res = await httpAdapter.get(`/topics?page=0&size=${topicsPerPage}&filter=${searchStr}`);
		if (res.data.content) {
			topics.set(res.data.content);
		} else {
			topics.set([]);
		}
		topicsTotalPages = res.data.totalPages;
		if (res.data.totalSize !== undefined) topicsTotalSize = res.data.totalSize;
		topicsCurrentPage = 0;
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

	const loadTopic = () => {
		addTopicVisible = false;
		topicsListVisible = false;
		topicDetailVisible = true;
	};

	const deleteSelectedTopics = async () => {
		try {
			for (const topic of topicsRowsSelected) {
				await httpAdapter.post(`/topics/delete/${topic.id}`);
			}
		} catch (err) {
			errorMessage('Error Deleting Topic', err.message);
		}
	};

	const returnToTopicsList = () => {
		topicDetailVisible = false;
		topicsListVisible = true;
	};

	const deselectAllTopicsCheckboxes = () => {
		topicsAllRowsSelectedTrue = false;
		topicsRowsSelectedTrue = false;
		topicsRowsSelected = [];
		let checkboxes = document.querySelectorAll('.topics-checkbox');
		checkboxes.forEach((checkbox) => (checkbox.checked = false));
	};

	const numberOfSelectedCheckboxes = () => {
		let checkboxes = document.querySelectorAll('.topics-checkbox');
		checkboxes = Array.from(checkboxes);
		return checkboxes.filter((checkbox) => checkbox.checked === true).length;
	};

	const addTopic = async () => {
		if (!selectedGroup) {
			const groupId = await httpAdapter.get(`/groups?page=0&size=1&filter=${searchGroups}`);
			if (
				groupId.data.content &&
				groupId.data.content[0]?.name.toUpperCase() === searchGroups.toUpperCase()
			) {
				selectedGroup = groupId.data.content[0]?.id;
			}
		}
		const res = await httpAdapter
			.post(`/topics/save/`, {
				name: newTopicName,
				kind: anyApplicationCanRead ? 'B' : 'C',
				group: selectedGroup,
				groupName: searchGroups
			})
			.catch((err) => {
				addTopicVisible = false;
				errorMessage('Error Adding Topic', err.message);
			});

		addTopicVisible = false;
		if (res) {
			selectedTopicId = res.data?.id;
			loadTopic();
		}

		if (res === undefined) {
			errorMessage('Error Adding Topic', errorMessages['topic']['exists']);
		}
	};
</script>

<svelte:head>
	<title>Topics | DDS Permissions Manager</title>
	<meta name="description" content="DDS Permissions Manager Topics" />
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

			{#if addTopicVisible}
				<Modal
					title="Add Topic"
					topicName={true}
					group={true}
					actionAddTopic={true}
					on:cancel={() => (addTopicVisible = false)}
					on:addTopic={(e) => {
						newTopicName = e.detail.newTopicName;
						searchGroups = e.detail.searchGroups;
						selectedGroup = e.detail.selectedGroup;
						anyApplicationCanRead = e.detail.anyApplicationCanRead;
						selectedApplicationList = e.detail.selectedApplicationList;
						addTopic();
					}}
				/>
			{/if}

			{#if topicDetailVisible && !topicsListVisible}
				<TopicDetails
					{selectedTopicId}
					{isTopicAdmin}
					on:addTopic={async (e) => {
						newTopicName = e.detail.newTopicName;
						searchGroups = e.detail.searchGroups;
						selectedGroup = e.detail.selectedGroup;
						anyApplicationCanRead = e.detail.anyApplicationCanRead;
						selectedApplicationList = e.detail.selectedApplicationList;
						addTopic();

						detailView.set();
						await reloadAllTopics();
						topicDetailVisible = false;
					}}
				/>
			{/if}

			{#if deleteTopicVisible}
				<Modal
					actionDeleteTopics={true}
					title="Delete {topicsRowsSelected.length > 1 ? 'Topics' : 'Topic'}"
					on:cancel={() => {
						if (topicsRowsSelected?.length === 1 && numberOfSelectedCheckboxes() === 0)
							topicsRowsSelected = [];
						deleteTopicVisible = false;
					}}
					on:deleteTopics={async () => {
						await deleteSelectedTopics();
						reloadAllTopics();
						deselectAllTopicsCheckboxes();
						deleteTopicVisible = false;
					}}
				/>
			{/if}

			{#if !topicDetailVisible}
				<div class="content">
					<h1 data-cy="topics">Topics</h1>

					<form class="searchbox">
						<input
							data-cy="search-topics-table"
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
						class:button-disabled={(!$isAdmin && !isTopicAdmin) || topicsRowsSelected.length === 0}
						style="margin-left: 0.5rem; margin-right: 1rem"
						on:click={() => {
							if (topicsRowsSelected.length > 0) deleteTopicVisible = true;
						}}
						on:keydown={(event) => {
							if (event.which === returnKey) {
								if (topicsRowsSelected.length > 0) deleteTopicVisible = true;
							}
						}}
						on:mouseenter={() => {
							deleteMouseEnter = true;
							if ($isAdmin || isTopicAdmin) {
								if (topicsRowsSelected.length === 0) {
									deleteToolip = 'Select topics to delete';
									const tooltip = document.querySelector('#delete-topics');
									setTimeout(() => {
										if (deleteMouseEnter) {
											tooltip.classList.remove('tooltip-hidden');
											tooltip.classList.add('tooltip');
										}
									}, 1000);
								}
							} else {
								deleteToolip = 'Topic Admin permissions required';
								const tooltip = document.querySelector('#delete-topics');
								setTimeout(() => {
									if (deleteMouseEnter) {
										tooltip.classList.remove('tooltip-hidden');
										tooltip.classList.add('tooltip');
										tooltip.setAttribute('style', 'margin-left:10.2rem; margin-top: -1.8rem');
									}
								}, 1000);
							}
						}}
						on:mouseleave={() => {
							deleteMouseEnter = false;
							if (topicsRowsSelected.length === 0) {
								const tooltip = document.querySelector('#delete-topics');
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
						id="delete-topics"
						class="tooltip-hidden"
						style="margin-left: 12.2rem; margin-top: -1.8rem"
						>{deleteToolip}
					</span>

					<img
						data-cy="add-topic"
						src={addSVG}
						alt="options"
						class="dot"
						class:button-disabled={(!$isAdmin &&
							!$permissionsByGroup?.find(
								(gm) => gm.groupName === $groupContext?.name && gm.isTopicAdmin === true
							)) ||
							!$groupContext}
						on:click={() => {
							if (
								$groupContext &&
								($isAdmin ||
									$permissionsByGroup?.find(
										(gm) => gm.groupName === $groupContext?.name && gm.isTopicAdmin === true
									))
							) {
								addTopicVisible = true;
							} else if (
								!$groupContext &&
								($permissionsByGroup?.some((gm) => gm.isTopicAdmin === true) || $isAdmin)
							)
								showSelectGroupContext.set(true);
						}}
						on:keydown={(event) => {
							if (event.which === returnKey) {
								if (
									$groupContext &&
									($isAdmin ||
										$permissionsByGroup?.find(
											(gm) => gm.groupName === $groupContext?.name && gm.isTopicAdmin === true
										))
								) {
									addTopicVisible = true;
								} else if (
									!$groupContext &&
									($permissionsByGroup?.some((gm) => gm.isTopicAdmin === true) || $isAdmin)
								)
									showSelectGroupContext.set(true);
							}
						}}
						on:mouseenter={() => {
							addMouseEnter = true;
							if (
								(!$isAdmin &&
									$groupContext &&
									!$permissionsByGroup?.find(
										(gm) => gm.groupName === $groupContext?.name && gm.isTopicAdmin === true
									)) ||
								(!$isAdmin &&
									!$groupContext &&
									!$permissionsByGroup?.some((gm) => gm.isTopicAdmin === true))
							) {
								addTooltip = 'Topic Admin permission required';
								const tooltip = document.querySelector('#add-topics');
								setTimeout(() => {
									if (addMouseEnter) {
										tooltip.classList.remove('tooltip-hidden');
										tooltip.classList.add('tooltip');
									}
								}, waitTime);
							} else if (
								!$groupContext &&
								($permissionsByGroup?.some((gm) => gm.isTopicAdmin === true) || $isAdmin)
							) {
								addTooltip = 'Select a group to add a Topic';
								const tooltip = document.querySelector('#add-topics');
								setTimeout(() => {
									if (addMouseEnter) {
										tooltip.classList.remove('tooltip-hidden');
										tooltip.classList.add('tooltip');
										tooltip.setAttribute('style', 'margin-left:8.6rem; margin-top: -1.8rem');
									}
								}, 1000);
							}
						}}
						on:mouseleave={() => {
							addMouseEnter = false;
							const tooltip = document.querySelector('#add-topics');
							setTimeout(() => {
								if (!addMouseEnter) {
									tooltip.classList.add('tooltip-hidden');
									tooltip.classList.remove('tooltip');
								}
							}, waitTime);
						}}
					/>
					<span
						id="add-topics"
						class="tooltip-hidden"
						style="margin-left: 8rem; margin-top: -1.8rem"
						>{addTooltip}
					</span>

					{#if $topics?.length > 0 && topicsListVisible && !topicDetailVisible}
						<table data-cy="topics-table" class="main" style="margin-top: 0.5rem">
							<thead>
								<tr style="border-top: 1px solid black; border-bottom: 2px solid">
									{#if $permissionsByGroup?.find((gm) => gm.groupName === $groupContext?.name && gm.isTopicAdmin === true) || $isAdmin}
										<td style="line-height: 1rem;">
											<input
												tabindex="-1"
												type="checkbox"
												class="topics-checkbox"
												style="margin-right: 0.5rem"
												bind:indeterminate={topicsRowsSelectedTrue}
												on:click={(e) => {
													topicsDropDownVisible = false;
													if (e.target.checked) {
														topicsRowsSelected = $topics;
														topicsRowsSelectedTrue = false;
														topicsAllRowsSelectedTrue = true;
													} else {
														topicsAllRowsSelectedTrue = false;
														topicsRowsSelectedTrue = false;
														topicsRowsSelected = [];
													}
												}}
												checked={topicsAllRowsSelectedTrue}
											/>
										</td>
									{/if}
									<td style="min-width: 7rem">Topic</td>
									<td>Group</td>
								</tr>
							</thead>
							<tbody>
								{#each $topics as topic, i}
									<tr>
										{#if $permissionsByGroup?.find((gm) => gm.groupName === $groupContext?.name && gm.isTopicAdmin === true) || $isAdmin}
											<td style="line-height: 1rem; width: 2rem; ">
												<input
													tabindex="-1"
													type="checkbox"
													class="topics-checkbox"
													checked={topicsAllRowsSelectedTrue}
													on:change={(e) => {
														topicsDropDownVisible = false;
														if (e.target.checked === true) {
															topicsRowsSelected.push(topic);
															// reactive statement
															topicsRowsSelected = topicsRowsSelected;
															topicsRowsSelectedTrue = true;
														} else {
															topicsRowsSelected = topicsRowsSelected.filter(
																(selection) => selection !== topic
															);
															if (topicsRowsSelected.length === 0) {
																topicsRowsSelectedTrue = false;
															}
														}
													}}
												/>
											</td>
										{/if}

										<td
											style="cursor: pointer; width: max-content"
											on:click={() => {
												selectedTopicId = topic.id;
												loadTopic();
											}}
											on:keydown={(event) => {
												if (event.which === returnKey) {
													selectedTopicId = topic.id;
													loadTopic();
												}
											}}
											>{topic.name}
										</td>

										<td style="padding-left: 0.5rem">{topic.groupName}</td>

										<td style="cursor: pointer; width:1rem">
											<img
												data-cy="detail-application-icon"
												src={detailSVG}
												height="18rem"
												width="18rem"
												alt="edit user"
												style="vertical-align: -0.2rem"
												on:click={() => {
													selectedTopicId = topic.id;
													loadTopic();
												}}
												on:keydown={(event) => {
													if (event.which === returnKey) {
														selectedTopicId = topic.id;
														loadTopic();
													}
												}}
											/>
										</td>

										<td
											style="cursor: pointer; text-align: right; padding-right: 0.25rem; width: 1rem"
										>
											<img
												data-cy="delete-topic-icon"
												src={deleteSVG}
												width="27px"
												height="27px"
												style="vertical-align: -0.45rem"
												alt="delete topic"
												disabled={!$isAdmin || !isTopicAdmin}
												on:click={() => {
													if (!topicsRowsSelected.some((tpc) => tpc === topic))
														topicsRowsSelected.push(topic);
													deleteTopicVisible = true;
												}}
											/>
										</td>
									</tr>
								{/each}
							</tbody>
						</table>
					{:else if !topicDetailVisible}
						<p>
							No Topics Found.
							<br />
							{#if $groupContext && ($permissionsByGroup?.find((gm) => gm.groupName === $groupContext?.name && gm.isTopicAdmin === true) || $isAdmin)}
								<span
									class="link"
									on:click={() => {
										if (
											$groupContext &&
											($permissionsByGroup?.find(
												(gm) => gm.groupName === $groupContext?.name && gm.isTopicAdmin === true
											) ||
												$isAdmin)
										)
											addTopicVisible = true;
										else if (
											!$groupContext &&
											($permissionsByGroup?.some((gm) => gm.isTopicAdmin === true) || $isAdmin)
										)
											showSelectGroupContext.set(true);
									}}
								>
									Click here
								</span>
								to create a new Topic.
							{:else if !$groupContext && ($permissionsByGroup?.some((gm) => gm.isTopicAdmin === true) || $isAdmin)}
								Select a group to get started.
							{/if}
						</p>
					{/if}
				</div>

				{#if topicsTotalSize !== undefined && topicsTotalSize != NaN}
					<div class="pagination">
						<span>Rows per page</span>
						<select
							tabindex="-1"
							on:change={(e) => {
								topicsPerPage = e.target.value;
								reloadAllTopics();
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
							{#if topicsTotalSize > 0}
								{1 + topicsCurrentPage * topicsPerPage}
							{:else}
								0
							{/if}
							- {Math.min(topicsPerPage * (topicsCurrentPage + 1), topicsTotalSize)} of
							{topicsTotalSize}
						</span>

						<img
							src={pagefirstSVG}
							alt="first page"
							class="pagination-image"
							class:disabled-img={topicsCurrentPage === 0}
							on:click={() => {
								deselectAllTopicsCheckboxes();
								if (topicsCurrentPage > 0) {
									topicsCurrentPage = 0;
									reloadAllTopics();
								}
							}}
						/>
						<img
							src={pagebackwardsSVG}
							alt="previous page"
							class="pagination-image"
							class:disabled-img={topicsCurrentPage === 0}
							on:click={() => {
								deselectAllTopicsCheckboxes();
								if (topicsCurrentPage > 0) {
									topicsCurrentPage--;
									reloadAllTopics(topicsCurrentPage);
								}
							}}
						/>
						<img
							src={pageforwardSVG}
							alt="next page"
							class="pagination-image"
							class:disabled-img={topicsCurrentPage + 1 === topicsTotalPages ||
								$topics?.length === undefined}
							on:click={() => {
								deselectAllTopicsCheckboxes();
								if (topicsCurrentPage + 1 < topicsTotalPages) {
									topicsCurrentPage++;
									reloadAllTopics(topicsCurrentPage);
								}
							}}
						/>
						<img
							src={pagelastSVG}
							alt="last page"
							class="pagination-image"
							class:disabled-img={topicsCurrentPage + 1 === topicsTotalPages ||
								$topics?.length === undefined}
							on:click={() => {
								deselectAllTopicsCheckboxes();
								if (topicsCurrentPage < topicsTotalPages) {
									topicsCurrentPage = topicsTotalPages - 1;
									reloadAllTopics(topicsCurrentPage);
								}
							}}
						/>
					</div>
					<p style="margin-top: 8rem">© 2022 Unity Foundation. All rights reserved.</p>
				{/if}
			{/if}
		{/await}
	{/if}
{/key}

<style>
	table.main {
		min-width: 34rem;
		line-height: 2.2rem;
	}

	.dot {
		float: right;
	}

	.content {
		width: 100%;
		min-width: fit-content;
		margin-right: 1rem;
	}

	p {
		font-size: large;
	}
</style>
