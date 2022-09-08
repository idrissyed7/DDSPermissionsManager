<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import groups from '../../stores/groups';
	import Modal from '../../lib/Modal.svelte';

	export let data, errors;

	// Modals
	let addGroupMembershipVisible = false;
	let errorMessageVisible = false;

	// Forms
	let emailValue = '';
	let selectedGroup;
	let selectedIsGroupAdmin = false;
	let selectedIsApplicationAdmin = false;
	let selectedIsTopicAdmin = false;

	// Group Permissions
	let isGroupAdmin = false;
	let groupAdminGroups = [];

	// Error Handling
	let errorMessage, errorObject;
	let invalidEmail = true;
	let validRegex =
		/^([a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+(?:\.[a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)$/gm;

	onMount(async () => {
		if ($permissionsByGroup) {
			$permissionsByGroup.forEach((group) => {
				if (group.isGroupAdmin) {
					groupAdminGroups.push(group);
				}
			});
			if (groupAdminGroups.length > 0) {
				isGroupAdmin = true;
			}
		}
	});

	const addGroupMembershipInput = async () => {
		addGroupMembershipVisible = true;
	};

	const addGroupMembership = async () => {
		console.log('Request:');
		console.log('email: ', emailValue);
		console.log('permissionsGroup: ', selectedGroup);
		console.log('isGroupAdmin: ', selectedIsGroupAdmin);
		console.log('isTopicAdmin: ', selectedIsTopicAdmin);
		console.log('isApplicationAdmin: ', selectedIsApplicationAdmin);
		const res = await httpAdapter
			.post(`/group_membership`, {
				email: emailValue,
				permissionsGroup: selectedGroup,
				isGroupAdmin: selectedIsGroupAdmin,
				isTopicAdmin: selectedIsTopicAdmin,
				isApplicationAdmin: selectedIsTopicAdmin
			})
			.catch((err) => {
				console.log('err', err);
				if (err.response.status === 403) {
					ErrorMessage('Error Saving Group Membership', err.message);
				} else if (err.response.status === 400) {
					ErrorMessage('Error: Group Membership it already exists.', err.message);
				}
			});
		addGroupMembershipVisible = false;
		console.log('result:', res);
	};

	const ValidateEmail = (input) => {
		input.match(validRegex) ? (invalidEmail = false) : (invalidEmail = true);
	};

	const ErrorMessage = (errMsg, errObj) => {
		errorMessage = errMsg;
		errorObject = errObj;
		errorMessageVisible = true;
	};

	const ErrorMessageClear = () => {
		errorMessage = '';
		errorObject = '';
		errorMessageVisible = false;
	};
</script>

<svelte:head>
	<title>Group Membership | Permission Manager</title>
	<meta name="description" content="Permission Manager Group Membership" />
</svelte:head>

{#if $isAuthenticated}
	{#if errorMessageVisible}
		<Modal
			title={errorMessage}
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

	{#if addGroupMembershipVisible}
		<table>
			<tr>
				<td style="width: 15rem"
					><input
						placeholder="Email Address"
						class:invalid={invalidEmail && emailValue.length >= 1}
						style="
						display: inline-flex;		
						height: 1.7rem;
						text-align: left;
						font-size: small;
						min-width: 12rem;"
						bind:value={emailValue}
						on:blur={() => ValidateEmail(emailValue)}
						on:keydown={(event) => {
							if (event.which === 13) {
								ValidateEmail(emailValue);
								document.querySelector('#name').blur();
							}
						}}
					/>
					<label for="groups">Group:</label>
					<select name="groups" bind:value={selectedGroup}>
						{#if $isAdmin}
							{#if $groups}
								{#each $groups as group}
									<option value={group.id}>{group.name}</option>
								{/each}
							{/if}
						{:else if groupAdminGroups}
							{#if $groups}
								{#each groupAdminGroups as group}
									<option value={group.groupId}>{group.groupName}</option>
								{/each}
							{/if}
						{/if}
					</select>
					<input type="checkbox" name="groupAdmin" bind:checked={selectedIsGroupAdmin} />
					<label for="groupAdmin">Group Admin</label>
					<input
						type="checkbox"
						name="applicationAdmin"
						bind:checked={selectedIsApplicationAdmin}
					/>
					<label for="applicationAdmin">Application Admin</label>
					<input type="checkbox" name="topicAdmin" bind:checked={selectedIsTopicAdmin} />
					<label for="topicAdmin">Topic Admin</label>
					<button
						class:button={!invalidEmail}
						style="margin-left: 1rem; width: 6.5rem"
						disabled={invalidEmail}
						on:click={() => addGroupMembership()}><span>Add</span></button
					>
					<button
						class="remove-button"
						on:click={() => {
							emailValue = '';
							addGroupMembershipVisible = false;
						}}>x</button
					></td
				>
			</tr>
		</table>
	{/if}
	{#if $isAdmin || isGroupAdmin}
		<center
			><button
				style="cursor:pointer"
				on:click={() => addGroupMembershipInput()}
				class:hidden={addGroupMembershipVisible}>+</button
			></center
		>
	{/if}
{:else}
	<center><h2>Please Log In to Continue...</h2></center>
{/if}

<style>
	.hidden {
		display: none;
	}

	.remove-button:hover {
		cursor: pointer;
		background-color: lightgray;
	}

	label {
		font-size: small;
	}

	input {
		vertical-align: middle;
	}

	table {
		display: flex;
		justify-content: space-evenly;
		width: 100%;
	}
</style>
