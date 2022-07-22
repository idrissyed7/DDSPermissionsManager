<script>
	import { isAuthenticated } from '../stores/authentication';
	import { onMount } from 'svelte';
	import axios from 'axios';
	import users from '../stores/users';

	// onMount(async () => {
	// 	try {
	// 		const res = await axios.get('http://localhost:8080/users', { withCredentials: true });
	// 		users.set(res.data.content);
	// 	} catch (err) {
	// 		console.error('Error loading Users');
	// 	}
	// });

	let firstName;
	let lastName;

	const addUser = async () => {
		const res = await axios.post('http://localhost:8080/users/save', {
			firstName: firstName,
			lastName: lastName
		});
		console.log('add user', res.data);
	};
</script>

<svelte:head>
	<title>Add Users | Permission Manager</title>
	<meta name="description" content="Permission Manager Add Users" />
</svelte:head>

{#if $isAuthenticated}
	<div class="content">
		<h1>Add New User</h1>
		<input type="text" placeholder="User First Name" bind:value={firstName} />
		<input type="text" placeholder="User Last Name" bind:value={lastName} />

		<br />
		<button on:click={addUser()}>Add User</button>
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

	button {
		width: 7rem;
		position: relative;
		left: 50%;
	}

	table {
		margin-left: auto;
		margin-right: auto;
		border-collapse: collapse;
		width: 60%;
	}

	th {
		font-size: 13.5pt;
		padding-left: 1rem;
		text-align: left;
	}

	td {
		font-size: 13pt;
		padding: 0.3rem 1rem 0.3rem 1rem;
	}

	tr:nth-child(even) {
		background-color: rgb(255, 255, 255);
	}
</style>
