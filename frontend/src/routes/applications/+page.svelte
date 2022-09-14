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

	export const data = {};
        export const errors = {};

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

	// Pagination
	const applicationsPerPage = 3;
	let applicationsPageIndex;
	let applicationsPages = [];
	let currentPage = 0;

	// App
	let applicationListVisible = true;
	let appName;
	let editAppName;
	let selectedGroup;

	// Selection
	let selectedAppName, selectedAppId, selectedAppGroup, selectedAppGroupId, selectedAppGroupName;

	// Validation
	let previousAppName;

	onMount(async () => {
		try {
			const applicationsData = await httpAdapter.get(`/applications`);
			applications.set(applicationsData.data.content);

			isApplicationAdmin = $permissionsByGroup.some(
				(groupPermission) => groupPermission.isApplicationAdmin === true
			);

			if ($applications) {
				// Pagination
				let totalApplicationsCount = 0;
				applicationsPageIndex = Math.floor(
					applicationsData.data.content.length / applicationsPerPage
				);
				if (applicationsData.data.content.length % applicationsPerPage > 0) applicationsPageIndex++;

				// Populate the applicationsPage Array
				for (let page = 0; page < applicationsPageIndex; page++) {
					let pageArray = [];
					for (
						let i = 0;
						i < applicationsPerPage &&
						totalApplicationsCount < applicationsData.data.content.length;
						i++
					) {
						applicationsData.data.content[page * applicationsPerPage + i].editable = false;
						pageArray.push(applicationsData.data.content[page * applicationsPerPage + i]);
						totalApplicationsCount++;
					}
					applicationsPages.push(pageArray);
				}
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

	const ErrorMessageClear = () => {
		errorMsg = '';
		errorObject = '';
		errorMessageVisible = false;
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

		await reloadApps().then(() => {
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
		await reloadApps();
	};

	const reloadApps = async () => {
		try {
			const res = await httpAdapter.get(`/applications`);
			applications.set(res.data.content);
			if ($applications) {
				calculatePagination();
			}
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
		reloadApps();
	};

	const calculatePagination = () => {
		applicationsPages = [];
		let totalApplicationsCount = 0;
		applicationsPageIndex = Math.floor($applications.length / applicationsPerPage);
		if ($applications.length % applicationsPerPage > 0) applicationsPageIndex++;

		if (applicationsPageIndex === currentPage) currentPage--;

		// Populate the applicationsPage Array
		let pageArray = [];
		for (let page = 0; page < applicationsPageIndex; page++) {
			for (
				let i = 0;
				i < applicationsPerPage && totalApplicationsCount < $applications.length;
				i++
			) {
				$applications[page * applicationsPerPage + i].editable = false;
				pageArray.push($applications[page * applicationsPerPage + i]);
				totalApplicationsCount++;
			}
			applicationsPages.push(pageArray);
			pageArray = [];
		}
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
	<meta name="description" content="Permission Manager Applications" />
</svelte:head>

{#if $isAuthenticated}
	{#if errorMessageVisible}
		<Modal
			title={errorMsg}
			description={errorObject}
			on:cancel={() => {
				errorMessageVisible = false;
				ErrorMessageClear();
			}}
			><br /><br />
			<div class="confirm">
				<button
					class="button-delete"
					on:click={() => {
						errorMessageVisible = false;
						ErrorMessageClear();
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
		{#if $applications && applicationListVisible && !applicationDetailVisible}
			<table align="center">
				<tr style="border-width: 0px">
					<th><strong>ID</strong></th>
					<th><strong>Application Name</strong></th>
				</tr>
				{#if applicationsPages.length > 0}
					{#each applicationsPages[currentPage] as app, index}
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
			</table>
			<br /> <br />

			{#if $applications}
				<center
					><button
						on:click={() => {
							if (currentPage > 0) currentPage--;
						}}
						class="button-pagination"
						style="width: 4.8rem; border-bottom-left-radius:9px; border-top-left-radius:9px;"
						disabled={currentPage === 0}>Previous</button
					>
					{#if applicationsPageIndex > 2}
						{#each applicationsPages as page, i}
							<button
								on:click={() => {
									currentPage = i;
								}}
								class="button-pagination"
								class:button-pagination-selected={i === currentPage}>{i + 1}</button
							>
						{/each}
					{/if}
					<button
						on:click={() => {
							if (currentPage < applicationsPages.length) currentPage++;
						}}
						class="button-pagination"
						style="width: 3.1rem; border-bottom-right-radius:9px; border-top-right-radius:9px;"
						disabled={currentPage === applicationsPages.length - 1 ||
							applicationsPages.length === 0}>Next</button
					></center
				>
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
