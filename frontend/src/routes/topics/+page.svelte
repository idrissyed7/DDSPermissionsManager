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
	let timer;
	const waitTime = 500;

	// Search Feature
	$: if (searchString?.trim().length >= 3) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchTopics(searchString.trim());
		}, waitTime);
	} else {
		reloadAllTopics();
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
	let selectedTopicId;
	let selectedTopicName;

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
	};

	const errorMessage = (errMsg, errObj) => {
		errorMsg = errMsg;
		errorObject = errObj;
		errorMessageVisible = true;
	};

	const ErrorMessageClear = () => {
		errorMsg = '';
		errorObject = '';
		errorMessageVisible = false;
	};

	const loadTopic = async (topicId) => {
		topicsListVisible = false;
		topicDetailVisible = true;
		try {
			const res = await httpAdapter.get(`/topics/${topicId}`);

			topicDetails.set(res.data);
			selectedTopicId = $topicDetails.group.id;
			selectedTopicName = $topicDetails.group.name;
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
			if (currentPage === topicsTotalPages) currentPage--;
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
				currentPage = topicsTotalPages - 1;
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
			const res = await httpAdapter.get(`/topics?page=${page}&size=${topicsPerPage}`);
			topics.set(res.data.content);
			topicsTotalPages = res.data.totalPages;
		} catch (err) {
			errorMessage('Error Loading Topics', err.message);
		}
	};

	const reloadTopicDetails = async () => {
		try {
			const res = await httpAdapter.get(`/topics/${selectedTopicId}`);

			topicDetails.set(res.data);
		} catch (err) {
			errorMessage('Error Loading Topic Details', err.message);
		}
	};
</script>

<svelte:head>
	<title>Topics | DDS Permissions Manager</title>
	<meta name="description" content="DDS Permission Manager Topics" />
</svelte:head>

{#if $isAuthenticated}
	{#if errorMessageVisible}
		<Modal
			title={errorMsg}
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
			title="Delete Topic {selectedTopicName}?"
			on:cancel={() => (confirmDeleteVisible = false)}
		>
			<div class="confirm">
				<button class="button-cancel" on:click={() => (confirmDeleteVisible = false)}>Cancel</button
				>
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

		{#if $topics.length > 0 && topicsListVisible && !topicDetailVisible}
			<table align="center">
				{#if $topics.length > 0}
					<tr style="border-width: 0px">
						<th><strong>Topic</strong></th>
					</tr>
					{#each $topics as topic}
						<tr>
							<td
								class="topic-td"
								on:click={() => {
									loadTopic(topic.id);
									selectedTopicId = topic.id;
								}}>{topic.name}</td
							>
						</tr>
					{/each}
				{/if}
			</table>
			<br /> <br />

			{#if $topics}
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
			{:else}
				<center><p>No Topics Found</p></center>
			{/if}

			<br /><br />

			<center> <button class="button" on:click={() => addTopicModal()}>Add Topic </button></center>
		{/if}

		{#if $topicDetails && topicDetailVisible && !topicsListVisible}
			<div class="name">
				<span on:click={() => returnToTopicsList()}>&laquo;</span>
				<div class="tooltip">
					<input
						id="name"
						on:click={() => (editTopicName = true)}
						on:blur={() => saveNewTopicName()}
						on:keydown={(event) => {
							if (event.which === 13) {
								saveNewTopicName();
								document.querySelector('#name').blur();
							}
						}}
						bind:value={selectedTopicName}
						readonly={!editTopicName}
						class:name-as-label={!editTopicName}
					/>
					<span class="tooltiptext">&#9998</span>
				</div>
			</div>

			<br /><br /><br />

			<center>
				<button
					class="button-delete"
					style="width: 7.5rem"
					on:click={() => (confirmDeleteVisible = true)}
					><span>Delete Topic</span>
				</button></center
			>
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
	}

	input {
		text-align: center;
		width: 20rem;
		z-index: 1;
	}

	.topic-td {
		cursor: pointer;
	}
</style>
