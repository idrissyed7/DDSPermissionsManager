<script>
	import { createEventDispatcher, onMount } from 'svelte';
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
		isPublic = selectedAppPublic;

	let editApplicationVisible = false;

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

<table>
	<tr>
		<td style="font-weight: 300; width: 11.5rem">
			{messages['application.detail']['row.one']}
		</td>

		<td style="font-weight: 500">{selectedAppName} </td>
		{#if $isAdmin || $permissionsByGroup.find((permission) => permission.groupId === selectedAppGroupId && permission.isApplicationAdmin)}
			<!-- svelte-ignore a11y-click-events-have-key-events -->
			<img
				data-cy="edit-application-icon"
				src={editSVG}
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
				style="width: 15px; height: 15px"
				bind:checked={isPublic}
				on:change={() => (isPublic = selectedAppPublic)}
				bind:this={checkboxSelector}
			/>
		</td>
	</tr>
</table>

<table style="max-width: 59rem; margin-top: 3.5rem">
	<thead>
		<tr>
			<td>{messages['application.detail']['table.applications.column.one']}</td>
			<td>{messages['application.detail']['table.applications.column.two']}</td>
			<td>{messages['application.detail']['table.applications.column.three']}</td>
			{#if isApplicationAdmin || $isAdmin}
				<td />
			{/if}
		</tr>
	</thead>
	{#if $applicationPermission}
		{#each $applicationPermission as appPermission}
			<tbody>
				<tr style="line-height: 2rem">
					<td style="min-width: 15rem">
						{appPermission.topicGroup}
					</td>
					<td style="min-width: 20rem">
						{appPermission.topicName}
					</td>
					<td style="min-width: 6.5rem">
						{appPermission.accessType === 'READ_WRITE' ? 'READ & WRITE' : appPermission.accessType}
					</td>
					{#if isApplicationAdmin || $isAdmin}
						<td>
							<!-- svelte-ignore a11y-click-events-have-key-events -->
							<img
								src={deleteSVG}
								alt="delete topic"
								height="23px"
								width="23px"
								style="vertical-align: -0.4rem; float: right; cursor: pointer"
								on:click={() => {
									dispatch('deleteTopicApplicationAssociation', appPermission.id);
								}}
							/>
						</td>
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
		<td style="border: none; min-width: 3.5rem; text-align:right">
			{#if $applicationPermission}
				{$applicationPermission.length} of {$applicationPermission.length}
			{:else}
				0 of 0
			{/if}
		</td>
	</tr>
</table>

<style>
	td {
		height: 2.2rem;
	}
</style>
