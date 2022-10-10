<script>
	import { error, redirect } from '@sveltejs/kit';
	import { onMount } from 'svelte';
	import { onLoggedIn, isAuthenticated, isAdmin } from '../stores/authentication';
	import { httpAdapter } from '../appconfig';
	import Header from '$lib/header/Header.svelte';
	import '../app.css';

	export let data;
	const userValidityInterval = 180000; // 3 minutes
	let expirationTime, nowTime, remindTime;

	onMount(async () => {
		try {
			const res = await httpAdapter.get(`/token_info`);
			onLoggedIn(res.data);
			// remindTime = 60 * 1000 * 5; // 5 minutes
			// expirationTime = new Date(res.data.exp * 1000);
			// nowTime = new Date();

			// console.log('exp:', expirationTime);
			// console.log('now:', nowTime);
			// console.log('Remind in:', expirationTime - nowTime - remindTime);
			// console.log(expirationTime - nowTime);
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
			console.log(res);
		} catch (err) {
			if (err.response.status === 404) {
				// Logout User
				onLoggedIn(false);
			}
		}
	};
</script>

<Header isAuthenticated={$isAuthenticated} />

<main>
	<slot />
</main>

<footer>
	<p>© 2022 Unity Foundation. All rights reserved.</p>
</footer>

<style>
	main {
		flex: 1;
		display: flex;
		flex-direction: column;
		padding: 1rem;
		width: 100%;
		max-width: 1024px;
		margin: 0 auto;
		box-sizing: border-box;
	}

	footer {
		display: flex;
		flex-direction: column;
		justify-content: center;
		align-items: center;
		padding: 40px;
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
