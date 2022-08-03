<script>
	import { isAuthenticated } from '../stores/authentication';
	import { onMount } from 'svelte';
	import axios from 'axios';
	import users from '../stores/users';
	import Modal from '../lib/Modal.svelte';

	const URL_PREFIX = 'http://localhost:8080';

	// Error Handling
	let errorMessage, errorObject;

	// Modals
	let errorMessageVisible = false;
	let confirmDeleteVisible = false;

	// Users
	let userFirstName;
	let userLastName;

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

			// Pagination
			let totalUsersCount = 0;
			usersPageIndex = Math.floor(usersData.data.content.length / usersPerPage);
			if (usersData.data.content.length % usersPerPage > 0) usersPageIndex++;

			// Populate the usersPage Array
			let pageArray = [];
			for (let page = 0; page < usersPageIndex; page++) {
				for (let i = 0; i < usersPerPage && totalUsersCount < usersData.data.content.length; i++) {
					pageArray.push(usersData.data.content[page * usersPerPage + i]);
					totalUsersCount++;
				}
				usersPages.push(pageArray);
				pageArray = [];
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
			console.error('Error Loading Users');
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
