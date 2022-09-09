<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import groupMembershipList from '../../stores/groupMembershipList';
	import groups from '../../stores/groups';
	import Modal from '../../lib/Modal.svelte';

	export let data, errors;

	// Modals
	let addGroupMembershipVisible = false;
	let errorMessageVisible = false;

	// SearchBox
	let searchString;
	let searchResults;
	let searchResultsVisible = false;
	let isEmail = false;

	// Forms
	let emailValue = '';
	let selectedGroup;
	let selectedIsGroupAdmin = false;
	let selectedIsApplicationAdmin = false;
	let selectedIsTopicAdmin = false;

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

	// Search Box
	$: if (searchString?.trim().length >= 3) {
		isEmail = extractEmails(searchString.trim());
		searchGroupMemberships(searchString.trim(), Boolean(isEmail?.length > 0));
	} else {
		searchResultsVisible = false;
	}

	$: if (searchResults?.data?.content?.length >= 1) {
		searchResultsVisible = true;
	} else {
		searchResultsVisible = false;
	}

	$: console.log('searchResults', searchResults?.data?.content);

	onMount(async () => {
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
	});

	const reloadGroupMemberships = async (page = 0) => {
		try {
			const res = await httpAdapter.get(
				`/group_membership?page=${page}&size=${groupMembershipsPerPage}`
			);
			if (res.data) {
				groupMembershipsTotalPages = res.data.totalPages;
			}

			if (res.data.content) {
				res.data.content.forEach((groupMembership) => {
					let newGroupMembership = {
						applicationAdmin: groupMembership.applicationAdmin,
						groupAdmin: groupMembership.groupAdmin,
						topicAdmin: groupMembership.topicAdmin,
						groupName: groupMembership.permissionsGroup.name,
						userEmail: groupMembership.permissionsUser.email
					};
					groupMembershipListArray.push(newGroupMembership);
				});
				groupMembershipList.set(groupMembershipListArray);
				groupMembershipListArray = [];
			}
		} catch (err) {
			errorMessage('Error Loading Group Memberships', err.message);
		}
	};

	const searchGroupMemberships = async (searchStr, isEmail) => {
		setTimeout(async () => {
			if (isEmail) {
				searchResults = await httpAdapter.get(`/group_membership?email=${searchStr}`);
			} else {
				searchResults = await httpAdapter.get(`/group_membership?group=${searchStr}`);
			}
		}, 1000);
	};

	const extractEmails = (text) => {
		return text.match(
			/^([a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+(?:\.[a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)$/gm
		);
	};

	const addGroupMembershipInput = async () => {
		addGroupMembershipVisible = true;
	};

	const addGroupMembership = async () => {
		console.log('Request:');
		console.log('email: ', emailValue);
		console.log('permissionsGroup: ', selectedGroup);
		console.log('isGroupAdmin: ', selectedIsGroupAdmin);
		console.log('isTopicAdmin: ', selectedIsTopicAdmin);
		console.log('isApplicationAdmin: ', selectedIsApplicationAdmin);
		const res = await httpAdapter
			.post(`/group_membership`, {
				email: emailValue,
				permissionsGroup: selectedGroup,
				isGroupAdmin: selectedIsGroupAdmin,
				isTopicAdmin: selectedIsTopicAdmin,
				isApplicationAdmin: selectedIsTopicAdmin
			})
			.catch((err) => {
				console.log('err', err);
				if (err.response.status === 403) {
					errorMessage('Error Saving Group Membership', err.message);
				} else if (err.response.status === 400) {
					errorMessage('Error: Group Membership it already exists.', err.message);
				}
			});
		addGroupMembershipVisible = false;
		console.log('result:', res);
	};

	const ValidateEmail = (input) => {
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
</script>

<svelte:head>
	<title>Group Membership | DDS Permissions Manager</title>
	<meta name="description" content="Permission Manager Group Membership" />
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

	<div class="content">
		<h1>Group Membership</h1>
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
				><table class="searchGroupMembership">
					<th>E-mail</th>
					<th>Group</th>
					<th><center>Group Admin</center></th>
					<th><center>Topic Admin</center></th>
					<th><center>Application Admin</center></th>
					{#each searchResults.data.content as result}
						<tr style="padding-left: 0.3rem"
							><td style="font-size: 0.9rem"> {result.permissionsUser.email}</td>
							<td style="font-size: 0.8rem">{result.permissionsGroup.name}</td>
							{#if result.groupAdmin}
								<td><center>&check;</center></td>
							{:else}
								<td />
							{/if}
							{#if result.applicationAdmin}
								<td><center>&check;</center></td>
							{:else}
								<td />
							{/if}
							{#if result.topicAdmin}
								<td><center>&check;</center></td>
							{:else}
								<td />
							{/if}
						</tr>
					{/each}
				</table></center
			>
		{/if}
		{#if $isAdmin || isGroupAdmin}
			{#if $groupMembershipList}
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
						</tr>
					{/each}
				</table>
			{/if}
			<br />
			<center
				><button
					style="cursor:pointer"
					on:click={() => addGroupMembershipInput()}
					class:hidden={addGroupMembershipVisible}>+</button
				></center
			>
			<br /><br />

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
		{:else}
			<h2>No group memberships</h2>
		{/if}

		{#if addGroupMembershipVisible}
			<table>
				<tr>
					<td style="width: 15rem"
						><input
							placeholder="Email Address"
							class:invalid={invalidEmail && emailValue.length >= 1}
							style="
						display: inline-flex;		
						height: 1.7rem;
						text-align: left;
						font-size: small;
						min-width: 12rem;"
							bind:value={emailValue}
							on:blur={() => ValidateEmail(emailValue)}
							on:keydown={(event) => {
								if (event.which === 13) {
									ValidateEmail(emailValue);
									document.querySelector('#name').blur();
								}
							}}
						/>
						<label for="groups">Group:</label>
						<select name="groups" bind:value={selectedGroup}>
							{#if $isAdmin}
								{#if $groups}
									{#each $groups as group}
										<option value={group.id}>{group.name}</option>
									{/each}
								{/if}
							{:else if groupAdminGroups}
								{#if $groups}
									{#each groupAdminGroups as group}
										<option value={group.groupId}>{group.groupName}</option>
									{/each}
								{/if}
							{/if}
						</select>
						<input type="checkbox" name="groupAdmin" bind:checked={selectedIsGroupAdmin} />
						<label for="groupAdmin">Group Admin</label>
						<input
							type="checkbox"
							name="applicationAdmin"
							bind:checked={selectedIsApplicationAdmin}
						/>
						<label for="applicationAdmin">Application Admin</label>
						<input type="checkbox" name="topicAdmin" bind:checked={selectedIsTopicAdmin} />
						<label for="topicAdmin">Topic Admin</label>
						<button
							class:button={!invalidEmail}
							style="margin-left: 1rem; width: 6.5rem"
							disabled={invalidEmail}
							on:click={() => addGroupMembership()}><span>Add</span></button
						>
						<button
							class="remove-button"
							on:click={() => {
								emailValue = '';
								addGroupMembershipVisible = false;
							}}>x</button
						></td
					>
				</tr>
			</table>
		{/if}
	</div>
{:else}
	<center><h2>Please Log In to Continue...</h2></center>
{/if}

<style>
	.hidden {
		display: none;
	}

	.remove-button:hover {
		cursor: pointer;
		background-color: lightgray;
	}

	.searchGroupMembership {
		max-width: 40rem;
		cursor: pointer;
		list-style-type: none;
		margin: 0;
		padding-top: 0.1rem;
		padding-bottom: 0.2rem;
		background-color: rgba(217, 221, 254, 0.4);
		text-align: left;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.26);
	}

	.searchGroupMembership tr:nth-child(even) {
		background-color: rgba(192, 196, 240, 0.4);
	}

	.searchGroupMembership th {
		font-size: 0.8rem;
	}

	label {
		font-size: small;
	}

	input {
		vertical-align: middle;

		text-align: center;
		width: 20rem;
		z-index: 1;
		background-color: rgba(0, 0, 0, 0);
	}

	table {
		width: 100%;
	}
</style>
