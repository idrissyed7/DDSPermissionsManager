<script>
	import { onMount } from 'svelte';
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import applicationPermission from '../../stores/applicationPermission';
	import Modal from '../../lib/Modal.svelte';
	import applications from '../../stores/applications';
	import { goto } from '$app/navigation';
	import { browser } from '$app/env';
	import headerTitle from '../../stores/headerTitle';
	import detailView from '../../stores/detailView';
	import deleteSVG from '../../icons/delete.svg';
	import editSVG from '../../icons/edit.svg';
	import addSVG from '../../icons/add.svg';
	import pageforwardSVG from '../../icons/pageforward.svg';
	import pagebackwardsSVG from '../../icons/pagebackwards.svg';
	import pagefirstSVG from '../../icons/pagefirst.svg';
	import pagelastSVG from '../../icons/pagelast.svg';
	import threedotsSVG from '../../icons/threedots.svg';
	import lockSVG from '../../icons/lock.svg';
	import copySVG from '../../icons/copy.svg';

	// Redirects the User to the Login screen if not authenticated
	$: if (browser) {
		setTimeout(() => {
			if (!$isAuthenticated) goto(`/`, true);
		}, waitTime);
	}

	export let data, errors;

	// Constants
	const minNameLength = 3;
	const fiveSeconds = 5000;
	const returnKey = 13;
	const searchStringLength = 3;
	const waitTime = 250;

	// Authentication
	let isApplicationAdmin = false;

	// Error Handling
	let errorMsg, errorObject;

	// Password
	let password;

	// Modals
	let errorMessageVisible = false;
	let addApplicationVisible = false;
	let deleteApplicationVisible = false;
	let applicationDetailVisible = false;
	let editApplicationNameVisible = false;

	// Tables
	let applicationsRowsSelected = [];
	let applicationsRowsSelectedTrue = false;
	let applicationsAllRowsSelectedTrue = false;
	let applicationsPerPage = 10;

	//Pagination
	let applicationsTotalPages, applicationsTotalSize;
	let applicationsCurrentPage = 0;

	// DropDowns
	let applicationsDropDownVisible = false;
	let applicationsDropDownMouseEnter = false;

	// Applications SearchBox
	let searchString;
	let searchAppResults;

	// Groups SearchBox
	let searchGroups;
	let searchGroupResults;
	// let searchGroupsResultsVisible = false;
	// let searchGroupActive = false;

	// Forms
	let groupsDropdownSuggestion = 7;
	let generateCredentialsVisible = false;
	let showCopyNotificationVisible = false;

	// Timer
	let timer;

	// App
	let applicationListVisible = true;
	let appName;
	let editAppName;
	let selectedGroup = '';

	// Selection
	let selectedAppName, selectedAppId, selectedAppGroupId, selectedAppGroupName;

	// Validation
	let previousAppName;

	// Return to List view
	$: if (!$detailView) {
		headerTitle.set('Applications');
		reloadAllApps();
		returnToApplicationsList();
	}

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

	// // Search Groups Feature///
	// $: if (searchGroups?.trim().length >= searchStringLength && searchGroupActive) {
	// 	clearTimeout(timer);
	// 	timer = setTimeout(() => {
	// 		searchGroup(searchGroups.trim());
	// 	}, waitTime);
	// } else {
	// 	searchGroupsResultsVisible = false;
	// }

	// // Search Groups Dropdown Visibility
	// $: if (searchGroupResults?.data?.content?.length >= 1 && searchGroupActive) {
	// 	searchGroupsResultsVisible = true;
	// } else {
	// 	searchGroupsResultsVisible = false;
	// }

	// Reset add group form once closed
	$: if (addApplicationVisible === false) {
		searchGroups = '';
		appName = '';
	}

	onMount(async () => {
		headerTitle.set('Applications');
		detailView.set();

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
		deleteApplicationVisible = false;
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
		if (searchAppResults.data.totalSize !== undefined)
			applicationsTotalSize = searchAppResults.data.totalSize;
		applicationsCurrentPage = 0;
	};

	const addApplication = async (
		forwardedAppName,
		forwardedSearchGroups,
		forwardedSelectedGroup
	) => {
		if (!forwardedSelectedGroup) {
			const groupId = await httpAdapter.get(
				`/groups?page=0&size=${applicationsPerPage}&filter=${forwardedSearchGroups}`
			);

			if (
				groupId.data.content &&
				groupId.data.content[0]?.name.toUpperCase() === forwardedSearchGroups.toUpperCase()
			) {
				forwardedSelectedGroup = groupId.data.content[0]?.id;
				// searchGroupActive = false;
			}
		}

		try {
			await httpAdapter.post(`/applications/save`, {
				name: forwardedAppName,
				group: forwardedSelectedGroup
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

	const deleteSelectedApplications = async () => {
		try {
			for (const app of applicationsRowsSelected) {
				await httpAdapter.post(`/applications/delete/${app.id}`, {
					id: app.id
				});
			}
		} catch (err) {
			errorMessage('Error Deleting Application', err.message);
		}

		selectedAppId = '';
		selectedAppName = '';

		returnToAppsList();
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

			if (res.data) {
				applicationsTotalPages = res.data.totalPages;
				applicationsTotalSize = res.data.totalSize;
			}
			applications.set(res.data.content);
			applicationsCurrentPage = page;
		} catch (err) {
			applications.set();
			errorMessage('Error Loading Applications', err.message);
		}
	};

	const saveNewAppName = async (newAppName) => {
		await httpAdapter
			.post(`/applications/save/`, {
				id: selectedAppId,
				name: newAppName,
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

	const deselectAllApplicationsCheckboxes = () => {
		applicationsAllRowsSelectedTrue = false;
		applicationsRowsSelectedTrue = false;
		applicationsRowsSelected = [];
		let checkboxes = document.querySelectorAll('.apps-checkbox');
		checkboxes.forEach((checkbox) => (checkbox.checked = false));
	};
</script>

<svelte:head>
	<title>Applications | DDS Permissions Manager</title>
	<meta name="description" content="DDS Permissions Manager Applications" />
</svelte:head>

{#if $isAuthenticated}
	{#if deleteApplicationVisible && !errorMessageVisible}
		<Modal
			actionDeleteApplications={true}
			title="Delete {applicationsRowsSelected.length > 1 ? 'Applications' : 'Application'}"
			on:cancel={() => (deleteApplicationVisible = false)}
			on:deleteApplications={async () => {
				await deleteSelectedApplications();
				reloadAllApps();
				deselectAllApplicationsCheckboxes();
				deleteApplicationVisible = false;
			}}
		/>
	{/if}

	{#if addApplicationVisible && !errorMessageVisible}
		<Modal
			title="Add Application"
			applicationName={true}
			group={true}
			actionAddApplication={true}
			on:cancel={() => (addApplicationVisible = false)}
			on:addApplication={(e) => {
				addApplication(e.detail.appName, e.detail.searchGroups, e.detail.selectedGroup);
			}}
		/>
	{/if}

	{#if editApplicationNameVisible}
		<Modal
			title="Edit Application"
			actionEditApplicationName={true}
			{previousAppName}
			on:cancel={() => (editApplicationNameVisible = false)}
			on:saveNewAppName={(e) => {
				saveNewAppName(e.detail.newAppName);
				editApplicationNameVisible = false;
			}}
		/>
	{/if}

	<div class="content">
		{#if !applicationDetailVisible}
			<h1>Applications</h1>

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

			{#if (($permissionsByGroup && $permissionsByGroup.find((groupPermission) => groupPermission?.isApplicationAdmin)) || $isAdmin) && !applicationDetailVisible}
				<div
					tabindex="0"
					class="dot"
					on:mouseleave={() => {
						setTimeout(() => {
							if (!applicationsDropDownMouseEnter) applicationsDropDownVisible = false;
						}, waitTime);
					}}
					on:focusout={() => {
						setTimeout(() => {
							if (!applicationsDropDownMouseEnter) applicationsDropDownVisible = false;
						}, waitTime);
					}}
					on:click={() => {
						if (!deleteApplicationVisible && !addApplicationVisible)
							applicationsDropDownVisible = !applicationsDropDownVisible;
					}}
					on:keydown={(event) => {
						if (event.which === returnKey) {
							if (!deleteApplicationVisible && !addApplicationVisible)
								applicationsDropDownVisible = !applicationsDropDownVisible;
						}
					}}
				>
					<img src={threedotsSVG} alt="options" style="scale:50%" />

					{#if applicationsDropDownVisible}
						<table
							class="dropdown"
							on:mouseenter={() => (applicationsDropDownMouseEnter = true)}
							on:mouseleave={() => {
								setTimeout(() => {
									applicationsDropDownVisible = !applicationsDropDownVisible;
									applicationsDropDownMouseEnter = false;
								}, waitTime);
							}}
						>
							<tr
								tabindex="0"
								disabled={!$isAdmin}
								class:disabled={!$isAdmin || applicationsRowsSelected.length === 0}
								on:click={async () => {
									applicationsDropDownVisible = false;
									if (applicationsRowsSelected.length > 0) deleteApplicationVisible = true;
								}}
								on:keydown={(event) => {
									if (event.which === returnKey) {
										applicationsDropDownVisible = false;
										if (applicationsRowsSelected.length > 0) deleteApplicationVisible = true;
									}
								}}
								on:focus={() => (applicationsDropDownMouseEnter = true)}
							>
								<td
									>Delete Selected {applicationsRowsSelected.length > 1
										? 'Applications'
										: 'Application'}
								</td>
								<td style="width: 0.1rem; padding-left: 0; vertical-align: middle">
									<img
										src={deleteSVG}
										alt="delete application"
										height="35rem"
										style="vertical-align: -0.8rem"
										class:disabled-img={!$isAdmin || applicationsRowsSelected.length === 0}
									/>
								</td>
							</tr>

							<tr
								tabindex="0"
								on:click={() => {
									applicationsDropDownVisible = false;
									addApplicationVisible = true;
								}}
								on:keydown={(event) => {
									if (event.which === returnKey) {
										applicationsDropDownVisible = false;
										addApplicationVisible = true;
									}
								}}
								on:focusout={() => (applicationsDropDownMouseEnter = false)}
								class:hidden={addApplicationVisible}
							>
								<td style="border-bottom-color: transparent">Add New Application</td>
								<td
									style="width: 0.1rem; height: 2.2rem; padding-left: 0; vertical-align: middle;border-bottom-color: transparent"
								>
									<img
										src={addSVG}
										alt="add application"
										height="27rem"
										style="vertical-align: middle; margin-left: 0.25rem"
									/>
								</td>
							</tr>
						</table>
					{/if}
				</div>
			{/if}
		{/if}

		{#if $applications && applicationListVisible && !applicationDetailVisible}
			<table
				style="margin-top: 0.5rem"
				class:application-table-admin={($permissionsByGroup &&
					$permissionsByGroup.find((groupPermission) => groupPermission?.isApplicationAdmin)) ||
					$isAdmin}
			>
				<tr style="border-top: 1px solid black; border-bottom: 2px solid">
					{#if (($permissionsByGroup && $permissionsByGroup.find((groupPermission) => groupPermission?.isApplicationAdmin)) || $isAdmin) && !applicationDetailVisible}
						<td>
							<input
								tabindex="-1"
								type="checkbox"
								class="apps-checkbox"
								style="margin-right: 0.5rem"
								bind:indeterminate={applicationsRowsSelectedTrue}
								on:click={(e) => {
									applicationsDropDownVisible = false;
									if (e.target.checked) {
										applicationsRowsSelected = $applications;
										applicationsRowsSelectedTrue = false;
										applicationsAllRowsSelectedTrue = true;
									} else {
										applicationsAllRowsSelectedTrue = false;
										applicationsRowsSelectedTrue = false;
										applicationsRowsSelected = [];
									}
								}}
								checked={applicationsAllRowsSelectedTrue}
							/>
						</td>
					{/if}
					<td style="line-height: 2.2rem">Application</td>
					<td>Group</td>
				</tr>

				{#if $applications.length > 0}
					{#each $applications as app, i}
						<tr>
							{#if (($permissionsByGroup && $permissionsByGroup.find((groupPermission) => groupPermission?.isApplicationAdmin)) || $isAdmin) && !applicationDetailVisible}
								<td style="width: 2rem">
									<input
										tabindex="-1"
										type="checkbox"
										class="apps-checkbox"
										checked={applicationsAllRowsSelectedTrue}
										on:change={(e) => {
											applicationsDropDownVisible = false;
											if (e.target.checked === true) {
												applicationsRowsSelected.push(app);
												applicationsRowsSelectedTrue = true;
											} else {
												applicationsRowsSelected = applicationsRowsSelected.filter(
													(selection) => selection !== app
												);
												if (applicationsRowsSelected.length === 0) {
													applicationsRowsSelectedTrue = false;
												}
											}
										}}
									/>
								</td>
							{/if}
							<td
								style="cursor: pointer; width: 20.8rem; line-height: 2.2rem"
								on:click={() => {
									loadApplicationDetail(app.id, app.group);
									headerTitle.set(app.name);
									detailView.set(true);
								}}
								on:keydown={(event) => {
									if (event.which === returnKey) {
										loadApplicationDetail(app.id, app.group);
									}
								}}
								>{app.name}
							</td>
							<td style="width: fit-content">{app.groupName}</td>

							{#if ($permissionsByGroup && $permissionsByGroup.find((groupPermission) => groupPermission.groupId === app.group))?.isApplicationAdmin || $isAdmin}
								<td
									style="cursor: pointer"
									on:keydown={(event) => {
										if (event.which === returnKey) {
											editApplicationNameVisible = true;
										}
									}}
								>
									<img
										src={editSVG}
										height="17rem"
										width="17rem"
										style="margin-left: 2rem"
										alt="edit user"
										on:click={() => {
											previousAppName = app.name;
											selectedAppGroupId = app.group;
											selectedAppId = app.id;
											editApplicationNameVisible = true;
										}}
									/>
								</td>

								<td style="cursor: pointer; text-align: right; padding-right: 0.25rem">
									<img
										src={deleteSVG}
										alt="delete application"
										width="27rem"
										style="cursor: pointer"
										on:click={() => {
											selectedAppId = app.id;
											selectedAppName = app.name;
											deleteApplicationVisible = true;
										}}
										on:click={() => {
											if (!applicationsRowsSelected.some((application) => application === app))
												applicationsRowsSelected.push(app);
											deleteApplicationVisible = true;
										}}
									/>
								</td>
							{/if}
						</tr>
					{/each}
				{/if}
			</table>
		{:else if !$applications && !applicationDetailVisible && applicationListVisible}
			<p>No Applications Found</p>
		{/if}

		{#if $applications && applicationDetailVisible && !applicationListVisible}
			<table style="width: 35rem; margin-top: 2rem">
				<tr style="border-width: 0px">
					<td style="width: 10rem">Group</td>
					<td style="width: 20rem">Topic</td>
					<td style="width: 5rem">Access</td>
				</tr>
				{#if $applicationPermission}
					{#each $applicationPermission as appPermission}
						<tr style="line-height: 2rem">
							<td>
								{appPermission.topicGroup}
							</td><td>
								{appPermission.topicName}
							</td>
							<td>
								{appPermission.accessType === 'READ_WRITE'
									? 'READ & WRITE'
									: appPermission.accessType}
							</td>
						</tr>
					{/each}
				{:else}
					<p>No Topics Associated</p>
				{/if}
			</table>
			<div
				style="font-size: 0.7rem; width:37.5rem; text-align:right; float: right; margin-top: 1rem"
			>
				{#if $applicationPermission}
					{$applicationPermission.length} of {$applicationPermission.length}
				{:else}
					0 of 0
				{/if}
			</div>

			{#if ($permissionsByGroup && $permissionsByGroup.find((groupPermission) => groupPermission.groupId === selectedAppGroupId))?.isApplicationAdmin || $isAdmin}
				<button
					style="width: 13.5rem; height: 3rem; margin-top: 4rem; padding: 0 1rem 0 1rem;"
					class="button-blue"
					on:click={() => generatePassword(selectedAppId)}
				>
					<img
						src={lockSVG}
						alt="generate password"
						height="20rem"
						style="vertical-align: middle; filter: invert(); margin-right: 0.4rem; margin-left: -0.5rem"
					/>
					<span style="vertical-align: middle">Generate Password</span>
				</button>

				<div style="margin-top: 1.5rem; font-weight: 500;  font-size: 0.9rem">
					Username: <span style="font-weight: 300">{selectedAppId}</span>
				</div>
				<div style="font-weight: 500;  font-size: 0.9rem; margin-top: 0.5rem;">
					Group ID: <span style="font-weight: 300">{selectedAppGroupId}</span>
				</div>

				{#if generateCredentialsVisible}
					<div style="margin-top: 1.5rem; font-weight: 500; font-size: 0.9rem">Password</div>
					<div
						style="margin-top: 0.3rem;  font-weight: 300; cursor: pointer"
						on:click={() => {
							copyPassword(selectedAppId);
							showCopyNotification();
						}}
					>
						<span style="vertical-align: middle">{password}</span>
						<img
							src={copySVG}
							alt="copy password"
							height="29rem"
							style="transform: scaleY(-1); filter: contrast(25%); vertical-align: middle; margin-left: 1rem"
							on:click={() => {
								copyPassword(selectedAppId);
								showCopyNotification();
							}}
						/>
					</div>

					{#if showCopyNotificationVisible}
						<div class="bubble">Password Copied!</div>
					{/if}
				{/if}
			{/if}
		{/if}
	</div>
	<div class="pagination">
		<span>Rows per page</span>
		<select
			tabindex="-1"
			on:change={(e) => {
				applicationsPerPage = e.target.value;
				reloadAllApps();
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
			{#if applicationsTotalSize > 0}
				{1 + applicationsCurrentPage * applicationsPerPage}
			{:else}
				0
			{/if}
			-{Math.min(applicationsPerPage * (applicationsCurrentPage + 1), applicationsTotalSize)} of
			{applicationsTotalSize}
		</span>
		<img
			src={pagefirstSVG}
			alt="first page"
			class="pagination-image"
			class:disabled-img={applicationsCurrentPage === 0}
			on:click={() => {
				deselectAllApplicationsCheckboxes();
				if (applicationsCurrentPage > 0) {
					applicationsCurrentPage = 0;
					reloadAllApps();
				}
			}}
		/>
		<img
			src={pagebackwardsSVG}
			alt="previous page"
			class="pagination-image"
			class:disabled-img={applicationsCurrentPage === 0}
			on:click={() => {
				deselectAllApplicationsCheckboxes();
				if (applicationsCurrentPage > 0) {
					applicationsCurrentPage--;
					reloadAllApps(applicationsCurrentPage);
				}
			}}
		/>
		<img
			src={pageforwardSVG}
			alt="next page"
			class="pagination-image"
			class:disabled-img={applicationsCurrentPage + 1 === applicationsTotalPages}
			on:click={() => {
				deselectAllApplicationsCheckboxes();
				if (applicationsCurrentPage + 1 < applicationsTotalPages) {
					applicationsCurrentPage++;
					reloadAllApps(applicationsCurrentPage);
				}
			}}
		/>
		<img
			src={pagelastSVG}
			alt="last page"
			class="pagination-image"
			class:disabled-img={applicationsCurrentPage + 1 === applicationsTotalPages}
			on:click={() => {
				deselectAllApplicationsCheckboxes();
				if (applicationsCurrentPage < applicationsTotalPages) {
					applicationsCurrentPage = applicationsTotalPages - 1;
					reloadAllApps(applicationsCurrentPage);
				}
			}}
		/>
	</div>
{/if}

<style>
	.content {
		min-width: 25rem;
		width: fit-content;
	}

	.dot {
		float: right;
	}

	.dropdown {
		margin-top: 8.5rem;
		margin-right: 9.5rem;
		width: 14rem;
		height: 5rem;
	}

	table.application-table-admin {
		width: 34rem;
		line-height: 1rem;
	}

	table {
		width: 25rem;
	}

	tr {
		height: 2rem;
	}

	span {
		position: relative;
		left: 0;
	}

	.bubble {
		position: relative;
		background: rgb(0, 0, 0);
		color: #ffffff;
		font-family: Arial;
		font-size: 15px;
		vertical-align: middle;
		text-align: center;
		width: 10.2rem;
		height: 25px;
		border-radius: 10px;
		padding-top: 8px;
		top: 1rem;
		left: 0.75rem;
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
