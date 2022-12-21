<script>
	import DDSLock from '../icons/ddslock.png';
	import logoutSVG from '../icons/logout.svg';
	import { goto } from '$app/navigation';
	import { isAuthenticated } from '../stores/authentication';
	import headerTitle from '../stores/headerTitle';
	import detailView from '../stores/detailView';
	import pagebackwardsSVG from '../icons/pagebackwards.svg';
	import renderAvatar from '../stores/renderAvatar';

	export let avatarName;
	export let userEmail;

	// Headers Constants
	let topicsHeader = 'Topics';
	let applicationsHeader = 'Applications';

	const waitTime = 1000;
	const returnKey = 13;

	let avatarDropdownMouseEnter = false;
	let avatarDropdownVisible = false;

	detailView.set();
</script>

<header>
	<div class="header-bar">
		<img src={DDSLock} alt="logo" class="logo" />
		<div class="logo-text">DDS Permissions Manager</div>

		{#if $isAuthenticated}
			{#if $detailView && $headerTitle !== topicsHeader && $headerTitle !== applicationsHeader}
				<img
					class="go-back"
					src={pagebackwardsSVG}
					alt="back to topics"
					on:click={() => detailView.set('backToList')}
				/>
			{/if}

			<div class="header-title">
				{$headerTitle}
			</div>

			{#if $renderAvatar === true}
				<div
					data-cy="avatar-dropdown"
					tabindex="0"
					class="dot"
					on:mouseenter={() => (avatarDropdownMouseEnter = true)}
					on:mouseleave={() => {
						setTimeout(() => {
							if (!avatarDropdownMouseEnter) avatarDropdownVisible = false;
						}, waitTime);
						avatarDropdownMouseEnter = false;
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
			{/if}
		{/if}
	</div>
</header>

<hr style="border-color: rgba(0, 0, 0, 0.15);" />

{#if avatarDropdownVisible}
	<div style="display: flex; justify-content: flex-end; margin-bottom: -1.87rem">
		<table
			class="avatar-dropdown"
			class:hidden={!$isAuthenticated}
			on:mouseenter={() => (avatarDropdownMouseEnter = true)}
			on:mouseleave={() => {
				setTimeout(() => {
					setTimeout(() => {
						if (!avatarDropdownMouseEnter) avatarDropdownVisible = false;
					});
				}, waitTime);
				avatarDropdownMouseEnter = false;
			}}
		>
			<tr>
				<td style="font-weight: 300">{userEmail}</td>
			</tr>

			<tr>
				<td
					data-cy="logout-button"
					on:click={() => goto('/api/logout', true)}
					on:keydown={(event) => {
						if (event.which === returnKey) {
							goto('/api/logout', true);
						}
					}}
					on:focusout={() => (avatarDropdownVisible = false)}
				>
					<a href="/api/logout" style="float:right">
						Logout
						<img src={logoutSVG} alt="logout" class="icon-logout" />
					</a>
				</td>
			</tr>
		</table>
	</div>
{/if}

<style>
	table > tr > td {
		line-height: 2.4rem;
		padding: 0 1rem 0 1rem;
	}

	.header-bar {
		display: flex;
		align-self: center;
		height: 2rem;
		justify-content: space-between;
		width: 100%;
	}

	.logo {
		height: 30px;
		width: 30px;
		margin-left: 1.55rem;
	}

	.logo-text {
		position: absolute;
		left: 10%;
		align-self: center;
		margin-left: 0.5rem;
		font-size: 1.2rem;
		letter-spacing: -0.02rem;
	}

	.header-title {
		position: absolute;
		left: max(50%, 50% + 4.5rem);
		align-self: center;
		font-size: 1.2rem;
	}

	.icon-logout {
		width: 21px;
		height: 21px;
		align-self: center;
		padding-left: 1rem;
	}

	.dot {
		position: sticky;
		position: -webkit-sticky; /* Safari */
		align-self: center;
		right: 1vw;
		height: 2.4rem;
		width: 2.4rem;
		border-radius: 50%;
		font-weight: 600;
		font-size: 0.9rem;
	}

	.dot:hover {
		cursor: pointer;
	}

	.avatar-dropdown {
		position: sticky;
		position: -webkit-sticky; /* Safari */
		align-self: center;
		right: 0vw;
		float: right;
		width: fit-content;
		margin: -0.65rem 0 -2.4rem 1vw;
		background-color: #f3edf7;
		padding-left: 1rem;
		font-size: 0.9rem;
		font-weight: 500;
		line-height: 2.8rem;
		box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.3), 0px 2px 6px 2px rgba(0, 0, 0, 0.15);
	}

	.go-back {
		position: absolute;
		left: min(40%, 30%);
		align-self: center;
		margin-left: 8rem;
		cursor: pointer;
		width: 30px;
		height: 30px;
	}

	a {
		display: flex;
		color: unset;
		height: 2.4rem;
		align-items: center;
	}

	a:hover {
		text-decoration: none;
		color: unset;
	}
</style>
