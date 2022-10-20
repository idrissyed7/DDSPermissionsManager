<script>
	import { isAdmin, isAuthenticated } from '../../stores/authentication';
	import { httpAdapter } from '../../appconfig';
	import { createEventDispatcher, onMount } from 'svelte';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import Modal from '../../lib/Modal.svelte';

	export let topicName = '';
	export let groupName = '';
	export let groupId = '';
	export let anyAppCanRead = false;
	export let applications = [];
	export let isCopyTopic = false;

	const dispatch = createEventDispatcher();

	// Group Name
	let newTopicName = '';

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

	// Forms
	let selectedGroup;
	const applicationsResult = 7;
	let anyApplicationCanRead = false;

	// Error Handling
	let errorMsg, errorObject;

	// Modals
	let errorMessageVisible = false;
	let addTopicVisible = false;

	// Selection
	let selectedApplicationList = [];

	// onMount /////////////////////
	onMount(() => {
		if (topicName !== '') {
			newTopicName = topicName;
			searchGroups = groupName;
			selectedGroup = groupId;
			anyApplicationCanRead = anyAppCanRead;
			selectedApplicationList = applications;
			searchGroupActive = false;
		}
	});

	// Reactive Variables /////////////////////

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

	// Functions /////////////////////

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
		selectedApplicationList.push({
			applicationId: appId,
			applicationName: appName,
			accessType: 'READ'
		});

		// This statement is used to trigger Svelte reactivity and re-render the component
		selectedApplicationList = selectedApplicationList;
		searchApplications = appName;
		searchApplicationsResultsVisible = false;
		searchApplicationActive = false;
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

	const addTopic = async () => {
		if (!selectedGroup) {
			const groupId = await httpAdapter.get(`/groups?page=0&size=1&filter=${searchGroups}`);
			if (
				groupId.data.content &&
				groupId.data.content[0]?.name.toUpperCase() === searchGroups.toUpperCase()
			) {
				selectedGroup = groupId.data.content[0]?.id;
				searchGroupActive = false;
			}
		}
		console.log('selectedGroup', selectedGroup);
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

		if (isCopyTopic) {
			dispatch('backtolist');
		} else {
			addTopicVisible = false;
			dispatch('reloadtopics');
		}
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
							on:focus={async () => {
								searchGroupResults = [];
								searchGroupActive = true;
								if (searchGroups?.length >= 3) {
									searchGroup(searchGroups);
								}
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
												{app.applicationName}
											</li>
										</td>
										<td>
											<select
												on:change={(e) => {
													const applicationIndex = selectedApplicationList.findIndex(
														(application) => application.applicationName === app.applicationName
													);

													selectedApplicationList[applicationIndex].accessType = e.target.value;
												}}
												name="AccessType"
												value={app.accessType}
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
														(selectedApplication) =>
															selectedApplication.applicationName != app.applicationName
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
							on:focus={() => {
								async () => {
									searchApplicationResults = [];
									searchApplicationActive = true;
									if (searchApplications?.length >= 3) {
										searchApplication(searchApplications);
									}
								};
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
					disabled={newTopicName?.length < 1 || searchGroups?.length < 3}
					on:click={() => addTopic()}><span>Add Topic</span></button
				>

				<button
					class="button-cancel"
					style="margin-left: 0.5rem"
					on:click={() => {
						newTopicName = '';
						searchGroups = '';
						dispatch('closecreate');
					}}
					>Cancel
				</button>
			</div>
			<br />
		</center>
	</table>
{/if}

<style>
	input {
		vertical-align: middle;
		height: 1.2rem;
		text-align: left;
		height: 1.5rem;
	}

	select {
		margin-left: 0.5rem;
		margin-top: 0rem;
		height: 1.5rem;
		font-size: small;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.26);
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
