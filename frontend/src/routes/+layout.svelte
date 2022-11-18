<script>
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { onLoggedIn, isAuthenticated, isAdmin } from '../stores/authentication';
	import { httpAdapter } from '../appconfig';
	import Header from '$lib/Header.svelte';
	import Navigation from '$lib//Navigation.svelte';
	import tokenInfoStore from '../stores/tokenInfoStore';
	import '../app.css';

	export let data;

	const userValidityInterval = 10000; // 3 minutes
	// const userValidityInterval = 180000; // 3 minutes
	let expirationTime, nowTime, remindTime;
	let avatarName;

	onMount(async () => {
		try {
			const res = await httpAdapter.get(`/token_info`);
			tokenInfoStore.set(res.data);

			onLoggedIn($tokenInfoStore);
			// onLoggedIn(res.data);

			avatarName = res.data.username.slice(0, 1).toUpperCase();

			console.log('is authenticated?', $isAuthenticated);
			console.log('is Admin? ', $isAdmin);
			setInterval(checkValidity, userValidityInterval);
		} catch (err) {
			console.error(err);
		}
	});

	const checkValidity = async () => {
		try {
			const res = await httpAdapter.get(`/group_membership/user-validity`);
			const verify = res.data;

			if (verify.isAdmin) {
				const tokenData = { roles: ['ADMIN'], sub: verify.name };
				Object.assign(verify, tokenData);
			}
			console.log('verify', verify);
			console.log('is authenticated?', $isAuthenticated);
			console.log('is Admin? ', $isAdmin);

			// Update user's token
			onLoggedIn(verify);
		} catch (err) {
			if (err.response.status === 404) {
				// Logout User
				onLoggedIn(false);
			}
		}
	};
</script>

<div
	class:grid-container={$isAuthenticated}
	class:grid-container-not-authenticated={!$isAuthenticated}
>
	<div class="grid-item-horizontal">
		<Header isAuthenticated={$isAuthenticated} {avatarName} />
	</div>
	{#if $isAuthenticated}
		<div class="grid-item-vertical-nav">
			<Navigation />
		</div>
	{/if}
	<div class="grid-item-vertical-main">
		<main>
			<slot />
		</main>
		<footer>
			{#if $isAuthenticated}
				<p class:footer-margin={$page.url.pathname === '/'}>
					© 2022 Unity Foundation. All rights reserved.
				</p>
			{/if}
		</footer>
	</div>
</div>

<style>
	.grid-container {
		display: grid;
		grid-template-columns: 0.3fr 1fr 1fr;
		grid-template-rows: auto;
		grid-template-areas:
			'header header header '
			'nav main main'
			'footer footer footer ';
	}

	.grid-container-not-authenticated {
		display: grid;
		grid-template-columns: 0.3fr 1fr 1fr;
		grid-template-rows: auto;
		grid-template-areas:
			'header header header '
			'main main main'
			'footer footer footer ';
	}

	.grid-item-horizontal {
		grid-area: header;
	}

	.grid-item-vertical-nav {
		grid-area: nav;
	}

	.grid-item-vertical-main {
		grid-area: main;
	}

	main {
		padding: 1rem;
		box-sizing: border-box;
	}

	footer {
		display: flex;
		flex-direction: column;
		padding: 40px;
		margin-left: 1rem;
		margin-top: 5rem;
	}

	.footer-margin {
		margin-left: 4rem;
	}

	p {
		bottom: 5px;
	}

	@media (min-width: 480px) {
		footer {
			padding: 40px 0;
		}
	}
</style>
