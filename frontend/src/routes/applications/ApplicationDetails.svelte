<script>
	import { createEventDispatcher, onMount } from 'svelte';
	import { isAdmin } from '../../stores/authentication';
	import { httpAdapter } from '../../appconfig';
	import Modal from '../../lib/Modal.svelte';
	import applicationPermission from '../../stores/applicationPermission';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import headerTitle from '../../stores/headerTitle';
	import messages from '$lib/messages.json';
	import editSVG from '../../icons/edit.svg';
	import deleteSVG from '../../icons/delete.svg';

	export let isApplicationAdmin,
		selectedAppId,
		selectedAppGroupId,
		selectedAppName,
		selectedAppDescription = '',
		selectedAppPublic,
		appCurrentGroupPublic;

	const dispatch = createEventDispatcher();

	let selectedAppDescriptionSelector, checkboxSelector, isPublic;

	let editApplicationVisible = false;

	const saveNewApp = async (newAppName, newAppDescription, newAppPublic) => {
		await httpAdapter
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

	onMount(() => {
		headerTitle.set(selectedAppName);
	});
</script>

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

<div style="margin-top: 2.1rem; width:100%">
	<span style="font-size: 1.1rem; font-weight: 300; display: inline-flex; width: 9.2rem">
		{messages['application.detail']['row.one']}
	</span>
	<span style="font-size: 1.3rem; font-weight: 500">{selectedAppName} </span>
	{#if $isAdmin || $permissionsByGroup.find((permission) => permission.groupId === selectedAppGroupId && permission.isApplicationAdmin)}
		<img
			data-cy="edit-application-icon"
			src={editSVG}
			alt="edit application"
			width="18rem"
			style="margin-left: 7rem; cursor: pointer"
			on:click={async () => {
				editApplicationVisible = true;
			}}
		/>
	{/if}
</div>
<div style="margin-top: 0.5rem; width:fit-content">
	<span
		style="font-weight: 300; font-size: 1.1rem; margin-right: 1rem; display: inline-flex; width: 6.2rem;"
	>
		{messages['application.detail']['row.two']}
	</span>
	<span
		style="font-weight: 400; font-size: 1.1rem; margin-left: 2rem"
		bind:this={selectedAppDescriptionSelector}
		>{selectedAppDescription ? selectedAppDescription : '-'}</span
	>
</div>
<div style="font-size: 1.1rem; margin-top: 0.5rem; width: fit-content">
	<span style="font-weight: 300; vertical-align: 1rem">
		{messages['application.detail']['row.three']}
	</span>
	<input
		type="checkbox"
		style="vertical-align: 1rem; margin-left: 6rem; width: 15px; height: 15px"
		bind:checked={isPublic}
		on:change={() => (isPublic = selectedAppPublic)}
		bind:this={checkboxSelector}
	/>
</div>

<table style="width: 35rem; margin-top: 1rem">
	<thead>
		<tr style="border-width: 0px">
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
					<td>
						{appPermission.topicGroup}
					</td>
					<td>
						{appPermission.topicName}
					</td>
					<td>
						{appPermission.accessType === 'READ_WRITE' ? 'READ & WRITE' : appPermission.accessType}
					</td>
					{#if isApplicationAdmin || $isAdmin}
						<td>
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
</table>
<div style="font-size: 0.7rem; width:35rem; text-align:right;  margin-top: 1rem">
	{#if $applicationPermission}
		{$applicationPermission.length} of {$applicationPermission.length}
	{:else}
		0 of 0
	{/if}
</div>
