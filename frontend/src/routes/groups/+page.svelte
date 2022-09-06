<script>
	import { isAdmin, isAuthenticated } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import groups from '../../stores/groups';
	import groupDetails from '../../stores/groupDetails';
	import Modal from '../../lib/Modal.svelte';

	// Error Handling
	let errorMsg, errorObject;

	// SearchBox
	let searchString;
	let searchResults;
	let searchResultsVisible = false;

	// Modals
	let errorMessageVisible = false;
	let groupsListVisible = true;
	let groupDetailVisible = false;
	let addGroupVisible = false;
	let confirmRemoveUserVisible = false;
	let confirmAddUserVisible = false;
	let confirmDeleteVisible = false;

	// Validation
	let disabled = false;
	let previousGroupName;

	// Group Name
	let newGroupName;
	let editGroupName;

	// Selection
	let selectedUserFirstName;
	let selectedUserLastName;
	let selectedUserEmail;
	let selectedUserId;
	let selectedGroupId;
	let selectedGroupName;

	// Pagination
	const groupsPerPage = 3;
	let groupsTotalPages;
	let groupsCurrentPage = 0;

	// Check the Add Group has more than 0 non-whitespace characters
	$: newGroupName?.length === 0 ? (disabled = true) : (disabled = false);

	// Search Box
	$: if (searchString?.trim().length >= 3) {
		searchGroups(searchString.trim());
	} else {
		searchResultsVisible = false;
	}

	$: if (searchResults?.data?.length >= 1) {
		searchResultsVisible = true;
	} else {
		searchResultsVisible = false;
	}

	onMount(async () => {
		try {
			reloadAllGroups();
		} catch (err) {
			errorMessage('Error Loading Groups', err.message);
		}
	});

	const searchGroups = async (searchStr) => {
		searchResults = await httpAdapter.get(`/groups/search/${searchStr}`);
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

	const loadGroup = async (groupId) => {
		groupsListVisible = false;
		groupDetailVisible = true;
		try {
			const res = await httpAdapter.get(`/groups/${groupId}`);
			groupDetails.set(res.data);
			selectedGroupId = $groupDetails.group.id;
			selectedGroupName = $groupDetails.group.name;
		} catch (err) {
			errorMessage('Error Loading Group Details', err.message);
		}
	};

	const confirmUserMemberRemove = (ID, firstName, lastName, groupId) => {
		confirmRemoveUserVisible = true;

		selectedUserFirstName = firstName;
		selectedUserLastName = lastName;
		selectedUserId = ID;
	};

	const userMemberRemove = async () => {
		confirmRemoveUserVisible = false;
		const res = await httpAdapter
			.post(`/groups/remove_member/${selectedGroupId}/${selectedUserId}`, {
				firstName: selectedUserFirstName,
				lastName: selectedUserLastName
			})
			.catch((err) => {
				errorMessage('Error Removing Group Member', err.message);
			});

		selectedUserId = '';
		selectedUserFirstName = '';
		selectedUserLastName = '';

		reloadGroupDetails();
	};

	const reloadGroupDetails = async () => {
		try {
			const res = await httpAdapter.get(`/groups/${selectedGroupId}`);
			groupDetails.set(res.data);
		} catch (err) {
			errorMessage('Error Loading Group Details', err.message);
		}
	};

	const reloadAllGroups = async (page = 0) => {
		try {
			const res = await httpAdapter.get(`/groups?page=${page}&size=${groupsPerPage}`);
			groups.set(res.data.content);
			groupsTotalPages = res.data.totalPages;
		} catch (err) {
			errorMessage('Error Loading Groups', err.message);
		}
	};

	const userCandidateAdd = async () => {
		const res = await httpAdapter
			.post(`/groups/add_member/${selectedGroupId}/${selectedUserId}`, {
				isGroupAdmin: true,
				isApplicationAdmin: true,
				isTopicAdmin: true
			})
			.catch((err) => {
				errorMessage('Error Adding Candidate Member', err.message);
			});
		confirmAddUserVisible = false;

		reloadGroupDetails();
	};

	const confirmUserCandidateAdd = (ID, firstName, lastName, email) => {
		confirmAddUserVisible = true;

		selectedUserFirstName = firstName;
		selectedUserLastName = lastName;
		selectedUserId = ID;
		selectedUserEmail = email;
	};

	const returnToGroupsList = () => {
		groupDetailVisible = false;
		groupsListVisible = true;
	};

	const addGroupModal = () => {
		newGroupName = '';
		addGroupVisible = true;
	};

	const addGroup = async () => {
		const res = await httpAdapter
			.post(`/groups/save/`, {
				name: newGroupName
			})
			.catch((err) => {
				if (err.response.status === 303) {
					err.message = 'Group already exists. Please choose a unique group name.';
				}
				addGroupVisible = false;
				errorMessage('Error Adding Group', err.message);
			});
		searchString = '';
		addGroupVisible = false;

		await reloadAllGroups();
	};

	const saveNewGroupName = async () => {
		editGroupName = false;
		await httpAdapter
			.post(`/groups/save/`, {
				id: selectedGroupId,
				name: selectedGroupName
			})
			.catch((err) => {
				errorMessage('Error Editing Group Name', err.message);
			});

		reloadAllGroups();
	};

	const deleteGroup = async () => {
		confirmDeleteVisible = false;

		const res = await httpAdapter
			.post(`/groups/delete/${selectedGroupId}`, {
				id: selectedGroupId
			})
			.catch((err) => {
				errorMessage('Error Deleting Group', err.message);
			});

		returnToGroupsList();
		await reloadAllGroups();
	};

	const callFocus = (input) => {
		input.focus();
	};
</script>

<svelte:head>
	<title>Groups | Permission Manager</title>
	<meta name="description" content="Permission Manager Groups" />
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

	{#if confirmDeleteVisible && !errorMessageVisible}
		<Modal
			title="Delete Group {selectedGroupName}?"
			on:cancel={() => (confirmDeleteVisible = false)}
		>
			<div class="confirm">
				<button class="button-cancel" on:click={() => (confirmDeleteVisible = false)}>Cancel</button
				>&nbsp;
				<button class="button-delete" disabled={!$isAdmin} on:click={() => deleteGroup()}
					><span>Delete</span></button
				>
			</div>
		</Modal>
	{/if}

	{#if addGroupVisible && !errorMessageVisible}
		<Modal title="Add New Group" on:cancel={() => (addGroupVisible = false)}>
			<div class="confirm">
				<input
					type="text"
					placeholder="Group Name"
					bind:value={newGroupName}
					on:blur={() => (newGroupName = newGroupName.trim())}
					use:callFocus
					style="border-width: 1px; vertical-align: middle"
				/>
				<button
					class:button={!disabled}
					class:button-disabled={disabled}
					style="margin-left: 1rem; width: 6.5rem; height: 2rem;"
					{disabled}
					on:click={() => addGroup()}><span>Add Group</span></button
				>
			</div>
		</Modal>
	{/if}

	{#if confirmRemoveUserVisible && !errorMessageVisible}
		<Modal
			title="Remove {selectedUserFirstName} {selectedUserLastName} from {selectedGroupName}?"
			on:cancel={() => (confirmRemoveUserVisible = false)}
		>
			<div class="confirm">
				<button class="button-cancel" on:click={() => (confirmRemoveUserVisible = false)}
					>Cancel</button
				>
				<button class="button-delete" style="width: 5.5rem;" on:click={() => userMemberRemove()}
					><span>Remove</span></button
				>
			</div>
		</Modal>
	{/if}

	{#if confirmAddUserVisible && !errorMessageVisible}
		<Modal
			title="Add {selectedUserFirstName} {selectedUserLastName} to {selectedGroupName}?"
			on:cancel={() => (confirmAddUserVisible = false)}
		>
			<div class="confirm">
				<button class="button-cancel" on:click={() => (confirmAddUserVisible = false)}
					>Cancel</button
				>
				<button class="button" style="width: 4rem;" on:click={() => userCandidateAdd()}
					><span>Add</span></button
				>
			</div>
		</Modal>
	{/if}

	<div class="content">
		{#if $groups && groupsListVisible && !groupDetailVisible}
			<h1>Groups</h1>
			<center
				><input
					style="border-width: 1px;"
					placeholder="Search"
					bind:value={searchString}
					on:blur={() => {
						searchString = searchString?.trim();
					}}
					on:keydown={(event) => {
						if (event.which === 13) {
							document.activeElement.blur();
							searchString = searchString?.trim();
						}
					}}
				/>&nbsp; &#x1F50E;</center
			>
			{#if searchResultsVisible}
				<center
					><ul class="search-results">
						{#each searchResults.data as result}
							<li style="padding-left: 0.3rem">{result.name}</li>
						{/each}
					</ul></center
				>
			{/if}
			<table align="center">
				<tr style="border-width: 0px">
					<th><strong>Group</strong></th>
				</tr>
				{#if $groups.length > 0}
					{#each $groups as group}
						<tr>
							<td
								class="group-td"
								on:click={() => {
									loadGroup(group.id);
									selectedGroupId = group.id;
								}}>{group.name}</td
							>
						</tr>
					{/each}
				{:else}
					<tr><td>No Groups Found</td></tr>
				{/if}
			</table>
			<br /> <br />
			{#if $groups}
				<center
					><button
						on:click={async () => {
							if (groupsCurrentPage > 0) groupsCurrentPage--;
							reloadAllGroups(groupsCurrentPage);
						}}
						class="button-pagination"
						style="width: 4.8rem; border-bottom-left-radius:9px; border-top-left-radius:9px;"
						disabled={groupsCurrentPage === 0}>Previous</button
					>
					{#if groupsTotalPages > 2}
						{#each Array.apply(null, { length: groupsTotalPages }).map(Number.call, Number) as page}
							<button
								on:click={() => {
									groupsCurrentPage = page;
									reloadAllGroups(page);
								}}
								class="button-pagination"
								class:button-pagination-selected={page === groupsCurrentPage}>{page + 1}</button
							>
						{/each}
					{/if}
					<button
						on:click={async () => {
							if (groupsCurrentPage + 1 < groupsTotalPages) groupsCurrentPage++;
							reloadAllGroups(groupsCurrentPage);
						}}
						class="button-pagination"
						style="width: 3.1rem; border-bottom-right-radius:9px; border-top-right-radius:9px;"
						disabled={groupsCurrentPage === groupsTotalPages - 1 || groupsTotalPages === 0}
						>Next</button
					></center
				>
			{/if}
			<br /><br />
			<center>
				<button class:hidden={!$isAdmin} class="button" on:click={() => addGroupModal()}
					>Add Group
				</button></center
			>
		{/if}
		{#if $groupDetails && groupDetailVisible && !groupsListVisible}
			<div class="name">
				<span on:click={() => returnToGroupsList()}>&laquo;</span>
				<div class="tooltip">
					<input
						id="name"
						on:click={() => {
							if ($isAdmin) {
								editGroupName = true;
								previousGroupName = selectedGroupName.trim();
							}
						}}
						on:blur={() => {
							if ($isAdmin) {
								selectedGroupName = selectedGroupName.trim();
								if (previousGroupName !== selectedGroupName) saveNewGroupName();
								editGroupName = false;
							}
						}}
						on:keydown={(event) => {
							if (event.which === 13) {
								if ($isAdmin) {
									selectedGroupName = selectedGroupName.trim();
									if (previousGroupName !== selectedGroupName) saveNewGroupName();
									document.querySelector('#name').blur();
								}
							}
						}}
						bind:value={selectedGroupName}
						readonly={!editGroupName}
						class:name-as-label={!editGroupName}
						class:name-edit={editGroupName}
					/>
					{#if $isAdmin}
						<span class="tooltiptext">&#9998</span>
					{/if}
				</div>
			</div>
			<h2>Group Members</h2>
			<table align="center">
				<tr style="border-width: 0px">
					<th><strong>Member Name</strong></th>
				</tr>
				{#if $groupDetails.group.users}
					{#each $groupDetails.group.users as userMember}
						<tr>
							<td
								>{userMember.firstName}
								{userMember.lastName}
							</td>
							<button
								class="button-delete"
								style="width: 5.5rem;"
								on:click={() =>
									confirmUserMemberRemove(userMember.id, userMember.firstName, userMember.lastName)}
								><span>Remove</span></button
							>
						</tr>
					{/each}
				{:else}
					<tr><td>No Members Found</td></tr>
				{/if}
			</table>
			<br />
			<h2>Candidate Members</h2>
			<table align="center">
				<tr style="border-width: 0px">
					<th><strong>Member Name</strong></th>
				</tr>
				{#if $groupDetails.candidateUsers}
					{#each $groupDetails.candidateUsers as userCandidate}
						<tr>
							<td
								>{userCandidate.firstName}
								{userCandidate.lastName}
							</td>
							<button
								class="button"
								on:click={() =>
									confirmUserCandidateAdd(
										userCandidate.id,
										userCandidate.firstName,
										userCandidate.lastName,
										userCandidate.email
									)}><span>Add User</span></button
							>
						</tr>
					{/each}
				{:else}
					<tr><td>No Members Found</td></tr>
				{/if}
			</table>
			<br /><br />
			<center>
				<button
					class:hidden={!$isAdmin}
					class="button-delete"
					style="width: 7.5rem; float: none"
					on:click={() => (confirmDeleteVisible = true)}
					><span>Delete Group</span>
				</button></center
			>
		{/if}
	</div>
{:else}
	<center><h2>Please Log In to Continue...</h2></center>
{/if}

<style>
	.group-td {
		cursor: pointer;
	}

	button {
		margin-left: auto;
	}

	tr {
		line-height: 1.7rem;
		align-items: center;
	}

	input {
		text-align: center;
		width: 20rem;
		z-index: 1;
		background-color: rgba(0, 0, 0, 0);
	}

	ul {
		cursor: pointer;
		list-style-type: none;
		margin: 0;
		padding-top: 0.1rem;
		padding-bottom: 0.2rem;
		background-color: rgba(217, 221, 254, 0.4);
		text-align: left;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.26);
	}

	li {
		margin-left: -2rem;
		padding: 0.2rem 0 0 0;
	}
</style>
