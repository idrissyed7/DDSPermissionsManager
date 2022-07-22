<script>
	import { isAuthenticated } from '../stores/authentication';
	import { onMount } from 'svelte';
	import axios from 'axios';
	import users from '../stores/users';
	import Modal from '../lib/Modal.svelte';

	let addUserVisible = false;
	let confirmDeleteVisible = false;
	let userFirstName;
	let userLastName;
	let userEmail;

	let selectedUserFirstName;
	let selectedUserLastName;
	let selectUserId;

	onMount(async () => {
		try {
			const res = await axios.get('http://localhost:8080/users', { withCredentials: true });
			users.set(res.data.content);
			console.log($users);
		} catch (err) {
			console.error('Error loading Users');
		}
	});

	const reloadUsers = async () => {
		try {
			const res = await axios.get('http://localhost:8080/users', { withCredentials: true });
			users.set(res.data.content);
			console.log('new users', $users);
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
				'http://localhost:8080/users/save',
				{
					firstName: userFirstName,
					lastName: userLastName,
					email: userEmail
				},
				{ withCredentials: true }
			)
			.catch((err) => console.error(err));

		addUserVisible = false;

		reloadUsers();
	};

	const userDelete = async () => {
		confirmDeleteVisible = false;
		console.log(`http://localhost:8080/users/delete/${selectUserId}`);
		const res = await axios
			.post(
				`http://localhost:8080/users/delete/${selectUserId}`,
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
				<button class="button" on:click={() => (confirmDeleteVisible = false)}>No</button>
				<button class="button-delete" on:click={() => userDelete()}>Yes</button>
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
			{#if $users}
				{#each $users as user}
					<tr>
						<td>{user.firstName}</td>
						<td>{user.lastName}</td>
						<td
							><button
								class="button-delete"
								on:click={() => confirmUserDelete(user.id, user.firstName, user.lastName)}
								>Delete</button
							></td
						>
					</tr>
				{/each}
			{:else}
				<p style="margin-left: 0.3rem">No Users Found</p>
			{/if}
		</table>
		<center
			><button class="button" style="margin-top: 2rem" on:click={() => addUserModal()}
				>Add User</button
			></center
		>
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
		min-width: 12rem;
		min-height: 1.5rem;
		margin-left: 1rem;
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
		margin-right: 1rem;
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

	.confirm-user-delete {
		display: inline-block;
	}

	button {
		width: 7rem;
		position: relative;
		/* left: 50%; */
	}

	table {
		margin-left: auto;
		margin-right: auto;
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
			to right,
			rgba(255, 255, 255, 1),
			rgba(255, 255, 255, 0.7),
			rgba(255, 255, 255, 0)
		);
	}

	input::placeholder {
		font-size: 8pt;
		padding-left: 0.1rem;
	}
</style>
