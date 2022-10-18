<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import groupMembershipList from '../../stores/groupMembershipList';
	import Modal from '../../lib/Modal.svelte';

	export let data, errors;

	// Modals
	let addGroupMembershipVisible = false;
	let errorMessageVisible = false;
	let updateGroupMembershipVisible = false;
	let deleteGroupMembershipVisible = false;

	// SearchBox
	let searchString;
	const searchStringLength = 3;
	let searchGroups;
	let searchGroupResults;
	let searchGroupsResultsVisible = false;
	let searchGroupActive = false;
	let timer;
	const waitTime = 500;

	// Forms
	let emailValue = '';
	let selectedGroup;
	let selectedIsGroupAdmin = false;
	let selectedIsApplicationAdmin = false;
	let selectedIsTopicAdmin = false;
	let groupsDropdownSuggestion = 7;

	// Group Permissions
	let isGroupAdmin = false;
	let groupAdminGroups = [];

	// Pagination
	const groupMembershipsPerPage = 10;
	let groupMembershipsTotalPages;
	let groupMembershipsCurrentPage = 0;

	// Error Handling
	let errorMsg, errorObject;
	let invalidEmail = true;
	let validRegex =
		/^([a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+(?:\.[a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)$/gm;

	// Group Membership List
	let groupMembershipListArray = [];

	// Search Group Membership Feature
	$: if (searchString?.trim().length >= searchStringLength) {
		searchGroupActive = false;
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchGroupMemberships(searchString.trim());
		}, waitTime);
	}

	$: if (searchString?.trim().length < searchStringLength) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			reloadGroupMemberships();
		}, waitTime);
	}

	// Search Groups Feature
	$: if (searchGroups?.trim().length >= searchStringLength && searchGroupActive) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchGroup(searchGroups.trim());
		}, waitTime);
	} else {
		searchGroupsResultsVisible = false;
	}

	// Search Groups Dropdown Visibility
	$: if (searchGroupResults?.data?.content?.length >= 1 && searchGroupActive) {
		searchGroupsResultsVisible = true;
	} else {
		searchGroupsResultsVisible = false;
	}

	// Reset add group form once closed
	$: if (addGroupMembershipVisible === false) {
		searchGroups = '';
		emailValue = '';
		selectedIsGroupAdmin = false;
		selectedIsApplicationAdmin = false;
		selectedIsTopicAdmin = false;
	}

	// Selection
	let selectedGroupMembership = {
		userEmail: '',
		groupName: '',
		groupId: '',
		groupAdmin: '',
		applicationAdmin: '',
		topicAdmin: '',
		groupMembershipId: '',
		userId: ''
	};

	// Data
	let dataUpdated = false;

	onMount(async () => {
		const res = await httpAdapter.get(`/token_info`);
		permissionsByGroup.set(res.data.permissionsByGroup);

		if ($permissionsByGroup) {
			$permissionsByGroup.forEach((group) => {
				if (group.isGroupAdmin) {
					groupAdminGroups.push(group);
				}
			});
			if (groupAdminGroups.length > 0) {
				isGroupAdmin = true;
			}
		}
		reloadGroupMemberships();

		if ($urlparameters?.type === 'prepopulate') {
			searchString = $urlparameters.data;
			urlparameters.set([]);
		}
	});

	const reloadGroupMemberships = async (page = 0) => {
		try {
			let res;
			if (searchString && searchString.length >= searchStringLength) {
				res = await httpAdapter.get(
					`/group_membership?page=${page}&size=${groupMembershipsPerPage}&filter=${searchString}`
				);
			} else {
				res = await httpAdapter.get(
					`/group_membership?page=${page}&size=${groupMembershipsPerPage}`
				);
			}
			if (res.data) {
				groupMembershipsTotalPages = res.data.totalPages;
			}
			if (res.data.content) {
				createGroupMembershipList(res.data.content, res.data.totalPages);
			}
			groupMembershipsCurrentPage = page;
		} catch (err) {
			errorMessage('Error Loading Group Memberships', err.message);
		}
	};

	const createGroupMembershipList = async (data, totalPages) => {
		data?.forEach((groupMembership) => {
			let newGroupMembership = {
				applicationAdmin: groupMembership.applicationAdmin,
				groupAdmin: groupMembership.groupAdmin,
				topicAdmin: groupMembership.topicAdmin,
				groupName: groupMembership.permissionsGroupName,
				groupId: groupMembership.permissionsGroup,
				groupMembershipId: groupMembership.id,
				userId: groupMembership.permissionsUser,
				userEmail: groupMembership.permissionsUserEmail
			};
			groupMembershipListArray.push(newGroupMembership);
		});
		groupMembershipList.set(groupMembershipListArray);
		groupMembershipListArray = [];
		groupMembershipsTotalPages = totalPages;
		groupMembershipsCurrentPage = 0;
	};

	const searchGroupMemberships = async (searchStr) => {
		const res = await httpAdapter.get(`/group_membership?filter=${searchStr}`);
		createGroupMembershipList(res.data.content, res.data.totalPages);
	};

	const searchGroup = async (searchGroupStr) => {
		setTimeout(async () => {
			searchGroupResults = await httpAdapter.get(
				`/groups?page=0&size=${groupsDropdownSuggestion}&filter=${searchGroupStr}`
			);
		}, 1000);
	};

	const addGroupMembershipInput = async () => {
		addGroupMembershipVisible = true;
	};

	const addGroupMembership = async () => {
		await httpAdapter
			.post(`/group_membership`, {
				email: emailValue,
				permissionsGroup: selectedGroup,
				isGroupAdmin: selectedIsGroupAdmin,
				isTopicAdmin: selectedIsTopicAdmin,
				isApplicationAdmin: selectedIsTopicAdmin
			})
			.catch((err) => {
				if (err.response.status === 403) {
					errorMessage('Error Saving Group Membership', err.message);
				} else if (err.response.status === 400 || 401) {
					err.message = 'Group Membership already exists.';
					errorMessage('Error Adding Group Membership', err.message);
				}
			});
		reloadGroupMemberships(groupMembershipsCurrentPage);
		addGroupMembershipVisible = false;
	};

	const validateEmail = (input) => {
		input.match(validRegex) ? (invalidEmail = false) : (invalidEmail = true);
	};

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

	const updateGroupMembershipModal = (selectedGM) => {
		updateGroupMembershipVisible = true;
		selectedGroupMembership.userEmail = selectedGM.userEmail;
		selectedGroupMembership.groupName = selectedGM.groupName;
		selectedGroupMembership.groupAdmin = selectedGM.groupAdmin;
		selectedGroupMembership.applicationAdmin = selectedGM.applicationAdmin;
		selectedGroupMembership.topicAdmin = selectedGM.topicAdmin;
		selectedGroupMembership.groupMembershipId = selectedGM.groupMembershipId;
		selectedGroupMembership.groupId = selectedGM.groupId;
		selectedGroupMembership.userId = selectedGM.userId;
	};

	const updateGroupMembership = async () => {
		if (dataUpdated) {
			const data = {
				id: selectedGroupMembership.groupMembershipId,
				groupAdmin: selectedGroupMembership.groupAdmin,
				topicAdmin: selectedGroupMembership.topicAdmin,
				applicationAdmin: selectedGroupMembership.applicationAdmin,
				permissionsGroup: selectedGroupMembership.groupId,
				email: selectedGroupMembership.userEmail
			};
			try {
				await httpAdapter.put(`/group_membership`, data);
				reloadGroupMemberships(groupMembershipsCurrentPage);
			} catch (err) {
				errorMessage('Error Updating Group Membership', err.message);
			}
			dataUpdated = false;
		}
	};

	const deleteGroupMembershipModal = (selectedGM) => {
		deleteGroupMembershipVisible = true;
		selectedGroupMembership.groupMembershipId = selectedGM.groupMembershipId;
	};

	const deleteGroupMembership = async () => {
		try {
			await httpAdapter.delete(`/group_membership`, {
				data: { id: selectedGroupMembership.groupMembershipId }
			});

			reloadGroupMemberships(groupMembershipsCurrentPage);
		} catch (err) {
			errorMessage('Error Deleting Group Membership', err.message);
		}
		deleteGroupMembershipVisible = false;
	};

	const selectedSearchGroup = (groupName, groupId) => {
		selectedGroup = groupId;
		searchGroups = groupName;
		searchGroupsResultsVisible = false;
		searchGroupActive = false;
	};
</script>

<svelte:head>
	<title>Group Membership | DDS Permissions Manager</title>
	<meta name="description" content="Permissions Manager Group Membership" />
</svelte:head>

{#if $isAuthenticated}
	{#if errorMessageVisible}
		<Modal
			title={errorMsg}
			description={errorObject}
			on:cancel={() => {
				errorMessageVisible = false;
				errorMessageClear();
			}}
			><br /><br />
			<div class="confirm">
				<button
					class="button-delete"
					on:click={() => {
						errorMessageVisible = false;
						errorMessageClear();
					}}>Ok</button
				>
			</div>
		</Modal>
	{/if}

	{#if updateGroupMembershipVisible}
		<Modal
			title="Update Group Membership"
			on:cancel={() => {
				updateGroupMembershipVisible = false;
				updateGroupMembership();
			}}
		>
			<table align="center" style="margin-left: 0.6rem; width:97.5%">
				<tr style="border-width: 0px">
					<th style="font-size: 0.9rem">E-mail</th>
					<th style="font-size: 0.9rem">Group</th>
					<th style="font-size: 0.9rem"><center>Group Admin</center></th>
					<th style="font-size: 0.9rem"><center>Topic Admin</center></th>
					<th style="font-size: 0.9rem"><center>Application Admin</center></th>
				</tr>
				<tr>
					<td style="font-size: 0.9rem">
						{selectedGroupMembership.userEmail}
					</td>
					<td style="font-size: 0.9rem">
						{selectedGroupMembership.groupName}
					</td>
					<td>
						<center
							><input
								type="checkbox"
								id="updatedGroupAdmin"
								bind:checked={selectedGroupMembership.groupAdmin}
								on:click={() => (dataUpdated = true)}
							/></center
						>
					</td>
					<td>
						<center
							><input
								type="checkbox"
								id="updatedTopicAdmin"
								bind:checked={selectedGroupMembership.topicAdmin}
								on:click={() => (dataUpdated = true)}
							/></center
						>
					</td>
					<td>
						<center
							><input
								type="checkbox"
								id="updatedApplicationAdmin"
								bind:checked={selectedGroupMembership.applicationAdmin}
								on:click={() => (dataUpdated = true)}
							/></center
						>
					</td>
				</tr>
			</table>
		</Modal>
	{/if}

	{#if deleteGroupMembershipVisible}
		<Modal
			title="Delete Group Membership?"
			on:cancel={() => {
				deleteGroupMembershipVisible = false;
				deleteGroupMembership();
			}}
			><div class="confirm">
				<button class="button-cancel" on:click={() => (deleteGroupMembershipVisible = false)}
					>Cancel</button
				>&nbsp;
				<button
					class="button-delete"
					disabled={!$isAdmin && !isGroupAdmin}
					on:click={() => deleteGroupMembership()}><span>Delete</span></button
				>
			</div></Modal
		>
	{/if}

	<div class="content">
		<h1>Group Membership</h1>
		<center
			><input
				style="border-width: 1px; width: 20rem"
				placeholder="Search"
				bind:value={searchString}
				on:blur={() => {
					searchString = searchString?.trim();
				}}
				on:keydown={(event) => {
					const returnKey = 13;
					if (event.which === returnKey) {
						document.activeElement.blur();
						searchString = searchString?.trim();
					}
				}}
			/>&nbsp; &#x1F50E;</center
		>

		{#if $groupMembershipList && $groupMembershipList.length > 0}
			<br /><br />
			<table align="center">
				<tr style="border-width: 0px">
					<th>E-mail</th>
					<th>Group</th>
					<th><center>Group Admin</center></th>
					<th><center>Topic Admin</center></th>
					<th><center>Application Admin</center></th>
				</tr>
				{#each $groupMembershipList as groupMembership}
					<tr>
						<td>{groupMembership.userEmail}</td>
						<td>{groupMembership.groupName}</td>
						<td
							><center
								>{#if groupMembership.groupAdmin}&check;
								{:else}
									-
								{/if}</center
							></td
						>
						<td
							><center
								>{#if groupMembership.topicAdmin}&check;
								{:else}
									-
								{/if}</center
							></td
						>
						<td
							><center
								>{#if groupMembership.applicationAdmin}&check;
								{:else}
									-
								{/if}</center
							></td
						>

						{#if $isAdmin || groupAdminGroups.some((group) => group.groupName === groupMembership.groupName)}
							<td
								><div class="pencil" on:click={() => updateGroupMembershipModal(groupMembership)}>
									&#9998
								</div></td
							>
							<td
								><button
									class="button-delete"
									on:click={() => deleteGroupMembershipModal(groupMembership)}
									><span>Delete</span></button
								></td
							>
						{:else}
							<td /><td />
						{/if}
					</tr>
				{/each}
			</table>
		{:else}
			<p><center>No group memberships</center></p>
		{/if}
		<br />

		{#if $isAdmin || isGroupAdmin}
			<center
				><button
					class="button"
					style="cursor:pointer; width: 10.5rem; margin: 1rem 0 2rem 0"
					on:click={() => addGroupMembershipInput()}
					class:hidden={addGroupMembershipVisible}>Add Group Membership</button
				></center
			>
			<br />
		{/if}

		{#if addGroupMembershipVisible}
			<table>
				<tr style="border-width: 0px">
					<td style="width: 15rem"
						><input
							placeholder="Email Address"
							class:invalid={invalidEmail && emailValue.length >= 1}
							style="
						display: inline-flex;		
						height: 1.7rem;
						text-align: left;
						font-size: small;
						min-width: 13rem;"
							bind:value={emailValue}
							on:blur={() => validateEmail(emailValue)}
							on:keydown={(event) => {
								if (event.which === 13) {
									validateEmail(emailValue);
									document.querySelector('#name').blur();
								}
							}}
						/>
						<div class="add-item">
							<label for="groups">Group:</label>
							<input
								placeholder="Group Name"
								style="
									display: inline-flex;       
									height: 1.7rem;
									text-align: left;
									font-size: small;
									min-width: 9rem;"
								bind:value={searchGroups}
								on:blur={() => {
									setTimeout(() => {
										searchGroupsResultsVisible = false;
									}, 500);
								}}
								on:click={async () => {
									searchGroupResults = [];
									searchGroupActive = true;
									if (searchGroups?.length >= searchStringLength) {
										searchGroup(searchGroups);
									}
								}}
							/>

							{#if searchGroupsResultsVisible}
								<table class="searchGroup">
									{#each searchGroupResults.data.content as result}
										<tr
											><td
												on:click={() => {
													selectedSearchGroup(result.name, result.id);
												}}>{result.name}</td
											></tr
										>
									{/each}
								</table>
							{/if}
						</div>

						<div class="add-item">
							<input type="checkbox" name="groupAdmin" bind:checked={selectedIsGroupAdmin} />
							<label for="groupAdmin">Group Admin</label>
						</div>
						<div class="add-item">
							<input
								type="checkbox"
								name="applicationAdmin"
								bind:checked={selectedIsApplicationAdmin}
							/>
							<label for="applicationAdmin">Application Admin</label>
						</div>
						<div class="add-item">
							<input type="checkbox" name="topicAdmin" bind:checked={selectedIsTopicAdmin} />

							<label for="topicAdmin">Topic Admin</label>
						</div>
						<div class="add-item">
							<button
								class:button={!invalidEmail}
								style="margin-left: 1rem; width: 3.5rem"
								disabled={invalidEmail || searchGroups === ''}
								on:click={() => addGroupMembership()}><span>Add</span></button
							>
							<button
								class="remove-button"
								style="margin-left: 0.5rem"
								on:click={() => {
									emailValue = '';
									addGroupMembershipVisible = false;
								}}>x</button
							>
						</div>
					</td>
				</tr>
			</table>
			<br /><br />
		{/if}

		{#if $groupMembershipList}
			<center
				><button
					on:click={() => {
						if (groupMembershipsCurrentPage > 0) groupMembershipsCurrentPage--;
						reloadGroupMemberships(groupMembershipsCurrentPage);
					}}
					class="button-pagination"
					style="width: 4.8rem; border-bottom-left-radius:9px; border-top-left-radius:9px;"
					disabled={groupMembershipsCurrentPage === 0}>Previous</button
				>
				{#if groupMembershipsTotalPages > 2}
					{#each Array.apply( null, { length: groupMembershipsTotalPages } ).map(Number.call, Number) as page}
						<button
							on:click={() => {
								groupMembershipsCurrentPage = page;
								reloadGroupMemberships(page);
							}}
							class="button-pagination"
							class:button-pagination-selected={page === groupMembershipsCurrentPage}
							>{page + 1}</button
						>
					{/each}
				{/if}

				<button
					on:click={() => {
						if (groupMembershipsCurrentPage + 1 < groupMembershipsTotalPages)
							groupMembershipsCurrentPage++;
						reloadGroupMemberships(groupMembershipsCurrentPage);
					}}
					class="button-pagination"
					style="width: 3.1rem; border-bottom-right-radius:9px; border-top-right-radius:9px;"
					disabled={groupMembershipsCurrentPage === groupMembershipsTotalPages - 1 ||
						groupMembershipsTotalPages === 0}>Next</button
				></center
			>
		{/if}
	</div>
{:else}
	<center><h2>Please Log In to Continue...</h2></center>
{/if}

<style>
	.pencil {
		transform: scaleX(-1);
		margin-right: 0.5rem;
		margin-top: 0.2rem;
		cursor: pointer;
	}

	.add-item {
		display: inline-block;
		padding-left: 1.6rem;
	}

	.hidden {
		display: none;
	}

	.remove-button:hover {
		cursor: pointer;
		background-color: lightgray;
	}

	/* .searchGroup {
		font-size: 0.75rem;
		width: 10rem;
		cursor: pointer;
		list-style-type: none;
		margin-left: 2.8rem;
		margin-top: -0.1rem;
		padding-top: 0.1rem;
		padding-bottom: 0.2rem;
		text-align: left;
		background-color: rgba(217, 221, 254, 0.4);
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.26);
	}

	.searchGroup tr {
		height: 1.9rem;
	}

	.searchGroup tr:nth-child(even) {
		background-color: rgba(192, 196, 240, 0.4);
	} */

	label {
		font-size: small;
	}

	input {
		vertical-align: middle;
	}

	table {
		width: 100%;
	}

	tr {
		height: 2.5rem;
	}
</style>
