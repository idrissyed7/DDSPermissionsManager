<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount, createEventDispatcher } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import topicDetails from '../../stores/groupDetails';
	import Modal from '../../lib/Modal.svelte';
	import headerTitle from '../../stores/headerTitle';
	import detailView from '../../stores/detailView';
	import deleteSVG from '../../icons/delete.svg';
	import addSVG from '../../icons/add.svg';
	import errorMessages from '$lib/errorMessages.json';

	export let selectedTopicId, isTopicAdmin;

	let selectedTopicName, selectedTopicKind, selectedTopicGroupName, selectedTopicGroupId;
	let selectedTopicCanonicalName;
	let selectedTopicApplications = [];
	let selectedApplicationList;
	let accessTypeSelection;

	const dispatch = createEventDispatcher();

	// Promises
	let promise;

	// Modals
	let errorMessageVisible = false;

	// Constants
	const waitTime = 1000;
	const returnKey = 13;

	// Error Handling
	let errorMsg, errorObject;
	let errorMessageApplication = '';

	// Bind Token
	let bindToken;

	onMount(async () => {
		try {
			promise = await httpAdapter.get(`/topics/show/${selectedTopicId}`);
			topicDetails.set(promise.data);

			await loadApplicationPermissions(selectedTopicId);
			selectedTopicId = $topicDetails.id;
			selectedTopicName = $topicDetails.name;
			selectedTopicCanonicalName = $topicDetails.canonicalName;
			selectedTopicGroupName = $topicDetails.groupName;
			selectedTopicGroupId = $topicDetails.group;
			selectedTopicKind = $topicDetails.kind;

			headerTitle.set(selectedTopicName);
			detailView.set(true);
		} catch (err) {
			errorMessage('Error Loading Topic Details', err.message);
		}
	});

	const errorMessage = (errMsg, errObj) => {
		errorMsg = errMsg;
		errorObject = errObj;
		errorMessageVisible = true;
	};

	const errorMessageClear = () => {
		errorMsg = '';
		errorObject = '';
		errorMessageVisible = false;
	};

	const loadApplicationPermissions = async (topicId) => {
		const resApps = await httpAdapter.get(`/application_permissions/?topic=${topicId}`);
		selectedTopicApplications = resApps.data.content;
	};

	const addTopicApplicationAssociation = async (topicId, reload = false) => {
		const config = {
			headers: {
				accept: 'application/json',
				APPLICATION_BIND_TOKEN: bindToken
			}
		};

		try {
			if (selectedApplicationList && !reload) {
				await httpAdapter.post(`/application_permissions/${topicId}/READ`, {}, config);
			}
			if (reload) {
				await httpAdapter
					.post(`/application_permissions/${topicId}/${accessTypeSelection}`, {}, config)
					.then(async () => await loadApplicationPermissions(topicId));
			}
		} catch (err) {
			if (err.request.status === 403) {
				errorMessage('Error Adding Application', errorMessages['bind_token']['forbidden']);
			}
		}
	};

	const deleteTopicApplicationAssociation = async (permissionId, topicId) => {
		await httpAdapter.delete(`/application_permissions/${permissionId}`);
		await loadApplicationPermissions(topicId);
	};

	const updateTopicApplicationAssociation = async (permissionId, accessType, topicId) => {
		await httpAdapter.put(`/application_permissions/${permissionId}/${accessType}`);
		await loadApplicationPermissions(topicId);
	};
</script>

{#if $isAuthenticated}
	{#if errorMessageVisible}
		<Modal
			title={errorMsg}
			errorMsg={true}
			errorDescription={errorObject}
			closeModalText={'Close'}
			on:cancel={() => (errorMessageVisible = false)}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					errorMessageVisible = false;
				}
			}}
		/>
	{/if}

	{#await promise then _}
		<table class="topics-details">
			<tr>
				<td>Name:</td>
				<td>{selectedTopicName} ({selectedTopicCanonicalName})</td>
				<td />
				<td />
			</tr>

			<tr>
				<td>Group:</td>
				<td>{selectedTopicGroupName}</td>
				<td />
				<td />
			</tr>

			<tr>
				<td>Any application can read:</td>
				<td
					>{#if selectedTopicKind === 'B'}
						Yes
					{:else}
						No
					{/if}
				</td>
				<td />
				<td />
			</tr>

			<tr style="border-width: 0px;">
				<td style="border-bottom-color: transparent;">
					<span style="margin-right: 1rem">Applications:</span>
				</td>

				<td style="border-bottom-color: transparent;">
					<input
						data-cy="bind-token-input"
						style="margin-top: 0.5rem; margin-bottom: 0.5rem"
						class="searchbox"
						type="search"
						placeholder="Bind Token"
						disabled={!$isAdmin && !isTopicAdmin}
						bind:value={bindToken}
						on:keydown={(event) => {
							if (event.which === returnKey) {
								bindToken = bindToken?.trim();
								document.activeElement.blur();
								if (bindToken?.length > 0) addTopicApplicationAssociation(selectedTopicId, true);
							}
						}}
						on:blur={() => {
							bindToken = bindToken?.trim();
						}}
					/>
					<div style="margin-left: 7.4rem">
						<span class="error-message" class:hidden={errorMessageApplication?.length === 0}>
							{errorMessageApplication}
						</span>
					</div>
				</td>
				<td style="border-bottom-color: transparent;">
					<select style="width: 8rem" bind:value={accessTypeSelection}>
						<option value="" disabled selected>Access Type</option>
						<option value="READ">Read</option>
						<option value="WRITE">Write</option>
						<option value="READ_WRITE">Read + Write</option>
					</select>
				</td>
				<td style="border-bottom-color: transparent;">
					<button
						data-cy="add-application-button"
						style="width: 11rem; height: 2.35rem; padding: 0 1rem 0 1rem"
						class="button-blue"
						class:button-disabled={(!$isAdmin && !isTopicAdmin) ||
							bindToken?.length === 0 ||
							accessTypeSelection === ''}
						disabled={(!$isAdmin && !isTopicAdmin) ||
							bindToken?.length === 0 ||
							accessTypeSelection === ''}
						on:click={() => {
							if (bindToken?.length > 0 && accessTypeSelection !== '')
								addTopicApplicationAssociation(selectedTopicId, true);
						}}
					>
						<img
							src={addSVG}
							alt="add application"
							height="20rem"
							style="vertical-align: middle; filter: invert(); margin-right: 0.4rem; margin-left: -0.5rem"
						/>
						<span style="vertical-align: middle">Add Application</span>
					</button>
				</td>
			</tr>
		</table>

		{#if selectedTopicApplications}
			<div>
				{#each selectedTopicApplications as application}
					<div style="display: flex; justify-content: flex-end; font-size: 0.9rem">
						<span style="width: 10.5rem; margin: auto 0">
							{application.applicationName} ({application.applicationGroupName})
						</span>

						<select
							style="width: 8rem; height: 2rem; margin: auto 0"
							bind:value={application.accessType}
							on:change={() => {
								updateTopicApplicationAssociation(
									application.id,
									application.accessType,
									application.topicId
								);
							}}
						>
							<option value="" disabled selected>Access Type</option>
							<option value="READ">Read</option>
							<option value="WRITE">Write</option>
							<option value="READ_WRITE">Read + Write</option>
						</select>

						<img
							src={deleteSVG}
							alt="remove application"
							style="background-color: transparent; cursor: pointer; scale: 50%; margin-right: -1rem"
							on:click={async () => {
								promise = deleteTopicApplicationAssociation(application.id, application.topicId);
							}}
						/>
					</div>
				{/each}
				<div style="font-size: 0.7rem; text-align:right; margin-top: 0.8rem">
					{selectedTopicApplications.length} of {selectedTopicApplications.length}
				</div>
			</div>
		{/if}
		<p style="margin-top: 8rem">© 2022 Unity Foundation. All rights reserved.</p>
	{/await}
{/if}

<style>
	.topics-details {
		font-size: 12pt;
		width: 41rem;
		margin-top: 1.6rem;
	}

	.topics-details td {
		height: 2.2rem;
	}
</style>
