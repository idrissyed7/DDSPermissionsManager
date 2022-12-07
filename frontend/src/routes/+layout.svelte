<script>
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { onLoggedIn, isAuthenticated, isAdmin } from '../stores/authentication';
	import { httpAdapter } from '../appconfig';
	import { goto } from '$app/navigation';
	import refreshPage from '../stores/refreshPage';
	import permissionsByGroup from '../stores/permissionsByGroup';
	import groupAdminGroups from '../stores/groupAdminGroups';
	import topicAdminTopics from '../stores/topicAdminTopics';
	import applicationAdminApplications from '../stores/applicationAdminApplications';
	import Modal from '$lib/Modal.svelte';
	import Header from '$lib/Header.svelte';
	import Navigation from '$lib//Navigation.svelte';
	import userValidityCheck from '../stores/userValidityCheck';
	import '../app.css';

	export let data;

	let userLoggedCookie;
	let errorMessageVisible = false;

	// Error Handling
	let errorMsg, errorObject;

	// const userValidityInterval = 180000; // 3 minutes
	let expirationTime, nowTime, remindTime;
	let avatarName;

	userValidityCheck.set(false);

	$: if ($userValidityCheck) {
		refreshToken();
	}

	onMount(async () => {
		userLoggedCookie = document.cookie;
		if (userLoggedCookie.includes('JWT_REFRESH_TOKEN')) {
			userLoggedCookie = userLoggedCookie.substring(
				userLoggedCookie.indexOf('=') + 1,
				userLoggedCookie.length
			);

			await refreshToken_Info();
		}
	});

	const errorMessage = (errMsg, errObj) => {
		errorMsg = errMsg;
		errorObject = errObj;
		errorMessageVisible = true;
	};

	const refreshToken_Info = async () => {
		const res = await httpAdapter.get(`/token_info`);
		permissionsByGroup.set(res.data.permissionsByGroup);

		if ($permissionsByGroup) {
			let groupsList = [];
			let topicsList = [];
			let applicationsList = [];

			for (const group of $permissionsByGroup) {
				if (group.isGroupAdmin) groupsList.push(group);
				if (group.isTopicAdmin) topicsList.push(group);
				if (group.isApplicationAdmin) applicationsList.push(group);
			}
			groupAdminGroups.set(groupsList);
			topicAdminTopics.set(topicsList);
			applicationAdminApplications.set(applicationsList);
		}

		onLoggedIn(res.data);
		avatarName = res.data.username.slice(0, 1).toUpperCase();

		refreshPage.set(true);
		console.log('is Admin? ', $isAdmin);
	};

	const refreshToken = async () => {
		var params = new URLSearchParams();
		params.append('grant_type', 'refresh_token');
		params.append('refresh_token', userLoggedCookie);

		const config = {
			headers: { 'content-type': 'application/x-www-form-urlencoded' }
		};

		try {
			await httpAdapter.post('/oauth/access_token', params, config);
			userValidityCheck.set(false);

			await refreshToken_Info();
		} catch (err) {
			goto('/api/logout', true);
		}
	};
</script>

{#if errorMessageVisible}
	<Modal
		title={errorMsg}
		errorMsg={true}
		errorDescription={errorObject}
		closeModalText={'Close'}
		on:cancel={() => {
			errorMessageVisible = false;
		}}
	/>
{/if}

<div
	class:grid-container={$isAuthenticated}
	class:grid-container-not-authenticated={!$isAuthenticated}
>
	<div class="grid-item-horizontal">
		<Header {avatarName} />
	</div>
	{#if $isAuthenticated}
		<div class="grid-item-vertical-nav">
			<Navigation />
		</div>
		<div class="grid-item-vertical-main">
			<main>
				<slot />
			</main>
			<footer>
				<p class:footer-margin={$page.url.pathname === '/'}>
					© 2022 Unity Foundation. All rights reserved.
				</p>
			</footer>
		</div>
	{:else}
		<div class="grid-item-vertical-main-not-authenticated">
			<main>
				<slot />
			</main>
		</div>
	{/if}
</div>

<style>
	.grid-container {
		display: grid;
		width: fit-content;
		grid-template-columns: 0.3fr 1fr 1fr;
		grid-template-rows: auto;
		grid-template-areas:
			'header header header'
			'nav main main'
			'footer footer footer';
	}

	.grid-container-not-authenticated {
		display: grid;
		width: 100%;
		grid-template-columns: 0.3fr 1fr 1fr;
		grid-template-rows: auto;
		grid-template-areas:
			'header header header'
			'main-not-authenticated main-not-authenticated main-not-authenticated'
			'footer footer footer';
	}

	.grid-item-horizontal {
		grid-area: header;
	}

	.grid-item-vertical-nav {
		grid-area: nav;
	}

	.grid-item-vertical-main {
		grid-area: main;
		grid-column: 2;
	}

	.grid-item-vertical-main-not-authenticated {
		grid-area: main-not-authenticated;
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
		margin-left: 3rem;
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
