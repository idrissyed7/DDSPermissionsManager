<script>
	import { isAuthenticated } from '../stores/authentication';
	import { onMount } from 'svelte';
	import axios from 'axios';
	import groups from '../stores/users';

	onMount(async () => {
		try {
			const res = await axios.get('http://localhost:8080/groups', { withCredentials: true });
			groups.set(res.data.content);
			console.log($groups);
		} catch (err) {
			console.error('Error loading Groups');
		}
	});
</script>

<svelte:head>
	<title>Groups | Permission Manager</title>
	<meta name="description" content="Permission Manager Groups" />
</svelte:head>

{#if $isAuthenticated}
	<div class="content">
		<h1>Groups</h1>
		<table>
			<tr>
				<th><strong>Group</strong></th>
			</tr>
			{#if $groups}
				{#each $groups as group}
					<tr>
						<td>{group.name}</td>
					</tr>
				{/each}
			{/if}
		</table>
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

	table {
		margin-left: auto;
		margin-right: auto;
		border-collapse: collapse;
		border: 1px solid;
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
