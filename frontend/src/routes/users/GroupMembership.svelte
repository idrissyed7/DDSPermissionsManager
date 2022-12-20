<script>
	import { isAuthenticated, isAdmin, onLoggedIn } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import { browser } from '$app/env';
	import urlparameters from '../../stores/urlparameters';
	import groupAdminGroups from '../../stores/groupAdminGroups';
	import groupMembershipList from '../../stores/groupMembershipList';
	import userValidityCheck from '../../stores/userValidityCheck';
	import refreshPage from '../../stores/refreshPage';
	import Modal from '$lib/Modal.svelte';
	import threedotsSVG from '../../icons/threedots.svg';
	import deleteSVG from '../../icons/delete.svg';
	import addSVG from '../../icons/add.svg';
	import editSVG from '../../icons/edit.svg';
	import pageforwardSVG from '../../icons/pageforward.svg';
	import pagebackwardsSVG from '../../icons/pagebackwards.svg';
	import pagefirstSVG from '../../icons/pagefirst.svg';
	import pagelastSVG from '../../icons/pagelast.svg';
	import userEmail from '../../stores/userEmail';

	// Promises
	let promise;

	// Constants
	const returnKey = 13;
	const waitTime = 1000;

	// DropDowns
	let usersDropDownVisible = false;
	let usersDropDownMouseEnter = false;

	// Modals
	let addGroupMembershipVisible = false;
	let errorMessageVisible = false;
	let updateGroupMembershipVisible = false;
	let deleteSelectedGroupMembershipsVisible = false;

	// SearchBox
	let searchString;
	const searchStringLength = 3;
	let searchGroups;
	let searchGroupResults;
	let searchGroupsResultsVisible = false;
	let searchGroupActive = false;
	let selectedGroup;
	let timer;

	// Forms
	let emailValue = '';
	let selectedIsGroupAdmin = false;
	let selectedIsApplicationAdmin = false;
	let selectedIsTopicAdmin = false;
	let groupsDropdownSuggestion = 7;

	// Tables
	let usersRowsSelected = [];
	let usersRowsSelectedTrue = false;
	let usersAllRowsSelectedTrue = false;

	// Group Permissions
	let isGroupAdmin = false;

	// Pagination
	let groupMembershipsPerPage = 10;
	let groupMembershipsTotalPages, groupMembershipsTotalSize;
	let groupMembershipsCurrentPage = 0;

	// Error Handling
	let errorMsg, errorObject;

	// Group Membership List
	let groupMembershipListArray = [];

	$: if (
		browser &&
		(addGroupMembershipVisible ||
			deleteSelectedGroupMembershipsVisible ||
			updateGroupMembershipVisible)
	) {
		document.body.classList.add('modal-open');
	} else if (
		browser &&
		!(
			addGroupMembershipVisible ||
			deleteSelectedGroupMembershipsVisible ||
			updateGroupMembershipVisible
		)
	) {
		document.body.classList.remove('modal-open');
	}

	// Search Group Membership Feature
	$: if (searchString?.trim().length >= searchStringLength && $urlparameters === null) {
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
		searchGroupResults = '';
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

	onMount(async () => {
		if ($urlparameters?.type === 'prepopulate') {
			searchString = $urlparameters.data;
		}

		refreshPage.set();

		promise = reloadGroupMemberships();
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
				groupMembershipsTotalSize = res.data.totalSize;
			}
			if (res.data.content) {
				createGroupMembershipList(res.data.content, res.data.totalPages);
			} else {
				groupMembershipList.set();
			}
			groupMembershipsCurrentPage = page;
		} catch (err) {
			userValidityCheck.set(true);

			if (err.response.status === 500) onLoggedIn(false);
			errorMessage('Error Loading Group Memberships', err.message);
		}

		if ($groupAdminGroups?.length > 0) {
			isGroupAdmin = true;
		}
	};

	const addGroupMembership = async (
		forwardedSelectedGroup,
		selectedIsGroupAdmin,
		selectedIsTopicAdmin,
		selectedIsApplicationAdmin,
		forwardedSearchGroups,
		emailValue
	) => {
		if (!forwardedSelectedGroup) {
			const groupId = await httpAdapter.get(
				`/groups?page=0&size=1&filter=${forwardedSearchGroups}`
			);
			if (
				groupId.data.content &&
				groupId.data.content[0]?.name.toUpperCase() === forwardedSearchGroups.toUpperCase()
			) {
				selectedGroup = groupId.data.content[0]?.id;
				searchGroupActive = false;
			}
		} else {
			selectedGroup = forwardedSelectedGroup;
		}

		await httpAdapter
			.post(`/group_membership`, {
				email: emailValue,
				permissionsGroup: selectedGroup,
				groupAdmin: selectedIsGroupAdmin,
				topicAdmin: selectedIsTopicAdmin,
				applicationAdmin: selectedIsApplicationAdmin
			})
			.catch((err) => {
				if (err.response.status === 403) {
					errorMessage('Error Saving Group Membership', err.message);
				} else if (err.response.status === 400 || 401) {
					// err.message = 'Group Membership already exists.';
					errorMessage('Error Adding Group Membership', err.message);
				}
			});

		addGroupMembershipVisible = false;

		userValidityCheck.set(true);

		await reloadGroupMemberships();
	};

	const createGroupMembershipList = async (data, totalPages, totalSize) => {
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
		if (totalSize !== undefined) groupMembershipsTotalSize = totalSize;
		groupMembershipsCurrentPage = 0;
	};

	const searchGroupMemberships = async (searchStr) => {
		const res = await httpAdapter.get(
			`/group_membership?filter=${searchStr}&size=${groupMembershipsPerPage}`
		);
		createGroupMembershipList(res.data.content, res.data.totalPages, res.data.totalSize);
	};

	const searchGroup = async (searchGroupStr) => {
		setTimeout(async () => {
			searchGroupResults = await httpAdapter.get(
				`/groups?page=0&size=${groupsDropdownSuggestion}&filter=${searchGroupStr}`
			);
		}, 1000);
	};

	const errorMessage = (errMsg, errObj) => {
		errorMsg = errMsg;
		errorObject = errObj;
		errorMessageVisible = true;
	};

	const updateGroupMembershipSelection = (selectedGM) => {
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

	const updateGroupMembership = async (gm) => {
		const data = {
			id: selectedGroupMembership.groupMembershipId,
			groupAdmin: gm.groupAdmin,
			topicAdmin: gm.topicAdmin,
			applicationAdmin: gm.applicationAdmin,
			permissionsGroup: selectedGroupMembership.groupId,
			email: selectedGroupMembership.userEmail
		};
		try {
			await httpAdapter.put(`/group_membership`, data);
		} catch (err) {
			console.error('Error Updating Group Membership', err.message);
		}
		updateGroupMembershipVisible = false;

		userValidityCheck.set(true);

		await reloadGroupMemberships();
	};

	const deleteSelectedGroupMemberships = async () => {
		try {
			for (const groupMembership of usersRowsSelected) {
				await httpAdapter.delete(`/group_membership`, {
					data: { id: groupMembership.groupMembershipId }
				});
			}
		} catch (err) {
			errorMessage('Error Deleting Users', err.message);
		}

		userValidityCheck.set(true);

		await reloadGroupMemberships();
	};

	const deselectAllGroupMembershipCheckboxes = () => {
		usersAllRowsSelectedTrue = false;
		usersRowsSelectedTrue = false;
		usersRowsSelected = [];
		let checkboxes = document.querySelectorAll('.group-membership-checkbox');
		checkboxes.forEach((checkbox) => (checkbox.checked = false));
	};

	const numberOfSelectedCheckboxes = () => {
		let checkboxes = document.querySelectorAll('.group-membership-checkbox');
		checkboxes = Array.from(checkboxes);
		return checkboxes.filter((checkbox) => checkbox.checked === true).length;
	};
</script>

{#key $refreshPage}
	{#if $isAuthenticated}
		{#await promise then _}
			{#if addGroupMembershipVisible}
				<Modal
					title="Add User"
					actionAddUser={true}
					email={true}
					group={true}
					adminRoles={true}
					on:cancel={() => (addGroupMembershipVisible = false)}
					on:addGroupMembership={async (e) => {
						addGroupMembershipVisible = false;
						await addGroupMembership(
							e.detail.selectedGroup,
							e.detail.selectedIsGroupAdmin,
							e.detail.selectedIsTopicAdmin,
							e.detail.selectedIsApplicationAdmin,
							e.detail.searchGroups,
							e.detail.emailValue
						);
						await reloadGroupMemberships(groupMembershipsCurrentPage);
					}}
				/>
			{/if}

			{#if updateGroupMembershipVisible}
				<Modal
					title="Change User"
					actionEditUser={true}
					email={true}
					emailValue={selectedGroupMembership.userEmail}
					group={true}
					searchGroups={selectedGroupMembership.groupName}
					adminRoles={true}
					noneditable={true}
					{selectedGroupMembership}
					on:cancel={() => (updateGroupMembershipVisible = false)}
					on:reloadGroupMemberships={() => reloadGroupMemberships(groupMembershipsCurrentPage)}
					on:updateGroupMembership={(e) => updateGroupMembership(e.detail)}
				/>
			{/if}

			{#if deleteSelectedGroupMembershipsVisible}
				<Modal
					title="Delete {usersRowsSelected.length > 1 ? 'Users' : 'User'}"
					actionDeleteUsers={true}
					on:cancel={() => {
						if (usersRowsSelected?.length === 1 && numberOfSelectedCheckboxes() === 0)
							usersRowsSelected = [];

						deleteSelectedGroupMembershipsVisible = false;
					}}
					on:deleteGroupMemberships={async () => {
						await deleteSelectedGroupMemberships();

						reloadGroupMemberships();
						deselectAllGroupMembershipCheckboxes();
						deleteSelectedGroupMembershipsVisible = false;
					}}
				/>
			{/if}

			<div class="content">
				<form class="searchbox">
					<input
						data-cy="search-users-table"
						class="searchbox"
						type="search"
						placeholder="Search"
						bind:value={searchString}
						on:blur={() => {
							searchString = searchString?.trim();
						}}
						on:keydown={(event) => {
							if (event.which === returnKey) {
								document.activeElement.blur();
								searchString = searchString?.trim();
							}
						}}
					/>
				</form>

				{#if searchString?.length > 0}
					<button
						class="button-blue"
						style="cursor: pointer; width: 4rem; height: 2.1rem"
						on:click={() => (searchString = '')}>Clear</button
					>
				{/if}

				{#if $isAdmin || isGroupAdmin}
					<div
						data-cy="dot-users"
						class="dot"
						tabindex="0"
						on:mouseleave={() => {
							setTimeout(() => {
								if (!usersDropDownMouseEnter) usersDropDownVisible = false;
							}, waitTime);
						}}
						on:focusout={() => {
							setTimeout(() => {
								if (!usersDropDownMouseEnter) usersDropDownVisible = false;
							}, waitTime);
						}}
						on:click={() => {
							if (!deleteSelectedGroupMembershipsVisible && !addGroupMembershipVisible)
								usersDropDownVisible = !usersDropDownVisible;
						}}
						on:keydown={(event) => {
							if (event.which === returnKey) {
								if (!deleteSelectedGroupMembershipsVisible && !addGroupMembershipVisible)
									usersDropDownVisible = !usersDropDownVisible;
							}
						}}
					>
						<img src={threedotsSVG} alt="options" style="scale:50%" />

						{#if usersDropDownVisible}
							<table
								class="dropdown"
								on:mouseenter={() => {
									usersDropDownMouseEnter = true;
								}}
								on:mouseleave={() => {
									setTimeout(() => {
										if (!usersDropDownMouseEnter) usersDropDownVisible = false;
									}, waitTime);
									usersDropDownMouseEnter = false;
								}}
							>
								<tr
									data-cy="delete-user"
									tabindex="0"
									class:disabled={usersRowsSelected.length === 0}
									on:click={() => {
										usersDropDownVisible = false;
										if (usersRowsSelected.length > 0) deleteSelectedGroupMembershipsVisible = true;
									}}
									on:keydown={(event) => {
										if (event.which === returnKey) {
											usersDropDownVisible = false;
											if (usersRowsSelected.length > 0)
												deleteSelectedGroupMembershipsVisible = true;
										}
									}}
									on:focus={() => (usersDropDownMouseEnter = true)}
								>
									<td>
										Delete Selected {usersRowsSelected.length > 1 ? 'Users' : 'User'}
									</td>

									<td>
										<img
											src={deleteSVG}
											alt="delete user"
											height="35rem"
											style="vertical-align: -0.8rem"
											class:disabled-img={usersRowsSelected.length === 0}
										/>
									</td>
								</tr>

								<tr
									data-cy="add-user"
									tabindex="0"
									on:click={() => {
										usersDropDownVisible = false;
										addGroupMembershipVisible = true;
									}}
									on:keydown={(event) => {
										if (event.which === returnKey) {
											usersDropDownVisible = false;
											addGroupMembershipVisible = true;
										}
									}}
									on:focusout={() => (usersDropDownMouseEnter = false)}
								>
									<td style="border-bottom-color: transparent"> Add New User </td>
									<td
										style="width: 0.1rem; height: 2.2rem; padding-left: 0; vertical-align: middle;border-bottom-color: transparent"
									>
										<img
											src={addSVG}
											alt="add user"
											height="27rem"
											style="vertical-align: middle; margin-left: 1.2rem"
										/>
									</td>
								</tr>
							</table>
						{/if}
					</div>
				{/if}

				{#if $groupMembershipList && $groupMembershipList.length > 0}
					<table
						data-cy="users-table"
						style="margin-top:0.5rem; min-width: 50rem; width:max-content"
					>
						<thead>
							<tr style="border-top: 1px solid black; border-bottom: 2px solid">
								{#if $isAdmin || isGroupAdmin}
									<td>
										<input
											tabindex="-1"
											type="checkbox"
											class="group-membership-checkbox"
											style="margin-right: 0.5rem"
											bind:indeterminate={usersRowsSelectedTrue}
											on:click={(e) => {
												usersDropDownVisible = false;
												if (e.target.checked) {
													usersRowsSelected = $groupMembershipList;
													usersRowsSelectedTrue = false;
													usersAllRowsSelectedTrue = true;
												} else {
													usersAllRowsSelectedTrue = false;
													usersRowsSelectedTrue = false;
													usersRowsSelected = [];
												}
											}}
											checked={usersAllRowsSelectedTrue}
										/>
									</td>
								{/if}
								<td style="font-stretch:ultra-condensed; width:fit-content">E-mail</td>
								<td style="font-stretch:ultra-condensed; width:fit-content">Group</td>
								<td style="font-stretch:ultra-condensed; width:6rem"
									><center>Group Admin</center></td
								>
								<td style="font-stretch:ultra-condensed; width:7rem"
									><center>Topic Admin</center></td
								>
								<td style="font-stretch:ultra-condensed; width:8rem"
									><center>Application Admin</center></td
								>
								<td /><td />
							</tr>
						</thead>
						<tbody>
							{#each $groupMembershipList as groupMembership, i}
								<tr class:highlighted={groupMembership.userEmail === $userEmail}>
									{#if $isAdmin || isGroupAdmin}
										<td style="width: 2rem">
											<input
												tabindex="-1"
												type="checkbox"
												style="margin-right: 0.5rem"
												class="group-membership-checkbox"
												checked={usersAllRowsSelectedTrue}
												on:change={(e) => {
													usersDropDownVisible = false;
													if (e.target.checked === true) {
														usersRowsSelected.push(groupMembership);
														usersRowsSelectedTrue = true;
													} else {
														usersRowsSelected = usersRowsSelected.filter(
															(selection) => selection !== groupMembership
														);
														if (usersRowsSelected.length === 0) {
															usersRowsSelectedTrue = false;
														}
													}
												}}
											/>
										</td>
									{/if}
									<td>{groupMembership.userEmail}</td>
									<td style="width:fit-content">{groupMembership.groupName}</td>
									<td>
										<center>
											{#if groupMembership.groupAdmin}&check;
											{:else}
												-
											{/if}
										</center>
									</td>
									<td>
										<center
											>{#if groupMembership.topicAdmin}&check;
											{:else}
												-
											{/if}
										</center>
									</td>
									<td data-cy="is-application-admin">
										<center
											>{#if groupMembership.applicationAdmin}&check;
											{:else}
												-
											{/if}
										</center>
									</td>

									{#if $isAdmin || $groupAdminGroups?.some((group) => group.groupName === groupMembership.groupName)}
										<td
											style="cursor: pointer; text-align: right"
											on:keydown={(event) => {
												if (event.which === returnKey) {
													updateGroupMembershipSelection(groupMembership);
												}
											}}
										>
											<img
												data-cy="edit-users-icon"
												src={editSVG}
												height="17rem"
												width="17rem"
												style="vertical-align: -0.225rem"
												alt="edit user"
												on:click={() => updateGroupMembershipSelection(groupMembership)}
											/>
										</td>
										<td
											style="cursor: pointer; text-align: right; padding-right: 0.25rem; width: 1rem"
										>
											<img
												data-cy="delete-users-icon"
												src={deleteSVG}
												height="27px"
												width="27px"
												style="vertical-align: -0.5rem"
												alt="delete user"
												on:click={() => {
													if (!usersRowsSelected.some((gm) => gm === groupMembership))
														usersRowsSelected.push(groupMembership);
													deleteSelectedGroupMembershipsVisible = true;
												}}
											/>
										</td>
									{:else}
										<td /><td />
									{/if}
								</tr>
							{/each}
						</tbody>
					</table>
				{:else}
					<p>
						No users found.&nbsp;<span
							class="link"
							on:click={() => (addGroupMembershipVisible = true)}
						>
							Click here
						</span>
						to create a new user
					</p>
				{/if}

				<br />
			</div>
			<div class="pagination">
				<span>Rows per page</span>
				<select
					tabindex="-1"
					on:change={(e) => {
						groupMembershipsPerPage = e.target.value;

						reloadGroupMemberships();
					}}
					name="RowsPerPage"
				>
					<option value="10">10</option>
					<option value="25">25</option>
					<option value="50">50</option>
					<option value="75">75</option>
					<option value="100">100&nbsp;</option>
				</select>
				<span style="margin: 0 2rem 0 2rem">
					{#if groupMembershipsTotalSize > 0}
						{1 + groupMembershipsCurrentPage * groupMembershipsPerPage}
					{:else}
						0
					{/if}
					- {Math.min(
						groupMembershipsPerPage * (groupMembershipsCurrentPage + 1),
						groupMembershipsTotalSize
					)} of {groupMembershipsTotalSize}
				</span>
				<img
					src={pagefirstSVG}
					alt="first page"
					class="pagination-image"
					class:disabled-img={groupMembershipsCurrentPage === 0}
					on:click={() => {
						deselectAllGroupMembershipCheckboxes();
						if (groupMembershipsCurrentPage > 0) {
							groupMembershipsCurrentPage = 0;

							reloadGroupMemberships();
						}
					}}
				/>
				<img
					src={pagebackwardsSVG}
					alt="previous page"
					class="pagination-image"
					class:disabled-img={groupMembershipsCurrentPage === 0}
					on:click={() => {
						deselectAllGroupMembershipCheckboxes();
						if (groupMembershipsCurrentPage > 0) {
							groupMembershipsCurrentPage--;
							reloadGroupMemberships(groupMembershipsCurrentPage);
						}
					}}
				/>
				<img
					src={pageforwardSVG}
					alt="next page"
					class="pagination-image"
					class:disabled-img={groupMembershipsCurrentPage + 1 === groupMembershipsTotalPages ||
						$groupMembershipList?.length === undefined}
					on:click={() => {
						deselectAllGroupMembershipCheckboxes();
						if (groupMembershipsCurrentPage + 1 < groupMembershipsTotalPages) {
							groupMembershipsCurrentPage++;
							reloadGroupMemberships(groupMembershipsCurrentPage);
						}
					}}
				/>
				<img
					src={pagelastSVG}
					alt="last page"
					class="pagination-image"
					class:disabled-img={groupMembershipsCurrentPage + 1 === groupMembershipsTotalPages ||
						$groupMembershipList?.length === undefined}
					on:click={() => {
						deselectAllGroupMembershipCheckboxes();
						if (groupMembershipsCurrentPage < groupMembershipsTotalPages) {
							groupMembershipsCurrentPage = groupMembershipsTotalPages - 1;
							reloadGroupMemberships(groupMembershipsCurrentPage);
						}
					}}
				/>
			</div>
		{/await}
	{/if}
{/key}

<style>
	.content {
		width: fit-content;
		min-width: 32rem;
		margin-right: 1rem;
	}

	.dropdown {
		width: 12.5rem;
		margin-right: 8.5rem;
		margin-top: 8rem;
	}

	.dot {
		float: right;
	}

	input {
		vertical-align: middle;
	}

	table {
		width: fit-content;
	}

	tr {
		line-height: 2.2rem;
	}

	p {
		font-size: large;
	}
</style>
