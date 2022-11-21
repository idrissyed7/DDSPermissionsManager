<script>
	import { fade, fly } from 'svelte/transition';
	import { createEventDispatcher } from 'svelte';
	import { httpAdapter } from '../appconfig';
	import closeSVG from '../icons/close.svg';
	import deleteSVG from '../icons/delete.svg';
	import Switch from './Switch.svelte';

	export let title;
	export let email = false;
	export let topicName = false;
	export let applicationName = false;
	export let groupNewName = false;
	export let group = false;
	export let adminRoles = false;
	export let application = false;
	export let actionAddUser = false;
	export let actionAddSuperUser = false;
	export let actionAddTopic = false;
	export let actionAddApplication = false;
	export let actionAddGroup = false;
	export let actionEditUser = false;
	export let actionEditApplicationName = false;
	export let actionDeleteUsers = false;
	export let actionDeleteSuperUsers = false;
	export let actionDeleteTopics = false;
	export let actionDeleteGroups = false;
	export let actionDeleteApplications = false;
	export let actionDuplicateTopic = false;
	export let noneditable = false;
	export let emailValue = '';
	export let newTopicName = '';
	export let anyApplicationCanRead = false;
	export let searchGroups = '';
	export let searchApplications = '';
	export let selectedGroupMembership = '';
	export let previousAppName = '';
	export let selectedApplicationList = [];

	const dispatch = createEventDispatcher();

	// Constants
	const returnKey = 13;
	const groupsDropdownSuggestion = 7;
	const minNameLength = 3;
	const applicationsResult = 7;
	const searchStringLength = 3;
	const waitTime = 250;

	// Forms
	let selectedIsGroupAdmin = false;
	let selectedIsTopicAdmin = false;
	let selectedIsApplicationAdmin = false;
	let appName = '';
	let newGroupName = '';

	// Error Handling
	let invalidTopic = false;
	let invalidGroup = false;
	let invalidApplication = false;
	let invalidEmail = false;
	let errorMessage = '';
	let validRegex =
		/^([a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+(?:\.[a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)$/gm;

	// Search Applications
	let searchApplicationActive = true;
	let searchApplicationResults;
	let searchApplicationsResultsVisible = false;

	// SearchBox
	let searchString;
	let searchGroupsResultsMouseEnter = false;
	let searchGroupResults;
	let searchGroupsResultsVisible = false;
	let searchGroupActive = false;
	let selectedGroup;
	let timer;
	let searchApplicationsResultsMouseEnter = false;

	// Search Groups Feature
	$: if (searchGroups?.trim().length >= searchStringLength && searchGroupActive) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchGroup(searchGroups.trim());
		}, waitTime);
	} else {
		searchGroupsResultsVisible = false;
		errorMessage = '';
	}

	// Search Groups Dropdown Visibility
	$: if (searchGroupResults?.data?.content?.length >= 1 && searchGroupActive) {
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

	const searchApplication = async (searchString) => {
		setTimeout(async () => {
			searchApplicationResults = await httpAdapter.get(
				`/applications/search?page=0&size=${applicationsResult}&filter=${searchString}`
			);
		}, 1000);
	};

	const selectedSearchApplication = (appName, appId, groupName) => {
		selectedApplicationList.push({
			applicationId: appId,
			applicationName: appName,
			applicationGroup: groupName,
			accessType: 'READ'
		});

		// This statement is used to trigger Svelte reactivity and re-render the component
		selectedApplicationList = selectedApplicationList;
		searchApplications = appName;
		searchApplicationsResultsVisible = false;
		searchApplicationActive = false;
	};

	const validateEmail = (input) => {
		input.match(validRegex) ? (invalidEmail = false) : (invalidEmail = true);
	};

	const validateGroupName = async () => {
		const res = await httpAdapter.get(
			`/groups?page=0&size=${groupsDropdownSuggestion}&filter=${searchGroups}`
		);
		if (
			res.data.content?.some((group) => group.name.toUpperCase() === searchGroups.toUpperCase())
		) {
			return true;
		} else {
			return false;
		}
	};

	function closeModal() {
		dispatch('cancel');
	}

	const actionAddUserEvent = async () => {
		let newGroupMembership = {
			selectedIsGroupAdmin: selectedIsGroupAdmin,
			selectedIsTopicAdmin: selectedIsTopicAdmin,
			selectedIsApplicationAdmin: selectedIsApplicationAdmin,
			selectedGroup: selectedGroup,
			searchGroups: searchGroups,
			emailValue: emailValue
		};

		const validGroupName = await validateGroupName(searchGroups);
		if (!validGroupName) {
			errorMessage = 'Invalid Group';
			return;
		}

		if (!invalidEmail || searchGroups?.length >= minNameLength)
			dispatch('addGroupMembership', newGroupMembership);
	};

	const actionAddSuperUserEvent = () => {
		validateEmail(emailValue);
		if (!invalidEmail) {
			dispatch('addSuperUser', emailValue);
			closeModal();
		}
	};

	const actionAddTopicEvent = async () => {
		let newTopic = {
			newTopicName: newTopicName,
			searchGroups: searchGroups,
			selectedGroup: selectedGroup,
			anyApplicationCanRead: anyApplicationCanRead,
			selectedApplicationList: selectedApplicationList
		};

		const validGroupName = await validateGroupName(searchGroups);
		if (!validGroupName) {
			errorMessage = 'Invalid Group';
			return;
		}

		dispatch('addTopic', newTopic);
		closeModal();
	};

	const actionAddApplicationEvent = async () => {
		let newApplication = {
			appName: appName,
			searchGroups: searchGroups,
			selectedGroup: selectedGroup
		};

		const validGroupName = await validateGroupName(searchGroups);
		if (!validGroupName) {
			errorMessage = 'Invalid Group';
			return;
		}

		dispatch('addApplication', newApplication);
		closeModal();
	};

	const actionAddGroupEvent = async () => {
		let returnGroupName = {
			newGroupName: newGroupName
		};

		const res = await httpAdapter.get(
			`/groups?page=0&size=${groupsDropdownSuggestion}&filter=${newGroupName}`
		);

		console.log('res', res);
		if (res.data.content) {
			if (
				res.data.content.some((group) => group.name.toUpperCase() === newGroupName.toUpperCase())
			) {
				errorMessage = 'Group already exists';
				return;
			} else {
				dispatch('addGroup', returnGroupName);
				closeModal();
			}
		} else {
			dispatch('addGroup', returnGroupName);
			closeModal();
		}
	};

	const actionEditUserEvent = () => {
		dispatch('updateGroupMembership', {
			groupAdmin: selectedGroupMembership.groupAdmin,
			topicAdmin: selectedGroupMembership.topicAdmin,
			applicationAdmin: selectedGroupMembership.applicationAdmin
		});
	};

	const actionDuplicateTopicEvent = () => {
		let newTopic = {
			newTopicName: newTopicName,
			searchGroups: searchGroups,
			selectedGroup: selectedGroup,
			anyApplicationCanRead: anyApplicationCanRead,
			selectedApplicationList: selectedApplicationList
		};
		dispatch('duplicateTopic', newTopic);
		closeModal();
	};
</script>

<div class="modal-backdrop" on:click={closeModal} transition:fade />
<div class="modal" transition:fly={{ y: 300 }}>
	<img src={closeSVG} alt="close" class="close-button" on:click={closeModal} />
	<h2>{title}</h2>
	<hr />
	<div class="content">
		{#if email}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				autofocus
				disabled={noneditable}
				placeholder="Email"
				class:invalid={invalidEmail && emailValue?.length >= 1}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={emailValue}
				on:blur={() => {
					emailValue = emailValue.trim();
					validateEmail(emailValue);
				}}
				on:keydown={(event) => {
					if (event.which === returnKey) {
						if (actionAddUser) {
							emailValue = emailValue.trim();
							validateEmail(emailValue);
							if (!invalidEmail && emailValue.length >= minNameLength && searchGroups?.length > 3) {
								actionAddUserEvent();
							}
						}
						if (actionAddSuperUser && emailValue.length >= 10) actionAddSuperUserEvent();
					}
				}}
			/>
			{#if noneditable}
				<span
					style="display: inline-flex; font-size: 0.65rem; position: relative; top: -3rem; left: 0.5rem; background-color: rgb(246,246,246); padding: 0 0.2rem 0 0.2rem; color: rgb(120,120,120)"
					>Email
				</span>
			{/if}
		{/if}

		{#if topicName}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				autofocus
				placeholder="Topic Name"
				class:invalid={invalidTopic}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={newTopicName}
				on:blur={() => {
					newTopicName = newTopicName.trim();
					newTopicName?.length < minNameLength ? (invalidTopic = true) : (invalidTopic = false);
				}}
				on:keydown={(event) => {
					if (event.which === returnKey) {
						newTopicName = newTopicName.trim();
						if (
							newTopicName?.length >= minNameLength &&
							searchGroups?.length >= searchStringLength
						) {
							actionAddTopicEvent();
						}
					}
				}}
			/>
		{/if}

		{#if applicationName}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				autofocus
				placeholder="Application Name"
				class:invalid={invalidApplication}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={appName}
				on:blur={() => {
					appName = appName.trim();
					appName?.length < minNameLength
						? (invalidApplication = true)
						: (invalidApplication = false);
				}}
				on:keydown={(event) => {
					if (event.which === returnKey) {
						appName = appName.trim();

						if (appName?.length >= minNameLength && searchGroups?.length >= searchStringLength) {
							actionAddApplicationEvent();
						}
					}
				}}
			/>
		{/if}

		{#if groupNewName}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				autofocus
				placeholder="Group Name"
				class:invalid={invalidGroup}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={newGroupName}
				on:blur={() => {
					newGroupName = newGroupName.trim();
					newGroupName?.length < minNameLength ? (invalidGroup = true) : (invalidGroup = false);
				}}
				on:keydown={(event) => {
					errorMessage = '';
					if (event.which === returnKey) {
						newGroupName = newGroupName.trim();
						if (newGroupName?.length >= minNameLength) actionAddGroupEvent();
					}
				}}
			/>
			<span
				class="error-message"
				style="	top: 9.7rem; right: 4.2rem"
				class:hidden={errorMessage?.length === 0}
			>
				Error: {errorMessage}
			</span>
		{/if}

		{#if actionEditApplicationName}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				autofocus
				placeholder="Application Name"
				class:invalid={appName?.length < minNameLength}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={previousAppName}
				on:blur={() => (previousAppName = previousAppName.trim())}
				on:keydown={(event) => {
					if (event.which === returnKey) {
						previousAppName = previousAppName.trim();
					}
				}}
			/>
		{/if}

		{#if group}
			<form class="searchbox" style="margin-bottom: 0.7rem">
				<input
					class="searchbox"
					type="search"
					placeholder="Group"
					disabled={noneditable}
					bind:value={searchGroups}
					on:keydown={(event) => {
						if (event.which === returnKey) {
							document.activeElement.blur();
							searchString = searchString?.trim();

							if (actionAddUser) {
								validateEmail(emailValue);
								if (
									!invalidEmail &&
									emailValue.length >= minNameLength &&
									searchGroups?.length >= searchStringLength
								) {
									actionAddUserEvent();
								}
							}

							if (actionAddTopic) {
								if (
									newTopicName?.length >= minNameLength &&
									searchGroups?.length >= searchStringLength
								) {
									actionAddTopicEvent();
								}
							}

							if (actionAddApplication) {
								if (
									appName?.length >= minNameLength &&
									searchGroups?.length >= searchStringLength
								) {
									actionAddApplicationEvent();
								}
							}
						}
					}}
					on:blur={() => {
						searchString = searchString?.trim();
						setTimeout(() => {
							searchGroupsResultsVisible = false;
						}, 500);
					}}
					on:focus={async () => {
						searchGroupResults = [];
						searchGroupActive = true;
						selectedGroup = '';
						if (searchGroups?.length >= searchStringLength) {
							searchGroup(searchGroups);
						}
					}}
					on:click={async () => {
						searchGroupResults = [];
						searchGroupActive = true;
						selectedGroup = '';
						if (searchGroups?.length >= searchStringLength) {
							searchGroup(searchGroups);
						}
					}}
					on:mouseleave={() => {
						setTimeout(() => {
							if (!searchGroupsResultsMouseEnter) searchGroupsResultsVisible = false;
						}, waitTime);
					}}
				/>
			</form>
			<span
				class="error-message"
				style="	top: 12.9rem; right: 4.2rem"
				class:hidden={errorMessage?.length === 0}
			>
				Error: {errorMessage}
			</span>

			{#if searchGroupsResultsVisible}
				<table
					class="search-group"
					style="position: absolute; z-index: 100"
					on:mouseenter={() => (searchGroupsResultsMouseEnter = true)}
					on:mouseleave={() => {
						setTimeout(() => {
							searchGroupsResultsVisible = !searchGroupsResultsVisible;
							searchGroupsResultsMouseEnter = false;
						}, waitTime);
					}}
					on:focusout={() => {
						searchGroupsResultsVisible = !searchGroupsResultsVisible;
						searchGroupsResultsMouseEnter = false;
					}}
				>
					{#each searchGroupResults.data?.content as result}
						<tr>
							<td
								on:click={() => {
									selectedSearchGroup(result.name, result.id);
								}}
								>{result.name}
							</td>
						</tr>
					{/each}
				</table>
			{/if}

			{#if noneditable}
				<span
					style="display: inline-flex; font-size: 0.65rem; position: relative; top: -3.6rem; left: 0.5rem; background-color: rgb(246,246,246); padding: 0 0.2rem 0 0.2rem; color: rgb(120,120,120)"
					>Group
				</span>
			{/if}
		{/if}

		{#if topicName}
			<div style="display:flex; align-items: center; margin-top: 1rem">
				<span style="font-size:0.9rem; width: 9rem; margin-right: 0.5rem; ">
					Any application can read topic:
				</span>
				<Switch bind:checked={anyApplicationCanRead} />
			</div>
		{/if}

		{#if application}
			<form class="searchbox">
				<input
					class="searchbox"
					type="search"
					placeholder="Application"
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
					on:mouseleave={() => {
						setTimeout(() => {
							if (!searchApplicationsResultsMouseEnter) searchApplicationsResultsVisible = false;
						}, waitTime);
					}}
				/>
			</form>
		{/if}

		{#if searchApplicationsResultsVisible}
			<table
				class="search-application"
				style="position: absolute;"
				on:mouseenter={() => (searchApplicationsResultsMouseEnter = true)}
				on:mouseleave={() => {
					setTimeout(() => {
						searchApplicationsResultsVisible = !searchApplicationsResultsVisible;
						searchApplicationsResultsMouseEnter = false;
					}, waitTime);
				}}
			>
				{#each searchApplicationResults.data as result}
					<tr style="border-width: 0px;">
						<td
							on:click={() => {
								selectedSearchApplication(result.name, result.id, result.groupName);
								searchApplications = '';
							}}>{result.name} ({result.groupName})</td
						>
					</tr>
				{/each}
			</table>
		{/if}

		{#if selectedApplicationList?.length > 0}
			{#each selectedApplicationList as app}
				<div class="application-list">
					<ul style="list-style-type: none; padding-left: 0; margin-right: -3rem">
						<li style="width: 9rem">
							{app.applicationName}
						</li>
						<li style="color: rgb(120,120,120)">
							({app.applicationGroup})
						</li>
					</ul>

					<ul style="list-style-type: none; margin-right: -2rem">
						<li style="margin-top: -0.05rem"><span style="font-size: 0.65rem">Read</span></li>
						<li style="margin-top: -0.05rem">
							<input
								type="checkbox"
								name="read"
								style="width:unset;"
								checked
								on:change={(e) => {
									const applicationIndex = selectedApplicationList.findIndex(
										(application) => application.applicationName === app.applicationName
									);

									if (e.target.checked) {
										if (selectedApplicationList[applicationIndex].accessType === 'WRITE') {
											selectedApplicationList[applicationIndex].accessType = 'READ_WRITE';
										} else {
											selectedApplicationList[applicationIndex].accessType = 'READ';
										}
									} else {
										if (selectedApplicationList[applicationIndex].accessType === 'READ_WRITE') {
											selectedApplicationList[applicationIndex].accessType = 'WRITE';
										} else {
											selectedApplicationList[applicationIndex].accessType = '';
										}
									}
								}}
							/>
						</li>
					</ul>

					<ul style="list-style-type: none; margin-right: -2.4rem">
						<li style="margin-top: -0.05rem"><span style="font-size: 0.65rem">Write</span></li>
						<li style="margin-top: -0.05rem">
							<input
								type="checkbox"
								name="write"
								style="width:unset;"
								on:change={(e) => {
									const applicationIndex = selectedApplicationList.findIndex(
										(application) => application.applicationName === app.applicationName
									);

									if (e.target.checked) {
										if (selectedApplicationList[applicationIndex].accessType === 'READ') {
											selectedApplicationList[applicationIndex].accessType = 'READ_WRITE';
										} else {
											selectedApplicationList[applicationIndex].accessType = 'WRITE';
										}
									} else {
										if (selectedApplicationList[applicationIndex].accessType === 'READ_WRITE') {
											selectedApplicationList[applicationIndex].accessType = 'READ';
										} else {
											selectedApplicationList[applicationIndex].accessType = '';
										}
									}
								}}
							/>
						</li>
					</ul>

					<ul style="list-style-type: none; margin-top: 0.35rem; margin-left: -0.5rem">
						<li />
						<li>
							<img
								src={deleteSVG}
								alt="remove application"
								style="background-color: transparent; cursor: pointer; scale: 50%;"
								on:click={() => {
									selectedApplicationList = selectedApplicationList.filter(
										(selectedApplication) =>
											selectedApplication.applicationName != app.applicationName
									);
								}}
							/>
						</li>
					</ul>
				</div>
			{/each}
		{/if}

		{#if adminRoles}
			<div class="admin-roles">
				{#if noneditable}
					<Switch bind:checked={selectedGroupMembership.groupAdmin} />
				{:else}
					<Switch bind:checked={selectedIsGroupAdmin} />
				{/if}
				<h3>Group Admin</h3>
			</div>
			<div class="admin-roles">
				{#if noneditable}
					<Switch bind:checked={selectedGroupMembership.topicAdmin} />
				{:else}
					<Switch bind:checked={selectedIsTopicAdmin} />
				{/if}
				<h3>Topic Admin</h3>
			</div>
			<div class="admin-roles">
				{#if noneditable}
					<Switch bind:checked={selectedGroupMembership.applicationAdmin} />
				{:else}
					<Switch bind:checked={selectedIsApplicationAdmin} />
				{/if}
				<h3>Application Admin</h3>
			</div>
		{/if}
	</div>

	{#if actionAddUser}
		<hr />
		<button
			class="action-button"
			class:action-button-invalid={invalidEmail || searchGroups?.length < 3}
			on:click={() => actionAddUserEvent()}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					actionAddUserEvent();
				}
			}}
			>Add User
		</button>
	{/if}

	{#if actionAddSuperUser}
		<hr />
		<button
			class="action-button"
			class:action-button-invalid={invalidEmail || emailValue.length < 10}
			on:click={() => {
				actionAddSuperUserEvent();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					actionAddSuperUserEvent();
				}
			}}
			>Add User
		</button>
	{/if}

	{#if actionAddTopic}
		<hr />
		<button
			class="action-button"
			class:action-button-invalid={newTopicName.length < minNameLength ||
				searchGroups?.length < minNameLength}
			on:click={() => {
				actionAddTopicEvent();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					actionAddTopicEvent();
				}
			}}
			>Add Topic
		</button>
	{/if}

	{#if actionDuplicateTopic}
		<hr />
		<button
			class="action-button"
			class:action-button-invalid={newTopicName.length < minNameLength ||
				searchGroups?.length < minNameLength}
			on:click={() => {
				actionDuplicateTopicEvent();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					actionDuplicateTopicEvent();
				}
			}}
			>Add Topic
		</button>
	{/if}

	{#if actionAddApplication}
		<hr />
		<button
			class="action-button"
			class:action-button-invalid={appName.length < minNameLength ||
				searchGroups?.length < minNameLength}
			on:click={() => {
				actionAddApplicationEvent();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					actionAddApplicationEvent();
				}
			}}
			>Add Topic
		</button>
	{/if}

	{#if actionAddGroup}
		<hr />
		<button
			class="action-button"
			class:action-button-invalid={newGroupName.length < minNameLength}
			on:click={() => {
				if (!newGroupName.length <= minNameLength) actionAddGroupEvent();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					if (!newGroupName.length <= minNameLength) actionAddGroupEvent();
				}
			}}
			>Add Group
		</button>
	{/if}

	{#if actionEditUser}
		<hr style="z-index: 1" />
		<button
			class="action-button"
			on:click={() => {
				actionEditUserEvent();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					actionEditUserEvent();
				}
			}}
			>Save Changes
		</button>
	{/if}

	{#if actionEditApplicationName}
		<hr style="z-index: 1" />
		<button
			class="action-button"
			class:action-button-invalid={previousAppName?.length < minNameLength}
			disabled={previousAppName?.length < minNameLength}
			on:click={() => {
				dispatch('saveNewAppName', { newAppName: previousAppName });
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('saveNewAppName', { newAppName: previousAppName });
				}
			}}
			>Save Changes
		</button>
	{/if}

	{#if actionDeleteUsers}
		<p style="margin: 0 1.7rem 1rem 2rem; font-size:0.9rem; font-stretch: condensed; ">
			Are you sure? This is not reversible.
		</p>
		<!-- svelte-ignore a11y-autofocus -->
		<button
			autofocus
			class="action-button"
			on:click={() => dispatch('deleteGroupMemberships')}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('deleteGroupMemberships');
				}
			}}>{title}</button
		>
	{/if}

	{#if actionDeleteSuperUsers}
		<p style="margin: 0 1.7rem 1rem 2rem; font-size:0.9rem; font-stretch: condensed; ">
			Are you sure? This is not reversible.
		</p>
		<!-- svelte-ignore a11y-autofocus -->
		<button
			autofocus
			class="action-button"
			on:click={() => dispatch('deleteSuperUsers')}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('deleteSuperUsers');
				}
			}}>{title}</button
		>
	{/if}

	{#if actionDeleteTopics}
		<p style="margin: 0 1.7rem 1rem 2rem; font-size:0.9rem; font-stretch: condensed; ">
			Are you sure? This is not reversible.
		</p>
		<!-- svelte-ignore a11y-autofocus -->
		<button
			autofocus
			class="action-button"
			on:click={() => dispatch('deleteTopics')}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('deleteTopics');
				}
			}}>{title}</button
		>
	{/if}

	{#if actionDeleteApplications}
		<p style="margin: 0 1.7rem 1rem 2rem; font-size:0.9rem; font-stretch: condensed; ">
			Are you sure? This is not reversible.
		</p>
		<!-- svelte-ignore a11y-autofocus -->
		<button
			autofocus
			class="action-button"
			on:click={() => dispatch('deleteApplications')}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('deleteApplications');
				}
			}}>{title}</button
		>
	{/if}

	{#if actionDeleteGroups}
		<p style="margin: 0 1.7rem 1rem 2rem; font-size:0.9rem; font-stretch: condensed; ">
			Are you sure? This is not reversible.
		</p>
		<!-- svelte-ignore a11y-autofocus -->
		<button
			autofocus
			class="action-button"
			on:click={() => dispatch('deleteGroups')}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('deleteGroups');
				}
			}}>{title}</button
		>
	{/if}

	<button
		class="action-button"
		on:click={() => {
			emailValue = '';
			closeModal();
		}}
		on:keydown={(event) => {
			if (event.which === returnKey) {
				emailValue = '';
				closeModal();
			}
		}}
		>Cancel
	</button>
</div>

<style>
	.application-list {
		display: inline-flex;
		height: 2rem;
		width: 14rem;
		font-size: 0.7rem;
		margin-left: 0.1rem;
		margin: -0.5rem 0 0.7rem 0;
	}

	.application-list:first-of-type {
		margin-top: 1rem;
	}

	.action-button {
		float: right;
		margin: 0.8rem 1.5rem 1rem 0;
		background-color: transparent;
		border-width: 0;
		font-size: 0.85rem;
		font-weight: 500;
		color: #6750a4;
		cursor: pointer;
	}

	.action-button-invalid {
		color: grey;
	}

	.admin-roles {
		display: flex;
		align-items: center;
		width: 14rem;
	}

	.admin-roles:first-child {
		margin-top: 1.5rem;
	}

	.admin-roles h3 {
		margin-left: 1.5rem;
		text-indent: -0.2rem;
	}

	.modal-backdrop {
		position: fixed;
		top: 0;
		left: 0;
		width: 100%;
		height: 100vh;
		background: rgba(0, 0, 0, 0.25);
		z-index: 10;
	}

	.modal {
		position: fixed;
		top: 10vh;
		left: 50%;
		margin-left: -10.25rem;
		width: 20.5rem;
		max-height: fit-content;
		background: rgb(246, 246, 246);
		border-radius: 15px;
		z-index: 100;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.26);
		/* overflow: scroll; */
	}

	.content {
		padding-bottom: 1rem;
		margin-top: 1rem;
		margin-left: 2.3rem;
		width: 14rem;
	}

	.content input {
		width: 11rem;
		border-width: 1px;
	}

	input.searchbox {
		margin-top: 1rem;
		width: 14rem;
	}

	input.searchbox:disabled {
		border-color: rgba(118, 118, 118, 0.3);
	}

	img.close-button {
		transform: scale(0.25, 0.35);
	}

	.close-button {
		background-color: transparent;
		position: absolute;
		padding-right: 1rem;
		color: black;
		border: none;
		height: 50px;
		width: 60px;
		border-radius: 0%;
		cursor: pointer;
	}

	h2 {
		margin-top: 3rem;
		margin-left: 2rem;
	}

	hr {
		width: 79%;
		border-color: rgba(0, 0, 0, 0.07);
	}

	input[type='checkbox'] {
		height: 0.7rem;
	}
</style>
