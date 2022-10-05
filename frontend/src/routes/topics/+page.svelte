<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import topics from '../../stores/groups';
	import topicDetails from '../../stores/groupDetails';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import Modal from '../../lib/Modal.svelte';

	export let data, errors;

	// Search
	let searchString;
	const searchStringLength = 3;
	let timer;
	const waitTime = 500;

	// Search Groups
	let searchGroups;
	let searchGroupResults;
	let searchGroupsResultsVisible = false;
	let searchGroupActive = true;

	// Search Applications
	let searchApplications;
	let searchApplicationActive = true;
	let searchApplicationResults;
	let searchApplicationsResultsVisible = false;

	// Authentication
	let isTopicAdmin = false;

	// Error Handling
	let errorMsg, errorObject;

	//Pagination
	const topicsPerPage = 10;
	let topicsTotalPages;
	let topicsCurrentPage = 0;

	// Forms
	let anyApplicationCanRead = false;
	let selectedGroup;
	// let selectedApp;
	const applicationsResult = 7;

	// Modals
	let errorMessageVisible = false;
	let topicsListVisible = true;
	let topicDetailVisible = false;
	let addTopicVisible = false;
	let confirmDeleteVisible = false;

	// Validation
	// let disabled = false;

	// Group Name
	let newTopicName;
	// let editTopicName;

	// Selection
	let selectedTopicId,
		selectedTopicName,
		selectedTopicKind,
		selectedTopicGroupName,
		selectedTopicGroupId;
	let selectedTopicApplications = [];
	let selectedApplicationList = [];

	// Search Groups Feature
	$: if (searchGroups?.trim().length >= 3 && searchGroupActive) {
		searchGroup(searchGroups.trim());
	} else {
		searchGroupsResultsVisible = false;
	}

	// Search Groups Dropdown Visibility
	$: if (searchGroupResults?.data?.content?.length >= 1 && searchGroupActive) {
		searchGroupsResultsVisible = true;
	} else {
		searchGroupsResultsVisible = false;
	}

	// Reset add group form once closed
	$: if (addTopicVisible === false) {
		searchGroups = '';
		searchApplications = '';
		newTopicName = '';
		selectedApplicationList = [];
		anyApplicationCanRead = false;
	}

	// Search Applications Feature
	$: if (searchApplications?.trim().length >= 3 && searchApplicationActive) {
		searchApplication(searchApplications.trim());
	} else {
		searchApplicationsResultsVisible = false;
	}

	// Search Applications Dropdown Visibility
	$: if (searchApplicationResults?.data?.length >= 1 && searchApplicationActive) {
		searchApplicationsResultsVisible = true;
	} else {
		searchApplicationsResultsVisible = false;
	}

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

	onMount(async () => {
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

	const searchGroup = async (searchString) => {
		setTimeout(async () => {
			searchGroupResults = await httpAdapter.get(`/groups?page=0&size=7&filter=${searchString}`);
		}, 1000);
	};

	const selectedSearchGroup = (groupName, groupId) => {
		selectedGroup = groupId;
		searchGroups = groupName;
		searchGroupsResultsVisible = false;
		searchGroupActive = false;
	};

	const searchApplication = async (searchString) => {
		setTimeout(async () => {
			searchApplicationResults = await httpAdapter.get(
				`/applications/search?page=0&size=${applicationsResult}&filter=${searchString}`
			);
		}, 1000);
	};

	const selectedSearchApplication = (appName, appId, appAccessType) => {
		selectedApplicationList.push({ id: appId, name: appName, accessType: appAccessType });
		// This statement is used to trigger Svelte reactivity and re-render the component
		selectedApplicationList = selectedApplicationList;
		// selectedApp = appId;
		searchApplications = appName;
		searchApplicationsResultsVisible = false;
		searchApplicationActive = false;
	};

	const searchTopics = async (searchStr) => {
		const res = await httpAdapter.get(`/topics?page=0&size=${topicsPerPage}&filter=${searchStr}`);
		if (res.data.content) {
			topics.set(res.data.content);
		} else {
			topics.set([]);
		}
		topicsTotalPages = res.data.totalPages;
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

	const loadTopic = async (topicId) => {
		addTopicVisible = false;
		topicsListVisible = false;
		topicDetailVisible = true;
		try {
			const res = await httpAdapter.get(`/topics/show/${topicId}`);
			topicDetails.set(res.data);

			const resApps = await httpAdapter.get(`/application_permissions/?topic=${topicId}`);
			selectedTopicApplications = resApps.data.content;
			console.log('selectedTopicApplications', selectedTopicApplications);
			selectedTopicId = $topicDetails.id;
			selectedTopicName = $topicDetails.name;
			selectedTopicGroupName = $topicDetails.groupName;
			selectedTopicGroupId = $topicDetails.groupId;
			selectedTopicKind = $topicDetails.kind;
		} catch (err) {
			errorMessage('Error Loading Topic Details', err.message);
		}
	};

	const deleteTopic = async () => {
		confirmDeleteVisible = false;

		const res = await httpAdapter
			.post(`/topics/delete/${selectedTopicId}`, {
				id: selectedTopicId
			})
			.catch((err) => {
				errorMessage('Error Deleting Topic', err.message);
			});

		returnToTopicsList();
		await reloadAllTopics().then(() => {
			if (topicsCurrentPage === topicsTotalPages) topicsCurrentPage--;
		});
	};

	const addTopic = async () => {
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

		const createdTopicId = res.data.id;
		addTopicApplicationAssociations(createdTopicId);

		addTopicVisible = false;

		await reloadAllTopics().then(() => {
			topicsCurrentPage = topicsTotalPages - 1;
		});
	};

	const addTopicApplicationAssociations = (topicId) => {
		let access = 'READ';
		selectedApplicationList.forEach(async (app) => {
			await httpAdapter.post(`/application_permissions/${app.id}/${topicId}/${access}`);
		});
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
			topics.set(res.data.content);
			topicsTotalPages = res.data.totalPages;
			topicsCurrentPage = page;
		} catch (err) {
			errorMessage('Error Loading Topics', err.message);
		}
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
			description={errorObject}
			on:cancel={() => {
				errorMessageVisible = false;
				errorMessageClear();
			}}
			><br /><br />
			<div class="confirm">
				<button
					class="button-delete"
					on:click={() => {
						errorMessageVisible = false;
						errorMessageClear();
					}}>Ok</button
				>
			</div>
		</Modal>
	{/if}

	{#if confirmDeleteVisible && !errorMessageVisible}
		<Modal
			title="Delete Topic {selectedTopicName}?"
			on:cancel={() => (confirmDeleteVisible = false)}
		>
			<div class="confirm">
				<button class="button-cancel" on:click={() => (confirmDeleteVisible = false)}>Cancel</button
				>&nbsp;
				<button class="button-delete" on:click={() => deleteTopic()}><span>Delete</span></button>
			</div>
		</Modal>
	{/if}

	<div class="content">
		<h1>Topics</h1>
		{#if topicsListVisible && !topicDetailVisible}
			<center>
				<input
					style="border-width: 1px; width: 20rem; text-align: center; height: 1.7rem"
					placeholder="Search"
					bind:value={searchString}
					on:blur={() => {
						searchString = searchString?.trim();
					}}
					on:keydown={(event) => {
						if (event.which === 13) {
							document.activeElement.blur();
							searchString = searchString?.trim();
						}
					}}
				/>&nbsp; &#x1F50E;
			</center>
		{/if}
		{#if $topics}
			{#if $topics.length > 0 && topicsListVisible && !topicDetailVisible}
				<table align="center">
					{#if $topics.length > 0}
						<tr style="border-width: 0px">
							<th><strong>Topic</strong></th>
						</tr>
						{#each $topics as topic}
							<tr>
								<td
									style="line-height: 1.7rem;"
									class="topic-td"
									on:click={() => {
										loadTopic(topic.id);
										selectedTopicId = topic.id;
									}}>{topic.name}</td
								>

								{#if $isAdmin || $permissionsByGroup.some((permissionTopic) => permissionTopic.isTopicAdmin === true)}
									<td>
										<button
											class="button-delete"
											style="float: right"
											on:click={() => {
												selectedTopicId = topic.id;
												selectedTopicName = topic.name;
												confirmDeleteVisible = true;
											}}
											><span>Delete</span>
										</button>
									</td>
								{:else}
									<td /><td />
								{/if}

								{#if $isAdmin || isTopicAdmin}
									<td />
								{/if}
							</tr>
						{/each}
					{/if}
				</table>
				<br /> <br />

				<center
					><button
						on:click={async () => {
							if (topicsCurrentPage > 0) topicsCurrentPage--;
							reloadAllTopics(topicsCurrentPage);
						}}
						class="button-pagination"
						style="width: 4.8rem; border-bottom-left-radius:9px; border-top-left-radius:9px;"
						disabled={topicsCurrentPage === 0}>Previous</button
					>
					{#if topicsTotalPages > 2}
						{#each Array.apply(null, { length: topicsTotalPages }).map(Number.call, Number) as page}
							<button
								on:click={() => {
									topicsCurrentPage = page;
									reloadAllTopics(page);
								}}
								class="button-pagination"
								class:button-pagination-selected={page === topicsCurrentPage}>{page + 1}</button
							>
						{/each}
					{/if}

					<button
						on:click={async () => {
							if (topicsCurrentPage + 1 < topicsTotalPages) topicsCurrentPage++;
							reloadAllTopics(topicsCurrentPage);
						}}
						class="button-pagination"
						style="width: 3.1rem; border-bottom-right-radius:9px; border-top-right-radius:9px;"
						disabled={topicsCurrentPage === topicsTotalPages - 1 || topicsTotalPages === 0}
						>Next</button
					></center
				>
			{:else if !topicDetailVisible}
				<center><p>No Topics Found</p></center>
			{/if}

			{#if topicsListVisible && !topicDetailVisible && !addTopicVisible}
				<br /><br />
				<center>
					<button class="button" on:click={() => (addTopicVisible = true)}>Add Topic </button>
				</center>
			{/if}
		{/if}

		{#if addTopicVisible && !errorMessageVisible}
			<table class="new-topic" align="center" style="margin-top: 2rem">
				<center>
					<tr style="border-width: 0px;">
						<div class="add-item">
							<td>
								<label for="groups">Name:</label>
							</td>
							<td>
								<input
									placeholder="Topic Name"
									style="width: 14rem; padding-left: 0.3rem"
									bind:value={newTopicName}
								/>
							</td>
						</div>
					</tr>

					<tr style="border-width: 0px;">
						<div class="add-item">
							<td>
								<label for="groups">Group:</label>
							</td>
							<td>
								<input
									placeholder="Group Name"
									style="width: 13.9rem; padding-left: 0.3rem"
									bind:value={searchGroups}
									on:blur={() => {
										setTimeout(() => {
											searchGroupsResultsVisible = false;
										}, 500);
									}}
									on:click={async () => {
										searchGroupResults = [];
										searchGroupActive = true;
										if (searchGroups?.length >= 3) {
											searchGroup(searchGroups);
										}
									}}
								/>
							</td>

							{#if searchGroupsResultsVisible}
								<table class="searchGroup">
									{#each searchGroupResults.data.content as result}
										<tr>
											<td
												on:click={() => {
													selectedSearchGroup(result.name, result.id);
												}}
												>{result.name}
											</td>
										</tr>
									{/each}
								</table>
							{/if}
						</div>
					</tr>
					<tr style="border-width: 0px;">
						<div class="add-item">
							<td>
								<label for="anyApplicationCanRead">Any application can read this topic:</label>
							</td>
							<td>
								<input type="checkbox" bind:checked={anyApplicationCanRead} />
							</td>
						</div>
					</tr>

					<tr style="border-width: 0px;">
						<div class="add-item">
							<td>
								<label for="applications">Application:</label>
							</td>
							<td>
								<input
									placeholder="Application Name"
									style="width: 11.8rem; padding-left: 0.3rem"
									bind:value={searchApplications}
									on:blur={() => {
										setTimeout(() => {
											searchApplicationsResultsVisible = false;
										}, 500);
									}}
									on:click={async () => {
										searchApplicationResults = [];
										searchApplicationActive = true;
										if (searchApplications?.length >= 3) {
											searchApplication(searchApplications);
										}
									}}
								/>
							</td>

							{#if searchApplicationsResultsVisible}
								<table class="searchApplication">
									{#each searchApplicationResults.data as result}
										<tr style="border-width: 0px;">
											<td
												on:click={() => {
													selectedSearchApplication(result.name, result.id, 'READ');
													searchApplications = '';
												}}>{result.name} (Read)</td
											>
										</tr>
										<tr style="border-width: 0px;">
											<td
												on:click={() => {
													selectedSearchApplication(result.name, result.id, 'WRITE');
													searchApplications = '';
												}}>{result.name} (Write)</td
											>
										</tr>
									{/each}
								</table>
							{/if}
						</div>

						<div class="add-item">
							{#if selectedApplicationList?.length > 0}
								<ul>
									{#each selectedApplicationList as app}
										<div style="display:inline-flex">
											<li style="margin-left: 2rem; margin-top: 0.5rem">
												{app.name} ({app.accessType})
											</li>
											<button
												class="remove-button"
												style="margin-left: 0.5rem; height: 1.2rem; width: 1.2rem; margin-top: 0.3rem"
												on:click={() => {
													selectedApplicationList = selectedApplicationList.filter(
														(selectedApplication) => selectedApplication.name != app.name
													);
												}}
												>x
											</button>
										</div>
									{/each}
								</ul>
							{/if}
						</div>
					</tr>

					<div class="nextline-item">
						<button
							class="button"
							style="width: 5.7rem"
							disabled={newTopicName.length < 3 ||
								searchGroups.length < 3 ||
								selectedApplicationList?.length === 0}
							on:click={() => addTopic()}><span>Add Topic</span></button
						>

						<button
							class="button-cancel"
							style="margin-left: 0.5rem"
							on:click={() => {
								newTopicName = '';
								searchGroups = '';
								addTopicVisible = false;
							}}
							>Cancel
						</button>
					</div>
					<br />
				</center>
			</table>
		{/if}

		{#if $topicDetails && topicDetailVisible && !topicsListVisible}
			<span
				style="font-size: medium; margin-left: 9.5rem; cursor: pointer"
				on:click={() => returnToTopicsList()}
				>&laquo; &nbsp; Back
			</span>

			{#if ($permissionsByGroup && $permissionsByGroup.some((groupPermission) => groupPermission.isTopicAdmin === true)) || $isAdmin}
				<span
					style="font-size: medium; float: right; margin-right: 9.5rem; cursor: pointer"
					on:click={() => returnToTopicsList()}
					>Edit &nbsp; <div style="display:inline-block; transform: scaleX(-1);">&#9998;</div>
				</span>
			{/if}

			<table align="center" class="topics-details">
				<tr>
					<td>Name:</td>
					<td>{selectedTopicName}</td>
				</tr>
				<tr>
					<td>Group:</td>
					<td>{selectedTopicGroupName}</td>
				</tr>
				<tr>
					<td>Any application can read:</td>
					<td
						>{#if selectedTopicKind === 'B'}
							Yes
						{:else}
							No
						{/if}
					</td>
				</tr>
				<tr style="border-width: 0px;">
					<td>Applications:</td>
					<td>
						<table class="associated-apps">
							{#if selectedTopicApplications}
								{#each selectedTopicApplications as application}
									<tr style="height:unset">
										<td>{application.applicationId}</td>
										<td style="padding: 0 0.5rem 0 3rem">Access Type:</td>
										<td>Read</td>
										<td>
											<input
												type="checkbox"
												style="width: 1rem"
												checked={application.accessType === 'READ' ||
													application.accessType === 'READ_WRITE'}
											/>
										</td>
										<td style="padding-left: 0.7rem">Write</td><td>
											<input
												type="checkbox"
												style="width: 1rem"
												checked={application.accessType === 'WRITE' ||
													application.accessType === 'READ_WRITE'}
											/>
										</td>
									</tr>
								{/each}
							{/if}
						</table>
					</td>
				</tr>
			</table>
			<br /><br /><br />
			<center>
				<button
					class="button-delete"
					style="width: 7.5rem; float: unset"
					on:click={() => (confirmDeleteVisible = true)}
					><span>Delete Topic</span>
				</button>
			</center>
		{/if}
	</div>
{:else}
	<center><h2>Please Log In to Continue...</h2></center>
{/if}

<style>
	span {
		position: relative;
		left: 0;
	}

	button {
		margin-left: auto;
	}

	input {
		vertical-align: middle;
		height: 1.2rem;
		text-align: left;
		height: 1.5rem;
	}

	label {
		font-size: medium;
	}

	ul {
		display: grid;
		font-size: 0.8rem;
		margin-left: 0.7rem;
		padding-left: 0;
		width: 2rem;
	}

	li {
		margin-bottom: 0.2rem;
	}

	td label {
		margin: 0.5rem;
	}

	.topics-details {
		margin-top: 2rem;
		font-size: 12pt;
		width: 40rem;
	}

	.topics-details tr {
		height: 2.8rem;
	}

	.topic-td {
		cursor: pointer;
	}

	.associated-apps tr {
		border-width: 0px;
	}

	.associated-apps tr:nth-child(even) {
		background-color: rgba(0, 0, 0, 0);
	}

	.associated-apps td {
		font-size: 11pt;
	}

	.add-item {
		/* height: 1.7rem; */
		text-align: left;
		font-size: small;
	}

	.new-topic tr:nth-child(even) {
		background-color: rgba(0, 0, 0, 0);
	}

	.nextline-item {
		display: block;
		margin-top: 1rem;
		margin-bottom: 2rem;
	}

	.searchGroup {
		font-size: 0.75rem;
		width: 9.5rem;
		cursor: pointer;
		list-style-type: none;
		margin-left: 4.75rem;
		margin-top: -0.3rem;
		padding-top: 0.1rem;
		padding-bottom: 0.2rem;
		text-align: left;
		background-color: rgba(217, 221, 254, 0.4);
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.26);
	}

	.searchGroup tr {
		height: 1.9rem;
	}

	.searchGroup tr:nth-child(even) {
		background-color: rgba(192, 196, 240, 0.4);
	}

	.searchApplication {
		font-size: 0.75rem;
		width: 9.5rem;
		cursor: pointer;
		list-style-type: none;
		margin-left: 6.9rem;
		margin-top: -0.3rem;
		padding-top: 0.1rem;
		padding-bottom: 0.2rem;
		text-align: left;
		background-color: rgba(217, 221, 254, 0.4);
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.26);
	}

	.searchApplication tr {
		height: 1.9rem;
	}

	.searchApplication tr:nth-child(even) {
		background-color: rgba(192, 196, 240, 0.4);
	}
</style>
