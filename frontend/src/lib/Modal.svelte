<script>
	import { isAdmin } from '../stores/authentication';
	import { fade, fly } from 'svelte/transition';
	import { createEventDispatcher } from 'svelte';
	import { httpAdapter } from '../appconfig';
	import closeSVG from '../icons/close.svg';
	import Switch from './Switch.svelte';
	import { inview } from 'svelte-inview';
	import errorMessages from '$lib/errorMessages.json';
	import groupAdminGroups from '../stores/groupAdminGroups';
	import topicAdminTopics from '../stores/topicAdminTopics';
	import applicationAdminApplications from '../stores/applicationAdminApplications';
	import { onMount } from 'svelte';

	export let title;
	export let email = false;
	export let topicName = false;
	export let applicationName = false;
	export let groupNewName = false;
	export let group = false;
	export let adminRoles = false;
	export let actionAddUser = false;
	export let actionAddSuperUser = false;
	export let actionAddTopic = false;
	export let actionAddApplication = false;
	export let actionAddGroup = false;
	export let actionEditUser = false;
	export let actionEditApplicationName = false;
	export let actionEditGroup = false;
	export let actionDeleteUsers = false;
	export let actionDeleteSuperUsers = false;
	export let actionDeleteTopics = false;
	export let actionDeleteGroups = false;
	export let actionDeleteApplications = false;
	export let noneditable = false;
	export let emailValue = '';
	export let newTopicName = '';
	export let groupId = '';
	export let anyApplicationCanRead = false;
	export let searchGroups = '';
	export let selectedGroupMembership = '';
	export let previousAppName = '';
	export let groupCurrentName = '';
	export let selectedApplicationList = [];
	export let errorDescription = '';
	export let reminderDescription = '';
	export let errorMsg = false;
	export let reminderMsg = false;
	export let closeModalText = 'Cancel';

	const dispatch = createEventDispatcher();

	// Constants
	const returnKey = 13;
	const groupsDropdownSuggestion = 7;
	const minNameLength = 3;
	const searchStringLength = 3;
	const waitTime = 1000;

	// Forms
	let selectedIsGroupAdmin = false;
	let selectedIsTopicAdmin = false;
	let selectedIsApplicationAdmin = false;
	let appName = '';
	let newGroupName = '';

	// Error Handling
	let invalidTopic = false;
	let invalidGroup = false;
	let invalidApplicationName = false;
	let invalidEmail = false;
	let errorMessageGroup = '';
	let errorMessageApplication = '';
	let errorMessageTopic = '';
	let errorMessageEmail = '';
	let errorMessageName = '';
	let validRegex =
		/^([a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+(?:\.[a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)$/gm;

	// SearchBox
	let searchGroupsResultsMouseEnter = false;
	let searchGroupResults;
	let searchGroupsResultsVisible = false;
	let searchGroupActive = false;
	let selectedGroup;
	let groupResultPage = 0;
	let hasMoreGroups = true;
	let stopSearchingGroups = false;
	let timer;

	if (actionEditGroup) newGroupName = groupCurrentName;

	// Search Groups Feature
	$: if (
		searchGroups?.trim().length >= searchStringLength &&
		searchGroupActive &&
		!stopSearchingGroups
	) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchGroup(searchGroups.trim());
			stopSearchingGroups = true;
		}, waitTime);
	}

	// Search Groups Dropdown Visibility
	$: if (
		searchGroupResults?.length >= 1 &&
		searchGroupActive &&
		searchGroups?.trim().length >= searchStringLength
	) {
		searchGroupsResultsVisible = true;
	} else {
		if (!searchGroupsResultsMouseEnter) searchGroupsResultsVisible = false;
		searchGroupResults = '';
	}

	onMount(() => {
		// Check if the action is to Add Application and change placeholder text
		if (actionAddApplication) {
			document.querySelector('#group-input').placeholder = 'Group **';
		}
	});

	const searchGroup = async (searchGroupStr) => {
		let res;
		if ($isAdmin) {
			res = await httpAdapter.get(
				`/groups?page=${groupResultPage}&size=${groupsDropdownSuggestion}&filter=${searchGroupStr}`
			);
		} else if ($groupAdminGroups?.length > 0 && actionAddUser && !$isAdmin) {
			res = await httpAdapter.get(`/groups/search/${searchGroupStr}?role=GROUP_ADMIN`);
		} else if ($topicAdminTopics?.length > 0 && actionAddTopic && !$isAdmin) {
			res = await httpAdapter.get(`/groups/search/${searchGroupStr}?role=TOPIC_ADMIN`);
		} else if ($applicationAdminApplications?.length > 0 && actionAddApplication && !$isAdmin) {
			res = await httpAdapter.get(`/groups/search/${searchGroupStr}?role=APPLICATION_ADMIN`);
		}

		// For all cases
		if (res.data && res.data?.content?.length < groupsDropdownSuggestion) {
			hasMoreGroups = false;
		}

		if (
			res.data?.content?.length > 0 &&
			JSON.stringify(searchGroupResults) !== JSON.stringify(res.data.content)
		) {
			searchGroupResults = [...searchGroupResults, ...res.data.content];
		}
	};

	const selectedSearchGroup = (groupName, groupId) => {
		searchGroupResults = [];
		groupResultPage = 0;

		selectedGroup = groupId;
		searchGroups = groupName;

		searchGroupsResultsVisible = false;
		searchGroupActive = false;
	};

	const validateEmail = (input) => {
		if (input.match(validRegex)) invalidEmail = false;
		else {
			invalidEmail = true;
			errorMessageEmail = errorMessages['email']['is_not_format'];
		}
	};

	const validateGroupName = async () => {
		const res = await httpAdapter.get(
			`/groups?page=0&size=${groupsDropdownSuggestion}&filter=${searchGroups}`
		);
		if (
			searchGroups?.length > 0 &&
			res.data.content?.some((group) => group.name.toUpperCase() === searchGroups.toUpperCase())
		) {
			return true;
		} else if (
			actionEditGroup &&
			res.data.content?.some((group) => group.name.toUpperCase() !== newGroupName.toUpperCase())
		) {
			return true;
		} else {
			return false;
		}
	};

	const validateTopicName = async () => {
		const res = await httpAdapter.get(
			`/topics?page=0&size=${groupsDropdownSuggestion}&filter=${newTopicName}`
		);
		if (
			newTopicName?.length > 0 &&
			res.data.content?.some((topic) => topic.name.toUpperCase() === newTopicName.toUpperCase())
		) {
			return false;
		} else {
			return true;
		}
	};

	const validateNameLength = (name, category) => {
		if (name?.length < minNameLength && category) {
			errorMessageName = errorMessages[category]['name.cannot_be_less_than_three_characters'];
			return false;
		}
		if (name?.length >= minNameLength) {
			return true;
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

		validateEmail(emailValue);

		const validGroupName = await validateGroupName();
		if (!validGroupName) {
			errorMessageGroup = errorMessages['group']['not_found'];
			return;
		}

		if (!invalidEmail || validateNameLength()) dispatch('addGroupMembership', newGroupMembership);
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

		invalidTopic = !validateNameLength(newTopicName, 'topic');
		if (invalidTopic) {
			errorMessageName = errorMessages['topic']['name.cannot_be_less_than_three_characters'];
			return;
		}

		const validGroupName = await validateGroupName();
		if (!validGroupName) {
			errorMessageGroup = errorMessages['group']['not_found'];
			return;
		}

		const validTopicName = await validateTopicName();
		if (!validTopicName) {
			errorMessageTopic = errorMessages['topic']['exists'];
			return;
		}

		if (!invalidTopic && validGroupName) {
			dispatch('addTopic', newTopic);
			closeModal();
		}
	};

	const actionAddApplicationEvent = async () => {
		let newApplication = {
			appName: appName,
			searchGroups: searchGroups,
			selectedGroup: selectedGroup
		};

		invalidApplicationName = !validateNameLength(appName, 'application');

		const validGroupName = await validateGroupName();
		if (!validGroupName) {
			errorMessageGroup = errorMessages['group']['not_found'];
			return;
		}

		dispatch('addApplication', newApplication);
		closeModal();
	};

	const actionAddGroupEvent = async () => {
		invalidGroup = !validateNameLength(newGroupName, 'group');

		let returnGroupName = {
			newGroupName: newGroupName
		};

		const res = await httpAdapter.get(
			`/groups?page=0&size=${groupsDropdownSuggestion}&filter=${newGroupName}`
		);

		if (res.data.content) {
			if (
				res.data.content.some((group) => group.name.toUpperCase() === newGroupName.toUpperCase())
			) {
				errorMessageGroup = errorMessages['group']['exists'];
				return;
			} else {
				if (actionEditGroup) {
					dispatch('addGroup', { groupId: groupId, newGroupName: newGroupName });
				} else {
					dispatch('addGroup', returnGroupName);
				}

				closeModal();
			}
		} else {
			if (actionEditGroup) {
				dispatch('addGroup', { groupId: groupId, newGroupName: newGroupName });
			} else {
				dispatch('addGroup', returnGroupName);
			}
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

	const loadMoreResultsGroups = (e) => {
		if (e.detail.inView && hasMoreGroups) {
			groupResultPage++;
			searchGroup(searchGroups);
		}
	};

	const options = {
		rootMargin: '20px',
		unobserveOnEnter: true
	};
</script>

<div class="modal-backdrop" on:click={closeModal} transition:fade />
<div class="modal" transition:fly={{ y: 300 }}>
	<img src={closeSVG} alt="close" class="close-button" on:click={closeModal} />
	<h2>{title}</h2>
	<hr />
	<div class="content">
		{#if errorMsg}
			<p>{errorDescription}</p>
		{/if}

		{#if reminderMsg}
			<p>{reminderDescription}</p>
		{/if}

		{#if email}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				data-cy="email-input"
				autofocus
				disabled={noneditable}
				placeholder="Email *"
				class:invalid={invalidEmail && emailValue?.length >= 1}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={emailValue}
				on:blur={() => {
					emailValue = emailValue.trim();
					if (emailValue?.length > 0) validateEmail(emailValue);
				}}
				on:click={() => {
					invalidEmail = false;
					errorMessageEmail = '';
				}}
				on:keydown={(event) => {
					invalidEmail = false;
					errorMessageEmail = '';

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
			<span
				class="error-message"
				style="	top: 9.6rem; right: 2.4rem"
				class:hidden={errorMessageEmail?.length === 0}
			>
				{errorMessageEmail}
			</span>
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
				data-cy="topic-name"
				autofocus
				placeholder="Topic Name *"
				class:invalid={invalidTopic}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={newTopicName}
				on:blur={() => {
					newTopicName = newTopicName.trim();
				}}
				on:keydown={(event) => {
					errorMessageName = '';

					if (event.which === returnKey) {
						newTopicName = newTopicName.trim();
						invalidTopic = !validateNameLength(newTopicName);
					}
				}}
				on:click={() => {
					errorMessageName = '';
				}}
			/>
		{/if}

		{#if errorMessageTopic?.substring(0, errorMessageTopic?.indexOf(' ')) === 'Topic' && errorMessageTopic?.length > 0}
			<span
				class="error-message"
				style="	top: 9.6rem; right: 2.2rem"
				class:hidden={errorMessageTopic?.length === 0}
			>
				{errorMessageTopic}
			</span>
		{/if}

		{#if applicationName}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				data-cy="application-name"
				autofocus
				placeholder="Application Name *"
				class:invalid={invalidApplicationName}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={appName}
				on:blur={() => {
					appName = appName.trim();
				}}
				on:keydown={(event) => {
					errorMessageName = '';
					errorMessageApplication = '';
					if (event.which === returnKey) {
						appName = appName.trim();
						if (searchGroups?.length >= searchStringLength) actionAddApplicationEvent();
					}
				}}
				on:click={() => {
					errorMessageName = '';
					errorMessageApplication = '';
				}}
			/>
		{/if}

		{#if errorMessageName?.substring(0, errorMessageName?.indexOf(' ')) === 'Application' && errorMessageName?.length > 0}
			<span
				class="error-message"
				style="	top: 9.6rem; right: 1.4rem"
				class:hidden={errorMessageName?.length === 0}
			>
				{errorMessageName}
			</span>
		{/if}

		{#if groupNewName}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				data-cy="group-new-name"
				autofocus
				placeholder="Group Name *"
				class:invalid={invalidGroup || errorMessageGroup?.length > 0}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={newGroupName}
				on:blur={() => {
					newGroupName = newGroupName.trim();
				}}
				on:keydown={(event) => {
					errorMessageName = '';
					errorMessageGroup = '';

					if (event.which === returnKey) {
						newGroupName = newGroupName.trim();
						invalidGroup = !validateNameLength(newGroupName, 'group');
						if (actionAddGroup && !invalidGroup) {
							actionAddGroupEvent();
						}

						if (actionEditGroup && !invalidGroup && newGroupName !== groupCurrentName) {
							actionAddGroupEvent();
						}
						if (newGroupName === groupCurrentName) {
							dispatch('cancel');
						}
					}
				}}
				on:click={() => {
					errorMessageName = '';
					errorMessageGroup = '';
				}}
			/>

			{#if errorMessageName?.substring(0, errorMessageName?.indexOf(' ')) === 'Group' && errorMessageName?.length > 0}
				<span
					class="error-message"
					style="	top: 9.6rem; right: 2.1rem"
					class:hidden={errorMessageName?.length === 0}
				>
					{errorMessageName}
				</span>
			{/if}

			{#if errorMessageGroup?.substring(0, errorMessageGroup?.indexOf(' ')) === 'Group' && errorMessageGroup?.length > 0}
				<span
					class="error-message"
					style="	top: 9.6rem; right: 2.3rem"
					class:hidden={errorMessageGroup?.length === 0}
				>
					{errorMessageGroup}
				</span>
			{/if}
		{/if}

		{#if actionEditApplicationName}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				autofocus
				data-cy="application-name"
				placeholder="Application Name *"
				class:invalid={invalidApplicationName}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={previousAppName}
				on:blur={() => {
					previousAppName = previousAppName.trim();
				}}
				on:keydown={(event) => {
					errorMessageName = '';
					if (event.which === returnKey) {
						previousAppName = previousAppName.trim();
						invalidApplicationName = !validateNameLength(previousAppName, 'application');
						if (!invalidApplicationName)
							dispatch('saveNewAppName', { newAppName: previousAppName });
					}
				}}
				on:click={() => (errorMessageName = '')}
			/>
		{/if}

		{#if group}
			<form class="searchbox" style="margin-bottom: 0.7rem">
				<input
					id="group-input"
					data-cy="group-input"
					class="searchbox"
					type="search"
					placeholder="Group *"
					disabled={noneditable}
					class:invalid={errorMessageGroup?.length > 0}
					bind:value={searchGroups}
					on:keydown={(event) => {
						searchGroupResults = [];
						stopSearchingGroups = false;
						hasMoreGroups = true;
						groupResultPage = 0;

						if (event.which === returnKey) {
							errorMessageApplication = '';
							document.activeElement.blur();
							searchGroups = searchGroups?.trim();

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
						searchGroups = searchGroups?.trim();
						setTimeout(() => {
							searchGroupsResultsVisible = false;
						}, waitTime);
					}}
					on:focus={async () => {
						searchGroupActive = true;
						searchGroupResults = [];
						errorMessageGroup = '';
						selectedGroup = '';
					}}
					on:focusout={() => {
						setTimeout(() => {
							searchGroupsResultsVisible = false;
							searchGroupResults = [];
						}, waitTime);
					}}
					on:click={async () => {
						searchGroupResults = [];
						groupResultPage = 0;
						hasMoreGroups = true;

						searchGroupActive = true;
						selectedGroup = '';
						errorMessageApplication = '';
						stopSearchingGroups = false;

						if (searchGroupResults?.length > 0) {
							searchGroupsResultsVisible = true;
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
				style="	top: 12.7rem; right: 2.2rem"
				class:hidden={errorMessageGroup?.length === 0}
			>
				{errorMessageGroup}
			</span>

			{#if searchGroupsResultsVisible && errorMessageGroup?.length === 0 && errorMessageApplication?.length === 0}
				<table
					class="search-group"
					class:hidden={searchGroupResults?.length === 0}
					style="position: absolute; z-index: 100; display: block; overflow-y: auto; max-height: 13.3rem"
					on:mouseenter={() => {
						searchGroupsResultsMouseEnter = true;
					}}
					on:mouseleave={() => {
						setTimeout(() => {
							if (!searchGroupsResultsMouseEnter) searchGroupsResultsVisible = false;
						}, waitTime);
						searchGroupsResultsMouseEnter = false;
					}}
					on:focusout={() => {
						searchGroupsResultsVisible = false;
						searchGroupsResultsMouseEnter = false;
					}}
				>
					{#each searchGroupResults as result}
						<tr>
							<td
								style="width: 14rem; padding-left: 0.5rem"
								on:click={() => {
									selectedSearchGroup(result.name, result.id);
								}}
								>{result.name}
							</td>
						</tr>
					{/each}
					<div use:inview={{ options }} on:change={loadMoreResultsGroups} />
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
		<span style="font-size:0.7rem; float: right; margin:0 2rem 0.5rem 0"
			>* Required & Immutable</span
		>
		<hr />
		<button
			data-cy="button-add-user"
			class="action-button"
			class:action-button-invalid={invalidEmail || searchGroups?.length < 3}
			disabled={invalidEmail || searchGroups?.length < 3}
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
		<span style="font-size:0.7rem; float: right; margin:0 2rem 0.5rem 0"
			>* Required & Immutable</span
		>
		<hr />
		<button
			data-cy="button-add-super-user"
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
		<span style="font-size:0.7rem; float: right; margin:0 2rem 0.5rem 0"
			>* Required & Immutable</span
		>
		<hr />
		<button
			data-cy="button-add-topic"
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

	{#if actionAddApplication}
		<span style="font-size:0.7rem; float: right; margin:0 2rem 0.5rem 0"
			>* Required &nbsp;**Required & Immutable</span
		>
		<hr />
		<button
			data-cy="button-add-application"
			class="action-button"
			class:action-button-invalid={appName?.length < minNameLength ||
				searchGroups?.length < minNameLength}
			disabled={appName?.length < minNameLength || searchGroups?.length < minNameLength}
			on:click={() => {
				actionAddApplicationEvent();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					actionAddApplicationEvent();
				}
			}}
			>Add Application
		</button>
	{/if}

	{#if actionAddGroup}
		<span style="font-size:0.7rem; float: right; margin:0 2rem 0.5rem 0">* Required</span>
		<hr />
		<button
			data-cy="button-add-group"
			class="action-button"
			class:action-button-invalid={newGroupName?.length < minNameLength}
			disabled={newGroupName?.length < minNameLength}
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
			data-cy="save-edit-user"
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
			data-cy="save-application"
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

	{#if actionEditGroup}
		<hr style="z-index: 1" />
		<button
			data-cy="edit-group"
			class="action-button"
			class:action-button-invalid={newGroupName?.length < minNameLength}
			disabled={newGroupName?.length < minNameLength}
			on:click={() => {
				if (newGroupName !== groupCurrentName) actionAddGroupEvent();
				else dispatch('cancel');
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					if (newGroupName !== groupCurrentName) actionAddGroupEvent();
					else dispatch('cancel');
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
			data-cy="delete-user"
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
			data-cy="delete-super-user"
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
			data-cy="delete-topic"
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
			data-cy="delete-application"
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
			data-cy="delete-group"
			class="action-button"
			on:click={() => dispatch('deleteGroups')}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('deleteGroups');
				}
			}}>{title}</button
		>
	{/if}

	{#if reminderMsg}
		<!-- svelte-ignore a11y-autofocus -->
		<button
			autofocus
			class="action-button"
			on:click={() => dispatch('extendSession')}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('extendSession');
				}
			}}>Yes</button
		>
	{/if}

	<!-- svelte-ignore a11y-autofocus -->
	<button
		autofocus={errorMsg}
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
		>{closeModalText}
	</button>
</div>

<style>
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

	.action-button:focus {
		outline: 0;
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
		width: 18.6rem;
		max-height: fit-content;
		background: rgb(246, 246, 246);
		border-radius: 15px;
		z-index: 100;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.26);
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

	.error-message {
		font-size: 0.7rem;
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
