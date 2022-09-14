<script>
	import { isAuthenticated } from '../stores/authentication';
	import urlparameters from '../stores/urlparameters';
	export const data = {};
        export const errors = {};
</script>

<svelte:head>
	<title>DDS Permissions Manager</title>
	<meta name="description" content="Permission Manager" />
</svelte:head>

{#if $isAuthenticated}
	<section>
		<h1>Welcome to the DDS Permissions Manager!</h1>
		<h2>To get started</h2>
		<ul>
			<li>
				<a href="/group_membership">Add topic, application, and group admins</a> to one of your groups
			</li>
			<li>
				Create a <a href="/topics" on:click={() => urlparameters.set('create')}>topic</a>
			</li>
			<li>
				Create an <a href="/applications" on:click={() => urlparameters.set('create')}
					>application</a
				>
				and generate credentials
			</li>
			<li>Find a <a href="/topics">topic</a> and grant access to an application</li>
		</ul>
		<p>
			Applications can use credentials to authenticate and download documents for DDS Security. See
			the README for more details.
		</p>
	</section>
{:else}
	<h1>DDS Permissions Manager</h1>
	<br />
	<h2 style="font-size: 1.4rem">Please Login to get started</h2>
{/if}

<style>
	section {
		display: flex;
		flex-direction: column;
		justify-content: center;
		align-items: center;
		flex: 1;
	}

	h1 {
		font-size: 2rem;
		width: 100%;
	}

	h2 {
		font-size: 1.7rem;
		margin-bottom: 2rem;
	}

	li {
		font-size: 1.1rem;
		margin-bottom: 1rem;
	}

	p {
		text-align: center;
		font-size: 0.9rem;
	}
</style>
