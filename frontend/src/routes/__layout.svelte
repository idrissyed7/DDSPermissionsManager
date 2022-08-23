<script>
	import { onMount } from 'svelte';
	import { onLoggedIn, isAuthenticated, isAdmin } from '../stores/authentication';
	import permissionsByGroup from '../stores/permissionsByGroup';
	import axios from 'axios';
	import Header from '$lib/header/Header.svelte';
	import '../app.css';

	const URL_PREFIX = 'http://localhost:8080';
	let expirationTime, nowTime, remindTime;

	onMount(async () => {
		try {
			const res = await axios.get(`${URL_PREFIX}/token_info`, { withCredentials: true });
			onLoggedIn(res.data);
			permissionsByGroup.set(res.data.permissionsByGroup);

			remindTime = 60 * 1000 * 5; // 5 minutes
			expirationTime = new Date(res.data.exp * 1000);
			nowTime = new Date();

			console.log('exp:', expirationTime);
			console.log('now:', nowTime);
			console.log('Remind in:', expirationTime - nowTime - remindTime);
			console.log(expirationTime - nowTime);
			// console.log('is authenticated?', $isAuthenticated);
			console.log('is Admin? ', $isAdmin);
		} catch (err) {
			if (err.response.status === 401) {
				// console.log('is authenticated?', $isAuthenticated);
			}
		}
	});
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
