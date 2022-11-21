<script>
	import DDSLock from '../icons/ddslock.png';
	import logoutSVG from '../icons/logout.svg';
	import { goto } from '$app/navigation';
	import { isAuthenticated } from '../stores/authentication';
	import headerTitle from '../stores/headerTitle';
	import detailView from '../stores/detailView';
	import pagebackwardsSVG from '../icons/pagebackwards.svg';

	export let avatarName;

	const waitTime = 250;
	const returnKey = 13;

	let avatarDropdownMouseEnter = false;
	let avatarDropdownVisible = false;
	detailView.set();
</script>

<header>
	<img src={DDSLock} alt="logo" class="logo" />
	<div class="logo-text">DDS Permission Manager</div>
	{#if $isAuthenticated}
		{#if $detailView}
			<img
				src={pagebackwardsSVG}
				alt="back to topics"
				style="margin-left: 10rem; cursor: pointer"
				on:click={() => detailView.set()}
			/>
		{/if}
		<div class="header-title">{$headerTitle}</div>
		<div
			tabindex="0"
			class="dot"
			align="right"
			on:mouseleave={() => {
				setTimeout(() => {
					if (!avatarDropdownMouseEnter) avatarDropdownVisible = false;
				}, waitTime);
			}}
			on:click={() => (avatarDropdownVisible = !avatarDropdownVisible)}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					avatarDropdownVisible = !avatarDropdownVisible;
				}
			}}
		>
			{avatarName}
		</div>
		{#if avatarDropdownVisible}
			<div
				class="avatar-dropdown"
				on:mouseenter={() => (avatarDropdownMouseEnter = true)}
				on:mouseleave={() => {
					setTimeout(() => {
						avatarDropdownVisible = !avatarDropdownVisible;
					}, waitTime);
				}}
				on:click={() => goto('/api/logout', true)}
				on:keydown={(event) => {
					if (event.which === returnKey) {
						goto('/api/logout', true);
					}
				}}
				on:focusout={() => (avatarDropdownVisible = false)}
			>
				<a href="/api/logout">
					Logout
					<img src={logoutSVG} alt="logout" class="icon-logout" />
				</a>
			</div>
		{/if}
	{/if}
</header>
<hr style="border-color: rgba(0, 0, 0, 0.15);" />

<style>
	.header-title {
		position: absolute;
		left: 50%;
		align-self: center;
		font-size: 1.2rem;
	}

	.icon-logout {
		scale: 45%;
		align-items: center;
		align-self: center;
		text-align: center;
		margin-left: 2.5rem;
	}

	.avatar-dropdown {
		position: absolute;
		width: fit-content;
		right: 0;
		top: 3rem;
		background-color: #f3edf7;
		padding-left: 1rem;
		font-size: 0.9rem;
		font-weight: 500;
		height: 2.8rem;
		box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.3), 0px 2px 6px 2px rgba(0, 0, 0, 0.15);
	}

	.dot {
		position: absolute;
		display: flex;
		flex-direction: row;
		flex-wrap: nowrap;
		justify-content: center;
		align-items: center;
		align-self: center;
		text-align: center;
		right: 1.3rem;
		height: 2.4rem;
		width: 2.4rem;
		color: white;
		background-color: #6750a4;
		border-radius: 50%;
		font-weight: 600;
		font-size: 0.9rem;
	}

	.dot:hover {
		cursor: pointer;
	}

	div.dot:focus {
		background-color: #7b61c4;
	}

	.logo {
		scale: 90%;
		margin-left: 1.55rem;
	}

	.logo-text {
		position: relative;
		top: 0.3rem;
		margin-left: 0.5rem;
		font-size: 1.2rem;
		letter-spacing: -0.02rem;
	}

	header {
		display: flex;
		align-items: left;
		height: 2rem;
	}

	a {
		display: flex;
		align-items: center;
		align-self: center;
		text-align: center;
		color: unset;
		margin-top: -0.1rem;
	}

	a:hover {
		text-decoration: none;
		color: unset;
	}
</style>
