<script>
	import { isAuthenticated } from '../stores/authentication';
	import { onMount } from 'svelte';
	import axios from 'axios';
	import groups from '../stores/groups';
	import groupDetails from '../stores/groupDetails';
	import Modal from '../lib/Modal.svelte';

	let groupsListVisible = true;
	let groupDetailVisible = false;
	let confirmDeleteVisible = false;
	let confirmAddUserVisible = false;
	let selectedUserFirstName;
	let selectedUserLastName;
	let selectedUserEmail;
	let selectedUserId;
	let selectedGroupId;

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

		reloadGroups();
	};

	const reloadGroups = async () => {
		try {
			const res = await axios.get(`http://localhost:8080/groups/${selectedGroupId}`, {
				withCredentials: true
			});

			groupDetails.set(res.data);
		} catch (err) {
			console.error('Error loading Group Details', err);
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

		reloadGroups();
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
		// reloadGroups();
	};
</script>

<svelte:head>
	<title>Groups | Permission Manager</title>
	<meta name="description" content="Permission Manager Groups" />
</svelte:head>

{#if $isAuthenticated}
	<div class="content">
		{#if confirmDeleteVisible && confirmAddUserVisible !== true}
			<Modal
				title="Remove {selectedUserFirstName} {selectedUserLastName} from {$groupDetails.group
					.name}?"
				on:cancel={() => (confirmDeleteVisible = false)}
			>
				<div class="confirm">
					<button class="button-cancel" on:click={() => (confirmDeleteVisible = false)}
						>Cancel</button
					>
					<button class="button-delete" on:click={() => userMemberRemove()}>Remove</button>
				</div>
			</Modal>
		{/if}
		{#if confirmAddUserVisible && confirmDeleteVisible !== true}
			<Modal
				title="Add {selectedUserFirstName} {selectedUserLastName} to {$groupDetails.group.name}?"
				on:cancel={() => (confirmAddUserVisible = false)}
			>
				<div class="confirm">
					<button class="button-cancel" on:click={() => (confirmAddUserVisible = false)}
						>Cancel</button
					>
					<button class="button" on:click={() => userCandidateAdd()}>Add</button>
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
		width: 7rem;
		position: relative;
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
			to right,
			rgba(255, 255, 255, 1),
			rgba(255, 255, 255, 0.7),
			rgba(255, 255, 255, 0)
		);
	}

	h2 {
		text-align: center;
	}
</style>
