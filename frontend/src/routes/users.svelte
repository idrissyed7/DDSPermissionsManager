<script>
	import { isAuthenticated } from '../stores/authentication';
	import { onMount } from 'svelte';
	import axios from 'axios';
	import users from '../stores/users';
	import Modal from '../lib/Modal.svelte';

	const URL_PREFIX = 'http://localhost:8080';

	let addUserVisible = false;
	let confirmDeleteVisible = false;
	let userFirstName;
	let userLastName;
	let userEmail;

	let selectedUserFirstName;
	let selectedUserLastName;
	let selectUserId;

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
			console.error('Error loading Users');
		}
	});

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
			console.error('Error loading Users');
		}
	};

	const addUserModal = () => {
		userFirstName = '';
		userLastName = '';
		userEmail = '';
		addUserVisible = true;
	};

	const addUser = async () => {
		const res = await axios
			.post(
				`${URL_PREFIX}/users/save`,
				{
					firstName: userFirstName,
					lastName: userLastName,
					email: userEmail
				},
				{ withCredentials: true }
			)
			.catch((err) => console.error(err));

		addUserVisible = false;

		await reloadUsers().then(() => {
			currentPage = usersPages.length - 1;
		});
	};

	const userDelete = async () => {
		confirmDeleteVisible = false;
		const res = await axios
			.post(
				`${URL_PREFIX}/users/delete/${selectUserId}`,
				{
					firstName: userFirstName,
					lastName: userLastName
				},
				{ withCredentials: true }
			)
			.catch((err) => console.error(err));

		selectUserId = '';
		selectedUserFirstName = '';
		selectedUserLastName = '';

		reloadUsers();
	};

	const confirmUserDelete = (ID, firstName, lastName) => {
		confirmDeleteVisible = true;

		selectedUserFirstName = firstName;
		selectedUserLastName = lastName;
		selectUserId = ID;
	};
</script>

<svelte:head>
	<title>Users | Permission Manager</title>
	<meta name="description" content="Permission Manager Users" />
</svelte:head>

{#if $isAuthenticated}
	{#if addUserVisible && confirmDeleteVisible != true}
		<Modal title="Add New User" on:cancel={() => (addUserVisible = false)}>
			<div class="add-user">
				<input type="text" placeholder="First Name" bind:value={userFirstName} />
				<input type="text" placeholder="Last Name" bind:value={userLastName} />
				<input type="text" placeholder="E-Mail" bind:value={userEmail} />
				<button class="button" style="margin-left: 1rem;" on:click={() => addUser()}
					><span>Add User</span></button
				>
			</div>
		</Modal>
	{/if}
	{#if confirmDeleteVisible && addUserVisible != true}
		<Modal
			title="Delete {selectedUserFirstName} {selectedUserLastName}?"
			on:cancel={() => (confirmDeleteVisible = false)}
		>
			<div class="confirm-user-delete">
				<button class="button-cancel" on:click={() => (confirmDeleteVisible = false)}>Cancel</button
				>
				<button class="button-delete" on:click={() => userDelete()}>Delete</button>
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

		<br /><br />
		<center><button class="button" on:click={() => addUserModal()}>Add User</button></center>
	</div>
{:else}
	<center><h2>Please Log In to Continue...</h2></center>
{/if}

<style>
	.content {
		width: 100%;
		max-width: var(--column-width);
		margin: var(--column-margin-top) auto 0 auto;
	}

	.add-user {
		display: flex;
		justify-content: center;
	}

	.add-user input {
		min-width: 10rem;
		min-height: 1.5rem;
		margin-left: 1rem;
		font-size: 9.5pt;
	}

	.button {
		display: inline-block;
		justify-content: center;
		border-radius: 4px;
		background-color: rgb(54, 70, 255);
		border: none;
		color: #ffffff;
		text-align: center;
		font-size: 14px;
		padding-left: 5x;
		height: 2rem;
		transition: all 0.5s;
		cursor: pointer;
		/* margin-right: 1rem; */
		left: 0%;
		min-width: 7rem;
	}

	.button:hover {
		background-color: rgb(44, 58, 209);
	}

	.button span {
		cursor: pointer;
		display: inline-block;
		position: relative;
		transition: 0.5s;
	}

	.button span:after {
		content: '\00ab';
		position: absolute;
		opacity: 0;
		top: 0;
		right: -20px;
		transition: 0.5s;
	}

	.button:hover span {
		padding-right: 20px;
	}

	.button:hover span:after {
		opacity: 1;
		right: 0;
	}

	.button-delete {
		display: inline-block;
		border-radius: 4px;
		background-color: red;
		border: none;
		color: #ffffff;
		text-align: center;
		font-size: 14px;
		height: 2rem;
		top: 90%;
		transition: all 0.5s;
		cursor: pointer;
	}

	.button-delete:hover {
		background-color: rgb(223, 1, 1);
	}

	.button-delete span {
		cursor: pointer;
		display: inline-block;
		position: relative;
		transition: 0.5s;
	}

	.button-delete span:after {
		content: '\00bb';
		position: absolute;
		opacity: 0;
		top: 0;
		left: -20px;
		transition: 0.5s;
	}

	.button-delete:hover span {
		padding-left: 20px;
	}

	.button-delete:hover span:after {
		opacity: 1;
		left: 0;
	}

	.button-cancel {
		display: inline-block;
		border-radius: 4px;
		background-color: rgb(128, 128, 128);
		border: none;
		color: #ffffff;
		text-align: center;
		font-size: 14px;
		height: 2rem;
		top: 90%;
		transition: all 0.5s;
		cursor: pointer;
	}

	.button-cancel:hover {
		background-color: rgb(110, 110, 110);
	}

	.button-pagination {
		display: inline-block;
		font-size: 10pt;
		width: 2.1rem;
		height: 2rem;
		border: solid;
		border-width: 1px;
		border-color: rgb(149, 149, 149);
		border-radius: 3%;
		cursor: pointer;
	}

	.button-pagination:disabled:hover {
		background-color: rgba(239, 239, 239, 0.3);
	}

	.button-pagination-selected {
		background-color: rgb(217, 217, 217);
	}

	.button-pagination:hover {
		background-color: rgb(217, 217, 217);
	}

	.confirm-user-delete {
		display: inline-block;
	}

	button {
		width: 5rem;
		position: relative;
		text-align: center;
	}

	table {
		margin-left: auto;
		margin-right: 4.5rem;
		border-collapse: collapse;
		width: 70%;
	}

	th {
		font-size: 13.5pt;
		padding-left: 1rem;
		padding-bottom: 0.3rem;
		text-align: left;
	}

	td {
		font-size: 13pt;
		padding: 0.3rem 1rem 0.3rem 1rem;
	}

	tr:nth-child(even) {
		background-image: linear-gradient(
			60deg,
			rgba(255, 255, 255, 0),
			rgba(255, 255, 255, 1),
			rgba(255, 255, 255, 0.5),
			rgba(255, 255, 255, 0),
			rgba(255, 255, 255, 0)
		);
		filter: drop-shadow(-1px -1px 3px rgb(0 0 0 / 0.1));
	}

	input::placeholder {
		font-size: 8pt;
		padding-left: 0.1rem;
	}
</style>
