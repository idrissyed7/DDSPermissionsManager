<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import topics from '../../stores/groups';
	import topicDetails from '../../stores/groupDetails';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import Modal from '../../lib/Modal.svelte';
	import TopicDetails from './TopicDetails.svelte';

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
	let searchApplications, searchApplicationsId;
	let searchApplicationActive = true;
	let searchApplicationResults;
	let searchApplicationsResultsVisible = false;

	// Authentication
	let isTopicAdmin = false;

	// Error Handling
	let errorMsg, errorObject;

	//Pagination
	const topicsPerPage = 3;
	let topicsTotalPages;
	let topicsCurrentPage = 0;

	// Forms
	let anyApplicationCanRead = false;
	let selectedGroup;
	const applicationsResult = 7;

	// Modals
	let errorMessageVisible = false;
	let topicsListVisible = true;
	let topicDetailVisible = false;
	let addTopicVisible = false;
	let confirmDeleteVisible = false;

	// Group Name
	let newTopicName;

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
	$: if (searchGroupResults?.length >= 1 && searchGroupActive) {
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
			if (!$isAdmin) {
				searchGroupResults = $permissionsByGroup.filter(
					(groupIsTopicAdmin) =>
						groupIsTopicAdmin.isTopicAdmin === true &&
						groupIsTopicAdmin.groupName.toUpperCase().includes(searchString.toUpperCase())
				);
			} else {
				searchGroupResults = await httpAdapter.get(`/groups?page=0&size=7&filter=${searchString}`);
				searchGroupResults = searchGroupResults.data.content;
			}
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

	const selectedSearchApplication = (appName, appId) => {
		selectedApplicationList.push({ id: appId, name: appName, accessType: 'READ' });

		// This statement is used to trigger Svelte reactivity and re-render the component
		selectedApplicationList = selectedApplicationList;
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

	const loadTopic = () => {
		addTopicVisible = false;
		topicsListVisible = false;
		topicDetailVisible = true;
	};

	const loadApplicationPermissions = async (topicId) => {
		const resApps = await httpAdapter.get(`/application_permissions/?topic=${topicId}`);
		selectedTopicApplications = resApps.data.content;
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
				if (err.response.status) err.message = 'Topic already exists. Topic name should be unique.';
				errorMessage('Error Adding Topic', err.message);
			});

		if (res) {
			const createdTopicId = res.data.id;
			addTopicApplicationAssociation(createdTopicId);
		}

		addTopicVisible = false;

		await reloadAllTopics().then(() => {
			topicsCurrentPage = 0;
		});
	};

	const addTopicApplicationAssociation = async (topicId, reload = false) => {
		if (selectedApplicationList && selectedApplicationList.length > 0 && !reload) {
			selectedApplicationList.forEach(async (app) => {
				await httpAdapter.post(`/application_permissions/${app.id}/${topicId}/${app.accessType}`);
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
										loadTopic();
										selectedTopicId = topic.id;
									}}
									>{topic.name}
								</td>

								{#if $isAdmin || $permissionsByGroup.find((Topic) => Topic.groupId === topic.group && Topic.isTopicAdmin === true)}
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

			{#if ($permissionsByGroup && $permissionsByGroup.some((groupPermission) => groupPermission.isTopicAdmin === true)) || $isAdmin}
				{#if topicsListVisible && !topicDetailVisible && !addTopicVisible}
					<br /><br />
					<center>
						<button class="button" on:click={() => (addTopicVisible = true)}>Add Topic</button>
					</center>
				{/if}
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
									style="width: 13rem; padding-left: 0.3rem"
									bind:value={newTopicName}
									on:blur={() => (newTopicName = newTopicName.trim())}
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
									style="width: 12.9rem; padding-left: 0.3rem"
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
								<table class="searchGroup" style="position: absolute;">
									{#each searchGroupResults as result}
										<tr>
											<td
												on:click={() => {
													if ($isAdmin) {
														selectedSearchGroup(result.name, result.id);
													} else {
														selectedSearchGroup(result.groupName, result.groupId);
													}
												}}
												>{#if $isAdmin}
													{result.name}
												{:else}
													{result.groupName}
												{/if}
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
						</div>

						<div class="add-item">
							{#if selectedApplicationList?.length > 0}
								<td>
									<ul style="margin-top: -0.2rem; margin-bottom: -0.1rem">
										{#each selectedApplicationList as app}
											<div style="display:inline-flex">
												<td style="width: 10rem">
													<li style="margin-left: 3rem; margin-top: 0.3rem; margin-bottom: 0.3rem">
														{app.name}
													</li>
												</td>
												<td>
													<select
														on:change={(e) => {
															const applicationIndex = selectedApplicationList.findIndex(
																(application) => application.name === app.name
															);

															selectedApplicationList[applicationIndex].accessType = e.target.value;
														}}
														name="AccessType"
													>
														<option value="READ">Read</option>
														<option value="WRITE">Write</option>
														<option value="READ_WRITE">Read/Write</option>
													</select>
												</td>
												<td>
													<button
														class="remove-button"
														style="margin-left: 0.7rem; height: 1.2rem; width: 1.2rem; margin-top: 0.1rem"
														on:click={() => {
															selectedApplicationList = selectedApplicationList.filter(
																(selectedApplication) => selectedApplication.name != app.name
															);
														}}
														>x
													</button>
												</td>
											</div>
										{/each}
									</ul>
								</td>
							{/if}
						</div>
					</tr>

					<tr style="border-width: 0px;">
						<div class="add-item">
							<td>
								<input
									placeholder="Search Application"
									style="width: 8.5rem; margin-left: 0.5rem; padding-left: 0.3rem"
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
								<table class="searchApplication" style="position: absolute;">
									{#each searchApplicationResults.data as result}
										<tr style="border-width: 0px;">
											<td
												on:click={() => {
													selectedSearchApplication(result.name, result.id);
													searchApplications = '';
												}}>{result.name}</td
											>
										</tr>
									{/each}
								</table>
							{/if}
						</div>
					</tr>

					<div class="nextline-item">
						<button
							class="button"
							style="width: 5.7rem"
							disabled={newTopicName.length < 1 || searchGroups.length < 3}
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

		{#if topicDetailVisible && !topicsListVisible}
			<TopicDetails
				{selectedTopicName}
				{selectedTopicId}
				{selectedTopicKind}
				{selectedTopicGroupName}
				{selectedTopicGroupId}
				on:back={() => returnToTopicsList()}
			/>
		{/if}
	</div>
{:else}
	<center><h2>Please Log In to Continue...</h2></center>
{/if}

<style>
	select {
		margin-left: 0.5rem;
		margin-top: 0rem;
		height: 1.5rem;
		font-size: small;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.26);
	}
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

	.topic-td {
		cursor: pointer;
	}

	.add-item {
		text-align: left;
		font-size: small;
	}

	.new-topic tr:nth-child(even) {
		background-color: rgba(0, 0, 0, 0);
	}

	.nextline-item {
		display: block;
		margin-top: 2rem;
		margin-bottom: 2rem;
	}

	.searchGroup {
		margin-left: 4.75rem;
		margin-top: -0.3rem;
	}
</style>
