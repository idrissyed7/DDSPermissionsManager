<script>
	import { onMount } from 'svelte';
	import headerTitle from '../../stores/headerTitle';

	export let data, errors;

	let universalSearchString, selectedUniversalSearchButton;

	// Constants
	const returnKey = 13;

	// Selection Options
	const UniversalSearchOption = {
		EVERYTHING: 'everything',
		GROUPS: 'groups',
		TOPICS: 'topics',
		APPLICATIONS: 'applications'
	};

	onMount(() => {
		headerTitle.set('Search');
	});
</script>

<svelte:head>
	<title>Universal Search | DDS Permissions Manager</title>
	<meta name="description" content="DDS Permissions Manager Search" />
</svelte:head>

<div class="content">
	<h1>Universal Search</h1>

	<form class="searchbox">
		<input
			data-cy="search-applications-table"
			class="searchbox"
			type="search"
			style="width: 25rem"
			placeholder="Search any Group, Topic or Application"
			bind:value={universalSearchString}
			on:blur={() => {
				universalSearchString = universalSearchString?.trim();
			}}
			on:keydown={(event) => {
				if (event.which === returnKey) {
					document.activeElement.blur();
					universalSearchString = universalSearchString?.trim();
				}
			}}
		/>
	</form>

	<div class="controls">
		<button
			class="button-universal-search"
			class:button-universal-search-selected={selectedUniversalSearchButton ===
				UniversalSearchOption.EVERYTHING}
			style="border-radius: 25px 0 0 25px; padding: 0 2rem 0 1.2rem"
			on:click={() => (selectedUniversalSearchButton = UniversalSearchOption.EVERYTHING)}
		>
			<span
				class:invisible={selectedUniversalSearchButton != UniversalSearchOption.EVERYTHING}
				style="margin-right: 0.5rem"
			>
				✓
			</span>
			Everything
		</button>

		<button
			class="button-universal-search"
			class:button-universal-search-selected={selectedUniversalSearchButton ===
				UniversalSearchOption.GROUPS}
			style="padding: 0 2.5rem 0 1rem"
			on:click={() => (selectedUniversalSearchButton = UniversalSearchOption.GROUPS)}
		>
			<span
				class:invisible={selectedUniversalSearchButton != UniversalSearchOption.GROUPS}
				style="margin-right: 0.5rem"
			>
				✓
			</span>
			Groups
		</button>

		<button
			class="button-universal-search"
			class:button-universal-search-selected={selectedUniversalSearchButton ===
				UniversalSearchOption.TOPICS}
			style="padding: 0 2.5rem 0 1rem"
			on:click={() => (selectedUniversalSearchButton = UniversalSearchOption.TOPICS)}
		>
			<span
				class:invisible={selectedUniversalSearchButton != UniversalSearchOption.TOPICS}
				style="margin-right: 0.5rem"
			>
				✓
			</span>
			Topics
		</button>

		<button
			class="button-universal-search"
			class:button-universal-search-selected={selectedUniversalSearchButton ===
				UniversalSearchOption.APPLICATIONS}
			style="border-radius: 0 25px 25px 0; padding: 0 2rem 0 1.2rem"
			on:click={() => (selectedUniversalSearchButton = UniversalSearchOption.APPLICATIONS)}
		>
			<span
				class:invisible={selectedUniversalSearchButton != UniversalSearchOption.APPLICATIONS}
				style="margin-right: 0.5rem"
			>
				✓
			</span>
			Applications
		</button>
	</div>

	<table class="universal-search-table">
		<thead>
			<tr>
				<td style="min-width: 5rem">Type</td>
				<td style="min-width: 5rem">Group</td>
				<td style="min-width: 5rem">Identifier</td>
				<td style="min-width: 5rem">Description</td>
			</tr>
		</thead>
	</table>
</div>

<style>
	.content {
		width: 100vw;
	}

	.controls {
		margin-top: 2rem;
		width: fit-content;
	}

	.universal-search-table {
		margin-top: 2rem;
	}
</style>
