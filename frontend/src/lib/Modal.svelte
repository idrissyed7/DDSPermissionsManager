<script>
	import { fade, fly } from 'svelte/transition';
	import { createEventDispatcher } from 'svelte';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../appconfig';
	import closeSVG from '../icons/close.svg';
	import Switch from './Switch.svelte';
	import errorMessages from '$lib/errorMessages.json';
	import errorMessageAssociation from '../stores/errorMessageAssociation';
	import groupContext from '../stores/groupContext';

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
	export let actionAssociateApplication = false;
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
	export let selectedTopicId = '';

	const dispatch = createEventDispatcher();

	// Constants
	const returnKey = 13;
	const groupsToCompare = 7;
	const minNameLength = 3;

	// Forms
	let selectedIsGroupAdmin = false;
	let selectedIsTopicAdmin = false;
	let selectedIsApplicationAdmin = false;
	let appName = '';
	let newGroupName = '';
	let accessTypeSelection;

	// Bind Token
	let bindToken;
	let tokenApplicationName, tokenApplicationGroup, tokenApplicationEmail;
	let invalidToken = false;

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
	let selectedGroup;

	if (actionEditGroup) newGroupName = groupCurrentName;

	// Bind Token Decode
	$: if (bindToken?.length > 0) {
		const tokenBody = bindToken.substring(
			bindToken.indexOf('.') + 1,
			bindToken.lastIndexOf('.') - 1
		);
		decodeToken(tokenBody);
	} else {
		errorMessageAssociation.set([]);
	}

	onMount(() => {
		if ($groupContext) selectedGroup = $groupContext.id;
	});

	const validateEmail = (input) => {
		if (input.match(validRegex)) invalidEmail = false;
		else {
			invalidEmail = true;
			errorMessageEmail = errorMessages['email']['is_not_format'];
		}
	};

	const validateTopicName = async () => {
		const res = await httpAdapter.get(
			`/topics?page=0&size=${groupsToCompare}&filter=${newTopicName}`
		);

		if (
			newTopicName?.length > 0 &&
			res.data.content?.some(
				(topic) =>
					topic.name.toUpperCase() === newTopicName.toUpperCase() &&
					topic.groupName.toUpperCase() === $groupContext.name.toUpperCase()
			)
		) {
			return false;
		} else {
			return true;
		}
	};

	const validateApplicationName = async () => {
		const res = await httpAdapter.get(
			`/applications?page=0&size=${groupsToCompare}&filter=${appName}`
		);
		if (
			appName?.length > 0 &&
			res.data.content?.some(
				(application) => application.name.toUpperCase() === appName.toUpperCase()
			)
		) {
			return false;
		} else {
			return true;
		}
	};

	const validateGroupMembership = async () => {
		const res = await httpAdapter.get(
			`/group_membership?page=0&size=${groupsToCompare}&filter=${emailValue}`
		);
		if (
			emailValue?.length > 0 &&
			res.data.content?.some(
				(user) =>
					user.permissionsUserEmail.toUpperCase() === emailValue.toUpperCase() &&
					user.permissionsGroup === $groupContext.id
			)
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

		const validGroupMembership = await validateGroupMembership();
		if (!validGroupMembership) {
			errorMessageEmail = errorMessages['group_membership']['exists'];
			return;
		}

		if (!invalidEmail && validGroupMembership) dispatch('addGroupMembership', newGroupMembership);
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

		const validTopicName = await validateTopicName();
		if (!validTopicName) {
			errorMessageTopic = errorMessages['topic']['exists'];
			return;
		}

		if (!invalidTopic) {
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

		const validApplication = await validateApplicationName();
		if (!validApplication) {
			errorMessageApplication = errorMessages['application']['exists'];
			return;
		}

		if (!invalidApplicationName && validApplication) {
			dispatch('addApplication', newApplication);
			closeModal();
		}
	};

	const actionAddGroupEvent = async () => {
		invalidGroup = !validateNameLength(newGroupName, 'group');

		let returnGroupName = {
			newGroupName: newGroupName
		};

		const res = await httpAdapter.get(
			`/groups?page=0&size=${groupsToCompare}&filter=${newGroupName}`
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

	const decodeToken = async (token) => {
		let res = atob(token);
		res = res + '}';

		try {
			res = JSON.parse(res);
			invalidToken = false;
		} catch (err) {
			errorMessageAssociation.set(errorMessages['bind_token']['invalid']);
			invalidToken = true;
		}

		tokenApplicationName = res.appName;
		tokenApplicationGroup = res.groupName;
		tokenApplicationEmail = res.email;
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
						actionAddTopicEvent();
					}
				}}
				on:click={() => {
					errorMessageTopic = '';
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

		{#if errorMessageName?.substring(0, errorMessageName?.indexOf(' ')) === 'Topic' && errorMessageName?.length > 0}
			<span
				class="error-message"
				style="	top: 9.6rem; right: 2.2rem"
				class:hidden={errorMessageName?.length === 0}
			>
				{errorMessageName}
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
						actionAddApplicationEvent();
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

		{#if errorMessageApplication?.substring(0, errorMessageApplication?.indexOf(' ')) === 'Application' && errorMessageApplication?.length > 0}
			<span
				class="error-message"
				style="	top: 9.6rem; right: 2.3rem"
				class:hidden={errorMessageApplication?.length === 0}
			>
				{errorMessageApplication}
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
			<input
				data-cy="group-name"
				id="group-context"
				readonly
				disabled
				style="background: rgb(246, 246, 246); width: 13.2rem; margin: 1.5rem 2rem 1rem 0"
				bind:value={$groupContext.name}
			/>

			<span
				style="display: inline-flex; font-size: 0.65rem; position: relative; top: -4rem; left: 0.5rem; background-color: rgb(246,246,246); padding: 0 0.2rem 0 0.2rem; color: rgb(120,120,120)"
				>Group
			</span>
		{/if}

		{#if noneditable}
			<input
				value={searchGroups}
				disabled="true"
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
			/>

			<span
				style="display: inline-flex; font-size: 0.65rem; position: relative; top: -3rem; left: 0.5rem; background-color: rgb(246,246,246); padding: 0 0.2rem 0 0.2rem; color: rgb(120,120,120)"
				>Group
			</span>
		{/if}

		{#if topicName}
			<div style="display:flex; align-items: center">
				<!-- <span style="font-size:0.9rem; width: 9rem; margin-right: 0.5rem; ">
					Any application can read topic:
				</span> -->
				<!-- <Switch bind:checked={anyApplicationCanRead} /> -->
				<fieldset>
					<legend style="font-size:0.85rem">Any application can read topic:</legend>

					<div style="display:inline">
						<label for="anyApplicationOption1" style="font-size:0.8rem">
							<input
								type="radio"
								style="height:0.8rem; width:0.8rem"
								id="anyApplicationOption1"
								name="anyApplicationCanRead"
								bind:group={anyApplicationCanRead}
								value={false}
								checked
							/>
							This topic requires each application to be authorized in order to read and to write it.</label
						>
					</div>

					<div>
						<label for="anyApplicationOption2" style="font-size:0.8rem">
							<input
								type="radio"
								style="height:0.8rem; width:0.8rem; margin-top: 1rem"
								id="anyApplicationOption2"
								name="anyApplicationCanRead"
								bind:group={anyApplicationCanRead}
								value={true}
							/>
							This topic requires each application to be authorized in order to write it, but any application
							may read it.
						</label>
					</div>
				</fieldset>
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

		{#if actionAssociateApplication}
			<textarea
				data-cy="bind-token-input"
				style="margin-top: 0.5rem; margin-bottom: 1.5rem; width: 13.5rem"
				type="search"
				rows="13"
				placeholder="Bind Token"
				bind:value={bindToken}
				on:keydown={(event) => {
					if (event.which === returnKey) {
						bindToken = bindToken?.trim();
						document.activeElement.blur();
						if (bindToken?.length > 0) {
							dispatch('addTopicApplicationAssociation', selectedTopicId);
						}
					}
				}}
				on:blur={() => {
					bindToken = bindToken?.trim();
				}}
			/>

			<select style="width: 8rem; margin: unset" bind:value={accessTypeSelection}>
				<option value="" disabled selected>Access Type</option>
				<option value="READ">Read</option>
				<option value="WRITE">Write</option>
				<option value="READ_WRITE">Read + Write</option>
			</select>

			{#if tokenApplicationName !== undefined && tokenApplicationGroup !== undefined}
				<div style="font-size:1rem; margin-top: 1rem">
					<strong>{tokenApplicationName}</strong> ({tokenApplicationGroup})
				</div>
				<div style="font-size:0.8rem; margin-top: 0.5rem">
					from {tokenApplicationEmail}
				</div>
			{/if}
			{#if $errorMessageAssociation}
				<span
					class="error-message"
					style="	top: 21rem; right: 2.3rem"
					class:hidden={$errorMessageAssociation?.length === 0}
				>
					{$errorMessageAssociation}
				</span>
			{/if}
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
			class:action-button-invalid={invalidEmail || !selectedGroup}
			disabled={invalidEmail || !selectedGroup}
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
			disabled={newTopicName.length < minNameLength || !selectedGroup}
			class:action-button-invalid={newTopicName.length < minNameLength || !selectedGroup}
			on:click={() => actionAddTopicEvent()}
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
			class:action-button-invalid={appName?.length < minNameLength || !selectedGroup}
			disabled={appName?.length < minNameLength || !selectedGroup}
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

	{#if actionAssociateApplication}
		<!-- svelte-ignore a11y-autofocus -->
		<button
			autofocus
			data-cy="add-application-topic-association"
			class="action-button"
			disabled={bindToken === undefined || accessTypeSelection?.length === 0 || invalidToken}
			class:button-disabled={bindToken === undefined ||
				accessTypeSelection?.length === 0 ||
				invalidToken}
			on:click={() =>
				dispatch('addTopicApplicationAssociation', {
					bindToken: bindToken,
					accessTypeSelection: accessTypeSelection
				})}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('addTopicApplicationAssociation', {
						bindToken: bindToken,
						accessTypeSelection: accessTypeSelection
					});
				}
			}}>Add</button
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
		cursor: default;
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

	.button-disabled {
		cursor: default;
	}
</style>
