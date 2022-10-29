<script>
	import { page } from '$app/stores';
	import { isAdmin } from '../../stores/authentication';

	export let isAuthenticated;

	const URL_PREFIX = import.meta.env.VITE_BACKEND_URL;
</script>

<header>
	<div class="corner" />
	<div class="menu">
		<nav>
			{#if isAuthenticated}
				<svg viewBox="0 0 2 3" aria-hidden="true">
					<path d="M0,0 L1,2 C1.5,3 1.5,3 2,3 L2,0 Z" />
				</svg>
				<ul>
					<!-- svelte-ignore a11y-positive-tabindex -->
					<li tabindex="1" class:active={$page.url.pathname === '/'}>
						<a sveltekit:prefetch href="/">Home</a>
					</li>
					{#if $isAdmin}
						<!-- svelte-ignore a11y-positive-tabindex -->
						<li tabindex="2" class:active={$page.url.pathname === '/superadmin'}>
							<a sveltekit:prefetch href="/superadmin">Super Admins</a>
						</li>
					{/if}
					<!-- svelte-ignore a11y-positive-tabindex -->
					<li tabindex="3" class:active={$page.url.pathname === '/group_membership'}>
						<a sveltekit:prefetch href="/group_membership">Group Memberships</a>
					</li>
					<!-- svelte-ignore a11y-positive-tabindex -->
					<li tabindex="4" class:active={$page.url.pathname === '/topics'}>
						<a sveltekit:prefetch href="/topics">Topics</a>
					</li>
					<!-- svelte-ignore a11y-positive-tabindex -->
					<li tabindex="5" class:active={$page.url.pathname === '/applications'}>
						<a sveltekit:prefetch href="/applications">Applications</a>
					</li>
					<!-- svelte-ignore a11y-positive-tabindex -->
					<li tabindex="6" class:active={$page.url.pathname === '/groups'}>
						<a sveltekit:prefetch href="/groups">Groups</a>
					</li>
				</ul>
				<svg viewBox="0 0 2 3" aria-hidden="true">
					<path d="M0,0 L0,3 C0.5,3 0.5,3 1,2 L2,0 Z" />
				</svg>
			{/if}
		</nav>
	</div>
	<div class="corner">
		<nav>
			<svg viewBox="0 0 2 3" aria-hidden="true">
				<path d="M0,0 L1,2 C1.5,3 1.5,3 2,3 L2,0 Z" />
			</svg>
			<ul>
				<!-- svelte-ignore a11y-positive-tabindex -->
				<li tabindex="7" class:active={$page.url.pathname === '/login'}>
					<a
						rel="external"
						href={isAuthenticated === true
							? `${URL_PREFIX}/logout`
							: `${URL_PREFIX}/oauth/login/google`}
						>{isAuthenticated === true ? 'Logout' : 'Login'}</a
					>
				</li>
			</ul>
		</nav>
	</div>
</header>

<style>
	.menu {
		margin-left: 7.9rem;
	}

	header {
		display: flex;
		justify-content: space-between;
	}

	nav {
		display: flex;
		justify-content: center;
		--background: rgba(217, 221, 254, 0.7);
	}

	svg {
		width: 2em;
		height: 3em;
		display: block;
	}

	path {
		fill: var(--background);
	}

	ul {
		position: relative;
		padding: 0;
		margin: 0;
		height: 3em;
		display: flex;
		justify-content: center;
		align-items: center;
		list-style: none;
		background: var(--background);
		background-size: contain;
	}

	li {
		position: relative;
		height: 100%;
	}

	li.active::before {
		--size: 6px;
		content: '';
		width: 0;
		height: 0;
		position: absolute;
		top: 0;
		left: calc(50% - var(--size));
		border: var(--size) solid transparent;
		border-top: var(--size) solid var(--accent-color);
	}

	nav a {
		display: flex;
		height: 100%;
		align-items: center;
		padding: 0 1em;
		color: var(--heading-color);
		font-weight: 700;
		font-size: 0.8rem;
		text-transform: uppercase;
		letter-spacing: 0.1em;
		text-decoration: none;
		transition: color 0.2s linear;
	}

	a:hover {
		color: var(--accent-color);
	}
</style>
