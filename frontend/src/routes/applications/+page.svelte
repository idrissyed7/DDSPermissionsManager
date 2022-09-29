<script>
	import { onMount } from 'svelte';
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import applicationPermission from '../../stores/applicationPermission';
	import groups from '../../stores/groups';
	import Modal from '../../lib/Modal.svelte';
	import applications from '../../stores/applications';

	export let data, errors;

	// Authentication
	let isApplicationAdmin = false;

	// Error Handling
	let errorMsg, errorObject;
	let duplicateAppName = false;

	// Modals
	let errorMessageVisible = false;
	let addApplicationVisible = false;
	let confirmDeleteVisible = false;
	let applicationDetailVisible = false;

	//Pagination
	const applicationsPerPage = 10;
	let applicationsTotalPages;
	let applicationsCurrentPage = 0;

	// SearchBox
	let searchString;
	let searchAppResults;
	let timer;
	const waitTime = 500;

	// App
	let applicationListVisible = true;
	let appName;
	let editAppName;
	let selectedGroup;

	// Selection
	let selectedAppName, selectedAppId, selectedAppGroup, selectedAppGroupId, selectedAppGroupName;

	// Validation
	let previousAppName;

	// Search Feature
	$: if (searchString?.trim().length >= 3) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchApp(searchString.trim());
		}, waitTime);
	}

	$: if (searchString?.trim().length < 3) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			reloadAllApps();
		}, waitTime);
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
		try {
			const res = await httpAdapter.post(`/applications/save`, {
				name: appName,
				permissionsGroup: selectedGroup
			});
			addApplicationVisible = false;
		} catch (err) {
			errorMessage('Error Creating Application', err.message);
		}

		await reloadAllApps().then(() => {
			currentPage = applicationsPages.length - 1;
		});
	};

	const confirmAppDelete = (ID, name) => {
		confirmDeleteVisible = true;

		selectedAppId = ID;
		selectedAppName = name;
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
			if (searchString && searchString.length >= 3) {
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

	const addAppModal = () => {
		if ($groups) {
			appName = '';
			addApplicationVisible = true;
		} else {
			errorMessage('Error', 'There are no Groups available');
		}
	};

	const saveNewAppName = async () => {
		await httpAdapter
			.post(`/applications/save/`, {
				id: selectedAppId,
				name: selectedAppName,
				permissionsGroup: selectedAppGroupId
			})
			.catch((err) => {
				errorMessage('Error Saving New Application Name', err.message);
			});
		reloadAllApps();
	};

	const checkAppDuplicates = (appName, appID) => {
		if ($applications) {
			duplicateAppName = $applications.some(
				(appStore) => appStore.name === appName && appStore.id !== appID
			);
		}

		if (!duplicateAppName && appID === 0) {
			addApplication();
		}

		if (!duplicateAppName && appID !== 0) {
			saveNewAppName();
		}
	};

	const loadApplicationDetail = async (appId, groupId) => {
		const appDetail = await httpAdapter.get(`/applications/show/${appId}`);
		applicationListVisible = false;
		applicationDetailVisible = true;

		selectedAppId = appId;
		selectedAppGroupId = groupId;
		selectedAppName = appDetail.data.application_name;
		selectedAppGroupName = appDetail.data.group_name;

		await getAppPermissions(appId);
	};

	const getAppPermissions = async (appId) => {
		const appPermissionData = await httpAdapter.get(
			`/application_permissions?application=${appId}`
		);

		applicationPermission.set(appPermissionData.data.content);
	};

	const returnToApplicationsList = () => {
		applicationDetailVisible = false;
		applicationListVisible = true;
	};

	const returnToAppsList = () => {
		applicationDetailVisible = false;
		applicationListVisible = true;
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
				title="Create Application"
				on:cancel={() => {
					addApplicationVisible = false;
					duplicateAppName = false;
				}}
			>
				<input
					type="text"
					placeholder="Application Name"
					class:invalid={duplicateAppName}
					bind:value={appName}
					on:click={() => (duplicateAppName = false)}
					on:keydown={(event) => {
						if (event.which === 13) {
							document.activeElement.blur();
							checkAppDuplicates(appName, 0);
						}
					}}
				/>
				&nbsp;
				<label for="groups">Group:</label>
				<select name="groups" bind:value={selectedGroup}>
					{#if $isAdmin}
						{#each $groups as group}
							<option value={group.id}>{group.name}</option>
						{/each}
					{:else}
						{#each $permissionsByGroup as group, i}
							<option value={group.groupId}>{group.groupName}</option>
						{/each}
					{/if}
				</select>
				<button
					class="button"
					style="margin-left: 1rem; width: 4.8rem"
					on:click={() => {
						checkAppDuplicates(appName, 0);
					}}
				>
					<span>Create</span></button
				>
				{#if duplicateAppName}
					<p style="position: absolute; left:33%; top: 38px; color: red">
						Application name must be unique
					</p>
				{/if}
			</Modal>
		</div>
	{/if}

	<div class="content">
		{#if !applicationDetailVisible}
			<h1>Applications</h1>
		{/if}
		<center>
			<input
				style="border-width: 1px; width: 20rem"
				placeholder="Search"
				bind:value={searchString}
				on:blur={() => {
					searchString = searchString?.trim();
				}}
				on:keydown={(event) => {
					const returnKey = 13;
					if (event.which === returnKey) {
						document.activeElement.blur();
						searchString = searchString?.trim();
					}
				}}
			/>&nbsp; &#x1F50E;
		</center>
		{#if $applications && applicationListVisible && !applicationDetailVisible}
			<table align="center" style="margin-top: 2rem">
				<tr style="border-width: 0px">
					<th><strong>ID</strong></th>
					<th><strong>Application Name</strong></th>
				</tr>
				{#if $applications}
					{#if $applications.length > 0}
						{#each $applications as app}
							<tr>
								<td>{app.id}</td>
								<td
									style="cursor: pointer"
									on:click={() => {
										loadApplicationDetail(app.id, app.permissionsGroup);
									}}>{app.name}</td
								>
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
				<span on:click={() => returnToApplicationsList()}>&laquo;</span>
				<div class="tooltip">
					<input
						id="name"
						class:editable={$isAdmin}
						on:click={() => {
							if ($isAdmin) {
								editAppName = true;
								previousAppName = selectedAppName.trim();
							}
						}}
						on:blur={() => {
							if ($isAdmin) {
								selectedAppName = selectedAppName.trim();
								if (previousAppName !== selectedAppName) saveNewAppName();
								editAppName = false;
							}
						}}
						on:keydown={(event) => {
							if (event.which === 13) {
								if ($isAdmin) {
									selectedAppName = selectedAppName.trim();
									if (previousAppName !== selectedAppName) saveNewAppName();
									document.querySelector('#name').blur();
								}
							}
						}}
						bind:value={selectedAppName}
						readonly={!editAppName}
						class:name-as-label={!editAppName}
						class:name-edit={editAppName}
					/>
					{#if $isAdmin}
						<span class="tooltiptext">&#9998</span>
					{/if}
				</div>
			</div>
			<br /><br />
			<table align="center">
				<tr style="border-width: 0px">
					<th><strong>ID</strong></th>
					<th><strong>Application Name</strong></th>
					<th><strong>Group</strong></th>
					<th><strong>Topic</strong></th>
					<th><strong>Access</strong></th>
				</tr>
				<tr>
					<td>{selectedAppId}</td>
					<td>{selectedAppName}</td>
					<td>{selectedAppGroupName}</td>
					<td>
						{#if $applicationPermission}
							<ul>
								{#each $applicationPermission as appPermission}
									<li>
										{appPermission.permissionsTopic.name}
									</li>
								{/each}
							</ul>
						{/if}
					</td>
					<td>
						{#if $applicationPermission}
							<ul style="list-style-type: none;">
								{#each $applicationPermission as appPermission}
									<li>
										{appPermission.accessType}
									</li>
								{/each}
							</ul>
						{/if}
					</td>
					{#if $isAdmin}
						<td
							><button
								class="button-delete"
								on:click={() => confirmAppDelete(selectedAppId, selectedAppName)}
								><span>Delete</span></button
							></td
						>
					{/if}
				</tr>
			</table>
		{/if}
		<br /><br />
		{#if $isAdmin && !applicationDetailVisible}
			<center
				><button class="button" style="width: 9rem" on:click={() => addAppModal()}
					>Create Application</button
				></center
			>
		{/if}
	</div>
{/if}

<style>
	ul {
		margin: 0;
		padding: 0.25rem 0 0.25rem 0.85rem;
	}
	input {
		margin-top: 1.1rem;
		text-align: left;
		text-align: center;
		width: 20rem;
		z-index: 1;
		background-color: rgba(0, 0, 0, 0);
	}

	.tooltip .tooltiptext {
		top: -6px;
		left: 303px;
	}
</style>
