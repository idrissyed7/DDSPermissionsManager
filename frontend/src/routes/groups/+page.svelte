<script>
	import { isAdmin, isAuthenticated } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import groups from '../../stores/groups';
	import Modal from '../../lib/Modal.svelte';

	export let data, errors;

	// Error Handling
	let errorMsg, errorObject;

	// Search
	let searchString;
	const searchStringLength = 3;
	let timer;
	const waitTime = 500;

	// Modals
	let errorMessageVisible = false;
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
	const groupsPerPage = 10;
	let groupsTotalPages;
	let groupsCurrentPage = 0;

	// Check the Add Group has more than 0 non-whitespace characters
	$: newGroupName?.length === 0 ? (disabled = true) : (disabled = false);

	// Search Feature
	$: if (searchString?.trim().length >= searchStringLength) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchGroups(searchString.trim());
		}, waitTime);
	}

	$: if (searchString?.trim().length < searchStringLength) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			reloadAllGroups();
		}, waitTime);
	}

	onMount(async () => {
		try {
			reloadAllGroups();
		} catch (err) {
			errorMessage('Error Loading Groups', err.message);
		}
	});

	const searchGroups = async (searchStr) => {
		const res = await httpAdapter.get(`/groups?page=0&size=${groupsPerPage}&filter=${searchStr}`);
		if (res.data.content) {
			groups.set(res.data.content);
		} else {
			groups.set([]);
		}
		groupsTotalPages = res.data.totalPages;
		groupsCurrentPage = 0;
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

	const reloadAllGroups = async (page = 0) => {
		try {
			let res;
			if (searchString && searchString.length >= searchStringLength) {
				res = await httpAdapter.get(
					`/groups?page=${page}&size=${groupsPerPage}&filter=${searchString}`
				);
			} else {
				res = await httpAdapter.get(`/groups?page=${page}&size=${groupsPerPage}`);
			}
			groups.set(res.data.content);
			groupsTotalPages = res.data.totalPages;
			groupsCurrentPage = page;
		} catch (err) {
			errorMessage('Error Loading Groups', err.message);
		}
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
		groupsCurrentPage = 0;

		await reloadAllGroups();
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

		await reloadAllGroups();
	};

	const callFocus = (input) => {
		input.focus();
	};
</script>

<svelte:head>
	<title>Groups | DDS Permissions Manager</title>
	<meta name="description" content="DDS Permissions Manager Groups" />
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
					style="border-width: 1px; vertical-align: middle; text-align: left; width: 13rem"
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

	<div class="content">
		<h1>Groups</h1>
		<center>
			<input
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
			/>&nbsp; &#x1F50E;
		</center>
		{#if $groups}
			{#if $groups.length > 0}
				<table align="center" style="margin-top: 2rem">
					<tr style="border-width: 0px">
						<th><strong>Group</strong></th>
						<th><strong><center>Memberships:</center></strong></th>
						<th><strong><center>Topics:</center></strong></th>
						<th><strong><center>Applications:</center></strong></th>
					</tr>
					{#each $groups as group}
						<tr>
							<td class="group-td">{group.name} </td>
							<td>
								<center>
									<a
										href="/group_membership"
										on:click={() => urlparameters.set({ type: 'prepopulate', data: group.name })}
										>{group.membershipCount}</a
									>
								</center>
							</td>
							<td>
								<center>
									<a
										href="/topics"
										on:click={() => urlparameters.set({ type: 'prepopulate', data: group.name })}
										>{group.topicCount}</a
									>
								</center>
							</td>
							<td>
								<center>
									<a
										href="/applications"
										on:click={() => urlparameters.set({ type: 'prepopulate', data: group.name })}
										>{group.applicationCount}</a
									>
								</center>
							</td>
						</tr>
					{/each}
				</table>
				<br />
				<center>
					<button
						class:hidden={!$isAdmin}
						class="button"
						style="margin: 1rem 0 2rem 0"
						on:click={() => addGroupModal()}
						>Add Group
					</button></center
				>
				<br />
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
		{:else}
			<p><center>No Groups Found</center></p>
		{/if}
	</div>
{:else}
	<center><h2>Please Log In to Continue...</h2></center>
{/if}

<style>
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
		padding-left: 0.3rem;
	}
</style>
