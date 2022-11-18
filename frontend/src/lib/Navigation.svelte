<script>
	import homeSVG from '../icons/home.svg';
	import usersSVG from '../icons/users.svg';
	import groupsSVG from '../icons/groups.svg';
	import topicsSVG from '../icons/topics.svg';
	import appsSVG from '../icons/apps.svg';
	import { page } from '$app/stores';
</script>

<nav>
	<ul>
		<!-- svelte-ignore a11y-positive-tabindex -->
		<li tabindex="1" class:active={$page.url.pathname === '/'}>
			<a sveltekit:prefetch href="/">
				<img src={homeSVG} alt="home" class="menu-icon" />Home
			</a>
		</li>

		<!-- svelte-ignore a11y-positive-tabindex -->
		<li tabindex="2" class:active={$page.url.pathname === '/users/'}>
			<a sveltekit:prefetch href="/users">
				<img src={usersSVG} alt="users" class="menu-icon" />Users
			</a>
		</li>

		<!-- svelte-ignore a11y-positive-tabindex -->
		<li tabindex="4" class:active={$page.url.pathname === '/topics/'}>
			<a sveltekit:prefetch href="/topics">
				<img src={topicsSVG} alt="topics" class="menu-icon" />Topics
			</a>
		</li>

		<!-- svelte-ignore a11y-positive-tabindex -->
		<li tabindex="5" class:active={$page.url.pathname === '/applications/'}>
			<a sveltekit:prefetch href="/applications">
				<img src={appsSVG} alt="applications" class="menu-icon" />Applications
			</a>
		</li>

		<!-- svelte-ignore a11y-positive-tabindex -->
		<li tabindex="6" class:active={$page.url.pathname === '/groups/'}>
			<a sveltekit:prefetch href="/groups">
				<img src={groupsSVG} alt="groups" class="menu-icon" />Groups
			</a>
		</li>
	</ul>
</nav>

<style>
	nav {
		width: 13rem;
		justify-content: center;
		margin-top: 2.5rem;
	}

	nav a {
		display: flex;
		height: 100%;
		align-items: center;
		padding: 0 1em;
		color: var(--heading-color);
		font-weight: 600;
		font-size: 1rem;
		letter-spacing: 0.1em;
	}

	nav a img {
		padding-right: 0.5rem;
	}

	a:hover {
		text-decoration: none;
	}

	ul {
		padding: 0;
		height: 3em;
		list-style: none;
		border-radius: 50px;
		border-width: 0px;
		border-style: solid;
		background-size: contain;
	}

	li {
		position: relative;
		height: 100%;
	}

	li.active {
		border-radius: 50px;
		border-width: 0px;
		border-style: solid;
		background-color: #e8def8;
	}

	.menu-icon {
		scale: 65%;
		margin: 0.1rem 0 0.1rem 0;
	}
</style>
