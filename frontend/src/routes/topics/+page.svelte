<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import topics from '../../stores/groups';
	import topicDetails from '../../stores/groupDetails';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import Modal from '../../lib/Modal.svelte';

	export const data = {};
        export const errors = {};

	// Authentication
	let isTopicAdmin = false;

	// Error Handling
	let errorMsg, errorObject;

	// Pagination
	const topicsPerPage = 3;
	let topicsPageIndex;
	let topicsPages = [];
	let currentPage = 0;

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
			const topicsData = await httpAdapter.get(`/topics`);
			topics.set(topicsData.data.content);

			isTopicAdmin = $permissionsByGroup.some(
				(groupPermission) => groupPermission.isTopicAdmin === true
			);

			if ($topics) {
				// Pagination
				let totalTopicsCount = 0;
				topicsPageIndex = Math.floor(topicsData.data.content.length / topicsPerPage);
				if (topicsData.data.content.length % topicsPerPage > 0) topicsPageIndex++;

				// Populate the usersPage Array
				for (let page = 0; page < topicsPageIndex; page++) {
					let pageArray = [];
					for (
						let i = 0;
						i < topicsPerPage && totalTopicsCount < topicsData.data.content.length;
						i++
					) {
						pageArray.push(topicsData.data.content[page * topicsPerPage + i]);
						totalTopicsCount++;
					}
					topicsPages.push(pageArray);
				}
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

	const calculatePagination = () => {
		topicsPages = [];
		let totalTopicsCount = 0;
		topicsPageIndex = Math.floor($topics.length / topicsPerPage);
		if ($topics.length % topicsPerPage > 0) topicsPageIndex++;

		if (topicsPageIndex === currentPage) currentPage--;

		// Populate the usersPage Array
		let pageArray = [];
		for (let page = 0; page < topicsPageIndex; page++) {
			for (let i = 0; i < topicsPerPage && totalTopicsCount < $topics.length; i++) {
				pageArray.push($topics[page * topicsPerPage + i]);
				totalTopicsCount++;
			}
			topicsPages.push(pageArray);
			pageArray = [];
		}
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
			if (currentPage === topicsPages.length) currentPage--;
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
				currentPage = topicsPages.length - 1;
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

	const reloadAllTopics = async () => {
		try {
			const res = await httpAdapter.get(`/topics`);
			topics.set(res.data.content);

			calculatePagination();
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
	<meta name="description" content="Permission Manager Topics" />
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
		{#if topicsPages && topicsListVisible && !topicDetailVisible}
			<h1>Topics</h1>
			<table align="center">
				{#if topicsPages.length > 0}
					<tr style="border-width: 0px">
						<th><strong>Topic</strong></th>
					</tr>
					{#each topicsPages[currentPage] as topic}
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
						on:click={() => {
							if (currentPage > 0) currentPage--;
						}}
						class="button-pagination"
						style="width: 4.8rem; border-bottom-left-radius:9px; border-top-left-radius:9px;"
						disabled={currentPage === 0}>Previous</button
					>
					{#if topicsPageIndex > 2}
						{#each topicsPages as page, i}
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
							if (currentPage < topicsPages.length) currentPage++;
						}}
						class="button-pagination"
						style="width: 3.1rem; border-bottom-right-radius:9px; border-top-right-radius:9px;"
						disabled={currentPage === topicsPages.length - 1 || topicsPages.length === 0}
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
