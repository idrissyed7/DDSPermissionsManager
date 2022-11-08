<script>
	import { isAuthenticated, isAdmin } from '../../stores/authentication';
	import { onMount } from 'svelte';
	import { httpAdapter } from '../../appconfig';
	import urlparameters from '../../stores/urlparameters';
	import topics from '../../stores/groups';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import Modal from '../../lib/Modal.svelte';
	import TopicDetails from './TopicDetails.svelte';
	import AddTopic from './AddTopic.svelte';

	export let data, errors;

	// Keys
	const returnKey = 13;

	// Search
	let searchString;
	const searchStringLength = 3;
	let timer;
	const waitTime = 500;

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

	// Selection
	let selectedTopicId,
		selectedTopicName,
		selectedTopicKind,
		selectedTopicGroupName,
		selectedTopicGroupId;

	// Reactive Variables /////////////////////

	// Topics Filter Feature
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

	// onMount /////////////////////

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

		if ($urlparameters?.type === 'prepopulate') {
			searchString = $urlparameters.data;
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

	const loadTopic = () => {
		addTopicVisible = false;
		topicsListVisible = false;
		topicDetailVisible = true;
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

	const returnToTopicsList = () => {
		topicDetailVisible = false;
		topicsListVisible = true;
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

	<div class="content">
		<h1>Topics</h1>
		<center>
			<!-- svelte-ignore a11y-positive-tabindex -->
			<input
				tabindex="8"
				style="border-width: 1px; width: 20rem; text-align: center; height: 1.7rem"
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
		{#if $topics && $topics.length > 0 && topicsListVisible && !topicDetailVisible}
			<table align="center" style="margin-top: 2rem">
				<tr style="border-width: 0px">
					<th><strong>Topic</strong></th>
					<th><strong>Group</strong></th>
				</tr>
				{#each $topics as topic, i}
					<tr>
						<td
							tabindex={i + 9}
							style="line-height: 1.7rem;"
							class="topic-td"
							on:click={() => {
								loadTopic(topic.id);
								selectedTopicId = topic.id;
							}}
							on:keydown={(event) => {
								if (event.which === returnKey) {
									loadTopic(topic.id);
									selectedTopicId = topic.id;
								}
							}}
							>{topic.name}
						</td>
						<td>{topic.groupName}</td>
						{#if $isAdmin || $permissionsByGroup.find((Topic) => Topic.groupId === topic.group && Topic.isTopicAdmin === true)}
							<td>
								<button
									tabindex="-1"
									class="button-delete"
									style="float: right"
									on:click={() => {
										selectedTopicId = topic.id;
										selectedTopicName = topic.name;
										confirmDeleteVisible = true;
									}}
									><span>Delete</span>
								</button>
							</td>
						{:else}
							<td />
						{/if}
					</tr>
				{/each}
			</table>
			<br /> <br />

			<center>
				<button
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

		{#if ($permissionsByGroup && $permissionsByGroup.some((groupPermission) => groupPermission.isTopicAdmin === true)) || $isAdmin}
			{#if topicsListVisible && !topicDetailVisible && !addTopicVisible}
				<br /><br />
				<center>
					<!-- svelte-ignore a11y-positive-tabindex -->
					<button tabindex="19" class="button" on:click={() => (addTopicVisible = true)}
						>Add Topic</button
					>
				</center>
			{/if}
		{/if}

		{#if addTopicVisible && !errorMessageVisible}
			<AddTopic
				on:closecreate={() => (addTopicVisible = false)}
				on:reloadtopics={() => {
					reloadAllTopics();
					topicsCurrentPage = 0;
					addTopicVisible = false;
				}}
			/>
		{/if}

		{#if topicDetailVisible && !topicsListVisible}
			<TopicDetails
				{selectedTopicName}
				{selectedTopicId}
				{selectedTopicKind}
				{selectedTopicGroupName}
				{selectedTopicGroupId}
				on:back={() => {
					reloadAllTopics();
					returnToTopicsList();
				}}
			/>
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
		vertical-align: middle;
		height: 1.2rem;
		text-align: left;
		height: 1.5rem;
	}

	.topic-td {
		cursor: pointer;
	}
</style>
