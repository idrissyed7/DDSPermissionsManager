<script>
	import { onMount } from 'svelte';
	import { onLoggedIn, isAuthenticated } from '../stores/authentication';
	import axios from 'axios';
	import Header from '$lib/header/Header.svelte';
	import '../app.css';

	onMount(async () => {
		try {
			const res = await axios.get('http://localhost:8080/token_info', { withCredentials: true });
			console.log(res.data);
			onLoggedIn(res.data);
			console.log('is authenticated?', $isAuthenticated);
		} catch (err) {
			if (err.response.status === 401) {
				console.log('is authenticated?', $isAuthenticated);
			}
		}
	});

	const getToken = async () => {
		const res = await axios.get('http://localhost:8080/token_info', { withCredentials: true });
		console.log(res);
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
			<path
				d="M600,112.77C268.63,112.77,0,65.52,0,7.23V120H1200V7.23C1200,65.52,931.37,112.77,600,112.77Z"
				class="shape-fill"
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

	.custom-shape-bottom {
		position: absolute;
		bottom: 0;
		left: 0;
		width: 100%;
		overflow: hidden;
		line-height: 0;
	}

	.custom-shape-bottom svg {
		position: relative;
		display: block;
		width: calc(100% + 1.3px);
		height: 150px;
		mix-blend-mode: overlay;
	}

	.custom-shape-bottom .shape-fill {
		fill: #ffffff;
	}

	@media (min-width: 480px) {
		footer {
			padding: 40px 0;
		}
	}
</style>
