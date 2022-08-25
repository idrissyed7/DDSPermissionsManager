<script>
	import { isAdmin, isAuthenticated } from '../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../appconfig';
	import groups from '../stores/groups';
	import groupDetails from '../stores/groupDetails';
	import Modal from '../lib/Modal.svelte';

	// Error Handling
	let errorMessage, errorObject;

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
	const groupsPerPage = 5;
	let groupsPageIndex;
	let groupsPages = [];
	let currentPage = 0;

	// Checks if the group already exists
	$: if ($groups) {
		if ($groups.some((group) => group.name === newGroupName)) {
			disabled = true;
		} else {
			disabled = false;
		}
	}

	onMount(async () => {
		try {
			const groupsData = await httpAdapter.get(`/groups`);
			groups.set(groupsData.data.content);
			console.log($groups);

			if ($groups) {
				// Pagination
				let totalGroupsCount = 0;
				groupsPageIndex = Math.floor(groupsData.data.content.length / groupsPerPage);
				if (groupsData.data.content.length % groupsPerPage > 0) groupsPageIndex++;

				// Populate the usersPage Array
				for (let page = 0; page < groupsPageIndex; page++) {
					let pageArray = [];
					for (
						let i = 0;
						i < groupsPerPage && totalGroupsCount < groupsData.data.content.length;
						i++
					) {
						pageArray.push(groupsData.data.content[page * groupsPerPage + i]);
						totalGroupsCount++;
					}
					groupsPages.push(pageArray);
				}
			}
		} catch (err) {
			ErrorMessage('Error Loading Groups', err.message);
		}
	});

	const ErrorMessage = (errMsg, errObj) => {
		errorMessage = errMsg;
		errorObject = errObj;
		errorMessageVisible = true;
	};

	const ErrorMessageClear = () => {
		errorMessage = '';
		errorObject = '';
		errorMessageVisible = false;
	};

	const calculatePagination = () => {
		groupsPages = [];
		let totalGroupsCount = 0;
		groupsPageIndex = Math.floor($groups.length / groupsPerPage);
		if ($groups.length % groupsPerPage > 0) groupsPageIndex++;

		if (groupsPageIndex === currentPage) currentPage--;

		// Populate the usersPage Array
		let pageArray = [];
		for (let page = 0; page < groupsPageIndex; page++) {
			for (let i = 0; i < groupsPerPage && totalGroupsCount < $groups.length; i++) {
				pageArray.push($groups[page * groupsPerPage + i]);
				totalGroupsCount++;
			}
			groupsPages.push(pageArray);
			pageArray = [];
		}
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
			ErrorMessage('Error Loading Group Details', err.message);
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
				ErrorMessage('Error Removing Group Member', err.message);
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
			ErrorMessage('Error Loading Group Details', err.message);
		}
	};

	const reloadAllGroups = async () => {
		try {
			const res = await httpAdapter.get(`/groups`);
			groups.set(res.data.content);

			calculatePagination();
		} catch (err) {
			ErrorMessage('Error Loading Groups', err.message);
		}
	};

	const userCandidateAdd = async () => {
		// work on this
		const res = await httpAdapter
			.post(`/groups/add_member/${selectedGroupId}/${selectedUserId}`, {
				isGroupAdmin: true,
				isApplicationAdmin: true,
				isTopicAdmin: true
			})
			.catch((err) => {
				ErrorMessage('Error Adding Candidate Member', err.message);
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
		if (!disabled) {
			await httpAdapter
				.post(`/groups/save/`, {
					name: newGroupName
				})
				.catch((err) => {
					addGroupVisible = false;
					ErrorMessage('Error Adding Group', err.message);
				});

			addGroupVisible = false;

			await reloadAllGroups().then(() => {
				currentPage = groupsPages.length - 1;
			});
		}
	};

	const saveNewGroupName = async () => {
		editGroupName = false;
		await httpAdapter
			.post(`/groups/save/`, {
				id: selectedGroupId,
				name: selectedGroupName
			})
			.catch((err) => {
				ErrorMessage('Error Editing Group Name', err.message);
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
				ErrorMessage('Error Deleting Group', err.message);
			});

		returnToGroupsList();
		await reloadAllGroups().then(() => {
			if (currentPage === groupsPages.length) currentPage--;
		});
	};
</script>

<svelte:head>
	<title>Groups | Permission Manager</title>
	<meta name="description" content="Permission Manager Groups" />
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
					class="input-add-new"
					bind:value={newGroupName}
				/>
				<button
					class:button={!disabled}
					style="margin-left: 1rem; width: 6.5rem"
					{disabled}
					on:click={() => addGroup()}><span>Add Group</span></button
				>
			</div>
			{#if disabled}
				<br />
				<center><span class="create-error">Please choose a unique name</span></center>
			{/if}
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
		{#if groupsPages && groupsListVisible && !groupDetailVisible}
			<h1>Groups</h1>
			<table align="center">
				<tr style="border-width: 0px">
					<th><strong>Group</strong></th>
				</tr>
				{#if groupsPages.length > 0}
					{#each groupsPages[currentPage] as group}
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
						on:click={() => {
							if (currentPage > 0) currentPage--;
						}}
						class="button-pagination"
						style="width: 4.8rem; border-bottom-left-radius:9px; border-top-left-radius:9px;"
						disabled={currentPage === 0}>Previous</button
					>
					{#if groupsPageIndex > 2}
						{#each groupsPages as page, i}
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
							if (currentPage < groupsPages.length) currentPage++;
						}}
						class="button-pagination"
						style="width: 3.1rem; border-bottom-right-radius:9px; border-top-right-radius:9px;"
						disabled={currentPage === groupsPages.length - 1 || groupsPages.length === 0}
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
		height: 2.1rem;
		z-index: 1;
		font-size: 30px;
		border-style: hidden;
		background-color: rgba(0, 0, 0, 0);
		vertical-align: middle;
	}
</style>
