<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount, createEventDispatcher } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import topicDetails from '../../stores/groupDetails';
	import Modal from '../../lib/Modal.svelte';
	import AddTopic from './AddTopic.svelte';

	export let selectedTopicName,
		selectedTopicId,
		selectedTopicKind,
		selectedTopicGroupName,
		selectedTopicGroupId;

	let selectedTopicApplications = [];
	let selectedApplicationList;

	const dispatch = createEventDispatcher();

	// Modals
	let errorMessageVisible = false;
	let addTopicVisible = false;
	let copyTopicVisible = false;

	// Forms
	const applicationsResult = 7;

	// Error Handling
	let errorMsg, errorObject;

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

	// Reactive Variables /////////////////////////////////

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

	// OnMount /////////////////////////////////

	onMount(async () => {
		try {
			const res = await httpAdapter.get(`/topics/show/${selectedTopicId}`);
			topicDetails.set(res.data);

			loadApplicationPermissions(selectedTopicId);
			selectedTopicId = $topicDetails.id;
			selectedTopicName = $topicDetails.name;
			selectedTopicGroupName = $topicDetails.groupName;
			selectedTopicGroupId = $topicDetails.group;

			selectedTopicKind = $topicDetails.kind;
		} catch (err) {
			errorMessage('Error Loading Topic Details', err.message);
		}
	});

	// Functions /////////////////////////////////

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

	const searchApplication = async (searchString) => {
		setTimeout(async () => {
			searchApplicationResults = await httpAdapter.get(
				`/applications/search?page=0&size=${applicationsResult}&filter=${searchString}`
			);
			const applicationPermissions = await httpAdapter.get(
				`/application_permissions/?topic=${selectedTopicId}`
			);
			searchApplicationResults.data.forEach((app) => {
				applicationPermissions.data.content?.forEach((appPermission) => {
					if (app.name === appPermission.applicationName) {
						searchApplicationResults.data = searchApplicationResults.data.filter(
							(result) => result.name !== appPermission.applicationName
						);
					}
				});
			});
		}, 1000);
	};

	const loadApplicationPermissions = async (topicId) => {
		const resApps = await httpAdapter.get(`/application_permissions/?topic=${topicId}`);
		selectedTopicApplications = resApps.data.content;
	};

	const addTopicApplicationAssociation = async (topicId, reload = false) => {
		if (selectedApplicationList && !reload) {
			await httpAdapter.post(
				`/application_permissions/${selectedApplicationList.id}/${topicId}/${selectedApplicationList.accessType}`
			);
		}
		if (reload) {
			await httpAdapter
				.post(
					`/application_permissions/${selectedApplicationList.id}/${topicId}/${selectedApplicationList.accessType}`
				)
				.then(() => loadApplicationPermissions(topicId));
		}
	};

	const deleteTopicApplicationAssociation = async (permissionId, topicId) => {
		await httpAdapter.delete(`/application_permissions/${permissionId}`);
		loadApplicationPermissions(topicId);
	};

	const updateTopicApplicationAssociation = async (permissionId, accessType, topicId) => {
		await httpAdapter.put(`/application_permissions/${permissionId}/${accessType}`);
		loadApplicationPermissions(topicId);
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
	<span
		style="font-size: medium; margin-left: 9.5rem; cursor: pointer"
		on:click={() => dispatch('back')}
		>&laquo; &nbsp; Back
	</span>
	<table align="center" class="topics-details">
		<tr>
			<td><strong>Name:</strong></td>
			<td>{selectedTopicName}</td>
		</tr>
		<tr>
			<td><strong>Group:</strong></td>
			<td>{selectedTopicGroupName}</td>
		</tr>
		<tr>
			<td><strong>Any application can read:</strong></td>
			<td
				>{#if selectedTopicKind === 'B'}
					Yes
				{:else}
					No
				{/if}
			</td>
		</tr>
		<tr style="border-width: 0px;">
			<td><strong>Applications:</strong></td>
			<td>
				<table class="associated-apps">
					{#if selectedTopicApplications}
						{#each selectedTopicApplications as application}
							<tr style="height:unset">
								<td>{application.applicationName}</td>
								<td style="padding: 0 0.5rem 0 3rem"><strong>Access Type:</strong></td>
								<td>
									<select
										bind:value={application.accessType}
										on:change={() =>
											updateTopicApplicationAssociation(
												application.id,
												application.accessType,
												application.topicId
											)}
										readonly={!isAdmin ||
											!(
												$permissionsByGroup &&
												$permissionsByGroup.some(
													(groupPermission) => groupPermission.isTopicAdmin === true
												)
											)}
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
										on:click={async () => {
											deleteTopicApplicationAssociation(application.id, application.topicId);
										}}
										>x
									</button>
								</td>
							</tr>
						{/each}
					{:else}
						<td style="width: 24.3rem; " />
					{/if}
				</table>
			</td>
		</tr>
	</table>

	<br /><br />
	{#if $isAdmin || $permissionsByGroup.find((Topic) => Topic.groupId === selectedTopicGroupId && Topic.isTopicAdmin === true)}
		<div class="add-item">
			<center>
				{#if !copyTopicVisible}
					<input
						placeholder="Search Application"
						style="width: 8.5rem; padding-left: 0.3rem"
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

					{#if searchApplicationsResultsVisible}
						<table
							class="searchApplication"
							style="position:absolute; margin-left: 11rem; margin-top: -0.05rem; width: 9rem"
						>
							{#each searchApplicationResults.data as result}
								<tr style="border-width: 0px;">
									<td
										on:click={() => {
											searchApplications = result.name;
											searchApplicationsId = result.id;
											searchApplicationActive = false;
										}}>{result.name}</td
									>
								</tr>
							{/each}
						</table>
					{/if}

					<button
						class="button"
						style="width:8rem; height: 1.9rem; margin-left: 1rem"
						disabled={searchApplications?.length < 3 || searchApplications === undefined}
						on:click={async () => {
							selectedApplicationList = { id: searchApplicationsId, accessType: 'READ' };
							addTopicApplicationAssociation(selectedTopicId, true);
							searchApplications = '';
							selectedApplicationList = '';
						}}
						>Add Application
					</button>

					<button
						class="button"
						style="width:5.5rem; height: 1.9rem; margin-left: 16rem"
						on:click={() => {
							copyTopicVisible = true;
							addTopicVisible = true;
						}}>Copy Topic</button
					>
				{/if}
			</center>

			{#if addTopicVisible && !errorMessageVisible}
				<h2>Copy Topic</h2>
				<AddTopic
					on:closecreate={() => {
						addTopicVisible = false;
						copyTopicVisible = false;
					}}
					on:backtolist={() => {
						addTopicVisible = false;
						dispatch('back');
					}}
					topicName={selectedTopicName}
					groupName={selectedTopicGroupName}
					groupId={selectedTopicGroupId}
					anyAppCanRead={selectedTopicKind === 'B' ? true : false}
					applications={selectedTopicApplications}
					isCopyTopic={true}
				/>
			{/if}
		</div>
	{/if}
{:else}
	<center><h2>Please Log In to Continue...</h2></center>
{/if}

<style>
	input {
		vertical-align: middle;
		height: 1.2rem;
		text-align: left;
		height: 1.5rem;
	}

	.topics-details {
		margin-top: 2rem;
		font-size: 12pt;
		width: 40rem;
	}

	.topics-details tr {
		height: 2.8rem;
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
		text-align: left;
		font-size: small;
	}
</style>
