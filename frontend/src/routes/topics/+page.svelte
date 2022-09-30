<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import topics from '../../stores/groups';
	import topicDetails from '../../stores/groupDetails';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import Modal from '../../lib/Modal.svelte';

	export let data, errors;

	// Search
	let searchString;
	const searchStringLength = 3;
	let timer;
	const waitTime = 500;

	// Search Feature
	$: if (searchString?.trim().length >= searchStringLength) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchTopics(searchString.trim());
		}, waitTime);
	}

	$: if (searchString?.trim().length < searchStringLength) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			reloadAllTopics();
		}, waitTime);
	}

	// Authentication
	let isTopicAdmin = false;

	// Error Handling
	let errorMsg, errorObject;

	//Pagination
	const topicsPerPage = 10;
	let topicsTotalPages;
	let topicsCurrentPage = 0;

	// Modals
	let errorMessageVisible = false;
	let topicsListVisible = true;
	let topicDetailVisible = false;
	let addTopicVisible = false;
	let confirmDeleteVisible = false;

	// Validation
	let disabled = false;

	// Group Name
	let newTopicName;
	let editTopicName;

	// Selection
	let selectedTopicId,
		selectedTopicName,
		selectedTopicKind,
		selectedTopicGroupName,
		selectedTopicGroupId;
	let selectedTopicApplications = [];

	onMount(async () => {
		try {
			reloadAllTopics();

			const res = await httpAdapter.get(`/token_info`);
			permissionsByGroup.set(res.data.permissionsByGroup);

			if ($permissionsByGroup) {
				isTopicAdmin = $permissionsByGroup.some(
					(groupPermission) => groupPermission.isTopicAdmin === true
				);
			}
		} catch (err) {
			errorMessage('Error Loading Topics', err.message);
		}

		if ($urlparameters === 'create') {
			if ($isAdmin || isTopicAdmin) {
				addTopicVisible = true;
			} else {
				errorMessage('Only Topic Admins can create topics.', 'Contact your Group Admin.');
			}
			urlparameters.set([]);
		}
	});

	const searchTopics = async (searchStr) => {
		const res = await httpAdapter.get(`/topics?page=0&size=${topicsPerPage}&filter=${searchStr}`);
		if (res.data.content) {
			topics.set(res.data.content);
		} else {
			topics.set([]);
		}
		topicsTotalPages = res.data.totalPages;
		topicsCurrentPage = 0;
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

	const loadTopic = async (topicId) => {
		topicsListVisible = false;
		topicDetailVisible = true;
		try {
			const res = await httpAdapter.get(`/topics/show/${topicId}`);
			topicDetails.set(res.data);

			const resApps = await httpAdapter.get(`/application_permissions/?topic=${topicId}`);
			selectedTopicApplications = resApps.data.content;

			selectedTopicId = $topicDetails.id;
			selectedTopicName = $topicDetails.name;
			selectedTopicGroupName = $topicDetails.groupName;
			selectedTopicGroupId = $topicDetails.groupId;
			selectedTopicKind = $topicDetails.kind;
		} catch (err) {
			errorMessage('Error Loading Topic Details', err.message);
		}
	};

	const deleteTopic = async () => {
		confirmDeleteVisible = false;

		const res = await httpAdapter
			.post(`/topics/delete/${selectedTopicId}`, {
				id: selectedTopicId
			})
			.catch((err) => {
				errorMessage('Error Deleting Topic', err.message);
			});

		returnToTopicsList();
		await reloadAllTopics().then(() => {
			if (topicsCurrentPage === topicsTotalPages) topicsCurrentPage--;
		});
	};

	const addTopic = async () => {
		if (!disabled) {
			await httpAdapter
				.post(`/topics/save/`, {
					name: newTopicName
				})
				.catch((err) => {
					addTopicVisible = false;
					errorMessage('Error Adding Topic', err.message);
				});

			addTopicVisible = false;

			await reloadAllTopics().then(() => {
				topicsCurrentPage = topicsTotalPages - 1;
			});
		}
	};

	const addTopicModal = () => {
		newTopicName = '';
		addTopicVisible = true;
	};

	const returnToTopicsList = () => {
		topicDetailVisible = false;
		topicsListVisible = true;
	};

	const saveNewTopicName = async () => {
		editTopicName = false;
		await httpAdapter
			.post(`/topics/save/`, {
				id: selectedTopicId,
				name: selectedTopicName.trim()
			})
			.catch((err) => {
				errorMessage('Error Editing Topic Name', err.message);
			});

		reloadAllTopics();
	};

	const reloadAllTopics = async (page = 0) => {
		try {
			let res;
			if (searchString && searchString.length >= searchStringLength) {
				res = await httpAdapter.get(
					`/topics?page=${page}&size=${topicsPerPage}&filter=${searchString}`
				);
			} else {
				res = await httpAdapter.get(`/topics?page=${page}&size=${topicsPerPage}`);
			}
			topics.set(res.data.content);
			topicsTotalPages = res.data.totalPages;
			topicsCurrentPage = page;
		} catch (err) {
			errorMessage('Error Loading Topics', err.message);
		}
	};
</script>

<svelte:head>
	<title>Topics | DDS Permissions Manager</title>
	<meta name="description" content="DDS Permissions Manager Topics" />
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
			title="Delete Topic {selectedTopicName}?"
			on:cancel={() => (confirmDeleteVisible = false)}
		>
			<div class="confirm">
				<button class="button-cancel" on:click={() => (confirmDeleteVisible = false)}>Cancel</button
				>&nbsp;
				<button class="button-delete" on:click={() => deleteTopic()}><span>Delete</span></button>
			</div>
		</Modal>
	{/if}

	{#if addTopicVisible && !errorMessageVisible}
		<Modal title="Add New Topic" on:cancel={() => (addTopicVisible = false)}>
			<div class="confirm">
				<input type="text" placeholder="Topic Name" bind:value={newTopicName} />
				<button
					class:button={!disabled}
					style="margin-left: 1rem; width: 6.5rem"
					{disabled}
					on:click={() => addTopic()}><span>Add Topic</span></button
				>
			</div>
			{#if disabled}
				<br />
				<center><span class="create-error">Please choose a unique name</span></center>
			{/if}
		</Modal>
	{/if}

	<div class="content">
		<h1>Topics</h1>
		{#if topicsListVisible && !topicDetailVisible}
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
		{/if}
		{#if $topics}
			{#if $topics.length > 0 && topicsListVisible && !topicDetailVisible}
				<table align="center">
					<tr style="border-width: 0px">
						<th><strong>Topic</strong></th>
						<th><strong>Group</strong></th>
					</tr>
					{#each $topics as topic}
						<tr>
							<td
								style="line-height: 1.7rem;"
								class="topic-td"
								on:click={() => {
									loadTopic(topic.id);
									selectedTopicId = topic.id;
								}}
								>{topic.name}
							</td>
							<td>{topic.groupName}</td>
						</tr>
					{/each}
				</table>
				<br /> <br />

				<center
					><button
						on:click={async () => {
							if (topicsCurrentPage > 0) topicsCurrentPage--;
							reloadAllTopics(topicsCurrentPage);
						}}
						class="button-pagination"
						style="width: 4.8rem; border-bottom-left-radius:9px; border-top-left-radius:9px;"
						disabled={topicsCurrentPage === 0}>Previous</button
					>
					{#if topicsTotalPages > 2}
						{#each Array.apply(null, { length: topicsTotalPages }).map(Number.call, Number) as page}
							<button
								on:click={() => {
									topicsCurrentPage = page;
									reloadAllTopics(page);
								}}
								class="button-pagination"
								class:button-pagination-selected={page === topicsCurrentPage}>{page + 1}</button
							>
						{/each}
					{/if}

					<button
						on:click={async () => {
							if (topicsCurrentPage + 1 < topicsTotalPages) topicsCurrentPage++;
							reloadAllTopics(topicsCurrentPage);
						}}
						class="button-pagination"
						style="width: 3.1rem; border-bottom-right-radius:9px; border-top-right-radius:9px;"
						disabled={topicsCurrentPage === topicsTotalPages - 1 || topicsTotalPages === 0}
						>Next</button
					></center
				>
			{:else if !topicDetailVisible}
				<center><p>No Topics Found</p></center>
			{/if}

			{#if topicsListVisible && !topicDetailVisible}
				<br /><br />
				<center>
					<button class="button" on:click={() => addTopicModal()}>Add Topic </button></center
				>
			{/if}
		{/if}

		{#if $topicDetails && topicDetailVisible && !topicsListVisible}
			<span
				style="font-size: medium; margin-left: 9.5rem; cursor: pointer"
				on:click={() => returnToTopicsList()}
				>&laquo; &nbsp; Back
			</span>

			<table align="center" class="topics-details">
				<tr>
					<td>Name:</td>
					<td>{selectedTopicName}</td>
				</tr>
				<tr>
					<td>Group:</td>
					<td>{selectedTopicGroupName}</td>
				</tr>
				<tr>
					<td>Any application can read:</td>
					<td
						>{#if selectedTopicKind === 'B'}
							Yes
						{:else}
							No
						{/if}
					</td>
				</tr>
				<tr>
					<td>Applications:</td>
					<td>
						<table class="associated-apps">
							{#if selectedTopicApplications}
								{#each selectedTopicApplications as application}
									<tr style="height:unset">
										<td>{application.permissionsApplication.name}</td>
										<td style="padding: 0 0.5rem 0 3rem">Access Type:</td>
										<td>Read</td>
										<td>
											<input
												type="checkbox"
												style="width: 1rem"
												checked={application.accessType === 'READ' ||
													application.accessType === 'READ_WRITE'}
											/>
										</td>
										<td style="padding-left: 0.7rem">Write</td><td>
											<input
												type="checkbox"
												style="width: 1rem"
												checked={application.accessType === 'WRITE' ||
													application.accessType === 'READ_WRITE'}
											/>
										</td>
									</tr>
								{/each}
							{/if}
						</table>
					</td>
				</tr>
			</table>
			<br /><br /><br />
			<center>
				<button
					class="button-delete"
					style="width: 7.5rem; float: unset"
					on:click={() => (confirmDeleteVisible = true)}
					><span>Delete Topic</span>
				</button>
			</center>
		{/if}
	</div>
{:else}
	<center><h2>Please Log In to Continue...</h2></center>
{/if}

<style>
	span {
		position: relative;
		left: 0;
	}

	button {
		margin-left: auto;
	}

	input {
		text-align: center;
		width: 20rem;
		z-index: 1;
	}

	.topics-details {
		margin-top: 2rem;
		font-size: 12pt;
		width: 40rem;
	}

	.topics-details tr {
		height: 2.8rem;
	}

	.topic-td {
		cursor: pointer;
	}

	.associated-apps tr {
		border-width: 0px;
	}

	.associated-apps tr:nth-child(even) {
		background-color: rgba(0, 0, 0, 0);
	}

	.associated-apps td {
		font-size: 11pt;
	}
</style>
