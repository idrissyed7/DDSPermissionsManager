<script>
	import { isAuthenticated } from '../stores/authentication';
	import { onMount } from 'svelte';
	import axios from 'axios';
	import groups from '../stores/groups';
	import groupDetails from '../stores/groupDetails';
	import Modal from '../lib/Modal.svelte';

	const URL_PREFIX = 'http://localhost:8080';

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
			const groupsData = await axios.get(`${URL_PREFIX}/groups`, { withCredentials: true });
			groups.set(groupsData.data.content);

			// Pagination
			let totalGroupsCount = 0;
			groupsPageIndex = Math.floor(groupsData.data.content.length / groupsPerPage);
			if (groupsData.data.content.length % groupsPerPage > 0) groupsPageIndex++;

			// Populate the usersPage Array
			let pageArray = [];
			for (let page = 0; page < groupsPageIndex; page++) {
				for (
					let i = 0;
					i < groupsPerPage && totalGroupsCount < groupsData.data.content.length;
					i++
				) {
					pageArray.push(groupsData.data.content[page * groupsPerPage + i]);
					totalGroupsCount++;
				}
				groupsPages.push(pageArray);
				pageArray = [];
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
			const res = await axios.get(`${URL_PREFIX}/groups/${groupId}`, {
				withCredentials: true
			});

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
		const res = await axios
			.post(
				`${URL_PREFIX}/groups/remove_member/${selectedGroupId}/${selectedUserId}`,
				{
					firstName: selectedUserFirstName,
					lastName: selectedUserLastName
				},
				{ withCredentials: true }
			)
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
			const res = await axios.get(`${URL_PREFIX}/groups/${selectedGroupId}`, {
				withCredentials: true
			});

			groupDetails.set(res.data);
		} catch (err) {
			ErrorMessage('Error Loading Group Details', err.message);
		}
	};

	const reloadAllGroups = async () => {
		try {
			const res = await axios.get(`${URL_PREFIX}/groups`, { withCredentials: true });
			groups.set(res.data.content);

			calculatePagination();
		} catch (err) {
			ErrorMessage('Error Loading Groups', err.message);
		}
	};

	const userCandidateAdd = async () => {
		const res = await axios
			.post(
				`${URL_PREFIX}/groups/add_member/${selectedGroupId}/${selectedUserId}`,
				{
					firstName: selectedUserFirstName,
					lastName: selectedUserLastName,
					email: selectedUserEmail
				},
				{ withCredentials: true }
			)
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
			const res = await axios
				.post(
					`${URL_PREFIX}/groups/save/`,
					{
						name: newGroupName
					},
					{ withCredentials: true }
				)
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
		await axios
			.post(
				`${URL_PREFIX}/groups/save/`,
				{
					id: selectedGroupId,
					name: selectedGroupName.trim()
				},
				{ withCredentials: true }
			)
			.catch((err) => {
				ErrorMessage('Error Editing Group Name', err.message);
			});

		reloadAllGroups();
	};

	const deleteGroup = async () => {
		confirmDeleteVisible = false;

		const res = await axios
			.post(
				`${URL_PREFIX}/groups/delete/${selectedGroupId}`,
				{
					id: selectedGroupId
				},
				{ withCredentials: true }
			)
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
				>
				<button class="button-delete" on:click={() => deleteGroup()}><span>Delete</span></button>
			</div>
		</Modal>
	{/if}

	{#if addGroupVisible && !errorMessageVisible}
		<Modal title="Add New Group" on:cancel={() => (addGroupVisible = false)}>
			<div class="confirm">
				<input
					type="text"
					placeholder="Group Name"
					class="input-add-new-group"
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
				<center><span class="group-create-error">Please choose a unique name</span></center>
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
			<table>
				<tr>
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
			<center> <button class="button" on:click={() => addGroupModal()}>Add Group </button></center>
		{/if}
		{#if $groupDetails && groupDetailVisible && !groupsListVisible}
			<div class="group-name">
				<span on:click={() => returnToGroupsList()}>&laquo;</span>
				<div class="tooltip">
					<input
						id="group-name"
						on:click={() => (editGroupName = true)}
						on:blur={() => saveNewGroupName()}
						on:keydown={(event) => {
							if (event.which === 13) {
								saveNewGroupName();
								document.querySelector('#group-name').blur();
							}
						}}
						bind:value={selectedGroupName}
						readonly={!editGroupName}
						class:group-name-as-label={!editGroupName}
					/>
					<span class="tooltiptext">&#9998</span>
				</div>
			</div>
			<h2>Group Members</h2>
			<table>
				<tr>
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
			<table>
				<tr>
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
					class="button-delete"
					style="width: 7.5rem"
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
	.group-name-as-label {
		border: none;
		font-size: 38px;
		text-align: center;
		background-color: rgba(0, 0, 0, 0);
	}

	.group-name-as-label:hover {
		color: rgb(103, 103, 103);
	}

	.group-create-error {
		color: red;
		text-align: center;
		position: absolute;
		justify-content: center;
		bottom: 5%;
		left: 0;
		right: 0;
		margin-left: auto;
		margin-right: auto;
		width: 100%;
	}

	.group-name {
		display: flex;
		justify-content: center;
		align-items: center;
	}

	.group-name span {
		position: relative;
		left: -20px;
		font-size: 15pt;
		cursor: pointer;
	}

	.group-name span:hover {
		color: grey;
	}

	.button-pagination:disabled:hover {
		background-color: rgba(239, 239, 239, 0.3);
	}

	.button-pagination:disabled {
		color: rgba(16, 16, 16, 0.3);
	}

	.button-pagination-selected {
		background-color: rgb(217, 217, 217);
	}

	.button-pagination:hover {
		background-color: rgb(217, 217, 217);
	}

	.input-add-new-group {
		font-size: 18px;
		padding-bottom: 0.3rem;
	}

	button {
		margin-left: auto;
	}

	.group-td {
		cursor: pointer;
	}

	tr {
		line-height: 1.7rem;
		display: flex;
		align-items: center;
	}

	tr:nth-child(even) {
		filter: drop-shadow(-1px -1px 3px rgb(0 0 0 / 0.15));
	}

	input {
		font-size: 38px;
		text-align: center;
		width: 20rem;
		z-index: 1;
	}
</style>
