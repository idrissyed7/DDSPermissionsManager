<script context="module">
	import { browser, dev } from '$app/env';
	export const hydrate = dev;
	export const router = browser;
</script>

<script>
	import { onMount } from 'svelte';
	import { isAuthenticated } from '../stores/authentication';
	import Modal from '../lib/Modal.svelte';
	import axios from 'axios';
	import applications from '../stores/applications';

	const URL_PREFIX = 'http://localhost:8080';

	// Error Handling
	let errorMessage, errorObject;
	let duplicateAppName = false;

	// Modals
	let errorMessageVisible = false;
	let addApplicationVisible = false;
	let confirmDeleteVisible = false;

	// Pagination
	const applicationsPerPage = 3;
	let applicationsPageIndex;
	let applicationsPages = [];
	let currentPage = 0;

	// App
	let appName;
	let appElement;

	// Selection
	let selectedAppName, selectedAppId;

	onMount(async () => {
		try {
			const applicationsData = await axios.get(`${URL_PREFIX}/applications`, {
				withCredentials: true
			});
			applications.set(applicationsData.data.content);

			if ($applications) {
				// Pagination
				let totalApplicationsCount = 0;
				applicationsPageIndex = Math.floor(
					applicationsData.data.content.length / applicationsPerPage
				);
				if (applicationsData.data.content.length % applicationsPerPage > 0) applicationsPageIndex++;

				// Populate the applicationsPage Array
				let pageArray = [];
				for (let page = 0; page < applicationsPageIndex; page++) {
					for (
						let i = 0;
						i < applicationsPerPage &&
						totalApplicationsCount < applicationsData.data.content.length;
						i++
					) {
						applicationsData.data.content[page * applicationsPerPage + i].editable = false;
						pageArray.push(applicationsData.data.content[page * applicationsPerPage + i]);
						totalApplicationsCount++;
					}
					applicationsPages.push(pageArray);
					pageArray = [];
				}
			}
		} catch (err) {
			ErrorMessage('Error Loading Applications', err.message);
		}
	});

	const ErrorMessage = (errMsg, errObj) => {
		errorMessage = errMsg;
		errorObject = errObj;
		addApplicationVisible = false;
		confirmDeleteVisible = false;
		errorMessageVisible = true;
	};

	const ErrorMessageClear = () => {
		errorMessage = '';
		errorObject = '';
		errorMessageVisible = false;
	};

	const addApplication = async () => {
		try {
			const res = await axios.post(
				`${URL_PREFIX}/applications/save`,
				{
					name: appName
				},
				{ withCredentials: true }
			);
			addApplicationVisible = false;
		} catch (err) {
			ErrorMessage('Error Creating Application', err.message);
		}

		await reloadApps().then(() => {
			currentPage = applicationsPages.length - 1;
		});
	};

	const confirmAppDelete = (ID, name) => {
		confirmDeleteVisible = true;

		selectedAppId = ID;
		selectedAppName = name;
	};

	const appDelete = async () => {
		confirmDeleteVisible = false;
		await axios
			.post(
				`${URL_PREFIX}/applications/delete/${selectedAppId}`,
				{
					id: selectedAppId
				},
				{ withCredentials: true }
			)
			.catch((err) => {
				ErrorMessage('Error Deleting Application', err.message);
			});

		selectedAppId = '';
		selectedAppName = '';

		reloadApps();
	};

	const reloadApps = async () => {
		try {
			const res = await axios.get(`${URL_PREFIX}/applications`, { withCredentials: true });
			applications.set(res.data.content);
			if ($applications) {
				calculatePagination();
			}
		} catch (err) {
			// TODO Error Modal
			console.error('Error loading Applications');
		}
	};

	const addAppModal = () => {
		appName = '';
		addApplicationVisible = true;
	};

	const saveNewAppName = async (name, id) => {
		selectedAppName = name;
		selectedAppId = id;

		const res = await axios
			.post(
				`${URL_PREFIX}/applications/save/`,
				{
					id: selectedAppId,
					name: selectedAppName.trim()
				},
				{ withCredentials: true }
			)
			.catch((err) => {
				// dispatch error message
				console.error(err);
			});
		reloadAllApps();
	};

	const reloadAllApps = async () => {
		try {
			const res = await axios.get(`${URL_PREFIX}/applications`, { withCredentials: true });
			applications.set(res.data.content);

			calculatePagination();
		} catch (err) {
			console.error('Error loading Applications');
		}
	};

	const calculatePagination = () => {
		applicationsPages = [];
		let totalApplicationsCount = 0;
		applicationsPageIndex = Math.floor($applications.length / applicationsPerPage);
		if ($applications.length % applicationsPerPage > 0) applicationsPageIndex++;

		if (applicationsPageIndex === currentPage) currentPage--;

		// Populate the applicationsPage Array
		let pageArray = [];
		for (let page = 0; page < applicationsPageIndex; page++) {
			for (
				let i = 0;
				i < applicationsPerPage && totalApplicationsCount < $applications.length;
				i++
			) {
				$applications[page * applicationsPerPage + i].editable = false;
				pageArray.push($applications[page * applicationsPerPage + i]);
				totalApplicationsCount++;
			}
			applicationsPages.push(pageArray);
			pageArray = [];
		}
	};

	const checkAppDuplicates = (appName, appID) => {
		if ($applications) {
			duplicateAppName = $applications.some(
				(appStore) => appStore.name === appName && appStore.id !== appID
			);
		}

		if (!duplicateAppName && appID === 0) {
			addApplication();
		}

		if (!duplicateAppName && appID !== 0) {
			saveNewAppName(appName, appID);
		}
	};
</script>

<svelte:head>
	<title>Applications | Permission Manager</title>
	<meta name="description" content="Permission Manager Applications" />
</svelte:head>

{#if $isAuthenticated}
	{#if errorMessageVisible}
		<Modal
			title={errorMessage}
			description={errorObject}
			on:cancel={() => {
				errorMessageVisible = false;
				ErrorMessageClear();
			}}
			><br /><br />
			<div class="confirm">
				<button
					class="button-delete"
					on:click={() => {
						errorMessageVisible = false;
						ErrorMessageClear();
					}}>Ok</button
				>
			</div>
		</Modal>
	{/if}

	{#if confirmDeleteVisible && !errorMessageVisible}
		<Modal title="Delete {selectedAppName}?" on:cancel={() => (confirmDeleteVisible = false)}>
			<div class="confirm-delete">
				<button class="button-cancel" on:click={() => (confirmDeleteVisible = false)}>Cancel</button
				>
				<button class="button-delete" on:click={() => appDelete()}><span>Delete</span></button>
			</div>
		</Modal>
	{/if}

	{#if addApplicationVisible && !errorMessageVisible}
		<div class="add">
			<Modal
				title="Create Application"
				on:cancel={() => {
					addApplicationVisible = false;
					duplicateAppName = false;
				}}
			>
				<input
					type="text"
					placeholder="Application Name"
					class:duplicate={duplicateAppName}
					bind:value={appName}
					on:click={() => (duplicateAppName = false)}
					on:keydown={(event) => {
						if (event.which === 13) {
							document.activeElement.blur();
							checkAppDuplicates(appName, 0);
						}
					}}
				/>
				<button
					class="button"
					style="margin-left: 1rem; width: 4.8rem"
					on:click={() => {
						checkAppDuplicates(appName, 0);
					}}
				>
					<span>Create</span></button
				>
				{#if duplicateAppName}
					<p style="position: absolute; left:33%; top: 38px; color: red">
						Application name must be unique
					</p>
				{/if}
			</Modal>
		</div>
	{/if}

	<div class="content">
		<h1>Applications</h1>
		{#if $applications}
			<table>
				<tr>
					<th><strong>ID</strong></th>
					<th><strong>&ensp; Application Name</strong></th>
				</tr>
				{#if applicationsPages.length > 0}
					{#each applicationsPages[currentPage] as app, index}
						<tr>
							<td>{app.id}</td>
							<div class="tooltip">
								<input
									id="app-name"
									on:click={() => (app.editable = true)}
									on:blur={() => checkAppDuplicates(app.name, app.id)}
									on:keydown={(event) => {
										if (event.which === 13) {
											document.activeElement.blur();
											checkAppDuplicates(app.name, app.id);
										}
									}}
									bind:value={app.name}
									bind:this={appElement}
									class:app-name-as-label={!app.editable}
									class:duplicate={duplicateAppName}
								/>
								<span class="tooltiptext">&#9998</span>
							</div>
							<td
								><button class="button-delete" on:click={() => confirmAppDelete(app.id, app.name)}
									><span>Delete</span></button
								></td
							>
						</tr>
					{/each}
				{/if}
			</table>
			{#if duplicateAppName && !addApplicationVisible}
				<br />
				<center><div style="color: red">Application name must be unique</div></center>
			{/if}
		{:else}
			<center><p>No Applications Found</p></center>
		{/if}
		<br /> <br />
		{#if $applications}
			<center
				><button
					on:click={() => {
						if (currentPage > 0) currentPage--;
					}}
					class="button-pagination"
					style="width: 4.8rem; border-bottom-left-radius:9px; border-top-left-radius:9px;"
					disabled={currentPage === 0}>Previous</button
				>
				{#if applicationsPageIndex > 2}
					{#each applicationsPages as page, i}
						<button
							on:click={() => {
								currentPage = i;
							}}
							class="button-pagination"
							class:button-pagination-selected={i === currentPage}>{i + 1}</button
						>
					{/each}
				{/if}
				<button
					on:click={() => {
						if (currentPage < applicationsPages.length) currentPage++;
					}}
					class="button-pagination"
					style="width: 3.1rem; border-bottom-right-radius:9px; border-top-right-radius:9px;"
					disabled={currentPage === applicationsPages.length - 1 || applicationsPages.length === 0}
					>Next</button
				></center
			>
		{/if}
		<br /><br />
		<center
			><button class="button" style="width: 9rem" on:click={() => addAppModal()}
				>Create Application</button
			></center
		>
	</div>
{/if}

<style>
	.tooltip .tooltiptext {
		top: 0px;
	}

	.app-name-as-label {
		text-align: left;
		border: none;
		padding-left: 1.1rem;
		background-color: rgba(0, 0, 0, 0);
	}

	.app-name-as-label:hover {
		color: rgb(103, 103, 103);
		cursor: pointer;
	}

	.tooltip .tooltiptext {
		transform: rotateY(0deg);
	}

	.duplicate {
		border-width: 3px;
		border-color: red;
	}
</style>
