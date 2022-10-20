<script>
	import { isAuthenticated } from '../stores/authentication';
	import urlparameters from '../stores/urlparameters';
	import wavesSVG from '../icons/waves.svg';
	import googleSVG from '../icons/google.svg';

	const URL_PREFIX = import.meta.env.VITE_BACKEND_URL;

	export let data, errors;
</script>

<svelte:head>
	<title>DDS Permissions Manager</title>
	<meta name="description" content="Permissions Manager" />
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
	<div class="container-box" style="width: 55%; scale:80%">
		<img
			src={wavesSVG}
			alt="Login"
			class="icon"
			style="margin: 1rem 0 1rem 1.5rem; filter: contrast(60%);"
		/>
		<span style="margin-left: 1rem; vertical-align: middle; filter: contrast(60%);">Login</span>
		<hr />
		<center>
			<div class="login-button">
				<img
					src={googleSVG}
					alt="Login"
					class="icon"
					style="scale: 55%; margin: 0.1rem 0 0.1rem 0;"
				/>
				<span style="margin-left: 0; font-size: 1.4rem"
					><a rel="external" href={`${URL_PREFIX}/oauth/login/google`}>Login with Google</a></span
				>
			</div>
		</center>
	</div>
{/if}

<style>
	span {
		font-weight: 500;
		margin-left: 0.5rem;
		font-size: 1.5rem;
		vertical-align: middle;
	}

	section {
		display: flex;
		flex-direction: column;
		justify-content: center;
		align-items: center;
		flex: 1;
	}

	h1 {
		font-size: 3.2rem;
		font-weight: 450;
		font-stretch: 110%;
		color: black;
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

	hr {
		margin: 1.5rem 1.3rem 1.8rem 1.3rem;
		border-color: rgba(0, 0, 0, 0.15);
		border-style: solid;
		border-width: 0.05rem;
	}

	.login-button {
		scale: 90%;
		cursor: pointer;
		vertical-align: middle;
		background-color: rgba(0, 0, 0, 0);
		border-color: rgb(237, 237, 237);
		border-radius: 50px;
		border-width: 1px;
		border-style: solid;
		margin-bottom: 1rem;
		text-align: center;
		box-shadow: 0px 2px 3px rgba(0, 0, 0, 0.4);
	}

	.login-button a {
		color: #6751a4;
	}
</style>
