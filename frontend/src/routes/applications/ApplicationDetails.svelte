<script>
	import { createEventDispatcher, onMount } from 'svelte';
	import { page } from '$app/stores';
	import { isAdmin } from '../../stores/authentication';
	import { httpAdapter } from '../../appconfig';
	import Modal from '../../lib/Modal.svelte';
	import applicationPermission from '../../stores/applicationPermission';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import headerTitle from '../../stores/headerTitle';
	import messages from '$lib/messages.json';
	import errorMessages from '$lib/errorMessages.json';
	import editSVG from '../../icons/edit.svg';
	import deleteSVG from '../../icons/delete.svg';

	export let isApplicationAdmin,
		selectedAppId,
		selectedAppGroupId,
		selectedAppName,
		selectedAppDescription = '',
		selectedAppPublic,
		appCurrentGroupPublic,
		selectedAppGroupName;

	const dispatch = createEventDispatcher();

	// Error Handling
	let errorMsg,
		errorObject,
		errorMessageVisible = false;

	let selectedAppDescriptionSelector,
		checkboxSelector,
		selectedPermissionId,
		isPublic = selectedAppPublic;

	let editApplicationVisible = false,
		deleteSelectedGrantsVisible = false;

	const decodeError = (errorObject) => {
		errorObject = errorObject.code.replaceAll('-', '_');
		const cat = errorObject.substring(0, errorObject.indexOf('.'));
		const code = errorObject.substring(errorObject.indexOf('.') + 1, errorObject.length);
		return { category: cat, code: code };
	};

	const errorMessage = (errMsg, errObj) => {
		errorMsg = errMsg;
		errorObject = errObj;
		errorMessageVisible = true;
	};

	const errorMessageClear = () => {
		errorMessageVisible = false;
		errorMsg = '';
		errorObject = '';
	};

	const getAppPermissions = async () => {
		const appPermissionData = await httpAdapter.get(
			`/application_permissions/application/${selectedAppId}`
		);

		applicationPermission.set(appPermissionData.data.content);
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

	const saveNewApp = async (newAppName, newAppDescription, newAppPublic) => {
		const res = await httpAdapter
			.post(`/applications/save/`, {
				id: selectedAppId,
				name: newAppName,
				group: selectedAppGroupId,
				description: newAppDescription,
				public: newAppPublic
			})
			.catch((err) => {
				if (err.response.data && err.response.status === 400) {
					const decodedError = decodeError(Object.create(...err.response.data));
					errorMessage(
						errorMessages['application']['saving.error.title'],
						errorMessages[decodedError.category][decodedError.code]
					);
				}
			});
		dispatch('reloadAllApps');
	};

	onMount(async () => {
		headerTitle.set(selectedAppName);
		await getAppPermissions();
		if (appCurrentGroupPublic === undefined) {
			appCurrentGroupPublic = await getGroupVisibilityPublic(selectedAppGroupName);
		}
	});
</script>

{#if errorMessageVisible}
	<Modal
		title={errorMsg}
		errorMsg={true}
		errorDescription={errorObject}
		closeModalText={errorMessages['modal']['button.close']}
		on:cancel={() => {
			errorMessageVisible = false;
			errorMessageClear();
		}}
	/>
{/if}

{#if editApplicationVisible}
	<Modal
		title={messages['application']['edit']}
		actionEditApplication={true}
		appCurrentName={selectedAppName}
		appCurrentDescription={selectedAppDescription}
		appCurrentPublic={isPublic}
		{appCurrentGroupPublic}
		on:cancel={() => (editApplicationVisible = false)}
		on:saveNewApp={(e) => {
			saveNewApp(e.detail.newAppName, e.detail.newAppDescription, e.detail.newAppPublic);
			headerTitle.set(e.detail.newAppName);
			editApplicationVisible = false;
			selectedAppName = e.detail.newAppName;
			selectedAppDescription = e.detail.newAppDescription;
			isPublic = e.detail.newAppPublic;
		}}
	/>
{/if}

{#if deleteSelectedGrantsVisible}
	<Modal
		actionDeleteGrants={true}
		title={messages['topic.detail']['delete.grants.title'] +
			messages['topic.detail']['delete.grants.single']}
		on:cancel={() => {
			deleteSelectedGrantsVisible = false;
		}}
		on:deleteGrants={async () => {
			dispatch('deleteTopicApplicationAssociation', selectedPermissionId);
			deleteSelectedGrantsVisible = false;
		}}
	/>
{/if}

<div class="content">
	<table>
		<tr>
			<td style="font-weight: 300; width: 11.5rem">
				{messages['application.detail']['row.one']}
			</td>

			<td style="font-weight: 500">{selectedAppName} </td>
			{#if $isAdmin || $permissionsByGroup.find((permission) => permission.groupId === selectedAppGroupId && permission.isApplicationAdmin)}
				<!-- svelte-ignore a11y-click-events-have-key-events -->
				<!-- svelte-ignore a11y-no-noninteractive-tabindex -->
				<img
					data-cy="edit-application-icon"
					src={editSVG}
					tabindex="0"
					alt="edit application"
					width="18rem"
					style="margin-left: 1rem; cursor: pointer"
					on:click={async () => {
						editApplicationVisible = true;
					}}
				/>
			{/if}
		</tr>
		<tr>
			<td style="font-weight: 300; margin-right: 1rem; width: 6.2rem;">
				{messages['application.detail']['row.two']}
			</td>

			<td style="font-weight: 400; margin-left: 2rem" bind:this={selectedAppDescriptionSelector}
				>{selectedAppDescription ? selectedAppDescription : '-'}</td
			>
		</tr>
		<tr>
			<td style="font-weight: 300">
				{messages['application.detail']['row.three']}
			</td>
			<td>
				<input
					type="checkbox"
					tabindex="-1"
					style="width: 15px; height: 15px"
					bind:checked={isPublic}
					on:change={() => (isPublic = selectedAppPublic)}
					bind:this={checkboxSelector}
				/>
			</td>
		</tr>
	</table>

	<div style="font-size:1.3rem; margin-top: 3.5rem; margin-bottom: 1rem">
		{messages['topic.detail']['table.grants.label']}
	</div>

	<table style="min-width: 59rem; max-width: 59rem">
		<thead>
			<tr style="border-top: 1px solid black; border-bottom: 2px solid">
				<td>{messages['application.detail']['table.applications.column.one']}</td>
				<td>{messages['application.detail']['table.applications.column.two']}</td>
				<td>{messages['application.detail']['table.applications.column.three']}</td>
				{#if !$page.url.pathname.includes('search')}
					<td>{messages['application.detail']['table.applications.column.four']}</td>
					<td>{messages['application.detail']['table.applications.column.five']}</td>
				{/if}

				{#if isApplicationAdmin || $isAdmin}
					<td />
				{/if}
			</tr>
		</thead>
		{#if $applicationPermission}
			{#each $applicationPermission as appPermission}
				<tbody>
					<tr style="line-height: 2rem">
						<td style="min-width: 10rem">
							{appPermission.topicGroup}
						</td>
						<td style="min-width: 18rem">
							{appPermission.topicName} ({appPermission.topicCanonicalName})
						</td>
						<td style="min-width: 6.5rem">
							{#if appPermission.read && appPermission.write}
								{messages['application.detail']['table.applications.access.readwrite']}
								{messages['application.detail']['table.applications.access.write']}
							{:else if appPermission.read}
								{messages['application.detail']['table.applications.access.read']}
							{:else if appPermission.write}
								{messages['application.detail']['table.applications.access.write']}
							{/if}
						</td>

						{#if !$page.url.pathname.includes('search')}
							<td style="min-width: 10rem; max-width: 10rem">
								{#if appPermission.writePartitions?.length > 0}
									{appPermission.writePartitions
										.map(function (item) {
											return '[' + item + ']';
										})
										.join(', ')}
								{/if}
							</td>
							<td style="min-width: 10rem; max-width: 10rem">
								{#if appPermission.readPartitions?.length > 0}
									{appPermission.readPartitions
										.map(function (item) {
											return '[' + item + ']';
										})
										.join(', ')}
								{/if}
							</td>
						{:else}
							<td />
							<td />
						{/if}

						{#if isApplicationAdmin || $isAdmin}
							<td>
								<!-- svelte-ignore a11y-click-events-have-key-events -->
								<!-- svelte-ignore a11y-no-noninteractive-tabindex -->
								<img
									src={deleteSVG}
									tabindex="0"
									alt="delete topic"
									height="23px"
									width="23px"
									style="vertical-align: -0.4rem; float: right; cursor: pointer"
									on:click={() => {
										selectedPermissionId = appPermission.id;
										deleteSelectedGrantsVisible = true;
									}}
								/>
							</td>
						{:else}
							<td />
						{/if}
					</tr>
				</tbody>
			{/each}
		{:else}
			<p style="margin:0.3rem 0 0.6rem 0">
				{messages['application.detail']['empty.topics.associated']}
			</p>
		{/if}
		<tr style="font-size: 0.7rem; text-align: right">
			<td style="border: none" />
			<td style="border: none" />
			<td style="border: none" />
			<td style="border: none" />
			<td style="border: none" />
			<td style="border: none; min-width: 3.5rem; text-align:right">
				{#if $applicationPermission}
					{$applicationPermission.length} of {$applicationPermission.length}
				{:else}
					0 of 0
				{/if}
			</td>
		</tr>
	</table>
</div>

<style>
	.content {
		width: 100%;
		min-width: 45rem;
	}

	td {
		height: 2.2rem;
	}
</style>
