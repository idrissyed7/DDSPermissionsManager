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
	import addSVG from '../../icons/add.svg';
	import pageforwardSVG from '../../icons/pageforward.svg';
	import pagebackwardsSVG from '../../icons/pagebackwards.svg';
	import pagefirstSVG from '../../icons/pagefirst.svg';
	import pagelastSVG from '../../icons/pagelast.svg';
	import threedotsSVG from '../../icons/threedots.svg';
	import errorMessages from '$lib/errorMessages.json';
	import renderAvatar from '../../stores/renderAvatar';

	export let data, errors;

	// Redirects the User to the Login screen if not authenticated
	$: if (browser) {
		setTimeout(() => {
			if (!$isAuthenticated) goto(`/`, true);
		}, waitTime);
	}

	$: if (browser && (addTopicVisible || deleteTopicVisible || errorMessageVisible)) {
		document.body.classList.add('modal-open');
	} else if (browser && !(addTopicVisible || deleteTopicVisible || errorMessageVisible)) {
		document.body.classList.remove('modal-open');
	}

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

	onMount(async () => {
		if ($urlparameters?.type === 'prepopulate') {
			searchString = $urlparameters.data;
		}

		detailView.set('first run');

		headerTitle.set('Topics');
		promise = reloadAllTopics();

		setTimeout(() => renderAvatar.set(true), 40);

		if ($permissionsByGroup) {
			isTopicAdmin = $permissionsByGroup.some(
				(groupPermission) => groupPermission.isTopicAdmin === true
			);
		}

		if ($urlparameters === 'create') {
			if ($isAdmin || isTopicAdmin) {
				addTopicVisible = true;
			} else {
				errorMessage('Only Topic Admins can create topics.', 'Contact your Group Admin.');
			}
		}
	});

	onDestroy(() => {
		renderAvatar.set(false);
		urlparameters.set([]);
	});

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
		errorMsg = '';
		errorObject = '';
		errorMessageVisible = false;
	};

	const loadTopic = () => {
		addTopicVisible = false;
		topicsListVisible = false;
		topicDetailVisible = true;
	};

	const deleteSelectedTopics = async () => {
		try {
			for (const topic of topicsRowsSelected) {
				await httpAdapter.post(`/topics/delete/${topic.id}`, {
					id: topic.id
				});
			}
		} catch (err) {
			errorMessage('Error Deleting Topic', err.message);
		}
	};

	const returnToTopicsList = () => {
		topicDetailVisible = false;
		topicsListVisible = true;
	};

	const reloadAllTopics = async (page = 0) => {
		try {
			let res;
			if (searchString && searchString.length >= searchStringLength) {
				res = await httpAdapter.get(
					`/topics?page=${page}&size=${topicsPerPage}&filter=${searchString}`
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

	const decodeError = (errorObject) => {
		errorObject = errorObject.code.replaceAll('-', '_');
		const cat = errorObject.substring(0, errorObject.indexOf('.'));
		const code = errorObject.substring(errorObject.indexOf('.') + 1, errorObject.length);
		return { category: cat, code: code };
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

					{#if isTopicAdmin || $isAdmin}
						<div
							data-cy="dot-topics"
							class="dot"
							tabindex="0"
							on:mouseleave={() => {
								setTimeout(() => {
									if (!topicsDropDownMouseEnter) topicsDropDownVisible = false;
								}, waitTime);
							}}
							on:click={() => {
								if (!deleteTopicVisible && !addTopicVisible)
									topicsDropDownVisible = !topicsDropDownVisible;
							}}
							on:keydown={(event) => {
								if (event.which === returnKey) {
									if (!deleteTopicVisible && !addTopicVisible)
										topicsDropDownVisible = !topicsDropDownVisible;
								}
							}}
							on:focusout={() => {
								setTimeout(() => {
									if (!topicsDropDownMouseEnter) topicsDropDownVisible = false;
								}, waitTime);
							}}
						>
							<img src={threedotsSVG} alt="options" style="scale:50%" />

							{#if topicsDropDownVisible}
								<table
									class="dropdown"
									on:mouseenter={() => (topicsDropDownMouseEnter = true)}
									on:mouseleave={() => {
										setTimeout(() => {
											if (!topicsDropDownMouseEnter) topicsDropDownVisible = false;
										}, waitTime);
										topicsDropDownMouseEnter = false;
									}}
								>
									<tr
										tabindex="0"
										disabled={!$isAdmin}
										class:disabled={!$isAdmin || topicsRowsSelected.length === 0}
										on:click={() => {
											topicsDropDownVisible = false;
											if (topicsRowsSelected.length > 0) deleteTopicVisible = true;
										}}
										on:keydown={(event) => {
											if (event.which === returnKey) {
												topicsDropDownVisible = false;
												if (topicsRowsSelected.length > 0) deleteTopicVisible = true;
											}
										}}
										on:focus={() => (topicsDropDownMouseEnter = true)}
									>
										<td>Delete Selected {topicsRowsSelected.length > 1 ? 'Topics' : 'Topic'} </td>
										<td>
											<img
												src={deleteSVG}
												alt="delete topic"
												height="35rem"
												style="vertical-align: -0.8rem"
												class:disabled-img={!$isAdmin || topicsRowsSelected.length === 0}
											/>
										</td>
									</tr>

									<tr
										data-cy="add-topic"
										tabindex="0"
										on:click={() => {
											topicsDropDownVisible = false;
											addTopicVisible = true;
										}}
										on:keydown={(event) => {
											if (event.which === returnKey) {
												topicsDropDownVisible = false;
												addTopicVisible = true;
											}
										}}
										on:focusout={() => (topicsDropDownMouseEnter = false)}
										class:hidden={addTopicVisible}
									>
										<td style="border-bottom-color: transparent">Add New Topic</td>
										<td
											style="width: 0.1rem; height: 2.2rem; padding-left: 0; vertical-align: middle;border-bottom-color: transparent"
										>
											<img
												src={addSVG}
												alt="add user"
												height="27rem"
												style="vertical-align: middle; margin-left: 1.25rem"
											/>
										</td>
									</tr>
								</table>
							{/if}
						</div>
					{/if}

					{#if $topics && $topics.length > 0 && topicsListVisible && !topicDetailVisible}
						<table data-cy="topics-table" class="main" style="margin-top: 0.5rem">
							<thead>
								<tr style="border-top: 1px solid black; border-bottom: 2px solid">
									{#if isTopicAdmin || $isAdmin}
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
									<td>Topic</td>
									<td style="text-align:right; padding-right: 1rem;">Group</td>
								</tr>
							</thead>
							<tbody>
								{#each $topics as topic, i}
									<tr>
										{#if isTopicAdmin || $isAdmin}
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

										<td style="text-align:right; padding-right: 1rem">{topic.groupName}</td>

										{#if $isAdmin || $permissionsByGroup?.find((Topic) => Topic.groupId === topic.group && Topic.isTopicAdmin === true)}
											<td style="cursor: pointer; text-align: right; padding-right: 0.25rem">
												<img
													data-cy="delete-topic-icon"
													src={deleteSVG}
													width="27px"
													height="27px"
													style="vertical-align: -0.45rem"
													alt="delete topic"
													on:click={() => {
														if (!topicsRowsSelected.some((tpc) => tpc === topic))
															topicsRowsSelected.push(topic);
														deleteTopicVisible = true;
													}}
												/>
											</td>
										{/if}
									</tr>
								{/each}
							</tbody>
						</table>
					{:else if !topicDetailVisible}
						<p>No Topics Found</p>
					{/if}
				</div>

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

	.dropdown {
		margin-top: 8.5rem;
		margin-right: 8.5rem;
		width: 13rem;
	}

	.content {
		width: fit-content;
		min-width: 32rem;
		margin-right: 1rem;
	}

	p {
		font-size: large;
	}
</style>
