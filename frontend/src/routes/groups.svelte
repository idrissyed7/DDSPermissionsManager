<script>
	import { isAuthenticated } from '../stores/authentication';
	import { onMount } from 'svelte';
	import axios from 'axios';
	import groups from '../stores/groups';
	import groupDetails from '../stores/groupDetails';
	import Modal from '../lib/Modal.svelte';

	let groupsListVisible = true;
	let groupDetailVisible = false;
	let addGroupVisible = false;
	let disabled = false;
	let confirmDeleteVisible = false;
	let confirmAddUserVisible = false;
	let groupNameExists = false;
	let newGroupName;
	let selectedUserFirstName;
	let selectedUserLastName;
	let selectedUserEmail;
	let selectedUserId;
	let selectedGroupId;

	$: if ($groups) {
		if ($groups.filter((group) => group.name === newGroupName).length > 0) {
			disabled = true;
		} else {
			disabled = false;
		}
	}

	onMount(async () => {
		try {
			const res = await axios.get('http://localhost:8080/groups', { withCredentials: true });
			groups.set(res.data.content);
		} catch (err) {
			console.error('Error loading Groups');
		}
	});

	const loadGroup = async (groupId) => {
		groupsListVisible = false;
		groupDetailVisible = true;
		try {
			const res = await axios.get(`http://localhost:8080/groups/${groupId}`, {
				withCredentials: true
			});

			groupDetails.set(res.data);
			selectedGroupId = $groupDetails.group.id;
		} catch (err) {
			console.error('Error loading Group Details', err);
		}
	};

	const confirmUserMemberRemove = (ID, firstName, lastName, groupId) => {
		confirmDeleteVisible = true;

		selectedUserFirstName = firstName;
		selectedUserLastName = lastName;
		selectedUserId = ID;
	};

	const userMemberRemove = async () => {
		confirmDeleteVisible = false;
		const res = await axios
			.post(
				`http://localhost:8080/groups/remove_member/${selectedGroupId}/${selectedUserId}`,
				{
					firstName: selectedUserFirstName,
					lastName: selectedUserLastName
				},
				{ withCredentials: true }
			)
			.catch((err) => console.error(err));

		selectedUserId = '';
		selectedUserFirstName = '';
		selectedUserLastName = '';

		reloadGroupDetails();
	};

	const reloadGroupDetails = async () => {
		try {
			const res = await axios.get(`http://localhost:8080/groups/${selectedGroupId}`, {
				withCredentials: true
			});

			groupDetails.set(res.data);
		} catch (err) {
			console.error('Error loading Group Details', err);
		}
	};

	const reloadAllGroups = async () => {
		try {
			const res = await axios.get('http://localhost:8080/groups', { withCredentials: true });
			groups.set(res.data.content);
		} catch (err) {
			console.error('Error loading Groups');
		}
	};

	const userCandidateAdd = async () => {
		const res = await axios
			.post(
				`http://localhost:8080/groups/add_member/${selectedGroupId}/${selectedUserId}`,
				{
					firstName: selectedUserFirstName,
					lastName: selectedUserLastName,
					email: selectedUserEmail
				},
				{ withCredentials: true }
			)
			.catch((err) => console.error(err));
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
					`http://localhost:8080/groups/save/`,
					{
						name: newGroupName
					},
					{ withCredentials: true }
				)
				.catch((err) => console.error(err));

			addGroupVisible = false;

			reloadAllGroups();
		}
	};
</script>

<svelte:head>
	<title>Groups | Permission Manager</title>
	<meta name="description" content="Permission Manager Groups" />
</svelte:head>

{#if $isAuthenticated}
	<div class="content">
		{#if addGroupVisible && confirmDeleteVisible !== true && confirmAddUserVisible !== true}
			<Modal title="Add New Group" on:cancel={() => (addGroupVisible = false)}>
				<div class="add-user">
					<input type="text" placeholder="Group Name" bind:value={newGroupName} />
					<button
						class:button={!disabled}
						style="margin-left: 1rem;"
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
		{#if confirmDeleteVisible && confirmAddUserVisible !== true && addGroupVisible !== true}
			<Modal
				title="Remove {selectedUserFirstName} {selectedUserLastName} from {$groupDetails.group
					.name}?"
				on:cancel={() => (confirmDeleteVisible = false)}
			>
				<div class="confirm">
					<button class="button-cancel" on:click={() => (confirmDeleteVisible = false)}
						>Cancel</button
					>
					<button class="button-delete" on:click={() => userMemberRemove()}
						><span>Remove</span></button
					>
				</div>
			</Modal>
		{/if}
		{#if confirmAddUserVisible && confirmDeleteVisible !== true && addGroupVisible !== true}
			<Modal
				title="Add {selectedUserFirstName} {selectedUserLastName} to {$groupDetails.group.name}?"
				on:cancel={() => (confirmAddUserVisible = false)}
			>
				<div class="confirm">
					<button class="button-cancel" on:click={() => (confirmAddUserVisible = false)}
						>Cancel</button
					>
					<button class="button" on:click={() => userCandidateAdd()}><span>Add</span></button>
				</div>
			</Modal>
		{/if}
		{#if groupsListVisible && !groupDetailVisible}
			<h1>Groups</h1>
			<table>
				<tr>
					<th><strong>Group</strong></th>
				</tr>
				{#if $groups}
					{#each $groups as group}
						<tr>
							<td
								on:click={() => {
									loadGroup(group.id);
									selectedGroupId = group.id;
								}}>{group.name}</td
							>
						</tr>
					{/each}
				{/if}
			</table>
			<br /><br />
			<center> <button class="button" on:click={() => addGroupModal()}>Add Group </button></center>
		{/if}
		{#if $groupDetails && groupDetailVisible && !groupsListVisible}
			<div class="group-name">
				<span on:click={() => returnToGroupsList()}>&laquo;</span>
				<h1>{$groupDetails.group.name} Group</h1>
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
								on:click={() =>
									confirmUserMemberRemove(userMember.id, userMember.firstName, userMember.lastName)}
								><span>Delete</span></button
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
		{/if}
	</div>
{:else}
	<center><h2>Please Log In to Continue...</h2></center>
{/if}

<style>
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

	.content {
		width: 100%;
		max-width: var(--column-width);
		margin: var(--column-margin-top) auto 0 auto;
	}

	.button {
		display: inline-block;
		justify-content: center;
		border-radius: 4px;
		background-color: rgb(54, 70, 255);
		border: none;
		color: #ffffff;
		text-align: center;
		font-size: 14px;
		padding-left: 5x;
		height: 2rem;
		transition: all 0.5s;
		cursor: pointer;
		left: 0%;
		min-width: 7rem;
	}

	.button:hover {
		background-color: rgb(44, 58, 209);
	}

	.button span {
		cursor: pointer;
		display: inline-block;
		position: relative;
		transition: 0.5s;
	}

	.button span:after {
		content: '\00ab';
		position: absolute;
		opacity: 0;
		top: 0;
		right: -20px;
		transition: 0.5s;
	}

	.button:hover span {
		padding-right: 20px;
	}

	.button:hover span:after {
		opacity: 1;
		right: 0;
	}

	.button-delete {
		display: inline-block;
		border-radius: 4px;
		background-color: red;
		border: none;
		color: #ffffff;
		text-align: center;
		font-size: 14px;
		height: 2rem;
		top: 90%;
		transition: all 0.5s;
		cursor: pointer;
	}

	.button-delete:hover {
		background-color: rgb(223, 1, 1);
	}

	.button-delete span {
		cursor: pointer;
		display: inline-block;
		position: relative;
		transition: 0.5s;
	}

	.button-delete span:after {
		content: '\00bb';
		position: absolute;
		opacity: 0;
		top: 0;
		left: -20px;
		transition: 0.5s;
	}

	.button-delete:hover span {
		padding-left: 20px;
	}

	.button-delete:hover span:after {
		opacity: 1;
		left: 0;
	}

	.button-cancel {
		display: inline-block;
		border-radius: 4px;
		background-color: rgb(128, 128, 128);
		border: none;
		color: #ffffff;
		text-align: center;
		font-size: 14px;
		height: 2rem;
		top: 90%;
		transition: all 0.5s;
		cursor: pointer;
	}

	.button-cancel:hover {
		background-color: rgb(110, 110, 110);
	}

	.confirm {
		display: inline-block;
	}

	button {
		display: inline-block;
		justify-content: center;
		border-radius: 4px;
		background-color: rgb(123, 123, 124);
		border: none;
		color: #ffffff;
		text-align: center;
		font-size: 14px;
		padding-left: 5x;
		height: 2rem;
		transition: all 0.5s;
		cursor: pointer;
		left: 0%;
		min-width: 7rem;
		margin-left: auto;
	}

	table {
		margin-left: auto;
		margin-right: auto;
		border-collapse: collapse;
		width: 70%;
	}

	th {
		font-size: 13.5pt;
		padding-left: 1rem;
		padding-bottom: 0.3rem;
		text-align: left;
	}

	td {
		font-size: 13pt;
		padding: 0.3rem 1rem 0.3rem 1rem;
		cursor: pointer;
	}

	tr {
		line-height: 1.7rem;
		display: flex;
		align-items: center;
	}

	tr:nth-child(even) {
		background-image: linear-gradient(
			60deg,
			rgba(255, 255, 255, 0),
			rgba(255, 255, 255, 1),
			rgba(255, 255, 255, 0.5),
			rgba(255, 255, 255, 0),
			rgba(255, 255, 255, 0)
		);
		filter: drop-shadow(-1px -1px 3px rgb(0 0 0 / 0.15));
	}

	h2 {
		text-align: center;
	}
</style>
