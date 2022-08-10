<script>
	import { onMount } from 'svelte';
	import { onLoggedIn, isAuthenticated } from '../stores/authentication';
	import axios from 'axios';
	import Header from '$lib/header/Header.svelte';
	import '../app.css';

	const URL_PREFIX = 'http://localhost:8080';
	let expirationTime, nowTime, remindTime;

	onMount(async () => {
		try {
			const res = await axios.get(`${URL_PREFIX}/token_info`, { withCredentials: true });
			onLoggedIn(res.data);
			remindTime = 60 * 1000 * 5; // 5 minutes
			expirationTime = new Date(res.data.exp * 1000);
			nowTime = new Date(Date.now());

			// console.log('exp:', expirationTime);
			// console.log('now', nowTime);
			// console.log('Remind in:', expirationTime - nowTime - remindTime);
			// console.log((expirationTime - nowTime));
			// setTimeout(() => alert('refresh token'), 5000);
			// console.log('is authenticated?', $isAuthenticated);
		} catch (err) {
			if (err.response.status === 401) {
				// console.log('is authenticated?', $isAuthenticated);
			}
		}
	});

	const getToken = async () => {
		const res = await axios.get(`${URL_PREFIX}/token_info`, { withCredentials: true });
	};
</script>

<Header isAuthenticated={$isAuthenticated} />

<main>
	<slot />
</main>

<footer>
	<p>© 2022 Unity Foundation. All rights reserved.</p>
	<div class="custom-shape-bottom">
		<svg
			data-name="Layer 1"
			xmlns="http://www.w3.org/2000/svg"
			viewBox="0 0 1200 120"
			preserveAspectRatio="none"
		>
			<defs>
				<linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0%">
					<stop offset="0%" style="stop-color:rgb(255,255,255);stop-opacity:0.15" />
					<stop offset="100%" style="stop-color:rgb(255,255,255);stop-opacity:0.05" />
				</linearGradient>
			</defs>
			<path
				d="M600,112.77C268.63,112.77,0,65.52,0,7.23V120H1200V7.23C1200,65.52,931.37,112.77,600,112.77Z"
				fill="url(#gradient)"
			/>
		</svg>
	</div>
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

	svg {
		filter: drop-shadow(-5px -5px 3px rgb(0 0 0 / 1));
	}

	p {
		bottom: 5px;
	}

	.custom-shape-bottom {
		position: fixed;
		bottom: 0;
		left: 0;
		width: 100%;
		overflow: hidden;
		line-height: 0;
		z-index: -10;
	}

	.custom-shape-bottom svg {
		position: relative;
		display: block;
		width: calc(100% + 1.3px);
		height: 150px;
		mix-blend-mode: overlay;
	}

	@media (min-width: 480px) {
		footer {
			padding: 40px 0;
		}
	}
</style>
