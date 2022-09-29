<script>
	import { isAuthenticated } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import users from '../../stores/users';
	import groups from '../../stores/groups';
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

	// SearchBox
	let searchString;
	const searchStringLength = 3;
	let searchUserResults;
	let timer;
	const waitTime = 500;

	// Forms
	let emailValue = '';

	// Reactive Statements
	$: if (emailValue.length > 10) invalidEmail = false;
	$: if (addUserVisible === false) emailValue = '';
	$: if (confirmDeleteVisible === true) addUserVisible = false;

	// Selection
	let selectedUserId;
	let selectedUserEmail;

	// Pagination
	const usersPerPage = 10;
	let usersTotalPages;
	let usersCurrentPage = 0;

	// Search Feature
	$: if (searchString?.trim().length >= searchStringLength) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchUser(searchString.trim());
		}, waitTime);
	}

	$: if (searchString?.trim().length < searchStringLength) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			reloadAllUsers();
		}, waitTime);
	}

	onMount(async () => {
		await reloadAllUsers();

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

	const searchUser = async (searchString) => {
		searchUserResults = await httpAdapter.get(
			`/admins?page=0&size=${usersPerPage}&filter=${searchString}`
		);
		if (searchUserResults.data.content) {
			users.set(searchUserResults.data.content);
		} else {
			users.set([]);
		}
		usersTotalPages = searchUserResults.data.totalPages;
		usersCurrentPage = 0;
	};

	const reloadAllUsers = async (page = 0) => {
		try {
			let res;
			if (searchString && searchString.length >= searchStringLength) {
				res = await httpAdapter.get(
					`/admins?page=${page}&size=${usersPerPage}&filter=${searchString}`
				);
			} else {
				res = await httpAdapter.get(`/admins?page=${page}&size=${usersPerPage}`);
			}
			users.set(res.data.content);
			usersTotalPages = res.data.totalPages;
			usersCurrentPage = page;
		} catch (err) {
			errorMessage('Error Loading Super Admins', err.message);
		}
	};

	const userDelete = async () => {
		confirmDeleteVisible = false;
		const res = await httpAdapter.put(`/admins/remove-admin/${selectedUserId}`, {}).catch((err) => {
			errorMessage('Error Deleting Super Admin', err.message);
		});

		selectedUserId = '';

		reloadAllUsers();
	};

	const confirmUserDelete = (ID, email) => {
		confirmDeleteVisible = true;
		selectedUserId = ID;
		selectedUserEmail = email;
	};

	const addUserInput = () => {
		addUserVisible = true;
	};

	const addUser = async (userEmail) => {
		await httpAdapter
			.post(`/admins/save`, {
				email: userEmail
			})
			.catch((err) => {
				errorMessage('Error Saving Super Admin', err.message);
			});

		addUserVisible = false;

		reloadAllUsers();
	};

	const validateEmail = (input) => {
		input.match(validRegex) ? (invalidEmail = false) : (invalidEmail = true);
	};
</script>

<svelte:head>
	<title>Super Admin | DDS Permissions Manager</title>
	<meta name="description" content="DDS Permissions Manager Super Admin" />
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
		<Modal title="Delete {selectedUserEmail}?" on:cancel={() => (confirmDeleteVisible = false)}>
			<div class="confirm">
				<button class="button-cancel" on:click={() => (confirmDeleteVisible = false)}>Cancel</button
				>&nbsp;
				<button class="button-delete" on:click={() => userDelete()}><span>Delete</span></button>
			</div>
		</Modal>
	{/if}

	<div class="content">
		<h1>Super Admin</h1>

		<center>
			<input
				style="border-width: 1px; width: 20rem"
				placeholder="Search"
				bind:value={searchString}
				on:blur={() => {
					searchString = searchString?.trim();
				}}
				on:keydown={(event) => {
					const returnKey = 13;
					if (event.which === returnKey) {
						document.activeElement.blur();
						searchString = searchString?.trim();
					}
				}}
			/>&nbsp; &#x1F50E;
		</center>

		<table align="center" style="margin-top: 2rem">
			{#if $users && $users.length > 0}
				<tr style="border-width: 0px">
					<th><strong>Email</strong></th>
					<th><strong>Role</strong></th>
				</tr>
				{#each $users as user}
					<tr>
						<td style="width: 23rem">{user.email}</td>
						<td>Admin</td>
						<td
							><button class="button-delete" on:click={() => confirmUserDelete(user.id, user.email)}
								><span>Delete</span></button
							></td
						>
					</tr>
				{/each}
			{:else}
				<center><p style="margin-left: 0.3rem">No Super Admin Found</p></center>
			{/if}
		</table>
		{#if addUserVisible}
			<table align="center">
				<tr>
					<td style="width: 23rem">
						<input
							placeholder="Email Address"
							class:invalid={invalidEmail && emailValue.length >= 1}
							style="display: inline-flex; width:12rem"
							bind:value={emailValue}
							on:blur={() => validateEmail(emailValue)}
							on:keydown={(event) => {
								if (event.which === 13) {
									validateEmail(emailValue);
									document.querySelector('#name').blur();
								}
							}}
						/>
					</td>

					<td>
						<button
							class="button"
							style="width: 4rem;"
							disabled={invalidEmail}
							on:click={() => addUser(emailValue)}
						>
							<span>Add</span>
						</button>
						&nbsp;
						<button
							class="remove-button"
							on:click={() => {
								emailValue = '';
								addUserVisible = false;
							}}
							>x
						</button>
					</td>
					<td />
				</tr>
			</table>
		{/if}
		<br />
		<center
			><button
				style="cursor: pointer; min-width: 8rem; margin: 1rem 0 2rem 0"
				class="button"
				on:click={() => addUserInput()}
				class:hidden={addUserVisible}>Add Super Admin</button
			></center
		>

		<br /><br />

		{#if $users}
			<center
				><button
					on:click={() => {
						if (usersCurrentPage > 0) usersCurrentPage--;
						reloadAllUsers(usersCurrentPage);
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
								reloadAllUsers(page);
							}}
							class="button-pagination"
							class:button-pagination-selected={page === usersCurrentPage}>{page + 1}</button
						>
					{/each}
				{/if}

				<button
					on:click={() => {
						if (usersCurrentPage + 1 < usersTotalPages) usersCurrentPage++;
						reloadAllUsers(usersCurrentPage);
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
	}
</style>
