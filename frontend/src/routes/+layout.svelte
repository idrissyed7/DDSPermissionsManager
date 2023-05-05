<script>
	import { onMount } from 'svelte';
	import { beforeNavigate, goto } from '$app/navigation';
	import { onLoggedIn, isAuthenticated, isAdmin } from '../stores/authentication';
	import { httpAdapter } from '../appconfig';
	import { browser } from '$app/environment';
	import { page } from '$app/stores';
	import refreshPage from '../stores/refreshPage';
	import lastRefresh from '../stores/lastRefresh';
	import detailView from '../stores/detailView';
	import permissionsByGroup from '../stores/permissionsByGroup';
	import groupAdminGroups from '../stores/groupAdminGroups';
	import topicAdminTopics from '../stores/topicAdminTopics';
	import applicationAdminApplications from '../stores/applicationAdminApplications';
	import universalSearchList from '../stores/universalSearchList';
	import Modal from '$lib/Modal.svelte';
	import Header from '$lib/Header.svelte';
	import headerTitle from '../stores/headerTitle';
	import Navigation from '$lib//Navigation.svelte';
	import userValidityCheck from '../stores/userValidityCheck';
	import loginCompleted from '../stores/loginCompleted';
	import lastActivity from '../stores/lastActivity';
	import messages from '$lib/messages.json';
	import userEmail from '../stores/userEmail';
	import updatePermissionsForAllGroups from '../stores/updatePermissionsForAllGroups';
	import permissionsLastUpdated from '../stores/permissionsLastUpdated';
	import '../app.css';

	export let data;

	let userLoggedCookie;
	let reminderMessageVisible = false;
	let timer;

	// Extend Session
	let reminderMsg, reminderObject;

	const userValidityInterval = 180000; // 3 minutes
	const sixtyMin = 3600000;

	let avatarName;

	userValidityCheck.set(false);

	$: if ($userValidityCheck) {
		refreshToken();
	}

	$: if (browser && reminderMessageVisible) {
		document.body.classList.add('modal-open');
	} else if (browser && !reminderMessageVisible) {
		document.body.classList.remove('modal-open');
	}

	onMount(async () => {
		document.body.addEventListener('click', userClicked);

		window.addEventListener('popstate', (event) => {
			// Search Button
			if (
				$page.url?.pathname === '/search/' &&
				$headerTitle !== messages['universal.search']['header.title'] &&
				$universalSearchList === false
			) {
				headerTitle.set(messages['universal.search']['header.title']);
				universalSearchList.set(true);
			}

			// Topics Button
			if ($page.url?.pathname === '/topics/' && $headerTitle !== messages['topic']['title'])
				detailView.set('backToList');

			// Applications Button
			if (
				$page.url?.pathname === '/applications/' &&
				$headerTitle !== messages['application']['title']
			)
				detailView.set('backToList');

			if ($page.url?.pathname === '/groups/' && $headerTitle !== messages['group']['title'])
				detailView.set('backToList');
		});

		userClicked();
		userLoggedCookie = document.cookie;
		if (userLoggedCookie.includes('JWT_REFRESH_TOKEN')) {
			userLoggedCookie = userLoggedCookie.substring(
				userLoggedCookie.indexOf('JWT_REFRESH_TOKEN=') + 18,
				userLoggedCookie.length
			);

			await refreshToken_Info();

			setInterval(checkValidity, userValidityInterval);
		} else loginCompleted.set(false);
	});

	const userClicked = () => {
		lastActivity.set(Date.now());

		clearTimeout(timer);
		timer = setTimeout(() => goto('/api/logout', true), sixtyMin);
	};

	const reminderMessage = (remMsg, remObj) => {
		reminderMsg = remMsg;
		reminderObject = remObj;
		reminderMessageVisible = true;
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
		loginCompleted.set(true);
		avatarName = res.data.username.slice(0, 1).toUpperCase();
		userEmail.set(res.data.username);
		permissionsLastUpdated.set(res.data.permissionsLastUpdated);

		updatePermissionsForAllGroups.set(true);

		refreshPage.set(Date.now());
		if ($lastRefresh === null) lastRefresh.set($refreshPage);
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

	const checkValidity = async () => {
		try {
			const fiftyFiveMin = 3300000;

			if (Date.now() - $lastActivity > fiftyFiveMin) {
				const msg = messages['refresh_token']['five_min_reminder'];
				reminderMessage('Session Expiration', msg);
			}

			const res = await httpAdapter.get(`/group_membership/user_validity`);

			if (res.data.permissionsLastUpdated !== $permissionsLastUpdated) refreshToken();
		} catch (err) {
			const oneHour = 3600000;

			// If: the user is not active for 1hr we log them out, Else: we refresh the session
			if (Date.now() - $lastActivity > oneHour) goto('/api/logout', true);
			else {
				refreshToken();
			}
		}
	};
</script>

{#if reminderMessageVisible}
	<Modal
		title={reminderMsg}
		reminderMsg={true}
		reminderDescription={reminderObject}
		closeModalText={'No'}
		on:cancel={() => {
			reminderMessageVisible = false;
		}}
		on:extendSession={() => {
			reminderMessageVisible = false;
			refreshToken();
		}}
	/>
{/if}

<div
	class:grid-container={$isAuthenticated}
	class:grid-container-not-authenticated={!$isAuthenticated}
>
	<div class="grid-item-horizontal">
		<Header {avatarName} userEmail={$userEmail} />
	</div>
	{#if $isAuthenticated}
		<div class="grid-item-vertical-nav">
			<Navigation />
		</div>
		<div class="grid-item-vertical-main">
			<main>
				<slot />
			</main>
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
			'nav main main';
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
</style>
