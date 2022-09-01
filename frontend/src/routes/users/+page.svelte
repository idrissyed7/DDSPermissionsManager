<script>
	import { isAuthenticated } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import users from '../../stores/users';
	import groups from '../../stores/groups';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import Modal from '../../lib/Modal.svelte';

	// Error Handling
	let errorMessage, errorObject;
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
	let usersPageIndex;
	let usersPages = [];
	let currentPage = 0;

	onMount(async () => {
		try {
			const usersData = await httpAdapter.get(`/users`);
			users.set(usersData.data.content);
			console.log($users);
			console.log($permissionsByGroup);

			const groupsData = await httpAdapter.get(`/groups`);
			groups.set(groupsData.data.content);

			if ($users) {
				// Pagination
				let totalUsersCount = 0;
				usersPageIndex = Math.floor(usersData.data.content.length / usersPerPage);
				if (usersData.data.content.length % usersPerPage > 0) usersPageIndex++;

				// Populate the usersPages Array
				for (let page = 0; page < usersPageIndex; page++) {
					let pageArray = [];
					for (
						let i = 0;
						i < usersPerPage && totalUsersCount < usersData.data.content.length;
						i++
					) {
						pageArray.push(usersData.data.content[page * usersPerPage + i]);
						totalUsersCount++;
					}
					usersPages.push(pageArray);
				}
			}
		} catch (err) {
			ErrorMessage('Error Loading Users', err.message);
		}
	});

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

	const calculatePagination = () => {
		usersPages = [];
		let totalUsersCount = 0;
		usersPageIndex = Math.floor($users.length / usersPerPage);
		if ($users.length % usersPerPage > 0) usersPageIndex++;

		if (usersPageIndex === currentPage) currentPage--;

		// Populate the usersPage Array
		let pageArray = [];
		for (let page = 0; page < usersPageIndex; page++) {
			for (let i = 0; i < usersPerPage && totalUsersCount < $users.length; i++) {
				pageArray.push($users[page * usersPerPage + i]);
				totalUsersCount++;
			}
			usersPages.push(pageArray);
			pageArray = [];
		}
	};

	const reloadUsers = async () => {
		try {
			const res = await httpAdapter.get(`/users`);
			users.set(res.data.content);
			calculatePagination();
		} catch (err) {
			ErrorMessage('Error Loading Users', err.message);
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
				ErrorMessage('Error Deleting User', err.message);
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
				ErrorMessage('Error Saving User', err.message);
			});

		addUserVisible = false;

		reloadUsers();
	};

	const ValidateEmail = (input) => {
		input.match(validRegex) ? (invalidEmail = false) : (invalidEmail = true);
	};
</script>

<svelte:head>
	<title>Users | Permission Manager</title>
	<meta name="description" content="Permission Manager Users" />
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
			{#if usersPages.length > 0}
				{#each usersPages[currentPage] as user}
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
						if (currentPage > 0) currentPage--;
					}}
					class="button-pagination"
					style="width: 4.8rem; border-bottom-left-radius:9px; border-top-left-radius:9px;"
					disabled={currentPage === 0}>Previous</button
				>
				{#if usersPageIndex > 2}
					{#each usersPages as page, i}
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
						if (currentPage < usersPages.length) currentPage++;
					}}
					class="button-pagination"
					style="width: 3.1rem; border-bottom-right-radius:9px; border-top-right-radius:9px;"
					disabled={currentPage === usersPages.length - 1}>Next</button
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
