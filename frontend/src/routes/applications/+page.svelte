<script>
	import { onMount } from 'svelte';
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import applicationPermission from '../../stores/applicationPermission';
	import Modal from '../../lib/Modal.svelte';
	import applications from '../../stores/applications';

	export let data, errors;

	// Constants
	const minNameLength = 3;
	const fiveSeconds = 5000;

	// Authentication
	let isApplicationAdmin = false;

	// Error Handling
	let errorMsg, errorObject;

	// Keys
	const returnKey = 13;

	// Password
	let password;

	// Modals
	let errorMessageVisible = false;
	let addApplicationVisible = false;
	let confirmDeleteVisible = false;
	let applicationDetailVisible = false;

	// Application Name
	let editSaveLabel = 'edit';

	//Pagination
	const applicationsPerPage = 10;
	let applicationsTotalPages;
	let applicationsCurrentPage = 0;

	// Applications SearchBox
	let searchString;
	const searchStringLength = 3;
	let searchAppResults;

	// Groups SearchBox
	let searchGroups;
	let searchGroupResults;
	let searchGroupsResultsVisible = false;
	let searchGroupActive = false;

	// Forms
	let groupsDropdownSuggestion = 7;
	let generateCredentialsVisible = false;
	let showCopyNotificationVisible = false;

	// Timer
	let timer;
	const waitTime = 500;

	// App
	let applicationListVisible = true;
	let appName;
	let editAppName;
	let selectedGroup = '';

	// Selection
	let selectedAppName, selectedAppId, selectedAppGroupId, selectedAppGroupName;

	// Validation
	let previousAppName;

	// Edit Name Label Reactive Statement
	$: editAppName === true ? (editSaveLabel = 'save') : (editSaveLabel = 'edit');

	// Search Feature
	$: if (searchString?.trim().length >= searchStringLength) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchApp(searchString.trim());
		}, waitTime);
	}

	$: if (searchString?.trim().length < searchStringLength) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			reloadAllApps();
		}, waitTime);
	}

	// Search Groups Feature///
	$: if (searchGroups?.trim().length >= searchStringLength && searchGroupActive) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchGroup(searchGroups.trim());
		}, waitTime);
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
	$: if (addApplicationVisible === false) {
		searchGroups = '';
		appName = '';
	}

	onMount(async () => {
		try {
			reloadAllApps();
			const res = await httpAdapter.get(`/token_info`);
			permissionsByGroup.set(res.data.permissionsByGroup);
			if ($permissionsByGroup) {
				isApplicationAdmin = $permissionsByGroup.some(
					(groupPermission) => groupPermission.isApplicationAdmin === true
				);
			}
		} catch (err) {
			errorMessage('Error Loading Applications', err.message);
		}

		if ($urlparameters === 'create') {
			if ($isAdmin || isApplicationAdmin) {
				addApplicationVisible = true;
			} else {
				errorMessage(
					'Only Application Admins can create applications.',
					'Contact your Group Admin.'
				);
			}
			urlparameters.set([]);
		}

		if ($urlparameters?.type === 'prepopulate') {
			searchString = $urlparameters.data;
			urlparameters.set([]);
		}
	});
	const errorMessage = (errMsg, errObj) => {
		errorMsg = errMsg;
		errorObject = errObj;
		addApplicationVisible = false;
		confirmDeleteVisible = false;
		errorMessageVisible = true;
	};

	const errorMessageClear = () => {
		errorMsg = '';
		errorObject = '';
		errorMessageVisible = false;
	};

	const searchGroup = async (searchGroupStr) => {
		setTimeout(async () => {
			searchGroupResults = await httpAdapter.get(
				`/groups?page=0&size=${groupsDropdownSuggestion}&filter=${searchGroupStr}`
			);
		}, 1000);
	};

	const selectedSearchGroup = (groupName, groupId) => {
		selectedGroup = groupId;
		searchGroups = groupName;
		searchGroupsResultsVisible = false;
		searchGroupActive = false;
	};

	const searchApp = async (searchString) => {
		searchAppResults = await httpAdapter.get(
			`/applications?page=0&size=${applicationsPerPage}&filter=${searchString}`
		);
		if (searchAppResults.data.content) {
			applications.set(searchAppResults.data.content);
		} else {
			applications.set([]);
		}
		applicationsTotalPages = searchAppResults.data.totalPages;
		applicationsCurrentPage = 0;
	};

	const addApplication = async () => {
		if (!selectedGroup) {
			const groupId = await httpAdapter.get(
				`/groups?page=0&size=${applicationsPerPage}&filter=${searchGroups}`
			);

			if (
				groupId.data.content &&
				groupId.data.content[0]?.name.toUpperCase() === searchGroups.toUpperCase()
			) {
				selectedGroup = groupId.data.content[0]?.id;
				searchGroupActive = false;
			}
		}

		try {
			await httpAdapter.post(`/applications/save`, {
				name: appName,
				group: selectedGroup
			});
			addApplicationVisible = false;
		} catch (err) {
			if (err.response.data && err.response.status === 303)
				err.message = 'Application name already exists.';
			if (err.response.data && err.response.status === 400) err.message = 'Group not found.';

			errorMessage('Error Creating Application', err.message);
		}
		selectedGroup = '';

		await reloadAllApps();
	};

	const appDelete = async () => {
		await httpAdapter
			.post(`/applications/delete/${selectedAppId}`, {
				id: selectedAppId
			})
			.catch((err) => {
				errorMessage('Error Deleting Application', err.message);
			});

		confirmDeleteVisible = false;
		selectedAppId = '';
		selectedAppName = '';

		returnToAppsList();
		await reloadAllApps();
	};

	const reloadAllApps = async (page = 0) => {
		try {
			let res;
			if (searchString && searchString.length >= searchStringLength) {
				res = await httpAdapter.get(
					`/applications?page=${page}&size=${applicationsPerPage}&filter=${searchString}`
				);
			} else {
				res = await httpAdapter.get(`/applications?page=${page}&size=${applicationsPerPage}`);
			}
			applications.set(res.data.content);
			applicationsTotalPages = res.data.totalPages;
			applicationsCurrentPage = page;
		} catch (err) {
			applications.set();
			errorMessage('Error Loading Applications', err.message);
		}
	};

	const saveNewAppName = async () => {
		await httpAdapter
			.post(`/applications/save/`, {
				id: selectedAppId,
				name: selectedAppName,
				group: selectedAppGroupId
			})
			.catch((err) => {
				errorMessage('Error Saving New Application Name', err.message);
			});

		reloadAllApps();
		editAppName = false;
	};

	const loadApplicationDetail = async (appId, groupId) => {
		const appDetail = await httpAdapter.get(`/applications/show/${appId}`);
		applicationListVisible = false;
		applicationDetailVisible = true;

		selectedAppId = appId;
		selectedAppGroupId = groupId;
		selectedAppName = appDetail.data.name;
		selectedAppGroupName = appDetail.data.groupName;
		await getAppPermissions(appId);
		await getCanonicalTopicName();
	};

	const getAppPermissions = async (appId) => {
		const appPermissionData = await httpAdapter.get(
			`/application_permissions?application=${appId}`
		);

		applicationPermission.set(appPermissionData.data.content);
	};

	const getCanonicalTopicName = async () => {
		if ($applicationPermission) {
			$applicationPermission.forEach(async (topic) => {
				const topicDetails = await httpAdapter.get(`/topics/show/${topic.topicId}`);
				$applicationPermission.find((permissionTopic) => {
					if (permissionTopic.topicId === topic.topicId) {
						permissionTopic.topicName =
							permissionTopic.topicName + ' ' + '(' + topicDetails.data.canonicalName + ')';
						permissionTopic.topicGroup = topicDetails.data.groupName;
					}
				});

				applicationPermission.update((appPermission) => {
					return [...appPermission];
				});
			});
		}
	};

	const returnToApplicationsList = () => {
		generateCredentialsVisible = false;
		password = '';
		applicationDetailVisible = false;
		applicationListVisible = true;
	};

	const returnToAppsList = () => {
		applicationDetailVisible = false;
		applicationListVisible = true;
	};

	const copyPassword = async (applicationId) => {
		navigator.clipboard.writeText(password);
	};

	const generatePassword = async (applicationId) => {
		try {
			const res = await httpAdapter.get(`/applications/generate-passphrase/${applicationId}`);
			password = res.data;
			generateCredentialsVisible = true;
		} catch (err) {
			errorMessage('Error Generating Passphrase', err.message);
		}
	};

	const showCopyNotification = () => {
		showCopyNotificationVisible = true;
		setTimeout(() => {
			showCopyNotificationVisible = false;
		}, fiveSeconds);
	};
</script>

<svelte:head>
	<title>Applications | DDS Permissions Manager</title>
	<meta name="description" content="DDS Permissions Manager Applications" />
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
		<Modal title="Delete {selectedAppName}?" on:cancel={() => (confirmDeleteVisible = false)}>
			<div class="confirm">
				<button class="button-cancel" on:click={() => (confirmDeleteVisible = false)}>Cancel</button
				>&nbsp;
				<button class="button-delete" on:click={() => appDelete()}><span>Delete</span></button>
			</div>
		</Modal>
	{/if}

	{#if addApplicationVisible && !errorMessageVisible}
		<div class="add">
			<Modal
				title="Add Application"
				on:cancel={() => {
					addApplicationVisible = false;
				}}
			>
				<label for="name">Name:</label>
				<input type="text" placeholder="Application Name" bind:value={appName} />
				&nbsp;
				<label for="groups">Group:</label>
				<div style="display: inline-block">
					<input
						placeholder="Group Name"
						style="
                    display: inline-flex;       
                    height: 1.7rem;
                    text-align: left;
                    font-size: small;
                    min-width: 9rem;"
						bind:value={searchGroups}
						on:blur={() => {
							setTimeout(() => {
								searchGroupsResultsVisible = false;
							}, waitTime);
						}}
						on:focus={() => {
							searchGroupResults = [];
							searchGroupActive = true;
							if (searchGroups?.length >= searchStringLength) {
								searchGroup(searchGroups);
							}
						}}
						on:click={async () => {
							searchGroupResults = [];
							searchGroupActive = true;
							if (searchGroups?.length >= searchStringLength) {
								searchGroup(searchGroups);
							}
						}}
					/>

					{#if searchGroupsResultsVisible}
						<table class="searchGroup" style="position: absolute; margin-left: 1rem; width: 9.5rem">
							{#each searchGroupResults.data.content as result}
								<tr
									><td
										on:click={() => {
											selectedSearchGroup(result.name, result.id);
										}}>{result.name}</td
									></tr
								>
							{/each}
						</table>
					{/if}
				</div>
				<button
					disabled={appName?.length < minNameLength || searchGroups?.length < minNameLength}
					class="button"
					style="margin-left: 1rem; width: 4.8rem"
					on:click={() => {
						addApplication();
					}}
				>
					<span>Add</span></button
				>
				<br /><br />
			</Modal>
		</div>
	{/if}

	<div class="content">
		{#if !applicationDetailVisible}
			<h1>Applications</h1>
			<center>
				<!-- svelte-ignore a11y-positive-tabindex -->
				<input
					tabindex="8"
					style="border-width: 1px; width: 20rem; text-align: center"
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
				/>&nbsp; &#x1F50E;
			</center>
		{/if}
		{#if $applications && applicationListVisible && !applicationDetailVisible}
			<table align="center" style="margin-top: 2rem; width: 60%">
				<tr style="border-width: 0px">
					<th><strong>Application</strong></th>
					<th><strong>Group</strong></th>
				</tr>
				{#if $applications}
					{#if $applications.length > 0}
						{#each $applications as app, i}
							<tr>
								<td
									tabindex={i + 9}
									style="cursor: pointer"
									on:click={() => {
										loadApplicationDetail(app.id, app.group);
									}}
									on:keydown={(event) => {
										if (event.which === returnKey) {
											loadApplicationDetail(app.id, app.group);
										}
									}}
									>{app.name}
								</td>
								<td>{app.groupName}</td>

								{#if ($permissionsByGroup && $permissionsByGroup.find((groupPermission) => groupPermission.groupId === app.group))?.isApplicationAdmin || $isAdmin}
									<td>
										<button
											tabindex="-1"
											class="button-delete"
											on:click={() => {
												selectedAppId = app.id;
												selectedAppName = app.name;
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
				{/if}
			</table>
			<br /> <br />

			{#if $applications}
				<center>
					<button
						on:click={async () => {
							if (applicationsCurrentPage > 0) applicationsCurrentPage--;
							reloadAllApps(applicationsCurrentPage);
						}}
						class="button-pagination"
						style="width: 4.8rem; border-bottom-left-radius:9px; border-top-left-radius:9px;"
						disabled={applicationsCurrentPage === 0}>Previous</button
					>
					{#if applicationsTotalPages > 2}
						{#each Array.apply( null, { length: applicationsTotalPages } ).map(Number.call, Number) as page}
							<button
								on:click={() => {
									applicationsCurrentPage = page;
									reloadAllApps(page);
								}}
								class="button-pagination"
								class:button-pagination-selected={page === applicationsCurrentPage}
								>{page + 1}</button
							>
						{/each}
					{/if}

					<button
						on:click={async () => {
							if (applicationsCurrentPage + 1 < applicationsTotalPages) applicationsCurrentPage++;
							reloadAllApps(applicationsCurrentPage);
						}}
						class="button-pagination"
						style="width: 3.1rem; border-bottom-right-radius:9px; border-top-right-radius:9px;"
						disabled={applicationsCurrentPage === applicationsTotalPages - 1 ||
							applicationsTotalPages === 0}>Next</button
					>
				</center>
			{/if}
			<br /><br />
		{:else if !$applications && !applicationDetailVisible && applicationListVisible}
			<center><p>No Applications Found</p></center>
		{/if}

		{#if $applications && applicationDetailVisible && !applicationListVisible}
			<div class="name">
				<input
					tabindex="-1"
					id="name"
					bind:value={selectedAppName}
					readonly={!editAppName}
					class:name-as-label={!editAppName}
					class:name-edit={editAppName}
					style="text-align: center"
					on:keydown={(event) => {
						if (event.which === returnKey) {
							selectedAppName = selectedAppName.trim();
							if (previousAppName !== selectedAppName) saveNewAppName();
							document.querySelector('#name').blur();
							editAppName = false;
						}
					}}
				/>
				{#if ($permissionsByGroup && $permissionsByGroup.find((groupPermission) => groupPermission.groupId === selectedAppGroupId))?.isApplicationAdmin || $isAdmin}
					<span
						style="position: absolute; font-size: medium; left: 65.5rem; top:4.9rem; cursor: pointer"
						on:click={() => {
							if (editSaveLabel === 'edit') {
								previousAppName = selectedAppName;
								editAppName = true;
							}
							if (editSaveLabel === 'save') {
								selectedAppName = selectedAppName.trim();
								if (previousAppName !== selectedAppName) {
									saveNewAppName();
								} else {
									editAppName = false;
								}
							}
						}}
					>
						&raquo; &nbsp; {editSaveLabel}
					</span>
				{/if}
			</div>
			<br />
			<span
				style="font-size: medium; margin-left: 11rem; cursor: pointer"
				on:click={() => returnToApplicationsList()}
				>&laquo; &nbsp; Back
			</span>
			<br /><br />
			<table align="center" style="width: 60%">
				<tr style="border-width: 0px">
					<th><strong>Group</strong></th>
					<th><strong>Topic</strong></th>
					<th><strong>Access</strong></th>
				</tr>
				{#if $applicationPermission}
					{#each $applicationPermission as appPermission}
						<tr>
							<td>
								{appPermission.topicGroup}
							</td><td>
								{appPermission.topicName}
							</td>
							<td>
								{appPermission.accessType}
							</td>
						</tr>
					{/each}
				{/if}
			</table>
			{#if ($permissionsByGroup && $permissionsByGroup.find((groupPermission) => groupPermission.groupId === selectedAppGroupId))?.isApplicationAdmin || $isAdmin}
				<center>
					<span style="">Username: {selectedAppId} &nbsp;</span>
					<button
						class="button"
						style="width: 11rem; margin-top: 2rem;"
						on:click={() => generatePassword(selectedAppId)}>Generate Credentials</button
					>

					{#if generateCredentialsVisible}
						<br />
						<div style="display:flex; width: 13rem; margin-top: 2rem">
							<button
								style=" background-color: rgba(0,0,0,0); border-width: 0px; cursor: pointer; font-size:larger"
								on:click={() => {
									copyPassword(selectedAppId);
									showCopyNotification();
								}}>&#128203;</button
							>
							{#if showCopyNotificationVisible}
								<div class="bubble">Password Copied!</div>
							{/if}
							<section>
								<p
									style="cursor: pointer;"
									on:click={() => {
										copyPassword(selectedAppId);
										showCopyNotification();
									}}
								>
									{password}
								</p>
							</section>
						</div>
					{/if}
				</center>
			{/if}
		{/if}
		<br /><br />
		{#if (($permissionsByGroup && $permissionsByGroup.find((groupPermission) => groupPermission?.isApplicationAdmin)) || $isAdmin) && !applicationDetailVisible}
			<center>
				<!-- svelte-ignore a11y-positive-tabindex -->
				<button
					tabindex="19"
					class="button"
					style="width: 9rem"
					on:click={() => (addApplicationVisible = true)}>Add Application</button
				>
			</center>
		{/if}
	</div>
{/if}

<style>
	section {
		display: flex;
		height: 1rem;
		padding: 0.5rem;
		border-radius: 10px;
		border: 1px dashed #1c1cc9;
		align-items: center;
		justify-content: center;
	}

	tr {
		line-height: 1.7rem;
	}

	input {
		text-align: left;
		z-index: 1;
		background-color: rgba(0, 0, 0, 0);
		padding-left: 0.3rem;
	}

	span {
		position: relative;
		left: 0;
	}

	.name input:focus {
		outline: none;
	}

	.bubble {
		position: absolute;
		background: rgb(0, 0, 0);
		color: #ffffff;
		font-family: Arial;
		font-size: 15px;
		vertical-align: middle;
		text-align: center;
		width: 150px;
		height: 25px;
		border-radius: 10px;
		padding-top: 8px;
		top: 23.2rem;
		left: 31.5rem;
	}
	.bubble:after {
		content: '';
		position: absolute;
		display: block;
		width: 0;
		z-index: 1;
		border-style: solid;
		border-color: #000000 transparent;
		border-width: 0 10px 10px;
		top: -10px;
		left: 26%;
		margin-left: -20px;
	}
</style>
