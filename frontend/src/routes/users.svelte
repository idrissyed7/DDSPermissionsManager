<script>
	import { isAuthenticated } from '../stores/authentication';
	import { onMount } from 'svelte';
	import axios from 'axios';
	import users from '../stores/users';
	import groups from '../stores/groups';
	import Modal from '../lib/Modal.svelte';

	const URL_PREFIX = 'http://localhost:8080';

	// Error Handling
	let errorMessage, errorObject;
	let invalidEmail = false;
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
	const usersPerPage = 3;
	let usersPageIndex;
	let usersPages = [];
	let currentPage = 0;

	onMount(async () => {
		try {
			const usersData = await axios.get(`${URL_PREFIX}/users`, { withCredentials: true });
			users.set(usersData.data.content);

			const groupsData = await axios.get(`${URL_PREFIX}/groups`, { withCredentials: true });
			groups.set(groupsData.data.content);

			if ($users) {
				// Pagination
				let totalUsersCount = 0;
				usersPageIndex = Math.floor(usersData.data.content.length / usersPerPage);
				if (usersData.data.content.length % usersPerPage > 0) usersPageIndex++;

				// Populate the usersPage Array
				let pageArray = [];
				for (let page = 0; page < usersPageIndex; page++) {
					for (
						let i = 0;
						i < usersPerPage && totalUsersCount < usersData.data.content.length;
						i++
					) {
						pageArray.push(usersData.data.content[page * usersPerPage + i]);
						totalUsersCount++;
					}
					usersPages.push(pageArray);
					pageArray = [];
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
			const res = await axios.get(`${URL_PREFIX}/users`, { withCredentials: true });
			users.set(res.data.content);
			calculatePagination();
		} catch (err) {
			ErrorMessage('Error Loading Users', err.message);
		}
	};

	const userDelete = async () => {
		confirmDeleteVisible = false;
		const res = await axios
			.post(
				`${URL_PREFIX}/users/delete/${selectedUserId}`,
				{
					firstName: userFirstName,
					lastName: userLastName
				},
				{ withCredentials: true }
			)
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
		const res = await axios
			.post(
				'http://localhost:8080/users/save',
				{
					group: userFirstName,
					role: userLastName,
					email: userEmail
				},
				{ withCredentials: true }
			)
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
				>
				<button class="button-delete" on:click={() => userDelete()}><span>Delete</span></button>
			</div>
		</Modal>
	{/if}

	<div class="content">
		<h1>Users</h1>
		<table>
			<tr>
				<th><strong>First Name</strong></th>
				<th><strong>Last Name</strong></th>
			</tr>
			{#if usersPages.length > 0}
				{#each usersPages[currentPage] as user}
					<tr>
						<td>{user.firstName}</td>
						<td>{user.lastName}</td>
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
					<td
						><input
							placeholder="Email Address"
							class:invalid={invalidEmail}
							bind:value={emailValue}
							on:blur={() => ValidateEmail(emailValue)}
							on:keydown={(event) => {
								if (event.which === 13) {
									ValidateEmail(emailValue);
									document.querySelector('#name').blur();
								}
							}}
						/></td
					>
					<div style="display:flex; justify-content: space-between;">
						<td style="width: 100%">
							<select
								name="group"
								style="color: white; background-color: rgb(54, 70, 255); margin-left: -0.2rem"
							>
								{#each $groups as group}
									<option value={group.name}>{group.name}</option>
								{/each}
							</select>
							<select name="role" style="background-color: yellow">
								<option value="super-admin">Super Admin</option>
								<option value="group-admin">Group Admin</option>
								<option value="topic-admin">Topic Admin</option>
								<option value="app-admin">Application Admin</option>
							</select>
							&nbsp;&nbsp;
							<button class="button" on:click={() => addUser()}>Create User</button></td
						>
					</div>
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
		text-align: left;
		font-size: medium;
	}

	.button {
		height: 1.55rem;
		cursor: pointer;
		background-color: rgb(0, 190, 0);
	}
	.button:hover {
		filter: brightness(90%);
	}

	table {
		width: 100%;
		margin-left: 3.5rem;
	}

	select {
		font-size: small;
		height: 1.55rem;
		border-radius: 5px;
		border-color: rgba(0, 0, 0, 0);
		text-align: center;
	}

	select:hover {
		filter: brightness(90%);
	}
</style>
