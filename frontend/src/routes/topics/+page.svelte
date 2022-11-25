<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import topics from '../../stores/groups';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import Modal from '../../lib/Modal.svelte';
	import TopicDetails from './TopicDetails.svelte';
	import { goto } from '$app/navigation';
	import { browser } from '$app/env';
	import headerTitle from '../../stores/headerTitle';
	import detailView from '../../stores/detailView';
	import deleteSVG from '../../icons/delete.svg';
	import addSVG from '../../icons/add.svg';
	import pageforwardSVG from '../../icons/pageforward.svg';
	import pagebackwardsSVG from '../../icons/pagebackwards.svg';
	import pagefirstSVG from '../../icons/pagefirst.svg';
	import pagelastSVG from '../../icons/pagelast.svg';
	import threedotsSVG from '../../icons/threedots.svg';

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

	// Constants
	const returnKey = 13;
	const waitTime = 250;
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
	let selectedTopicId,
		selectedTopicName,
		selectedTopicKind,
		selectedTopicGroupName,
		selectedTopicGroupId;

	// Topic Creation
	let newTopicName = '';
	let searchGroups = '';
	let selectedGroup = '';
	let anyApplicationCanRead;
	let selectedApplicationList = [];

	// Topics Filter Feature
	$: if (searchString?.trim().length >= searchStringLength) {
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
	$: if (!$detailView) {
		headerTitle.set('Topics');
		topicDetailVisible = false;
		reloadAllTopics();
		returnToTopicsList();
	}

	onMount(async () => {
		headerTitle.set('Topics');
		detailView.set();

		try {
			reloadAllTopics();
			const res = await httpAdapter.get(`/token_info`);
			permissionsByGroup.set(res.data.permissionsByGroup);

			if ($permissionsByGroup) {
				isTopicAdmin = $permissionsByGroup.some(
					(groupPermission) => groupPermission.isTopicAdmin === true
				);
			}
		} catch (err) {
			errorMessage('Error Loading Topics', err.message);
		}

		if ($urlparameters === 'create') {
			if ($isAdmin || isTopicAdmin) {
				addTopicVisible = true;
			} else {
				errorMessage('Only Topic Admins can create topics.', 'Contact your Group Admin.');
			}
			urlparameters.set([]);
		}

		if ($urlparameters?.type === 'prepopulate') {
			searchString = $urlparameters.data;
			urlparameters.set([]);
		}
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
				if (err.response.status) err.message = 'Topic already exists. Topic name should be unique.';
				errorMessage('Error Adding Topic', err.message);
			});

		if (res) {
			const createdTopicId = res.data.id;
			addTopicApplicationAssociation(createdTopicId);
		}
		if (res === undefined) {
			errorMessage('Error Adding Topic', 'Topic already exists.');
		}

		addTopicVisible = false;
		await reloadAllTopics();
	};

	const addTopicApplicationAssociation = async (topicId, reload = false) => {
		if (selectedApplicationList && selectedApplicationList.length > 0 && !reload) {
			selectedApplicationList.forEach(async (app) => {
				try {
					await httpAdapter.post(
						`/application_permissions/${app.applicationId}/${topicId}/${app.accessType}`
					);
				} catch (err) {
					errorMessage('Error Adding Applications to the Topic', err.message);
				}
			});
		}
		if (reload) {
			await httpAdapter
				.post(
					`/application_permissions/${selectedApplicationList.id}/${topicId}/${selectedApplicationList.accessType}`
				)
				.then(() => loadApplicationPermissions(topicId));
		}
	};

	const loadApplicationPermissions = async (topicId) => {
		const resApps = await httpAdapter.get(`/application_permissions/?topic=${topicId}`);
		selectedTopicApplications = resApps.data.content;
	};
</script>

<svelte:head>
	<title>Topics | DDS Permissions Manager</title>
	<meta name="description" content="DDS Permissions Manager Topics" />
</svelte:head>

{#if $isAuthenticated}
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
			application={true}
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
			{selectedTopicName}
			{selectedTopicId}
			{selectedTopicKind}
			{selectedTopicGroupName}
			{selectedTopicGroupId}
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
			on:cancel={() => (deleteTopicVisible = false)}
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
			<h1>Topics</h1>

			<form class="searchbox">
				<input
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

			{#if ($permissionsByGroup && $permissionsByGroup.some((groupPermission) => groupPermission.isTopicAdmin === true)) || $isAdmin}
				<div
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
									topicsDropDownVisible = !topicsDropDownVisible;
									topicsDropDownMouseEnter = false;
								}, waitTime);
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
								tabindex="0"
								on:keydown={(event) => {
									if (event.which === returnKey) {
										topicsDropDownVisible = false;
										addTopicVisible = true;
									}
								}}
								on:click={() => {
									topicsDropDownVisible = false;
									addTopicVisible = true;
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
				<table class="main" style="margin-top: 0.5rem">
					<tr style="border-top: 1px solid black; border-bottom: 2px solid">
						{#if ($permissionsByGroup && $permissionsByGroup.some((groupPermission) => groupPermission.isTopicAdmin === true)) || $isAdmin}
							<td>
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
						<td style="line-height: 2.2rem">Topic</td>
						<td>Group</td>
					</tr>
					{#each $topics as topic, i}
						<tr>
							{#if ($permissionsByGroup && $permissionsByGroup.some((groupPermission) => groupPermission.isTopicAdmin === true)) || $isAdmin}
								<td style="width: 2rem">
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
								style="line-height: 2.2rem; cursor: pointer; width: 20.8rem"
								on:click={() => {
									loadTopic(topic.id);
									selectedTopicId = topic.id;
								}}
								on:keydown={(event) => {
									if (event.which === returnKey) {
										loadTopic(topic.id);
										selectedTopicId = topic.id;
									}
								}}
								>{topic.name}
							</td>
							<td style="width: fit-content">{topic.groupName}</td>
							{#if $isAdmin || $permissionsByGroup.find((Topic) => Topic.groupId === topic.group && Topic.isTopicAdmin === true)}
								<td style="cursor: pointer; text-align: right; padding-right: 0.25rem">
									<img
										src={deleteSVG}
										width="27rem"
										alt="delete user"
										style="vertical-align: -0.5rem; margin-left: 2rem"
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
				class:disabled-img={topicsCurrentPage + 1 === topicsTotalPages}
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
				class:disabled-img={topicsCurrentPage + 1 === topicsTotalPages}
				on:click={() => {
					deselectAllTopicsCheckboxes();
					if (topicsCurrentPage < topicsTotalPages) {
						topicsCurrentPage = topicsTotalPages - 1;
						reloadAllTopics(topicsCurrentPage);
					}
				}}
			/>
		</div>
	{/if}
{/if}

<style>
	table.main {
		min-width: 25rem;
		width: fit-content;
	}

	tr {
		height: 2.2rem;
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
		min-width: 25rem;
		width: fit-content;
	}
</style>
