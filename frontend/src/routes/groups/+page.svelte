<script>
	import { isAdmin, isAuthenticated } from '../../stores/authentication';
	import { onMount, onDestroy } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import groups from '../../stores/groups';
	import Modal from '../../lib/Modal.svelte';
	import refreshPage from '../../stores/refreshPage';
	import { goto } from '$app/navigation';
	import { browser } from '$app/env';
	import deleteSVG from '../../icons/delete.svg';
	import editSVG from '../../icons/edit.svg';
	import threedotsSVG from '../../icons/threedots.svg';
	import addSVG from '../../icons/add.svg';
	import pageforwardSVG from '../../icons/pageforward.svg';
	import pagebackwardsSVG from '../../icons/pagebackwards.svg';
	import pagefirstSVG from '../../icons/pagefirst.svg';
	import pagelastSVG from '../../icons/pagelast.svg';
	import headerTitle from '../../stores/headerTitle';
	import detailView from '../../stores/detailView';
	import errorMessages from '$lib/errorMessages.json';
	import renderAvatar from '../../stores/renderAvatar';

	export let data, errors;

	// Redirects the User to the Login screen if not authenticated
	$: if (browser) {
		setTimeout(() => {
			if (!$isAuthenticated) goto(`/`, true);
		}, waitTime);
	}

	$: if (browser && (addGroupVisible || deleteGroupVisible || errorMessageVisible)) {
		document.body.classList.add('modal-open');
	} else if (browser && !(addGroupVisible || deleteGroupVisible || errorMessageVisible)) {
		document.body.classList.remove('modal-open');
	}

	// Promises
	let promise;

	// Constants
	const returnKey = 13;
	const waitTime = 1000;
	const searchStringLength = 3;

	// Error Handling
	let errorMsg, errorObject;

	// Search
	let searchString;
	let timer;

	// Modals
	let errorMessageVisible = false;
	let addGroupVisible = false;
	let deleteGroupVisible = false;
	let editGroupVisible = false;

	// Tables
	let groupsRowsSelected = [];
	let groupsRowsSelectedTrue = false;
	let groupsAllRowsSelectedTrue = false;

	// Pagination
	let groupsPerPage = 10;
	let groupsTotalPages, groupsTotalSize;
	let groupsCurrentPage = 0;

	// Selection
	let selectedGroupId;
	let selectedGroupName;

	// DropDowns
	let groupsDropDownVisible = false;
	let groupsDropDownMouseEnter = false;

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
		headerTitle.set('Groups');
		detailView.set();

		promise = reloadAllGroups();
		setTimeout(() => renderAvatar.set(true), 40);
	});

	onDestroy(() => renderAvatar.set(false));

	const searchGroups = async (searchStr) => {
		const res = await httpAdapter.get(`/groups?page=0&size=${groupsPerPage}&filter=${searchStr}`);
		if (res.data.content) {
			groups.set(res.data.content);
		} else {
			groups.set([]);
		}
		groupsTotalPages = res.data.totalPages;
		if (res.data.totalSize !== undefined) groupsTotalSize = res.data.totalSize;
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
			if (res.data) {
				groupsTotalPages = res.data.totalPages;
				groupsTotalSize = res.data.totalSize;
			}
			groups.set(res.data.content);
			groupsTotalPages = res.data.totalPages;
			groupsCurrentPage = page;
		} catch (err) {
			userValidityCheck.set(true);

			errorMessage('Error Loading Groups', err.message);
		}
	};

	const addGroup = async (newGroupName) => {
		await httpAdapter
			.post(`/groups/save/`, {
				name: newGroupName
			})
			.catch((err) => {
				addGroupVisible = false;
				const decodedError = decodeError(Object.create(...err.response.data));
				errorMessage('Error Adding Group', errorMessages[decodedError.category][decodedError.code]);
			});

		searchString = '';
		addGroupVisible = false;
		groupsCurrentPage = 0;

		await reloadAllGroups();
	};

	const editGroupName = async (groupId, groupNewName) => {
		await httpAdapter
			.post(`/groups/save/`, {
				id: groupId,
				name: groupNewName
			})
			.catch((err) => {
				errorMessage('Error Editing Group Name', err.message);
			});

		await reloadAllGroups();
		editGroupVisible = false;
	};

	const deleteSelectedGroups = async () => {
		try {
			for (const group of groupsRowsSelected) {
				await httpAdapter.post(`/groups/delete/${group.id}`, {
					id: group.id
				});
			}
		} catch (err) {
			deleteGroupVisible = false;
			errorMessage('Error Deleting Group', err.message);
		}
	};

	const deselectAllGroupsCheckboxes = () => {
		groupsAllRowsSelectedTrue = false;
		groupsRowsSelectedTrue = false;
		groupsRowsSelected = [];
		let checkboxes = document.querySelectorAll('.groups-checkbox');
		checkboxes.forEach((checkbox) => (checkbox.checked = false));
	};

	const numberOfSelectedCheckboxes = () => {
		let checkboxes = document.querySelectorAll('.groups-checkbox');
		checkboxes = Array.from(checkboxes);
		return checkboxes.filter((checkbox) => checkbox.checked === true).length;
	};

	const decodeError = (errorObject) => {
		errorObject = errorObject.code.replaceAll('-', '_');
		const cat = errorObject.substring(0, errorObject.indexOf('.'));
		const code = errorObject.substring(errorObject.indexOf('.') + 1, errorObject.length);
		return { category: cat, code: code };
	};
</script>

<svelte:head>
	<title>Groups | DDS Permissions Manager</title>
	<meta name="description" content="DDS Permissions Manager Groups" />
</svelte:head>

{#key $refreshPage}
	{#if $isAuthenticated}
		{#await promise then _}
			{#if errorMessageVisible}
				<Modal
					title={errorMsg}
					errorMsg={true}
					errorDescription={errorObject}
					closeModalText={'Close'}
					on:cancel={() => {
						errorMessageVisible = false;
						errorMessageClear();
					}}
				/>
			{/if}

			{#if deleteGroupVisible}
				<Modal
					title="Delete {groupsRowsSelected.length > 1 ? 'Groups' : 'Group'}"
					actionDeleteGroups={true}
					on:deleteGroups={async () => {
						await deleteSelectedGroups();
						reloadAllGroups();
						deselectAllGroupsCheckboxes();
						deleteGroupVisible = false;
					}}
					on:cancel={() => {
						if (groupsRowsSelected?.length === 1 && numberOfSelectedCheckboxes() === 0)
							groupsRowsSelected = [];

						deleteGroupVisible = false;
					}}
				/>
			{/if}

			{#if addGroupVisible}
				<Modal
					title="Add New Group"
					actionAddGroup={true}
					groupNewName={true}
					on:addGroup={(e) => addGroup(e.detail.newGroupName)}
					on:cancel={() => (addGroupVisible = false)}
				/>
			{/if}

			{#if editGroupVisible}
				<Modal
					title="Edit Group"
					actionEditGroup={true}
					groupCurrentName={selectedGroupName}
					groupNewName={true}
					groupId={selectedGroupId}
					on:addGroup={(e) => {
						editGroupName(e.detail.groupId, e.detail.newGroupName);
					}}
					on:cancel={() => (editGroupVisible = false)}
				/>
			{/if}

			<div class="content">
				<h1 data-cy="groups">Groups</h1>

				<form class="searchbox">
					<input
						data-cy="search-groups-table"
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

				{#if $isAdmin}
					<div
						data-cy="dot-groups"
						tabindex="0"
						class="dot"
						on:mouseleave={() => {
							setTimeout(() => {
								if (!groupsDropDownMouseEnter) groupsDropDownVisible = false;
							}, waitTime);
						}}
						on:click={() => {
							if (!deleteGroupVisible && !addGroupVisible)
								groupsDropDownVisible = !groupsDropDownVisible;
						}}
						on:keydown={(event) => {
							if (event.which === returnKey) {
								if (!deleteGroupVisible && !addGroupVisible)
									groupsDropDownVisible = !groupsDropDownVisible;
							}
						}}
						on:focusout={() => {
							setTimeout(() => {
								if (!groupsDropDownMouseEnter) groupsDropDownVisible = false;
							}, waitTime);
						}}
					>
						<img src={threedotsSVG} alt="options" style="scale:50%" />

						{#if groupsDropDownVisible}
							<table
								class="dropdown"
								on:mouseenter={() => (groupsDropDownMouseEnter = true)}
								on:mouseleave={() => {
									setTimeout(() => {
										if (!groupsDropDownMouseEnter) groupsDropDownVisible = false;
									}, waitTime);
									groupsDropDownMouseEnter = false;
								}}
							>
								<tr
									tabindex="0"
									on:focus={() => (groupsDropDownMouseEnter = true)}
									disabled={!$isAdmin}
									class:disabled={!$isAdmin || groupsRowsSelected.length === 0}
									on:click={async () => {
										groupsDropDownVisible = false;
										if (groupsRowsSelected.length > 0) deleteGroupVisible = true;
									}}
									on:keydown={(event) => {
										if (event.which === returnKey) {
											groupsDropDownVisible = false;
											if (groupsRowsSelected.length > 0) deleteGroupVisible = true;
										}
									}}
								>
									<td>Delete Selected {groupsRowsSelected.length > 1 ? 'Groups' : 'Group'} </td>
									<td style="width: 0.1rem; padding-left: 0; vertical-align: middle">
										<img
											src={deleteSVG}
											alt="delete group"
											height="35rem"
											style="vertical-align: -0.8rem"
											class:disabled-img={!$isAdmin || groupsRowsSelected.length === 0}
										/>
									</td>
								</tr>

								<tr
									data-cy="add-group"
									tabindex="0"
									on:click={() => {
										groupsDropDownVisible = false;
										addGroupVisible = true;
									}}
									on:keydown={(event) => {
										if (event.which === returnKey) {
											groupsDropDownVisible = false;
											addGroupVisible = true;
										}
									}}
									on:focusout={() => (groupsDropDownMouseEnter = false)}
									class:hidden={addGroupVisible}
								>
									<td style="border-bottom-color: transparent">Add New Group</td>
									<td
										on:click={() => (addGroupVisible = true)}
										style="width: 0.1rem; height: 2.2rem;padding-left: 0; vertical-align: middle; border-bottom-color: transparent"
									>
										<img
											src={addSVG}
											alt="add group"
											height="27rem"
											style="vertical-align: middle; margin-left: 0.2rem"
										/>
									</td>
								</tr>
							</table>
						{/if}
					</div>
				{/if}

				{#if $groups}
					{#if $groups.length > 0}
						<table
							data-cy="groups-table"
							style="margin-top: 0.5rem; min-width: 33rem; width:max-content"
						>
							<tr style="border-top: 1px solid black; border-bottom: 2px solid">
								{#if $isAdmin}
									<td>
										<input
											tabindex="-1"
											type="checkbox"
											class="groups-checkbox"
											style="margin-right: 0.5rem; vertical-align: middle;"
											bind:indeterminate={groupsRowsSelectedTrue}
											on:click={(e) => {
												groupsDropDownVisible = false;
												if (e.target.checked) {
													groupsRowsSelected = $groups;
													groupsRowsSelectedTrue = false;
													groupsAllRowsSelectedTrue = true;
												} else {
													groupsAllRowsSelectedTrue = false;
													groupsRowsSelectedTrue = false;
													groupsRowsSelected = [];
												}
											}}
											checked={groupsAllRowsSelectedTrue}
										/>
									</td>
								{/if}
								<td style="min-width: 12rem">Group</td>
								<td style="width: 5rem; text-align:center">Users</td>
								<td style="width: 5rem; text-align:center">Topics</td>
								<td style="width: 5rem; text-align:right; padding-right: 1rem">Applications</td>
							</tr>
							{#each $groups as group}
								<tr>
									{#if $isAdmin}
										<td style="width: 2rem">
											<input
												tabindex="-1"
												type="checkbox"
												class="groups-checkbox"
												style="vertical-align: middle;"
												checked={groupsAllRowsSelectedTrue}
												on:change={(e) => {
													groupsDropDownVisible = false;
													if (e.target.checked === true) {
														groupsRowsSelected.push(group);
														groupsRowsSelectedTrue = true;
													} else {
														groupsRowsSelected = groupsRowsSelected.filter(
															(selection) => selection !== group
														);
														if (groupsRowsSelected.length === 0) {
															groupsRowsSelectedTrue = false;
														}
													}
												}}
											/>
										</td>
									{/if}
									<td style="width: max-content">{group.name}</td>
									<td style="width: max-content">
										<center>
											<a
												tabindex="-1"
												style="vertical-align: middle"
												href="/users"
												on:click={() =>
													urlparameters.set({ type: 'prepopulate', data: group.name })}
												>{group.membershipCount}</a
											>
										</center>
									</td>
									<td style="width: max-content">
										<center>
											<a
												tabindex="-1"
												style="vertical-align: middle"
												href="/topics"
												on:click={() =>
													urlparameters.set({ type: 'prepopulate', data: group.name })}
												>{group.topicCount}</a
											>
										</center>
									</td>
									<td style="width: max-content">
										<center>
											<a
												tabindex="-1"
												style="vertical-align: middle"
												href="/applications"
												on:click={() =>
													urlparameters.set({ type: 'prepopulate', data: group.name })}
												>{group.applicationCount}</a
											>
										</center>
									</td>
									{#if $isAdmin}
										<td style="cursor: pointer; text-align: right; padding-right: 0.25rem">
											<img
												src={editSVG}
												alt="edit group"
												style="cursor: pointer; vertical-align: -0.25rem"
												height="17rem"
												width="17rem"
												on:click={() => {
													editGroupVisible = true;
													selectedGroupId = group.id;
													selectedGroupName = group.name;
												}}
											/>
										</td>

										<td style="cursor: pointer; text-align: right; padding-right: 0.25rem">
											<img
												src={deleteSVG}
												alt="delete group"
												style="cursor: pointer; vertical-align: -0.5rem"
												height="27rem"
												on:click={() => {
													if (!groupsRowsSelected.some((grp) => grp === group))
														groupsRowsSelected.push(group);
													deleteGroupVisible = true;
												}}
											/>
										</td>
									{/if}
								</tr>
							{/each}
						</table>
					{/if}
				{:else}
					<p>No Groups Found</p>
				{/if}
			</div>

			<div class="pagination">
				<span>Rows per page</span>
				<select
					tabindex="-1"
					on:change={(e) => {
						groupsPerPage = e.target.value;
						reloadAllGroups();
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
					{#if groupsTotalSize > 0}
						{1 + groupsCurrentPage * groupsPerPage}
					{:else}
						0
					{/if}
					- {Math.min(groupsPerPage * (groupsCurrentPage + 1), groupsTotalSize)} of
					{groupsTotalSize}
				</span>
				<img
					src={pagefirstSVG}
					alt="first page"
					class="pagination-image"
					class:disabled-img={groupsCurrentPage === 0}
					on:click={() => {
						deselectAllGroupsCheckboxes();
						if (groupsCurrentPage > 0) {
							groupsCurrentPage = 0;
							reloadAllGroups();
						}
					}}
				/>
				<img
					src={pagebackwardsSVG}
					alt="previous page"
					class="pagination-image"
					class:disabled-img={groupsCurrentPage === 0}
					on:click={() => {
						deselectAllGroupsCheckboxes();
						if (groupsCurrentPage > 0) {
							groupsCurrentPage--;
							reloadAllGroups(groupsCurrentPage);
						}
					}}
				/>
				<img
					src={pageforwardSVG}
					alt="next page"
					class="pagination-image"
					class:disabled-img={groupsCurrentPage + 1 === groupsTotalPages ||
						$groups?.length === undefined}
					on:click={() => {
						deselectAllGroupsCheckboxes();
						if (groupsCurrentPage + 1 < groupsTotalPages) {
							groupsCurrentPage++;
							reloadAllGroups(groupsCurrentPage);
						}
					}}
				/>
				<img
					src={pagelastSVG}
					alt="last page"
					class="pagination-image"
					class:disabled-img={groupsCurrentPage + 1 === groupsTotalPages ||
						$groups?.length === undefined}
					on:click={() => {
						deselectAllGroupsCheckboxes();
						if (groupsCurrentPage < groupsTotalPages) {
							groupsCurrentPage = groupsTotalPages - 1;
							reloadAllGroups(groupsCurrentPage);
						}
					}}
				/>
			</div>
			<p style="margin-top: 8rem">© 2022 Unity Foundation. All rights reserved.</p>
		{/await}
	{/if}
{/key}

<style>
	.content {
		width: fit-content;
		min-width: 32rem;
		margin-right: 1rem;
	}

	.dot {
		float: right;
	}

	.dropdown {
		margin-top: 8.5rem;
		margin-right: 8rem;
		width: 12rem;
	}

	tr {
		line-height: 2.2rem;
	}

	p {
		font-size: large;
	}
</style>
