<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount, createEventDispatcher } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import topicDetails from '../../stores/groupDetails';
	import Modal from '../../lib/Modal.svelte';
	// import AddTopic from './AddTopic.svelte';
	import headerTitle from '../../stores/headerTitle';
	import detailView from '../../stores/detailView';
	import deleteSVG from '../../icons/delete.svg';
	import duplicateSVG from '../../icons/duplicate.svg';

	export let selectedTopicName,
		selectedTopicId,
		selectedTopicKind,
		selectedTopicGroupName,
		selectedTopicGroupId;

	let selectedTopicCanonicalName;
	let selectedTopicApplications = [];
	let selectedApplicationList;

	const dispatch = createEventDispatcher();

	// Modals
	let errorMessageVisible = false;
	let addTopicVisible = false;
	let duplicateTopicVisible = false;

	// Forms
	const applicationsResult = 17;

	// Error Handling
	let errorMsg, errorObject;

	// Search Groups
	let searchGroups;
	let searchGroupResults;
	let searchGroupsResultsVisible = false;
	let searchGroupActive = true;

	// Search Applications
	let searchApplications, searchApplicationsId, searchApplicationsGroup;
	let searchApplicationActive = true;
	let searchApplicationResults;
	let searchApplicationsResultsVisible = false;

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

	onMount(async () => {
		try {
			const res = await httpAdapter.get(`/topics/show/${selectedTopicId}`);
			topicDetails.set(res.data);

			loadApplicationPermissions(selectedTopicId);
			selectedTopicId = $topicDetails.id;
			selectedTopicName = $topicDetails.name;
			selectedTopicCanonicalName = $topicDetails.canonicalName;
			selectedTopicGroupName = $topicDetails.groupName;
			selectedTopicGroupId = $topicDetails.group;

			selectedTopicKind = $topicDetails.kind;

			headerTitle.set(selectedTopicName);
			detailView.set(true);
		} catch (err) {
			errorMessage('Error Loading Topic Details', err.message);
		}
	});

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
		if (selectedTopicApplications) await getApplicationGroupNames(resApps.data.content);
		selectedTopicApplications = resApps.data.content;
	};

	const getApplicationGroupNames = async (applicationsList) => {
		if (applicationsList) {
			for (const application of applicationsList) {
				const res = await httpAdapter.get(`/applications/show/${application.applicationId}`);
				const appendApp = applicationsList.find(
					(app) => app.applicationName === application.applicationName
				);
				appendApp.applicationGroup = res.data.groupName;
			}
			selectedTopicApplications = selectedTopicApplications;
		}
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
			closeModalText={'Close'}
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

	{#if $isAdmin || $permissionsByGroup.find((Topic) => Topic.groupId === selectedTopicGroupId && Topic.isTopicAdmin === true)}
		<div>
			<button
				class="button-blue"
				style="width:9.2rem; height: 2.2rem; cursor:pointer"
				on:click={async () => {
					duplicateTopicVisible = true;
					addTopicVisible = true;
				}}
			>
				<img
					src={duplicateSVG}
					alt="duplicate topic"
					width="20rem"
					style="vertical-align: middle; filter: invert(); margin-left: -0.5rem; margin-right: 0.2rem "
					on:click={async () => {
						duplicateTopicVisible = true;
						addTopicVisible = true;
					}}
				/>

				<span style="vertical-align: middle; font-size: 0.8rem">Duplicate Topic</span>
			</button>

			{#if duplicateTopicVisible}
				<Modal
					title="Duplicate Topic"
					actionDuplicateTopic={true}
					topicName={true}
					newTopicName={selectedTopicName}
					group={true}
					searchGroups={selectedTopicGroupName}
					application={true}
					selectedApplicationList={selectedTopicApplications}
					anyApplicationCanRead={selectedTopicKind === 'B' ? true : false}
					on:cancel={() => (duplicateTopicVisible = false)}
					on:duplicateTopic={(e) => {
						duplicateTopicVisible = false;
						dispatch('addTopic', e.detail);
					}}
				/>
			{/if}
		</div>
	{/if}

	<table class="topics-details">
		<tr>
			<td>Name:</td>
			<td>{selectedTopicName} ({selectedTopicCanonicalName})</td>
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
			<td style="border-bottom-color: transparent;">
				<span style="margin-right: 1rem">Applications:</span>
			</td>
			{#if ($permissionsByGroup && $permissionsByGroup.some((groupPermission) => groupPermission.isTopicAdmin === true)) || $isAdmin}
				<td style="border-bottom-color: transparent;">
					<input
						style="margin-top: 0.5rem; margin-bottom: 0.5rem"
						class="searchbox"
						type="search"
						placeholder="Search Application"
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
			{/if}
		</tr>
	</table>

	{#if searchApplicationsResultsVisible}
		<table
			class="search-application"
			style="position:absolute; margin-left: 18rem; margin-top: -0.8rem; width: 12rem"
		>
			{#each searchApplicationResults.data as result}
				<tr style="border-width: 0px">
					<td
						on:click={() => {
							searchApplications = result.name;
							searchApplicationsId = result.id;

							searchApplicationActive = false;

							selectedApplicationList = {
								id: searchApplicationsId,
								accessType: 'READ'
							};

							addTopicApplicationAssociation(selectedTopicId, true);
							searchApplications = '';
							selectedApplicationList = '';
						}}>{result.name} ({result.groupName})</td
					>
				</tr>
			{/each}
		</table>
	{/if}

	{#if selectedTopicApplications}
		<div>
			{#each selectedTopicApplications as application}
				<div style="display: flex; font-size: 0.8rem; margin-left: 16.5rem; margin-bottom: -0.2rem">
					<span style="width: 10.5rem">
						{application.applicationName} ({application.applicationGroup})
					</span>
					<span style="border-bottom-color: transparent; padding-left: 0.5rem">Access Type:</span>

					<span style="border-bottom-color: transparent; margin-right: -1.5rem; margin-top: -2rem">
						<ul
							style="list-style-type: none; margin-left: -2.3rem; margin-right: -2rem; top: -2rem"
						>
							<li>
								<span style="font-size: 0.6rem">Read</span>
							</li>
							<li style="margin-top: -0.25rem;">
								<input
									type="checkbox"
									style="width:unset; height: 1.5rem"
									bind:value={application.accessType}
									checked={application.accessType.includes('READ')}
									readonly={!isAdmin ||
										!(
											$permissionsByGroup &&
											$permissionsByGroup.some(
												(groupPermission) => groupPermission.isTopicAdmin === true
											)
										)}
									on:change={(e) => {
										if (e.target.checked) {
											if (application.accessType === 'WRITE') {
												application.accessType = 'READ_WRITE';
											} else {
												application.accessType = 'READ';
											}
										} else {
											if (application.accessType === 'READ_WRITE') {
												application.accessType = 'WRITE';
											} else {
												application.accessType = 'READ';
												e.target.checked = true;
											}
										}
										updateTopicApplicationAssociation(
											application.id,
											application.accessType,
											application.topicId
										);
									}}
								/>
							</li>
						</ul>

						<ul style="list-style-type: none; margin-right: -2.4rem; margin-top: -3.35rem">
							<li>
								<span style="font-size: 0.6rem">Write</span>
							</li>
							<li style="margin-top: -0.25rem">
								<input
									type="checkbox"
									bind:value={application.accessType}
									checked={application.accessType.includes('WRITE')}
									style="width:unset; height: 1.5rem"
									readonly={!isAdmin ||
										!(
											$permissionsByGroup &&
											$permissionsByGroup.some(
												(groupPermission) => groupPermission.isTopicAdmin === true
											)
										)}
									on:change={(e) => {
										if (e.target.checked) {
											if (application.accessType === 'READ') {
												application.accessType = 'READ_WRITE';
											} else {
												application.accessType = 'WRITE';
											}
										} else {
											if (application.accessType === 'READ_WRITE') {
												application.accessType = 'READ';
											} else {
												application.accessType = 'READ';
												e.target.checked = true;
											}
										}
										updateTopicApplicationAssociation(
											application.id,
											application.accessType,
											application.topicId
										);
									}}
								/>
							</li>
						</ul>

						<ul
							style="list-style-type: none; margin-top: 0.55rem; margin-left: 1rem; margin-top: -3.28rem"
						>
							<li>
								<img
									src={deleteSVG}
									alt="remove application"
									style="background-color: transparent; cursor: pointer; scale: 50%; align-content:center"
									on:click={async () => {
										deleteTopicApplicationAssociation(application.id, application.topicId);
									}}
								/>
							</li>
						</ul>
					</span>
				</div>
			{/each}
			<div style="font-size: 0.7rem; width:37.5rem; text-align:right; margin-top: -1rem">
				{selectedTopicApplications.length} of {selectedTopicApplications.length}
			</div>
		</div>
	{/if}
{/if}

<style>
	.topics-details {
		font-size: 12pt;
		width: 38rem;
	}

	.topics-details td {
		height: 2.2rem;
	}
</style>
