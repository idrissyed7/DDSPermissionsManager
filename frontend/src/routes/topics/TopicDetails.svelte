<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount, createEventDispatcher } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import topicDetails from '../../stores/groupDetails';
	import Modal from '../../lib/Modal.svelte';
	import headerTitle from '../../stores/headerTitle';
	import detailView from '../../stores/detailView';
	import deleteSVG from '../../icons/delete.svg';
	import addSVG from '../../icons/add.svg';
	import editSVG from '../../icons/edit.svg';
	import errorMessages from '$lib/errorMessages.json';
	import messages from '$lib/messages.json';
	import errorMessageAssociation from '../../stores/errorMessageAssociation';
	import permissionsByGroup from '../../stores/permissionsByGroup';

	export let selectedTopicId, isTopicAdmin;

	const dispatch = createEventDispatcher();

	let selectedTopicName, selectedTopicKind, selectedTopicGroupName, selectedTopicGroupId;
	let selectedTopicCanonicalName;
	let selectedTopicDescription;
	let selectedTopicPublic;
	let selectedTopicApplications = [];
	let selectedApplicationList;
	let accessTypeSelection;
	let topicCurrentGroupPublic;

	// Success Message
	let notifyApplicationAccessTypeSuccess = false;
	$: if (notifyApplicationAccessTypeSuccess) {
		setTimeout(() => (notifyApplicationAccessTypeSuccess = false), waitTime);
	}

	// Promises
	let promise;

	// Modals
	let errorMessageVisible = false;
	let associateApplicationVisible = false;
	let editTopicVisible = false;

	// Constants
	const returnKey = 13;
	const waitTime = 2000;

	// Public flag
	let isPublic;

	// Error Handling
	let errorMsg, errorObject;

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
			selectedTopicDescription = $topicDetails.description;
			selectedTopicPublic = $topicDetails.public;
			selectedTopicGroupName = $topicDetails.groupName;
			selectedTopicGroupId = $topicDetails.group;
			selectedTopicKind = $topicDetails.kind;
			isPublic = $topicDetails.public;

			topicCurrentGroupPublic = await getGroupVisibilityPublic(selectedTopicGroupName);

			headerTitle.set(selectedTopicName);
			detailView.set(true);
		} catch (err) {
			errorMessage(errorMessages['topic']['loading.detail.error.title'], err.message);
		}
	});

	const errorMessage = (errMsg, errObj) => {
		errorMsg = errMsg;
		errorObject = errObj;
		errorMessageVisible = true;
	};

	const loadApplicationPermissions = async (topicId) => {
		const resApps = await httpAdapter.get(`/application_permissions/topic/${topicId}`);
		selectedTopicApplications = resApps.data.content;
	};

	const addTopicApplicationAssociation = async (topicId, reload = false) => {
		const config = {
			headers: {
				accept: 'application/json',
				APPLICATION_GRANT_TOKEN: bindToken
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
			errorMessageAssociation.set([]);
			associateApplicationVisible = false;
		} catch (err) {
			if (err.response.data && err.response.status === 400) {
				const decodedError = decodeError(Object.create(...err.response.data));
				errorMessageAssociation.set(errorMessages[decodedError.category][decodedError.code]);
			} else {
				errorMessageAssociation.set(errorMessages['bind_token']['forbidden']);
			}
		}
	};

	const deleteTopicApplicationAssociation = async (permissionId, topicId) => {
		await httpAdapter.delete(`/application_permissions/${permissionId}`);
		await loadApplicationPermissions(topicId);
	};

	const updateTopicApplicationAssociation = async (permissionId, accessType, topicId) => {
		try {
			const res = await httpAdapter.put(`/application_permissions/${permissionId}/${accessType}`);
			if (res.status === 200) notifyApplicationAccessTypeSuccess = true;
		} catch {
			errorMessage(errorMessages['topic']['updating.access.type.error.title'], err.message);
		}
		await loadApplicationPermissions(topicId);
	};

	const decodeError = (errorObject) => {
		errorObject = errorObject.code.replaceAll('-', '_');
		const cat = errorObject.substring(0, errorObject.indexOf('.'));
		const code = errorObject.substring(errorObject.indexOf('.') + 1, errorObject.length);
		return { category: cat, code: code };
	};

	const saveNewTopic = async (newTopicName, newTopicDescription, newTopicPublic) => {
		try {
			await httpAdapter.post(`/topics/save/`, {
				name: newTopicName,
				id: selectedTopicId,
				kind: selectedTopicKind,
				group: selectedTopicGroupId,
				groupName: selectedTopicGroupName,
				description: newTopicDescription,
				public: newTopicPublic
			});

			dispatch('reloadTopics');
			selectedTopicDescription = newTopicDescription;
			isPublic = newTopicPublic;
			editTopicVisible = false;
		} catch (err) {
			errorMessage(errorMessages['topic']['adding.error.title'], err.message);
			editTopicVisible = false;
			errorMessageVisible = true;
		}
	};

	const getGroupVisibilityPublic = async (groupName) => {
		try {
			const res = await httpAdapter.get(`/groups?filter=${groupName}`);
			if (res.data.content?.length > 0 && res.data?.content[0]?.public) return true;
			else return false;
		} catch (err) {
			errorMessage(errorMessages['group']['error.loading.visibility'], err.message);
		}
	};
</script>

{#if $isAuthenticated}
	{#if errorMessageVisible}
		<Modal
			title={errorMsg}
			errorMsg={true}
			errorDescription={errorObject}
			closeModalText={errorMessages['modal']['button.close']}
			on:cancel={() => (errorMessageVisible = false)}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					errorMessageVisible = false;
				}
			}}
		/>
	{/if}

	{#if associateApplicationVisible}
		<Modal
			title={messages['topic.detail']['application.associate']}
			actionAssociateApplication={true}
			{selectedTopicId}
			on:cancel={() => {
				errorMessageAssociation.set([]);
				associateApplicationVisible = false;
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					errorMessageAssociation.set([]);
					associateApplicationVisible = false;
				}
			}}
			on:addTopicApplicationAssociation={(e) => {
				accessTypeSelection = e.detail.accessTypeSelection;
				bindToken = e.detail.bindToken;
				addTopicApplicationAssociation(selectedTopicId, true);
			}}
		/>
	{/if}

	{#if editTopicVisible}
		<Modal
			title={messages['topic.detail']['edit']}
			actionEditTopic={true}
			topicCurrentName={selectedTopicName}
			topicCurrentDescription={selectedTopicDescription}
			topicCurrentPublic={selectedTopicPublic}
			{topicCurrentGroupPublic}
			on:saveNewTopic={(e) => {
				saveNewTopic(e.detail.newTopicName, e.detail.newTopicDescription, e.detail.newTopicPublic);
				selectedTopicName = e.detail.newTopicName;
				selectedTopicDescription = e.detail.newTopicDescription;
				selectedTopicPublic = e.detail.newTopicPublic;
				editTopicVisible = false;
			}}
			on:cancel={() => (editTopicVisible = false)}
		/>
	{/if}

	{#await promise then _}
		<div style="display: inline-flex; align-items: baseline">
			<table class="topics-details">
				<tr>
					<td>{messages['topic.detail']['row.one']}</td>
					<td>{selectedTopicName} ({selectedTopicCanonicalName})</td>
				</tr>
				<td>{messages['topic.detail']['row.two']}</td>
				<td style="width: fit-content">
					{#if selectedTopicDescription}
						{selectedTopicDescription}
					{:else}
						-
					{/if}
				</td>
				<tr />

				<tr>
					<td>{messages['topic.detail']['row.three']}</td>
					<td>{selectedTopicGroupName}</td>
				</tr>

				<tr>
					<td style="min-width:12rem">{messages['topic.detail']['row.four']}</td>
					<td
						>{#if selectedTopicKind === 'B'}
							{messages['topic.detail']['any.application.can.read.yes']}
						{:else}
							{messages['topic.detail']['any.application.can.read.no']}
						{/if}
					</td>
				</tr>

				<tr>
					<td>{messages['topic.detail']['row.five']}</td>
					<td>
						<input
							type="checkbox"
							style="vertical-align: middle; margin-left: 0.1rem; width: 15px; height: 15px"
							bind:checked={isPublic}
							on:change={() => (isPublic = selectedTopicPublic)}
						/>
					</td>
				</tr>
			</table>

			{#if $isAdmin || $permissionsByGroup.find((permission) => permission.groupId === selectedTopicGroupId && permission.isTopicAdmin)}
				<!-- svelte-ignore a11y-click-events-have-key-events -->
				<img
					src={editSVG}
					alt="edit topic"
					height="20rem"
					style="margin-left: 2rem; cursor:pointer"
					on:click={() => (editTopicVisible = true)}
				/>
			{/if}
		</div>

		<div
			style="display: flex; padding-left: 0.3rem; padding-top: 0.25rem; padding-bottom: 0.25rem; justify-content: space-between"
		>
			{messages['topic.detail']['row.six']}

			<button
				data-cy="add-application-button"
				style="width: 12rem; height: 2.35rem; padding: 0 1rem 0 1rem; margin-right: 3rem"
				class="button-blue"
				class:button-disabled={!$isAdmin && !isTopicAdmin}
				disabled={!$isAdmin && !isTopicAdmin}
				on:click={() => (associateApplicationVisible = true)}
			>
				<img
					src={addSVG}
					alt="grant application"
					height="20rem"
					style="vertical-align: middle; filter: invert(); margin-right: 0.4rem; margin-left: -0.5rem"
				/>
				<span style="vertical-align: middle">
					{messages['topic.detail']['add.application']}
				</span>
			</button>
		</div>
		<div>
			{#if selectedTopicApplications}
				<div>
					{#each selectedTopicApplications as application}
						<div style="display: flex; justify-content: flex-end; font-size: 0.9rem">
							<span style="width: 10.5rem; margin: auto 0; margin-right: 0.3rem">
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
								<option value="" disabled selected>
									{messages['topic.detail']['selected.applications.access.type']}
								</option>
								<option value="READ">
									{messages['topic.detail']['selected.applications.read']}
								</option>
								<option value="WRITE">
									{messages['topic.detail']['selected.applications.write']}
								</option>
								<option value="READ_WRITE">
									{messages['topic.detail']['selected.applications.read.write']}
								</option>
							</select>

							<!-- svelte-ignore a11y-click-events-have-key-events -->
							<img
								src={deleteSVG}
								alt="remove application"
								style="background-color: transparent; cursor: pointer; scale: 50%; margin-right: -1rem"
								on:click={async () => {
									promise = await deleteTopicApplicationAssociation(
										application.id,
										application.topicId
									);
								}}
							/>
						</div>
					{/each}
					<div style="font-size: 0.7rem; text-align:right; margin-top: 1.1rem">
						{selectedTopicApplications.length} of {selectedTopicApplications.length}
					</div>
				</div>
				{#if notifyApplicationAccessTypeSuccess}
					<span
						style="float: right; margin-top: -2.1rem; font-size: 0.65rem; color: white; background-color: black; padding: 0.2rem 0.4rem 0.2rem 0.4rem; border-radius: 15px"
						>{messages['topic.detail']['updated.success']}</span
					>
				{/if}
			{/if}
		</div>
		<p style="margin-top: 8rem">{messages['footer']['message']}</p>
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
