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
	import groupContext from '../../stores/groupContext';

	export let selectedTopicId, isTopicAdmin;

	const dispatch = createEventDispatcher();

	let selectedTopicName,
		selectedTopicKind,
		selectedTopicGroupName,
		selectedTopicGroupId,
		selectedTopicCanonicalName,
		selectedTopicDescription,
		selectedTopicPublic,
		selectedTopicApplications = [],
		selectedApplicationList,
		readChecked,
		writeChecked,
		topicCurrentGroupPublic,
		partitionListRead = [],
		partitionListWrite = [];

	// Messages
	let deleteToolip,
		deleteMouseEnter = false;

	// Selection
	let grantsRowsSelected = [],
		grantsRowsSelectedTrue = false,
		grantsAllRowsSelectedTrue = false;

	// checkboxes selection
	$: if (selectedTopicApplications?.length === grantsRowsSelected?.length) {
		grantsRowsSelectedTrue = false;
		grantsAllRowsSelectedTrue = true;
	} else if (grantsRowsSelected?.length > 0) {
		grantsRowsSelectedTrue = true;
	} else {
		grantsAllRowsSelectedTrue = false;
	}

	// Success Message
	let notifyApplicationAccessTypeSuccess = false;
	$: if (notifyApplicationAccessTypeSuccess) {
		setTimeout(() => (notifyApplicationAccessTypeSuccess = false), waitTime);
	}

	// Promises
	let promise;

	// Modals
	let errorMessageVisible = false,
		associateApplicationVisible = false,
		editTopicVisible = false,
		deleteSelectedGrantsVisible = false;

	// Constants
	const returnKey = 13,
		waitTime = 2000;

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
		console.log('selectedTopicApplications', selectedTopicApplications);
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
				await httpAdapter.post(
					`/application_permissions/${topicId}`,
					{
						read: readChecked,
						write: writeChecked,
						readPartitions: partitionListRead,
						writePartitions: partitionListWrite
					},
					config
				);
			}
			if (reload) {
				const res = await httpAdapter
					.post(
						`/application_permissions/${topicId}`,
						{
							read: readChecked,
							write: writeChecked,
							readPartitions: partitionListRead,
							writePartitions: partitionListWrite
						},
						config
					)
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

	const deleteTopicApplicationAssociation = async () => {
		for (const grant of grantsRowsSelected) {
			await httpAdapter.delete(`/application_permissions/${grant.id}`);
		}
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

	const deselectAllGrantsCheckboxes = () => {
		grantsAllRowsSelectedTrue = false;
		grantsRowsSelectedTrue = false;
		grantsRowsSelected = [];
		let checkboxes = document.querySelectorAll('.grants-checkbox');
		checkboxes.forEach((checkbox) => (checkbox.checked = false));
	};

	const numberOfSelectedCheckboxes = () => {
		let checkboxes = document.querySelectorAll('.grants-checkbox');
		checkboxes = Array.from(checkboxes);
		return checkboxes.filter((checkbox) => checkbox.checked === true).length;
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
				partitionListRead = e.detail.partitionListRead;
				partitionListWrite = e.detail.partitionListWrite;
				readChecked = e.detail.read;
				writeChecked = e.detail.write;
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

	{#if deleteSelectedGrantsVisible}
		<Modal
			actionDeleteGrants={true}
			title="{messages['topic.detail']['delete.grants.title']} {grantsRowsSelected.length > 1
				? messages['topic.detail']['delete.grants.multiple']
				: messages['topic.detail']['delete.grants.single']}"
			on:cancel={() => {
				if (grantsRowsSelected?.length === 1 && numberOfSelectedCheckboxes() === 0)
					grantsRowsSelected = [];
				deleteSelectedGrantsVisible = false;
			}}
			on:deleteGrants={async () => {
				await deleteTopicApplicationAssociation();

				await loadApplicationPermissions(selectedTopicId);
				deselectAllGrantsCheckboxes();
				deleteSelectedGrantsVisible = false;
			}}
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

		<div>
			<div
				style="display: flex; justify-content: space-between; align-items:center; margin-top: 2rem"
			>
				<div style="font-size:1.3rem; margin-bottom: 1rem">
					{messages['topic.detail']['table.grants.label']}
				</div>
				<div style="margin-bottom: 0.5rem; margin-right: -1rem">
					<img
						src={deleteSVG}
						alt="options"
						class="dot"
						class:button-disabled={(!$isAdmin && !isTopicAdmin) || grantsRowsSelected?.length === 0}
						style="margin-left: 0.5rem; margin-right: 1rem"
						on:click={() => {
							if (grantsRowsSelected.length > 0) deleteSelectedGrantsVisible = true;
						}}
						on:keydown={(event) => {
							if (event.which === returnKey) {
								if (grantsRowsSelected.length > 0) deleteSelectedGrantsVisible = true;
							}
						}}
						on:mouseenter={() => {
							deleteMouseEnter = true;
							if ($isAdmin || isTopicAdmin) {
								if (grantsRowsSelected.length === 0) {
									deleteToolip = messages['topic.detail']['delete.tooltip'];
									const tooltip = document.querySelector('#delete-topics');
									setTimeout(() => {
										if (deleteMouseEnter) {
											tooltip.classList.remove('tooltip-hidden');
											tooltip.classList.add('tooltip');
										}
									}, 1000);
								}
							} else {
								deleteToolip = messages['topic']['delete.tooltip.topic.admin.required'];
								const tooltip = document.querySelector('#delete-topics');
								setTimeout(() => {
									if (deleteMouseEnter) {
										tooltip.classList.remove('tooltip-hidden');
										tooltip.classList.add('tooltip');
										tooltip.setAttribute('style', 'margin-left:10.2rem; margin-top: -1.8rem');
									}
								}, 1000);
							}
						}}
						on:mouseleave={() => {
							deleteMouseEnter = false;
							if (grantsRowsSelected.length === 0) {
								const tooltip = document.querySelector('#delete-topics');
								setTimeout(() => {
									if (!deleteMouseEnter) {
										tooltip.classList.add('tooltip-hidden');
										tooltip.classList.remove('tooltip');
									}
								}, 1000);
							}
						}}
					/>
					<span id="delete-topics" class="tooltip-hidden" style="margin-top: -1.8rem"
						>{deleteToolip}
					</span>

					<img
						data-cy="add-topic"
						src={addSVG}
						alt="options"
						class="dot"
						class:button-disabled={!$isAdmin &&
							!$permissionsByGroup?.find(
								(gm) => gm.groupName === $groupContext?.name && gm.isTopicAdmin === true
							)}
						on:click={() => {
							if ($isAdmin || isTopicAdmin) associateApplicationVisible = true;
						}}
						on:keydown={(event) => {
							if (event.which === returnKey) {
								if ($isAdmin || isTopicAdmin) associateApplicationVisible = true;
							}
						}}
					/>
				</div>
			</div>

			<table style="max-width: 50rem; min-width: 53rem">
				<thead>
					<tr style="border-top: 1px solid black; border-bottom: 2px solid">
						<td>
							<input
								tabindex="-1"
								type="checkbox"
								class="grants-checkbox"
								style="margin-right: 0.5rem"
								bind:indeterminate={grantsRowsSelectedTrue}
								on:click={(e) => {
									if (e.target.checked) {
										grantsRowsSelected = selectedTopicApplications;
										grantsRowsSelectedTrue = false;
										grantsAllRowsSelectedTrue = true;
									} else {
										grantsAllRowsSelectedTrue = false;
										grantsRowsSelectedTrue = false;
										grantsRowsSelected = [];
									}
								}}
								checked={grantsAllRowsSelectedTrue}
							/>
						</td>
						<td>{messages['topic.detail']['table.grants.column.one']}</td>
						<td>{messages['topic.detail']['table.grants.column.two']}</td>
						<td>{messages['topic.detail']['table.grants.column.three']}</td>
						<td>{messages['topic.detail']['table.grants.column.four']}</td>
						<td>{messages['topic.detail']['table.grants.column.five']}</td>
						<td />
						<td />
					</tr>
				</thead>

				{#if selectedTopicApplications?.length > 0}
					{#each selectedTopicApplications as appPermission}
						<tbody>
							<tr style="line-height: 2rem">
								<td style="line-height: 1rem;">
									<input
										tabindex="-1"
										type="checkbox"
										class="grants-checkbox"
										style="margin-right: 0.5rem"
										checked={grantsAllRowsSelectedTrue}
										on:change={(e) => {
											if (e.target.checked === true) {
												grantsRowsSelected.push(appPermission);
												// reactive statement
												grantsRowsSelected = grantsRowsSelected;
												grantsRowsSelectedTrue = true;
											} else {
												grantsRowsSelected = grantsRowsSelected.filter(
													(selection) => selection !== appPermission
												);
												if (grantsRowsSelected.length === 0) {
													grantsRowsSelectedTrue = false;
												}
											}
										}}
									/>
								</td>
								<td style="min-width: fit-content"> {appPermission.applicationGroupName} </td>
								<td style="min-width: fit-content"> {appPermission.applicationName} </td>
								<td style="min-width: 5.5rem; max-width: 5.5rem">
									{#if appPermission.read && appPermission.write}
										{messages['topic.detail']['table.grants.access.readwrite']}
									{:else if appPermission.read}
										{messages['topic.detail']['table.grants.access.read']}
									{/if}
									{#if appPermission.write}
										{messages['topic.detail']['table.grants.access.write']}
									{:else}
										-
									{/if}
								</td>
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
								<td
									style="cursor: pointer; text-align: right"
									on:keydown={(event) => {
										if (event.which === returnKey) {
											// updateGroupMembershipSelection(groupMembership);
										}
									}}
								>
									<!-- svelte-ignore a11y-click-events-have-key-events -->
									<img
										data-cy="edit-users-icon"
										src={editSVG}
										height="17rem"
										width="17rem"
										style="vertical-align: -0.225rem"
										alt="edit user"
										on:click={() => console.log('a')}
									/>
								</td>
								<td style="cursor: pointer; text-align: right; padding-right: 0.25rem; width: 1rem">
									<!-- svelte-ignore a11y-click-events-have-key-events -->
									<img
										data-cy="delete-users-icon"
										src={deleteSVG}
										height="27px"
										width="27px"
										style="vertical-align: -0.5rem"
										alt="delete user"
										on:click={() => {
											if (!grantsRowsSelected.some((grant) => grant === appPermission))
												grantsRowsSelected.push(appPermission);
											deleteSelectedGrantsVisible = true;
										}}
									/>
								</td>
							</tr>
						</tbody>
					{/each}
				{:else}
					<td style="border: none">{messages['topic.detail']['table.grants.empty']}</td>
				{/if}
			</table>
			{#if notifyApplicationAccessTypeSuccess}
				<span
					style="float: right; margin-top: -2.1rem; font-size: 0.65rem; color: white; background-color: black; padding: 0.2rem 0.4rem 0.2rem 0.4rem; border-radius: 15px"
					>{messages['topic.detail']['updated.success']}</span
				>
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
