<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount, createEventDispatcher } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import { inview } from 'svelte-inview';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import topicDetails from '../../stores/groupDetails';
	import Modal from '../../lib/Modal.svelte';
	import headerTitle from '../../stores/headerTitle';
	import detailView from '../../stores/detailView';
	import deleteSVG from '../../icons/delete.svg';
	import duplicateSVG from '../../icons/duplicate.svg';
	import errorMessages from '$lib/errorMessages.json';

	export let selectedTopicName,
		selectedTopicId,
		selectedTopicKind,
		selectedTopicGroupName,
		selectedTopicGroupId;

	let selectedTopicCanonicalName;
	let selectedTopicApplications = [];
	let selectedApplicationList;

	const dispatch = createEventDispatcher();

	// Promises
	let promise;

	// Modals
	let errorMessageVisible = false;
	let addTopicVisible = false;
	let duplicateTopicVisible = false;

	// Constants
	const applicationsResult = 7;
	const waitTime = 1000;
	const returnKey = 13;
	const searchStringLength = 3;
	const applicationsDropdownSuggestion = 7;

	// Error Handling
	let errorMsg, errorObject;
	let errorMessageApplication = '';

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
	let applicationResultPage = 0;
	let hasMoreApps = true;
	let stopSearchingApps = false;
	let searchApplicationsResultsMouseEnter = false;
	let timer;

	// Search Applications Feature
	$: if (
		searchApplications?.trim().length >= searchStringLength &&
		searchApplicationActive &&
		!stopSearchingApps
	) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchApplications = searchApplications.trim();
			searchApplication(searchApplications);
			stopSearchingApps = true;
		}, waitTime);
	}

	// Search Applications Dropdown Visibility
	$: if (
		searchApplicationResults?.length >= 1 &&
		searchApplicationActive &&
		searchApplications?.trim().length >= searchStringLength
	) {
		searchApplicationsResultsVisible = true;
	} else {
		searchApplicationsResultsVisible = false;
	}

	onMount(async () => {
		try {
			promise = await httpAdapter.get(`/topics/show/${selectedTopicId}`);
			topicDetails.set(promise.data);

			await loadApplicationPermissions(selectedTopicId);
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
		let res = await httpAdapter.get(
			`/applications/search?page=${applicationResultPage}&size=${applicationsDropdownSuggestion}&filter=${searchString}`
		);

		if (res.data.length < applicationsDropdownSuggestion) {
			hasMoreApps = false;
		}
		const applicationPermissions = await httpAdapter.get(
			`/application_permissions/?topic=${selectedTopicId}`
		);

		if (res.data?.length > 0) {
			if (selectedTopicApplications?.length > 0)
				for (const selectedApp of selectedTopicApplications) {
					res.data = res.data.filter((results) => results.name !== selectedApp.applicationName);
				}

			searchApplicationResults = [...searchApplicationResults, ...res.data];
		}

		searchApplicationResults.forEach((app) => {
			applicationPermissions.data.content?.forEach((appPermission) => {
				if (app.name === appPermission.applicationName) {
					searchApplicationResults.data = searchApplicationResults.filter(
						(result) => result.name !== appPermission.applicationName
					);
				}
			});
		});
	};

	const validateApplicationName = async () => {
		// if there is data in the applications input field, we verify it's validity
		if (searchApplications?.length > 0) {
			const res = await httpAdapter.get(
				`/applications/search?page=0&size=1&filter=${searchApplications}`
			);

			if (
				res.data.length > 0 &&
				res.data?.[0].name?.toUpperCase() === searchApplications.toUpperCase()
			) {
				selectedApplicationList = {
					id: res.data[0].id,
					accessType: 'READ'
				};

				searchApplications = '';
				await addTopicApplicationAssociation(selectedTopicId, true);

				selectedApplicationList = '';
				searchApplicationResults = [];

				return true;
			} else {
				searchApplicationActive = false;
				errorMessageApplication = errorMessages['application']['not_found'];

				return false;
			}
		} else {
			return true;
		}
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
				.then(async () => await loadApplicationPermissions(topicId));
		}
	};

	const deleteTopicApplicationAssociation = async (permissionId, topicId) => {
		await httpAdapter.delete(`/application_permissions/${permissionId}`);
		await loadApplicationPermissions(topicId);
	};

	const updateTopicApplicationAssociation = async (permissionId, accessType, topicId) => {
		await httpAdapter.put(`/application_permissions/${permissionId}/${accessType}`);
		await loadApplicationPermissions(topicId);
	};

	const loadMoreResultsApp = (e) => {
		if (e.detail.inView && hasMoreApps) {
			applicationResultPage++;
			searchApplication(searchApplications);
		}
	};

	const options = {
		rootMargin: '20px',
		unobserveOnEnter: true
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

	{#await promise then _}
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
							on:keydown={(event) => {
								searchApplicationResults = [];
								stopSearchingApps = false;
								hasMoreApps = true;
								applicationResultPage = 0;

								if (event.which === returnKey) {
									document.activeElement.blur();

									validateApplicationName();
									searchApplicationActive = false;
								}
							}}
							on:blur={() => {
								searchApplications = searchApplications?.trim();
								setTimeout(() => {
									searchApplicationsResultsVisible = false;
								}, waitTime);
							}}
							on:focus={() => {
								searchApplicationActive = true;
								errorMessageApplication = '';
							}}
							on:focusout={() => {
								setTimeout(() => {
									searchApplicationsResultsVisible = false;
								}, waitTime);
							}}
							on:click={async () => {
								if (searchApplications?.length === 0) searchApplicationResults = [];

								searchApplicationActive = true;
								errorMessageApplication = '';
								if (searchApplicationResults?.length > 0) {
									searchApplicationsResultsVisible = true;
								}
							}}
							on:mouseleave={() => {
								setTimeout(() => {
									if (!searchApplicationsResultsMouseEnter)
										searchApplicationsResultsVisible = false;
								}, waitTime);
							}}
						/>
						<div style="margin-left: 7.4rem">
							<span class="error-message" class:hidden={errorMessageApplication?.length === 0}>
								{errorMessageApplication}
							</span>
						</div>
					</td>
				{/if}
			</tr>
		</table>

		{#if searchApplicationsResultsVisible}
			<table
				class="search-application"
				style="position:absolute; margin-left: 17rem; margin-top: -0.8rem; width: 12rem; max-height: 13.3rem; display: block; overflow-y: auto"
				on:mouseenter={() => (searchApplicationsResultsMouseEnter = true)}
				on:mouseleave={() => {
					setTimeout(() => {
						if (!searchApplicationsResultsMouseEnter) searchApplicationsResultsVisible = false;
					}, waitTime);
					searchApplicationsResultsMouseEnter = false;
				}}
			>
				{#each searchApplicationResults as result}
					<tr style="border-width: 0px">
						<td
							style="width: 14rem; padding-left: 0.5rem"
							on:click={async () => {
								searchApplications = result.name;
								searchApplicationsId = result.id;

								searchApplicationActive = false;

								selectedApplicationList = {
									id: searchApplicationsId,
									accessType: 'READ'
								};

								await addTopicApplicationAssociation(selectedTopicId, true);
								searchApplications = '';
								selectedApplicationList = '';
								searchApplicationResults = [];
							}}>{result.name} ({result.groupName})</td
						>
					</tr>
				{/each}
				<div use:inview={{ options }} on:change={loadMoreResultsApp} />
			</table>
		{/if}

		{#if selectedTopicApplications}
			<div>
				{#each selectedTopicApplications as application}
					<div
						style="display: flex; font-size: 0.8rem; margin-left: 16.5rem; margin-top: 1rem; margin-bottom: -0.2rem"
					>
						<span style="width: 10.5rem">
							{application.applicationName} ({application.applicationGroupName})
						</span>
						<span style="border-bottom-color: transparent; padding-left: 0.5rem">Access Type:</span>

						<span
							style="border-bottom-color: transparent; margin-right: -1.5rem; margin-top: -2rem"
						>
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
		<p style="margin-top: 8rem">© 2022 Unity Foundation. All rights reserved.</p>
	{/await}
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
