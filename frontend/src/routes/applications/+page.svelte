<script>
	import { onMount, onDestroy } from 'svelte';
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import applicationPermission from '../../stores/applicationPermission';
	import refreshPage from '../../stores/refreshPage';
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
	import errorMessages from '$lib/errorMessages.json';
	import renderAvatar from '../../stores/renderAvatar';
	import userValidityCheck from '../../stores/userValidityCheck';

	export let data, errors;

	// Redirects the User to the Login screen if not authenticated
	$: if (browser) {
		setTimeout(() => {
			if (!$isAuthenticated) goto(`/`, true);
		}, waitTime);
	}

	$: if (browser && (addApplicationVisible || deleteApplicationVisible || errorMessageVisible)) {
		document.body.classList.add('modal-open');
	} else if (
		browser &&
		!(addApplicationVisible || deleteApplicationVisible || errorMessageVisible)
	) {
		document.body.classList.remove('modal-open');
	}

	// Promises
	let promise, promiseDetail;

	// Constants
	const fiveSeconds = 5000;
	const returnKey = 13;
	const searchStringLength = 3;
	const waitTime = 1000;

	// Authentication
	let isApplicationAdmin = false;

	// Error Handling
	let errorMsg, errorObject;

	// Password & Tokens
	let password;
	let bindToken;

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

	// Forms
	let generateCredentialsVisible = false;
	let generateBindTokenVisible = false;
	let showCopyPasswordNotificationVisible = false;
	let showCopyBindTokenNotificationVisible = false;

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

	// Application Detail
	let applicationDetailId, ApplicationDetailGroupId;

	// Return to List view
	$: if ($detailView === 'backToList') {
		headerTitle.set('Applications');
		reloadAllApps();
		returnToApplicationsList();
	}

	// Search Feature
	$: if (searchString?.trim().length >= searchStringLength && $urlparameters === null) {
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

	// Reset add group form once closed
	$: if (addApplicationVisible === false) {
		searchGroups = '';
		appName = '';
	}

	onMount(async () => {
		if ($urlparameters?.type === 'prepopulate') {
			searchString = $urlparameters.data;
		}

		detailView.set('first run');

		headerTitle.set('Applications');

		promise = reloadAllApps();

		setTimeout(() => renderAvatar.set(true), 40);

		if ($permissionsByGroup) {
			isApplicationAdmin = $permissionsByGroup.some(
				(groupPermission) => groupPermission.isApplicationAdmin === true
			);
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
		}
	});

	onDestroy(() => {
		renderAvatar.set(false);
		urlparameters.set([]);
	});

	const errorMessage = (errMsg, errObj) => {
		errorMsg = errMsg;
		errorObject = errObj;
		errorMessageVisible = true;
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
			}
		}

		try {
			await httpAdapter.post(`/applications/save`, {
				name: forwardedAppName,
				group: forwardedSelectedGroup
			});
			addApplicationVisible = false;
		} catch (err) {
			if (err.response.data && err.response.status === 400) {
				const decodedError = decodeError(Object.create(...err.response.data));
				errorMessage(
					'Error Adding Application',
					errorMessages[decodedError.category][decodedError.code]
				);
			}
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
			userValidityCheck.set(true);
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
				if (err.response.data && err.response.status === 400) {
					const decodedError = decodeError(Object.create(...err.response.data));
					errorMessage(
						'Error Saving Application',
						errorMessages[decodedError.category][decodedError.code]
					);
				}
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
		promiseDetail = await getAppPermissions(appId);
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
		generateBindTokenVisible = false;
		password = '';
		bindToken = '';
		applicationDetailVisible = false;
		applicationListVisible = true;
	};

	const returnToAppsList = () => {
		applicationDetailVisible = false;
		applicationListVisible = true;
	};

	const copyPassword = async () => {
		navigator.clipboard.writeText(password);
	};

	const copyBindToken = async () => {
		navigator.clipboard.writeText(bindToken);
	};

	const generatePassword = async (applicationId) => {
		try {
			const res = await httpAdapter.get(`/applications/generate_passphrase/${applicationId}`);
			password = res.data;
			generateCredentialsVisible = true;
		} catch (err) {
			errorMessage('Error Generating Passphrase', err.message);
		}
	};

	const generateBindToken = async (applicationId) => {
		try {
			const res = await httpAdapter.get(`/applications/generate_bind_token/${applicationId}`);
			bindToken = res.data;
			generateBindTokenVisible = true;
		} catch (err) {
			errorMessage('Error Generating Token', err.message);
		}
	};

	const showCopyPasswordNotification = () => {
		showCopyPasswordNotificationVisible = true;
		setTimeout(() => {
			showCopyPasswordNotificationVisible = false;
		}, fiveSeconds);
	};

	const showCopyBindTokenNotification = () => {
		showCopyBindTokenNotificationVisible = true;
		setTimeout(() => {
			showCopyBindTokenNotificationVisible = false;
		}, fiveSeconds);
	};

	const deselectAllApplicationsCheckboxes = () => {
		applicationsAllRowsSelectedTrue = false;
		applicationsRowsSelectedTrue = false;
		applicationsRowsSelected = [];
		let checkboxes = document.querySelectorAll('.apps-checkbox');
		checkboxes.forEach((checkbox) => (checkbox.checked = false));
	};

	const numberOfSelectedCheckboxes = () => {
		let checkboxes = document.querySelectorAll('.apps-checkbox');
		checkboxes = Array.from(checkboxes);
		return checkboxes.filter((checkbox) => checkbox.checked === true).length;
	};

	const deleteTopicApplicationAssociation = async (permissionId) => {
		await httpAdapter.delete(`/application_permissions/${permissionId}`);
		await loadApplicationDetail(applicationDetailId, ApplicationDetailGroupId);
	};

	const decodeError = (errorObject) => {
		errorObject = errorObject.code.replaceAll('-', '_');
		const cat = errorObject.substring(0, errorObject.indexOf('.'));
		const code = errorObject.substring(errorObject.indexOf('.') + 1, errorObject.length);
		return { category: cat, code: code };
	};
</script>

<svelte:head>
	<title>Applications | DDS Permissions Manager</title>
	<meta name="description" content="DDS Permissions Manager Applications" />
</svelte:head>
{#key $refreshPage}
	{#if $isAuthenticated}
		{#await promise then _}
			{#if errorMessageVisible}
				<Modal
					title={errorMsg}
					errorMsg={true}
					errorDescription={errorObject}
					closeModalText={'Close'}
					on:cancel={() => (errorMessageVisible = false)}
					on:keydown={(event) => {
						if (event.which === returnKey) {
							errorMessageVisible = false;
						}
					}}
				/>
			{/if}
			{#if deleteApplicationVisible && !errorMessageVisible}
				<Modal
					actionDeleteApplications={true}
					title="Delete {applicationsRowsSelected.length > 1 ? 'Applications' : 'Application'}"
					on:cancel={() => {
						if (applicationsRowsSelected?.length === 1 && numberOfSelectedCheckboxes() === 0)
							applicationsRowsSelected = [];

						deleteApplicationVisible = false;
					}}
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
					<h1 data-cy="applications">Applications</h1>

					<form class="searchbox">
						<input
							data-cy="search-applications-table"
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

					{#if (isApplicationAdmin || $isAdmin) && !applicationDetailVisible}
						<div
							data-cy="dot-applications"
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
											if (!applicationsDropDownMouseEnter) applicationsDropDownVisible = false;
										}, waitTime);
										applicationsDropDownMouseEnter = false;
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
										<td style="padding-left: 0; vertical-align: middle">
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
										data-cy="add-application"
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
											style="height: 2.2rem; padding-left: 0; vertical-align: middle;border-bottom-color: transparent"
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

				{#if $applications && $applications.length > 0 && applicationListVisible && !applicationDetailVisible}
					<table
						data-cy="applications-table"
						class="main-table"
						style="margin-top: 0.5rem"
						class:application-table-admin={isApplicationAdmin || $isAdmin}
					>
						<thead>
							<tr style="border-top: 1px solid black; border-bottom: 2px solid">
								{#if (isApplicationAdmin || $isAdmin) && !applicationDetailVisible}
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
								<td style="text-align:right; padding-right: 1rem">Group</td>
							</tr>
						</thead>

						{#if $applications.length > 0}
							<tbody>
								{#each $applications as app, i}
									<tr>
										{#if (isApplicationAdmin || $isAdmin) && !applicationDetailVisible}
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
											style="cursor: pointer; line-height: 2.2rem"
											on:click={() => {
												applicationDetailId = app.id;
												ApplicationDetailGroupId = app.group;

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
										<td style="padding-right: 1rem; text-align: right">{app.groupName}</td>

										{#if ($permissionsByGroup && $permissionsByGroup.find((groupPermission) => groupPermission.groupId === app.group))?.isApplicationAdmin || $isAdmin}
											<td
												style="cursor: pointer; width:1rem"
												on:keydown={(event) => {
													if (event.which === returnKey) {
														editApplicationNameVisible = true;
													}
												}}
											>
												<img
													data-cy="edit-application-icon"
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

											<td
												style="cursor: pointer; text-align: right; padding-right: 0.25rem; width:1rem"
											>
												<img
													data-cy="delete-application-icon"
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
														if (
															!applicationsRowsSelected.some((application) => application === app)
														)
															applicationsRowsSelected.push(app);
														deleteApplicationVisible = true;
													}}
												/>
											</td>
										{/if}
									</tr>
								{/each}
							</tbody>
						{/if}
					</table>
				{:else if !applicationDetailVisible && applicationListVisible}
					<p>No Applications Found</p>
				{/if}
				{#await promiseDetail then _}
					{#if $applications && applicationDetailVisible && !applicationListVisible}
						<table style="width: 35rem; margin-top: 2rem">
							<thead>
								<tr style="border-width: 0px">
									<td>Group</td>
									<td>Topic</td>
									<td>Access</td>
									{#if isApplicationAdmin || $isAdmin}
										<td />
									{/if}
								</tr>
							</thead>
							{#if $applicationPermission}
								{#each $applicationPermission as appPermission}
									<tbody>
										<tr style="line-height: 2rem">
											<td>
												{appPermission.topicGroup}
											</td>
											<td>
												{appPermission.topicName}
											</td>
											<td>
												{appPermission.accessType === 'READ_WRITE'
													? 'READ & WRITE'
													: appPermission.accessType}
											</td>
											{#if isApplicationAdmin || $isAdmin}
												<td>
													<img
														src={deleteSVG}
														alt="delete topic"
														height="23px"
														width="23px"
														style="vertical-align: -0.4rem; float: right; cursor: pointer"
														on:click={() => {
															deleteTopicApplicationAssociation(appPermission.id);
														}}
													/>
												</td>
											{/if}
										</tr>
									</tbody>
								{/each}
							{:else}
								<p style="margin:0.3rem 0 0.6rem 0">No Topics Associated</p>
							{/if}
						</table>
						<div
							style="font-size: 0.7rem; width:17.5rem; text-align:right; float: right; margin-top: 1rem"
						>
							{#if $applicationPermission}
								{$applicationPermission.length} of {$applicationPermission.length}
							{:else}
								0 of 0
							{/if}
						</div>

						{#if ($permissionsByGroup && $permissionsByGroup.find((groupPermission) => groupPermission.groupId === selectedAppGroupId))?.isApplicationAdmin || $isAdmin}
							<div style="display: inline-flex; height: 7rem; margin-left: -1rem">
								<button
									data-cy="generate-bind-token-button"
									style="width: 13.5rem; height: 3rem; padding: 0 1rem; margin: 4rem 1rem;"
									class="button-blue"
									on:click={() => generateBindToken(selectedAppId)}
								>
									<img
										src={lockSVG}
										alt="generate bind token"
										height="20rem"
										style="vertical-align: middle; filter: invert(); margin-right: 0.4rem; margin-left: -0.5rem"
									/>
									<span style="vertical-align: middle">Generate Bind Token</span>
								</button>

								<button
									data-cy="generate-password-button"
									style="width: 13.5rem; height: 3rem; margin-top: 4rem; padding: 0 1rem;"
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
							</div>
							<div style="margin-top: 1.5rem; font-weight: 500;  font-size: 0.9rem">
								Username: <span style="font-weight: 300">{selectedAppId}</span>
							</div>
							<div style="font-weight: 500;  font-size: 0.9rem; margin-top: 0.5rem;">
								Group ID: <span style="font-weight: 300">{selectedAppGroupId}</span>
							</div>

							{#if generateCredentialsVisible}
								<div style="margin-top: 2rem; font-weight: 500; font-size: 0.9rem">Password</div>
								<div
									style="margin-top: 0.3rem;  font-weight: 300; cursor: pointer"
									on:click={() => {
										copyPassword(selectedAppId);
										showCopyPasswordNotification();
									}}
								>
									<span data-cy="generated-password" style="vertical-align: middle">{password}</span
									>
									<img
										src={copySVG}
										alt="copy password"
										height="29rem"
										style="transform: scaleY(-1); filter: contrast(25%); vertical-align: middle; margin-left: 1rem"
										on:click={() => {
											copyPassword(selectedAppId);
											showCopyPasswordNotification();
										}}
									/>
								</div>

								{#if showCopyPasswordNotificationVisible}
									<div class="bubble">Password Copied!</div>
								{/if}
							{/if}

							{#if generateBindTokenVisible}
								<div style="margin-top: 2rem; font-weight: 500; font-size: 0.9rem">
									Bind Token
									<img
										src={copySVG}
										alt="copy bind token"
										height="29rem"
										style="transform: scaleY(-1); filter: contrast(25%); vertical-align: middle; margin-left: 1rem"
										on:click={() => {
											copyBindToken(selectedAppId);
											showCopyBindTokenNotification();
										}}
									/>
									<textarea
										rows="4"
										cols="50"
										style="vertical-align: middle; width: 50vw; margin-top: 1rem"
										>{bindToken}</textarea
									>
								</div>

								{#if showCopyBindTokenNotificationVisible}
									<div class="bubble">Bind Token Copied!</div>
								{/if}
							{/if}
						{/if}
					{/if}
				{/await}
			</div>

			{#if !applicationDetailVisible}
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
						- {Math.min(applicationsPerPage * (applicationsCurrentPage + 1), applicationsTotalSize)}
						of
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
						class:disabled-img={applicationsCurrentPage + 1 === applicationsTotalPages ||
							$applications?.length === undefined}
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
						class:disabled-img={applicationsCurrentPage + 1 === applicationsTotalPages ||
							$applications?.length === undefined}
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
			<p style="margin-top: 8rem">© 2022 Unity Foundation. All rights reserved.</p>
		{/await}
	{/if}
{/key}

<style>
	.content {
		width: fit-content;
		min-width: 32rem;
		margin-right: 1rem;
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

	.main-table {
		min-width: 34rem;
	}

	tr {
		height: 2.2rem;
	}

	span {
		position: relative;
		left: 0;
	}

	p {
		font-size: large;
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
