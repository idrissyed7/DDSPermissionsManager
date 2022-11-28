<script>
	import { fade, fly } from 'svelte/transition';
	import { createEventDispatcher } from 'svelte';
	import { httpAdapter } from '../appconfig';
	import closeSVG from '../icons/close.svg';
	import deleteSVG from '../icons/delete.svg';
	import Switch from './Switch.svelte';
	import { inview } from 'svelte-inview';
	import errorMessages from '$lib/errorMessages.json';

	export let title;
	export let email = false;
	export let topicName = false;
	export let applicationName = false;
	export let groupNewName = false;
	export let group = false;
	export let adminRoles = false;
	export let application = false;
	export let actionAddUser = false;
	export let actionAddSuperUser = false;
	export let actionAddTopic = false;
	export let actionAddApplication = false;
	export let actionAddGroup = false;
	export let actionEditUser = false;
	export let actionEditApplicationName = false;
	export let actionDeleteUsers = false;
	export let actionDeleteSuperUsers = false;
	export let actionDeleteTopics = false;
	export let actionDeleteGroups = false;
	export let actionDeleteApplications = false;
	export let actionDuplicateTopic = false;
	export let noneditable = false;
	export let emailValue = '';
	export let newTopicName = '';
	export let anyApplicationCanRead = false;
	export let searchGroups = '';
	export let searchApplications = '';
	export let selectedGroupMembership = '';
	export let previousAppName = '';
	export let selectedApplicationList = [];
	export let errorDescription = '';
	export let errorMsg = false;
	export let closeModalText = 'Cancel';

	const dispatch = createEventDispatcher();

	// Constants
	const returnKey = 13;
	const groupsDropdownSuggestion = 7;
	const applicationsDropdownSuggestion = 7;
	const minNameLength = 3;
	const searchStringLength = 3;
	const waitTime = 250;

	// Forms
	let selectedIsGroupAdmin = false;
	let selectedIsTopicAdmin = false;
	let selectedIsApplicationAdmin = false;
	let appName = '';
	let newGroupName = '';

	// Error Handling
	let invalidTopic = false;
	let invalidGroup = false;
	let invalidApplication = false;
	let invalidApplicationName = false;
	let invalidEmail = false;
	let errorMessageGroup = '';
	let errorMessageApplication = '';
	let errorMessageEmail = '';
	let errorMessageName = '';
	let validRegex =
		/^([a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+(?:\.[a-zA-Z0-9!#$%&'*+\/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)$/gm;

	// Search Applications
	let searchApplicationActive = false;
	let searchApplicationResults = [];
	let searchApplicationsResultsVisible = false;
	let applicationResultPage = 0;
	let hasMoreApps = true;
	let stopSearchingApps = false;

	// SearchBox
	let searchString;
	let searchGroupsResultsMouseEnter = false;
	let searchGroupResults;
	let searchGroupsResultsVisible = false;
	let searchGroupActive = false;
	let selectedGroup;
	let groupResultPage = 0;
	let hasMoreGroups = true;
	let stopSearchingGroups = false;
	let timer;
	let searchApplicationsResultsMouseEnter = false;

	// Search Groups Feature
	$: if (
		searchGroups?.trim().length >= searchStringLength &&
		searchGroupActive &&
		!stopSearchingGroups
	) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchGroups = searchGroups.trim();
			searchGroup(searchGroups);
			stopSearchingGroups = true;
		}, waitTime);
	}

	// else {
	// 	searchGroupsResultsVisible = false;
	// }

	// Search Groups Dropdown Visibility
	$: if (
		searchGroupResults?.length >= 1 &&
		searchGroupActive &&
		searchGroups?.trim().length >= searchStringLength
	) {
		searchGroupsResultsVisible = true;
	} else {
		searchGroupsResultsVisible = false;
	}

	// Search Applications Feature
	$: if (
		searchApplications?.trim().length >= searchStringLength &&
		searchApplicationActive &&
		!stopSearchingApps
	) {
		clearTimeout(timer);
		timer = setTimeout(() => {
			searchApplications = searchApplications.trim();
			searchApplication(searchApplications);
			stopSearchingApps = true;
		}, waitTime);
	}

	// Search Applications Dropdown Visibility
	$: if (
		searchApplicationResults?.length >= 1 &&
		searchApplicationActive &&
		searchApplications?.trim().length >= searchStringLength
	) {
		searchApplicationsResultsVisible = true;
	} else {
		searchApplicationsResultsVisible = false;
	}

	const searchGroup = async (searchGroupStr) => {
		let res = await httpAdapter.get(
			`/groups?page=${groupResultPage}&size=${groupsDropdownSuggestion}&filter=${searchGroupStr}`
		);
		if (res.data && res.data?.content?.length < groupsDropdownSuggestion) {
			hasMoreGroups = false;
		}

		if (res.data?.content?.length > 0)
			searchGroupResults = [...searchGroupResults, ...res.data.content];
	};

	const selectedSearchGroup = (groupName, groupId) => {
		searchGroupResults = [];
		groupResultPage = 0;

		selectedGroup = groupId;
		searchGroups = groupName;

		searchGroupsResultsVisible = false;
		searchGroupActive = false;
	};

	const searchApplication = async (searchString) => {
		let res = await httpAdapter.get(
			`/applications/search?page=${applicationResultPage}&size=${applicationsDropdownSuggestion}&filter=${searchString}`
		);

		if (res?.data?.length < applicationsDropdownSuggestion) {
			hasMoreApps = false;
		}

		if (res.data.length > 0) {
			if (selectedApplicationList?.length > 0)
				for (const selectedApp of selectedApplicationList) {
					res.data = res.data.filter((results) => results.name !== selectedApp.applicationName);
				}

			searchApplicationResults = [...searchApplicationResults, ...res.data];
		}
	};

	const selectedSearchApplication = (appName, appId, groupName) => {
		searchApplicationResults = [];
		applicationResultPage = 0;

		selectedApplicationList.push({
			applicationId: appId,
			applicationName: appName,
			applicationGroup: groupName,
			accessType: 'READ'
		});

		// This statement is used to trigger Svelte reactivity and re-render the component
		selectedApplicationList = selectedApplicationList;
		searchApplications = appName;

		searchApplicationsResultsVisible = false;
		searchApplications = '';
		searchApplicationActive = false;
	};

	const validateEmail = (input) => {
		if (input.match(validRegex)) invalidEmail = false;
		else {
			invalidEmail = true;
			errorMessageEmail = errorMessages['email']['is_not_format'];
		}
	};

	const validateGroupName = async () => {
		const res = await httpAdapter.get(
			`/groups?page=0&size=${groupsDropdownSuggestion}&filter=${searchGroups}`
		);
		if (
			res.data.content?.some((group) => group.name.toUpperCase() === searchGroups.toUpperCase())
		) {
			return true;
		} else {
			return false;
		}
	};

	const validateApplicationName = async () => {
		// if there is data in the applications input field, we verify it's validity
		if (searchApplications?.length > 0) {
			const res = await httpAdapter.get(
				`/applications/search?page=0&size=1&filter=${searchApplications}`
			);

			console.log('search string', searchApplications);
			console.log('res', res.data);

			if (
				res.data.length > 0 &&
				res.data?.[0].name?.toUpperCase() === searchApplications.toUpperCase()
			) {
				selectedSearchApplication(res.data[0].name, res.data[0].id, res.data[0].groupName);
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	};

	const validateNameLength = (name, category) => {
		if (name?.length < minNameLength && category) {
			errorMessageName = errorMessages[category]['name.cannot_be_less_than_three_characters'];
			return false;
		}
		if (name?.length >= minNameLength) {
			return true;
		}
	};

	function closeModal() {
		dispatch('cancel');
	}

	const actionAddUserEvent = async () => {
		let newGroupMembership = {
			selectedIsGroupAdmin: selectedIsGroupAdmin,
			selectedIsTopicAdmin: selectedIsTopicAdmin,
			selectedIsApplicationAdmin: selectedIsApplicationAdmin,
			selectedGroup: selectedGroup,
			searchGroups: searchGroups,
			emailValue: emailValue
		};

		const validGroupName = await validateGroupName();
		if (!validGroupName) {
			errorMessageGroup = errorMessages['group']['not_found'];
			return;
		}

		if (!invalidEmail || searchGroups?.length >= minNameLength)
			dispatch('addGroupMembership', newGroupMembership);
	};

	const actionAddSuperUserEvent = () => {
		validateEmail(emailValue);
		if (!invalidEmail) {
			dispatch('addSuperUser', emailValue);
			closeModal();
		}
	};

	const actionAddTopicEvent = async () => {
		let newTopic = {
			newTopicName: newTopicName,
			searchGroups: searchGroups,
			selectedGroup: selectedGroup,
			anyApplicationCanRead: anyApplicationCanRead,
			selectedApplicationList: selectedApplicationList
		};

		const validName = await validateNameLength(newTopicName, 'topic');
		if (!validName) {
			errorMessageName = errorMessages['topic']['name.cannot_be_less_than_three_characters'];
			return;
		}

		const validGroupName = await validateGroupName();
		if (!validGroupName) {
			errorMessageGroup = errorMessages['group']['not_found'];
			return;
		}

		const validApplicationName = await validateApplicationName();
		if (!validApplicationName) {
			errorMessageApplication = errorMessages['application']['not_found'];
			return;
		}

		if (validName && validGroupName && validApplicationName) {
			dispatch('addTopic', newTopic);
			closeModal();
		}
	};

	const actionAddApplicationEvent = async () => {
		let newApplication = {
			appName: appName,
			searchGroups: searchGroups,
			selectedGroup: selectedGroup
		};

		const validGroupName = await validateGroupName();
		if (!validGroupName) {
			errorMessageGroup = errorMessages['group']['not_found'];
			return;
		}

		dispatch('addApplication', newApplication);
		closeModal();
	};

	const actionAddGroupEvent = async () => {
		let returnGroupName = {
			newGroupName: newGroupName
		};

		const res = await httpAdapter.get(
			`/groups?page=0&size=${groupsDropdownSuggestion}&filter=${newGroupName}`
		);

		if (res.data.content) {
			if (
				res.data.content.some((group) => group.name.toUpperCase() === newGroupName.toUpperCase())
			) {
				errorMessageGroup = errorMessages['group']['exists'];

				return;
			} else {
				dispatch('addGroup', returnGroupName);
				closeModal();
			}
		} else {
			dispatch('addGroup', returnGroupName);
			closeModal();
		}
	};

	const actionEditUserEvent = () => {
		dispatch('updateGroupMembership', {
			groupAdmin: selectedGroupMembership.groupAdmin,
			topicAdmin: selectedGroupMembership.topicAdmin,
			applicationAdmin: selectedGroupMembership.applicationAdmin
		});
	};

	const actionDuplicateTopicEvent = () => {
		let newTopic = {
			newTopicName: newTopicName,
			searchGroups: searchGroups,
			selectedGroup: selectedGroup,
			anyApplicationCanRead: anyApplicationCanRead,
			selectedApplicationList: selectedApplicationList
		};
		dispatch('duplicateTopic', newTopic);
		closeModal();
	};

	const decodeError = (errorObject) => {
		errorObject = errorObject.code.replaceAll('-', '_');
		const cat = errorObject.substring(0, errorObject.indexOf('.'));
		const code = errorObject.substring(errorObject.indexOf('.') + 1, errorObject.length);
		return { category: cat, code: code };
	};

	const loadMoreResultsApp = (e) => {
		if (e.detail.inView && hasMoreApps) {
			applicationResultPage++;
			searchApplication(searchApplications);
		}
	};

	const loadMoreResultsGroups = (e) => {
		if (e.detail.inView && hasMoreGroups) {
			groupResultPage++;
			searchGroup(searchGroups);
		}
	};

	const options = {
		rootMargin: '20px',
		unobserveOnEnter: true
	};
</script>

<div class="modal-backdrop" on:click={closeModal} transition:fade />
<div class="modal" transition:fly={{ y: 300 }}>
	<img src={closeSVG} alt="close" class="close-button" on:click={closeModal} />
	<h2>{title}</h2>
	<hr />
	<div class="content">
		{#if errorMsg}
			<p>{errorDescription}</p>
		{/if}

		{#if email}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				data-cy="email-input"
				autofocus
				disabled={noneditable}
				placeholder="Email"
				class:invalid={invalidEmail && emailValue?.length >= 1}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={emailValue}
				on:blur={() => {
					emailValue = emailValue.trim();
					validateEmail(emailValue);
				}}
				on:click={() => {
					invalidEmail = false;
					errorMessageEmail = '';
				}}
				on:keydown={(event) => {
					invalidEmail = false;
					errorMessageEmail = '';

					if (event.which === returnKey) {
						if (actionAddUser) {
							emailValue = emailValue.trim();
							validateEmail(emailValue);
							if (!invalidEmail && emailValue.length >= minNameLength && searchGroups?.length > 3) {
								actionAddUserEvent();
							}
						}
						if (actionAddSuperUser && emailValue.length >= 10) actionAddSuperUserEvent();
					}
				}}
			/>
			<span
				class="error-message"
				style="	top: 9.6rem; right: 2.4rem"
				class:hidden={errorMessageEmail?.length === 0}
			>
				{errorMessageEmail}
			</span>
			{#if noneditable}
				<span
					style="display: inline-flex; font-size: 0.65rem; position: relative; top: -3rem; left: 0.5rem; background-color: rgb(246,246,246); padding: 0 0.2rem 0 0.2rem; color: rgb(120,120,120)"
					>Email
				</span>
			{/if}
		{/if}

		{#if topicName}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				data-cy="topic-name"
				autofocus
				placeholder="Topic Name"
				class:invalid={invalidTopic}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={newTopicName}
				on:blur={() => {
					newTopicName = newTopicName.trim();
					invalidTopic = !validateNameLength(newTopicName, 'topic');
				}}
				on:keydown={(event) => {
					errorMessageName = '';

					if (event.which === returnKey) {
						newTopicName = newTopicName.trim();
						invalidTopic = !validateNameLength(newTopicName);

						if (!invalidTopic && searchGroups?.length >= searchStringLength) {
							actionDuplicateTopicEvent();
						}
					}
				}}
				on:click={() => {
					errorMessageName = '';
				}}
			/>
		{/if}

		{#if errorMessageName?.substring(0, errorMessageName?.indexOf(' ')) === 'Topic' && errorMessageName?.length > 0}
			<span
				class="error-message"
				style="	top: 9.6rem; right: 2.2rem"
				class:hidden={errorMessageName?.length === 0}
			>
				{errorMessageName}
			</span>
		{/if}

		{#if applicationName}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				data-cy="application-name"
				autofocus
				placeholder="Application Name"
				class:invalid={invalidApplication}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={appName}
				on:blur={() => {
					appName = appName.trim();
					invalidApplication = !validateNameLength(appName, 'application');
				}}
				on:keydown={(event) => {
					errorMessageName = '';
					if (event.which === returnKey) {
						appName = appName.trim();
						invalidApplication = !validateNameLength(appName, 'application');

						if (!invalidApplication && searchGroups?.length >= searchStringLength) {
							actionAddApplicationEvent();
						}
					}
				}}
				on:click={() => (errorMessageName = '')}
			/>
		{/if}

		{#if errorMessageName?.substring(0, errorMessageName?.indexOf(' ')) === 'Application' && errorMessageName?.length > 0}
			<span
				class="error-message"
				style="	top: 9.6rem; right: 1.4rem"
				class:hidden={errorMessageName?.length === 0}
			>
				{errorMessageName}
			</span>
		{/if}

		{#if groupNewName}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				data-cy="group-new-name"
				autofocus
				placeholder="Group Name"
				class:invalid={invalidGroup}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={newGroupName}
				on:blur={() => {
					newGroupName = newGroupName.trim();
					invalidGroup = !validateNameLength(newGroupName, 'group');
				}}
				on:keydown={(event) => {
					errorMessageName = '';
					if (event.which === returnKey) {
						newGroupName = newGroupName.trim();
						invalidGroup = !validateNameLength(newGroupName, 'group');
						if (!invalidGroup) actionAddGroupEvent();
					}
				}}
				on:click={() => (errorMessageName = '')}
			/>

			{#if errorMessageName?.substring(0, errorMessageName?.indexOf(' ')) === 'Group' && errorMessageName?.length > 0}
				<span
					class="error-message"
					style="	top: 9.6rem; right: 2.1rem"
					class:hidden={errorMessageName?.length === 0}
				>
					{errorMessageName}
				</span>
			{/if}
		{/if}

		{#if actionEditApplicationName}
			<!-- svelte-ignore a11y-autofocus -->
			<input
				autofocus
				placeholder="Application Name"
				class:invalid={invalidApplicationName}
				style="background: rgb(246, 246, 246); width: 13.2rem; margin-right: 2rem"
				bind:value={previousAppName}
				on:blur={() => {
					previousAppName = previousAppName.trim();
					invalidApplicationName = !validateNameLength(previousAppName, 'application');
				}}
				on:keydown={(event) => {
					errorMessageName = '';
					if (event.which === returnKey) {
						previousAppName = previousAppName.trim();
						invalidApplicationName = !validateNameLength(previousAppName, 'application');
						if (!invalidApplicationName)
							dispatch('saveNewAppName', { newAppName: previousAppName });
					}
				}}
				on:click={() => (errorMessageName = '')}
			/>
		{/if}

		{#if group}
			<form class="searchbox" style="margin-bottom: 0.7rem">
				<input
					data-cy="group-input"
					class="searchbox"
					type="search"
					placeholder="Group"
					disabled={noneditable}
					class:invalid={errorMessageGroup?.length > 0}
					bind:value={searchGroups}
					on:keydown={(event) => {
						searchGroupResults = [];
						stopSearchingGroups = false;
						hasMoreGroups = true;
						groupResultPage = 0;

						if (event.which === returnKey) {
							document.activeElement.blur();
							searchString = searchString?.trim();

							if (actionAddUser) {
								validateEmail(emailValue);
								if (
									!invalidEmail &&
									emailValue.length >= minNameLength &&
									searchGroups?.length >= searchStringLength
								) {
									actionAddUserEvent();
								}
							}

							if (actionAddTopic) {
								if (
									newTopicName?.length >= minNameLength &&
									searchGroups?.length >= searchStringLength
								) {
									actionAddTopicEvent();
								}
							}

							if (actionAddApplication) {
								if (
									appName?.length >= minNameLength &&
									searchGroups?.length >= searchStringLength
								) {
									actionAddApplicationEvent();
								}
							}
						}
					}}
					on:blur={() => {
						searchString = searchString?.trim();
						setTimeout(() => {
							searchGroupsResultsVisible = false;
						}, waitTime);
					}}
					on:focus={async () => {
						searchGroupActive = true;
						errorMessageGroup = '';
						selectedGroup = '';
					}}
					on:focusout={() => {
						setTimeout(() => {
							searchGroupsResultsVisible = false;
						}, waitTime);
					}}
					on:click={async () => {
						searchGroupActive = true;
						selectedGroup = '';
						stopSearchingGroups = false;

						if (searchGroupResults?.length > 0) {
							searchGroupsResultsVisible = true;
						}
					}}
					on:mouseleave={() => {
						setTimeout(() => {
							if (!searchGroupsResultsMouseEnter) searchGroupsResultsVisible = false;
						}, waitTime);
					}}
				/>
			</form>
			<span
				class="error-message"
				style="	top: 12.7rem; right: 2.2rem"
				class:hidden={errorMessageGroup?.length === 0}
			>
				{errorMessageGroup}
			</span>

			{#if searchGroupsResultsVisible}
				<table
					class="search-group"
					style="position: absolute; z-index: 100; display: block; overflow-y: scroll; max-height: 13.3rem"
					on:mouseenter={() => (searchGroupsResultsMouseEnter = true)}
					on:mouseleave={() => {
						setTimeout(() => {
							searchGroupsResultsVisible = false;
							searchGroupsResultsMouseEnter = false;
						}, waitTime);
					}}
					on:focusout={() => {
						searchGroupsResultsVisible = false;
						searchGroupsResultsMouseEnter = false;
					}}
				>
					{#each searchGroupResults as result}
						<tr>
							<td
								style="width: 14rem; padding-left: 0.5rem"
								on:click={() => {
									selectedSearchGroup(result.name, result.id);
								}}
								>{result.name}
							</td>
						</tr>
					{/each}
					<div use:inview={{ options }} on:change={loadMoreResultsGroups} />
				</table>
			{/if}

			{#if noneditable}
				<span
					style="display: inline-flex; font-size: 0.65rem; position: relative; top: -3.6rem; left: 0.5rem; background-color: rgb(246,246,246); padding: 0 0.2rem 0 0.2rem; color: rgb(120,120,120)"
					>Group
				</span>
			{/if}
		{/if}

		{#if topicName}
			<div style="display:flex; align-items: center; margin-top: 1rem">
				<span style="font-size:0.9rem; width: 9rem; margin-right: 0.5rem; ">
					Any application can read topic:
				</span>
				<Switch bind:checked={anyApplicationCanRead} />
			</div>
		{/if}

		{#if application}
			<form class="searchbox">
				<input
					class="searchbox"
					type="search"
					placeholder="Application"
					bind:value={searchApplications}
					on:keydown={(event) => {
						searchApplicationResults = [];
						stopSearchingApps = false;
						hasMoreApps = true;
						applicationResultPage = 0;

						if (event.which === returnKey) {
							document.activeElement.blur();
							newTopicName = newTopicName?.trim();
							searchGroups = searchGroups?.trim();

							validateApplicationName();
						}
					}}
					on:blur={() => {
						searchApplications = searchApplications?.trim();
						setTimeout(() => {
							searchApplicationsResultsVisible = false;
						}, waitTime);
					}}
					on:focus={() => {
						searchApplicationActive = true;
						errorMessageApplication = '';
					}}
					on:focusout={() => {
						setTimeout(() => {
							searchApplicationsResultsVisible = false;
						}, waitTime);
					}}
					on:click={async () => {
						searchApplicationActive = true;
						errorMessageApplication = '';
						if (searchApplicationResults?.length > 0) {
							searchApplicationsResultsVisible = true;
						}
					}}
					on:mouseleave={() => {
						setTimeout(() => {
							if (!searchApplicationsResultsMouseEnter) searchApplicationsResultsVisible = false;
						}, waitTime);
					}}
				/>
			</form>
			<span
				class="error-message"
				style="	top: 19.9rem; right: 2.2rem"
				class:hidden={errorMessageApplication?.length === 0}
			>
				{errorMessageApplication}
			</span>
		{/if}

		{#if searchApplicationsResultsVisible}
			<table
				class="search-application"
				style="position: absolute; display: block; overflow-y: scroll; max-height: 13.3rem"
				on:mouseenter={() => (searchApplicationsResultsMouseEnter = true)}
				on:mouseleave={() => {
					setTimeout(() => {
						searchApplicationsResultsVisible = false;
						searchApplicationsResultsMouseEnter = false;
					}, waitTime);
				}}
			>
				{#each searchApplicationResults as result}
					<tr style="border-width: 0px;">
						<td
							style="width: 14rem; padding-left: 0.5rem"
							on:click={() => {
								selectedSearchApplication(result.name, result.id, result.groupName); ///
							}}
							>{result.name} ({result.groupName})
						</td>
					</tr>
				{/each}
				<div use:inview={{ options }} on:change={loadMoreResultsApp} />
			</table>
		{/if}

		{#if selectedApplicationList?.length > 0}
			<div style="margin-bottom: 0.8rem">
				{#each selectedApplicationList as app}
					<div class="application-list">
						<ul style="list-style-type: none; padding-left: 0; margin-right: -3rem">
							<li style="width: 9rem">
								{app.applicationName}
							</li>
							<li style="color: rgb(120,120,120)">
								({app.applicationGroup})
							</li>
						</ul>

						<ul style="list-style-type: none; margin-right: -2rem">
							<li style="margin-top: -0.05rem"><span style="font-size: 0.65rem">Read</span></li>
							<li style="margin-top: -0.05rem">
								<input
									type="checkbox"
									name="read"
									style="width:unset;"
									checked
									on:change={(e) => {
										const applicationIndex = selectedApplicationList.findIndex(
											(application) => application.applicationName === app.applicationName
										);

										if (e.target.checked) {
											if (selectedApplicationList[applicationIndex].accessType === 'WRITE') {
												selectedApplicationList[applicationIndex].accessType = 'READ_WRITE';
											} else {
												selectedApplicationList[applicationIndex].accessType = 'READ';
											}
										} else {
											if (selectedApplicationList[applicationIndex].accessType === 'READ_WRITE') {
												selectedApplicationList[applicationIndex].accessType = 'WRITE';
											} else {
												selectedApplicationList[applicationIndex].accessType = '';
											}
										}
									}}
								/>
							</li>
						</ul>

						<ul style="list-style-type: none; margin-right: -2.4rem">
							<li style="margin-top: -0.05rem"><span style="font-size: 0.65rem">Write</span></li>
							<li style="margin-top: -0.05rem">
								<input
									type="checkbox"
									name="write"
									style="width:unset;"
									on:change={(e) => {
										const applicationIndex = selectedApplicationList.findIndex(
											(application) => application.applicationName === app.applicationName
										);

										if (e.target.checked) {
											if (selectedApplicationList[applicationIndex].accessType === 'READ') {
												selectedApplicationList[applicationIndex].accessType = 'READ_WRITE';
											} else {
												selectedApplicationList[applicationIndex].accessType = 'WRITE';
											}
										} else {
											if (selectedApplicationList[applicationIndex].accessType === 'READ_WRITE') {
												selectedApplicationList[applicationIndex].accessType = 'READ';
											} else {
												selectedApplicationList[applicationIndex].accessType = '';
											}
										}
									}}
								/>
							</li>
						</ul>

						<ul style="list-style-type: none; margin-top: 0.35rem; margin-left: -0.5rem">
							<li />
							<li>
								<img
									src={deleteSVG}
									alt="remove application"
									style="background-color: transparent; cursor: pointer; scale: 50%;"
									on:click={() => {
										selectedApplicationList = selectedApplicationList.filter(
											(selectedApplication) =>
												selectedApplication.applicationName != app.applicationName
										);
									}}
								/>
							</li>
						</ul>
					</div>
				{/each}
			</div>
		{/if}

		{#if adminRoles}
			<div class="admin-roles">
				{#if noneditable}
					<Switch bind:checked={selectedGroupMembership.groupAdmin} />
				{:else}
					<Switch bind:checked={selectedIsGroupAdmin} />
				{/if}
				<h3>Group Admin</h3>
			</div>
			<div class="admin-roles">
				{#if noneditable}
					<Switch bind:checked={selectedGroupMembership.topicAdmin} />
				{:else}
					<Switch bind:checked={selectedIsTopicAdmin} />
				{/if}
				<h3>Topic Admin</h3>
			</div>
			<div class="admin-roles">
				{#if noneditable}
					<Switch bind:checked={selectedGroupMembership.applicationAdmin} />
				{:else}
					<Switch bind:checked={selectedIsApplicationAdmin} />
				{/if}
				<h3>Application Admin</h3>
			</div>
		{/if}
	</div>

	{#if actionAddUser}
		<hr />
		<button
			data-cy="button-add-user"
			class="action-button"
			class:action-button-invalid={invalidEmail || searchGroups?.length < 3}
			disabled={invalidEmail || searchGroups?.length < 3}
			on:click={() => actionAddUserEvent()}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					actionAddUserEvent();
				}
			}}
			>Add User
		</button>
	{/if}

	{#if actionAddSuperUser}
		<hr />
		<button
			data-cy="button-add-super-user"
			class="action-button"
			class:action-button-invalid={invalidEmail || emailValue.length < 10}
			on:click={() => {
				actionAddSuperUserEvent();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					actionAddSuperUserEvent();
				}
			}}
			>Add User
		</button>
	{/if}

	{#if actionAddTopic}
		<hr />
		<button
			data-cy="button-add-topic"
			class="action-button"
			class:action-button-invalid={newTopicName.length < minNameLength ||
				searchGroups?.length < minNameLength}
			on:click={() => {
				actionAddTopicEvent();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					actionAddTopicEvent();
				}
			}}
			>Add Topic
		</button>
	{/if}

	{#if actionDuplicateTopic}
		<hr />
		<button
			class="action-button"
			class:action-button-invalid={newTopicName.length < minNameLength ||
				searchGroups?.length < minNameLength}
			on:click={() => {
				actionDuplicateTopicEvent();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					actionDuplicateTopicEvent();
				}
			}}
			>Add Topic
		</button>
	{/if}

	{#if actionAddApplication}
		<hr />
		<button
			data-cy="button-add-application"
			class="action-button"
			class:action-button-invalid={appName?.length < minNameLength ||
				searchGroups?.length < minNameLength}
			disabled={appName?.length < minNameLength || searchGroups?.length < minNameLength}
			on:click={() => {
				actionAddApplicationEvent();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					actionAddApplicationEvent();
				}
			}}
			>Add Topic
		</button>
	{/if}

	{#if actionAddGroup}
		<hr />
		<button
			data-cy="button-add-group"
			class="action-button"
			class:action-button-invalid={newGroupName?.length < minNameLength}
			disabled={newGroupName?.length < minNameLength}
			on:click={() => {
				if (!newGroupName.length <= minNameLength) actionAddGroupEvent();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					if (!newGroupName.length <= minNameLength) actionAddGroupEvent();
				}
			}}
			>Add Group
		</button>
	{/if}

	{#if actionEditUser}
		<hr style="z-index: 1" />
		<button
			class="action-button"
			on:click={() => {
				actionEditUserEvent();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					actionEditUserEvent();
				}
			}}
			>Save Changes
		</button>
	{/if}

	{#if actionEditApplicationName}
		<hr style="z-index: 1" />
		<button
			class="action-button"
			class:action-button-invalid={previousAppName?.length < minNameLength}
			disabled={previousAppName?.length < minNameLength}
			on:click={() => {
				dispatch('saveNewAppName', { newAppName: previousAppName });
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('saveNewAppName', { newAppName: previousAppName });
				}
			}}
			>Save Changes
		</button>
	{/if}

	{#if actionDeleteUsers}
		<p style="margin: 0 1.7rem 1rem 2rem; font-size:0.9rem; font-stretch: condensed; ">
			Are you sure? This is not reversible.
		</p>
		<!-- svelte-ignore a11y-autofocus -->
		<button
			data-cy="delete-user"
			autofocus
			class="action-button"
			on:click={() => dispatch('deleteGroupMemberships')}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('deleteGroupMemberships');
				}
			}}>{title}</button
		>
	{/if}

	{#if actionDeleteSuperUsers}
		<p style="margin: 0 1.7rem 1rem 2rem; font-size:0.9rem; font-stretch: condensed; ">
			Are you sure? This is not reversible.
		</p>
		<!-- svelte-ignore a11y-autofocus -->
		<button
			data-cy="delete-super-user"
			autofocus
			class="action-button"
			on:click={() => dispatch('deleteSuperUsers')}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('deleteSuperUsers');
				}
			}}>{title}</button
		>
	{/if}

	{#if actionDeleteTopics}
		<p style="margin: 0 1.7rem 1rem 2rem; font-size:0.9rem; font-stretch: condensed; ">
			Are you sure? This is not reversible.
		</p>
		<!-- svelte-ignore a11y-autofocus -->
		<button
			data-cy="delete-topic"
			autofocus
			class="action-button"
			on:click={() => dispatch('deleteTopics')}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('deleteTopics');
				}
			}}>{title}</button
		>
	{/if}

	{#if actionDeleteApplications}
		<p style="margin: 0 1.7rem 1rem 2rem; font-size:0.9rem; font-stretch: condensed; ">
			Are you sure? This is not reversible.
		</p>
		<!-- svelte-ignore a11y-autofocus -->
		<button
			data-cy="delete-application"
			autofocus
			class="action-button"
			on:click={() => dispatch('deleteApplications')}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('deleteApplications');
				}
			}}>{title}</button
		>
	{/if}

	{#if actionDeleteGroups}
		<p style="margin: 0 1.7rem 1rem 2rem; font-size:0.9rem; font-stretch: condensed; ">
			Are you sure? This is not reversible.
		</p>
		<!-- svelte-ignore a11y-autofocus -->
		<button
			autofocus
			class="action-button"
			on:click={() => dispatch('deleteGroups')}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					dispatch('deleteGroups');
				}
			}}>{title}</button
		>
	{/if}

	<button
		class="action-button"
		on:click={() => {
			emailValue = '';
			closeModal();
		}}
		on:keydown={(event) => {
			if (event.which === returnKey) {
				emailValue = '';
				closeModal();
			}
		}}
		>{closeModalText}
	</button>
</div>

<style>
	.application-list {
		display: inline-flex;
		height: 2rem;
		width: 14rem;
		font-size: 0.7rem;
		margin-left: 0.1rem;
		margin: -0.5rem 0 0.7rem 0;
	}

	.application-list:first-of-type {
		margin-top: 1rem;
	}

	.action-button {
		float: right;
		margin: 0.8rem 1.5rem 1rem 0;
		background-color: transparent;
		border-width: 0;
		font-size: 0.85rem;
		font-weight: 500;
		color: #6750a4;
		cursor: pointer;
	}

	.action-button-invalid {
		color: grey;
	}

	.admin-roles {
		display: flex;
		align-items: center;
		width: 14rem;
	}

	.admin-roles:first-child {
		margin-top: 1.5rem;
	}

	.admin-roles h3 {
		margin-left: 1.5rem;
		text-indent: -0.2rem;
	}

	.modal-backdrop {
		position: fixed;
		top: 0;
		left: 0;
		width: 100%;
		height: 100vh;
		background: rgba(0, 0, 0, 0.25);
		z-index: 10;
	}

	.modal {
		position: fixed;
		top: 10vh;
		left: 50%;
		margin-left: -10.25rem;
		width: 18.6rem;
		max-height: fit-content;
		background: rgb(246, 246, 246);
		border-radius: 15px;
		z-index: 100;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.26);
	}

	.content {
		padding-bottom: 1rem;
		margin-top: 1rem;
		margin-left: 2.3rem;
		width: 14rem;
	}

	.content input {
		width: 11rem;
		border-width: 1px;
	}

	input.searchbox {
		margin-top: 1rem;
		width: 14rem;
	}

	.error-message {
		font-size: 0.7rem;
	}

	input.searchbox:disabled {
		border-color: rgba(118, 118, 118, 0.3);
	}

	img.close-button {
		transform: scale(0.25, 0.35);
	}

	.close-button {
		background-color: transparent;
		position: absolute;
		padding-right: 1rem;
		color: black;
		border: none;
		height: 50px;
		width: 60px;
		border-radius: 0%;
		cursor: pointer;
	}

	h2 {
		margin-top: 3rem;
		margin-left: 2rem;
	}

	hr {
		width: 79%;
		border-color: rgba(0, 0, 0, 0.07);
	}

	input[type='checkbox'] {
		height: 0.7rem;
	}
</style>
