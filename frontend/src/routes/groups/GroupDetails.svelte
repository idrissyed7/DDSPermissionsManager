<script>
	import { createEventDispatcher, onMount } from 'svelte';
	import { isAdmin, isAuthenticated } from '../../stores/authentication';
	import permissionsByGroup from '../../stores/permissionsByGroup';
	import headerTitle from '../../stores/headerTitle';
	import detailView from '../../stores/detailView';
	import Modal from '../../lib/Modal.svelte';
	import editSVG from '../../icons/edit.svg';
	import { httpAdapter } from '../../appconfig';
	import messages from '$lib/messages.json';

	const dispatch = createEventDispatcher();

	export let group;

	$: if (group) isPublic = group.public;

	let descriptionSelector;
	let isPublic = group.public;
	let editGroupVisible = false;

	headerTitle.set(group.name);
	detailView.set(true);

	$: if ($detailView === 'backToList') dispatch('groupList');

	onMount(() => {
		// Adjust style when there is no description
		if (!group.description) descriptionSelector.style.marginLeft = '0.27rem';
	});

	const editGroup = async (id, name, description, isPublic) => {
		const res = await httpAdapter.post(`/groups/save`, {
			id: id,
			name: name,
			description: description,
			public: isPublic
		});

		group = res.data;
		headerTitle.set(group.name);

		dispatch('reload');
	};
</script>

{#if $isAuthenticated}
	<div style="width: 100%; min-width: 32rem; margin-right: 1rem">
		{#if editGroupVisible}
			<Modal
				title={messages['group']['edit.title']}
				actionEditGroup={true}
				groupCurrentName={group.name}
				groupCurrentDescription={group.description}
				groupCurrentPublic={group.public}
				groupNewName={true}
				groupId={group.id}
				on:addGroup={(e) =>
					editGroup(
						e.detail.groupId,
						e.detail.newGroupName,
						e.detail.newGroupDescription,
						e.detail.newGroupPublic
					)}
				on:cancel={() => (editGroupVisible = false)}
			/>
		{/if}
		<div style="min-width: 43.5rem">
			<div style="width: fit-content">
				<div style="margin-top: 1.7rem">
					<span style="font-size: 1.1rem; font-weight: 300; display: inline-flex; width: 7.2rem">
						{messages['group.detail']['row.one']}
					</span>
					<span style="font-size: 1.3rem; font-weight: 500">{group.name} </span>

					{#if $isAdmin || $permissionsByGroup.find((permission) => permission.groupId === group.id && permission.isGroupAdmin)}
						<!-- svelte-ignore a11y-click-events-have-key-events -->
						<img
							src={editSVG}
							alt="edit group"
							width="18rem"
							style="margin-left: 1.5rem; float: right; cursor: pointer"
							on:click={() => (editGroupVisible = true)}
						/>
					{/if}
				</div>
				<div style="margin-top: 0.5rem; width:fit-content">
					<span
						style="font-weight: 300; font-size: 1.1rem; margin-right: 1rem; display: inline-flex; width: 6.2rem;"
					>
						{messages['group.detail']['row.two']}
					</span>
					<span style="font-weight: 400; font-size: 1.1rem" bind:this={descriptionSelector}
						>{group.description ? group.description : '-'}</span
					>
				</div>
				<div style="font-size: 1.1rem; margin-top: 0.5rem; width: fit-content">
					<span style="font-weight: 300; vertical-align: 1rem">
						{messages['group.detail']['row.three']}
					</span>
					<input
						type="checkbox"
						style="vertical-align: 1rem; margin-left: 4.1rem; width: 15px; height: 15px"
						bind:checked={isPublic}
						on:change={() => (isPublic = group.public)}
					/>
				</div>
				<p style="margin-top: 8rem">{messages['footer']['message']}</p>
			</div>
		</div>
	</div>
{/if}
