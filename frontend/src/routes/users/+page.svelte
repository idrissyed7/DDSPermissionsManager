<script>
	import { isAuthenticated } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import users from '../../stores/users';
	import groups from '../../stores/groups';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import Modal from '../../lib/Modal.svelte';

	export let data, errors;

	// Error Handling
	let errorMsg, errorObject;
	let invalidEmail = true;
	let validRegex =
		/^([a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+(?:\.[a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)$/gm;

	// Modals
	let errorMessageVisible = false;
	let confirmDeleteVisible = false;
	let addUserVisible = false;

	// Users
	let userFirstName;
	let userLastName;

	// Forms
	let emailValue = '';

	// Selection
	let selectedUserFirstName;
	let selectedUserLastName;
	let selectedUserId;

	// Pagination
	const usersPerPage = 5;
	let usersTotalPages;
	let usersCurrentPage = 0;

	onMount(async () => {
		await reloadUsers();
		console.log($users);
		console.log($permissionsByGroup);

		const groupsData = await httpAdapter.get(`/groups`);
		groups.set(groupsData.data.content);
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

	const reloadUsers = async (page = 0) => {
		try {
			const res = await httpAdapter.get(`/users?page=${page}&size=${usersPerPage}`);
			users.set(res.data.content);
			usersTotalPages = res.data.totalPages;
		} catch (err) {
			errorMessage('Error Loading Users', err.message);
		}
	};

	const userDelete = async () => {
		confirmDeleteVisible = false;
		const res = await httpAdapter
			.post(`/users/delete/${selectedUserId}`, {
				firstName: userFirstName,
				lastName: userLastName
			})
			.catch((err) => {
				errorMessage('Error Deleting User', err.message);
			});

		selectedUserId = '';
		selectedUserFirstName = '';
		selectedUserLastName = '';

		reloadUsers();
	};

	const confirmUserDelete = (ID, firstName, lastName) => {
		confirmDeleteVisible = true;

		selectedUserFirstName = firstName;
		selectedUserLastName = lastName;
		selectedUserId = ID;
	};

	const addUserInput = () => {
		addUserVisible = true;
	};

	const addUser = async () => {
		const res = await httpAdapter
			.post(`/users/save`, {
				firstName: userFirstName,
				lastName: userLastName,
				email: userEmail,
				isAdmin: false
			})
			.catch((err) => {
				errorMessage('Error Saving User', err.message);
			});

		addUserVisible = false;

		reloadUsers();
	};

	const ValidateEmail = (input) => {
		input.match(validRegex) ? (invalidEmail = false) : (invalidEmail = true);
	};
</script>

<svelte:head>
	<title>Users | DDS Permissions Manager</title>
	<meta name="description" content="Permission Manager Users" />
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
		<Modal
			title="Delete {selectedUserFirstName} {selectedUserLastName}?"
			on:cancel={() => (confirmDeleteVisible = false)}
		>
			<div class="confirm">
				<button class="button-cancel" on:click={() => (confirmDeleteVisible = false)}>Cancel</button
				>&nbsp;
				<button class="button-delete" on:click={() => userDelete()}><span>Delete</span></button>
			</div>
		</Modal>
	{/if}

	<div class="content">
		<h1>Users</h1>
		<table align="center">
			<tr style="border-width: 0px">
				<th><strong>Email</strong></th>
				<th><strong>Group</strong></th>
				<th><strong>Role</strong></th>
			</tr>
			{#if $users && $users.length > 0}
				{#each $users as user}
					<tr>
						<td style="width: 25rem;">{user.email}</td>
						<td>Group Name</td>
						<td>Admin</td>
						<td
							><button
								class="button-delete"
								on:click={() => confirmUserDelete(user.id, user.firstName, user.lastName)}
								><span>Delete</span></button
							></td
						>
					</tr>
				{/each}
			{:else}
				<p style="margin-left: 0.3rem">No Users Found</p>
			{/if}
		</table>
		{#if addUserVisible}
			<table>
				<tr>
					<td style="width: 15rem"
						><input
							placeholder="Email Address"
							class:invalid={invalidEmail && emailValue.length >= 1}
							style="display: inline-flex"
							bind:value={emailValue}
							on:blur={() => ValidateEmail(emailValue)}
							on:keydown={(event) => {
								if (event.which === 13) {
									ValidateEmail(emailValue);
									document.querySelector('#name').blur();
								}
							}}
						/>
						<button
							class="remove-button"
							on:click={() => {
								emailValue = '';
								addUserVisible = false;
							}}>x</button
						></td
					>
				</tr>
			</table>
		{/if}
		<br />
		<center
			><button style="cursor:pointer" on:click={() => addUserInput()} class:hidden={addUserVisible}
				>+</button
			></center
		>

		<br /><br />

		{#if $users}
			<center
				><button
					on:click={() => {
						if (usersCurrentPage > 0) usersCurrentPage--;
						reloadUsers(usersCurrentPage);
					}}
					class="button-pagination"
					style="width: 4.8rem; border-bottom-left-radius:9px; border-top-left-radius:9px;"
					disabled={usersCurrentPage === 0}>Previous</button
				>
				{#if usersTotalPages > 2}
					{#each Array.apply(null, { length: usersTotalPages }).map(Number.call, Number) as page}
						<button
							on:click={() => {
								usersCurrentPage = page;
								reloadUsers(page);
							}}
							class="button-pagination"
							class:button-pagination-selected={page === usersCurrentPage}>{page + 1}</button
						>
					{/each}
				{/if}

				<button
					on:click={() => {
						if (usersCurrentPage + 1 < usersTotalPages) usersCurrentPage++;
						reloadUsers(usersCurrentPage);
					}}
					class="button-pagination"
					style="width: 3.1rem; border-bottom-right-radius:9px; border-top-right-radius:9px;"
					disabled={usersCurrentPage === usersTotalPages - 1 || usersTotalPages === 0}>Next</button
				></center
			>
		{/if}
	</div>
{:else}
	<center><h2>Please Log In to Continue...</h2></center>
{/if}

<style>
	.hidden {
		display: none;
	}

	input {
		height: 1.7rem;
		text-align: left;
		font-size: small;
		min-width: 12rem;
	}

	table {
		width: 100%;
	}
</style>
