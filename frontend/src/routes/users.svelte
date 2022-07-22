<script>
	import { isAuthenticated } from '../stores/authentication';
	import { onMount } from 'svelte';
	import axios from 'axios';
	import users from '../stores/users';

	onMount(async () => {
		try {
			const res = await axios.get('http://localhost:8080/users', { withCredentials: true });
			users.set(res.data.content);
		} catch (err) {
			console.error('Error loading Users');
		}
	});
</script>

<svelte:head>
	<title>Users | Permission Manager</title>
	<meta name="description" content="Permission Manager Users" />
</svelte:head>

{#if $isAuthenticated}
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
					</tr>
				{/each}
			{/if}
		</table>
	</div>
{:else}
	<h1>Please Log In to Continue...</h1>
{/if}

<style>
	.content {
		width: 100%;
		max-width: var(--column-width);
		margin: var(--column-margin-top) auto 0 auto;
	}

	table {
		margin-left: auto;
		margin-right: auto;
		border-collapse: collapse;
		width: 50%;
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
