<script>
	import { fade, fly } from 'svelte/transition';
	import { createEventDispatcher } from 'svelte';

	export let title;

	const dispatch = createEventDispatcher();

	function closeModal() {
		dispatch('cancel');
	}
</script>

<div class="modal-backdrop" on:click={closeModal} transition:fade />
<div class="modal" transition:fly={{ y: 300 }}>
	<button class="close-button" on:click={closeModal}>X</button>
	<h1>{title}</h1>
	<hr />
	<div class="content">
		<slot />
	</div>
</div>

<style>
	.modal-backdrop {
		position: fixed;
		top: 0;
		left: 0;
		width: 100%;
		height: 100vh;
		background: rgba(0, 0, 0, 0.75);
		z-index: 10;
	}

	.modal {
		position: fixed;
		top: 10vh;
		max-height: fit-content;
		min-width: 710px;
		max-width: 50vw;
		left: calc(50% - 355px);
		background: white;
		border-radius: 15px;
		z-index: 100;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.26);
		overflow: scroll;
	}

	.content {
		padding-bottom: 2rem;
		margin-top: 2rem;
		text-align: center;
	}

	.close-button {
		position: absolute;
		padding-left: 1.2rem;
		padding-right: 1rem;
		color: #ffffff;
		background-color: coral;
		border: none;
		margin-left: -0.15rem;
		height: 50px;
		width: 60px;
		border-radius: 0%;
	}

	.close-button:hover {
		transition: 0.5s;
		background-color: rgb(223, 98, 53);
	}

	h1 {
		padding: 0.1rem;
		margin: 0.5rem;
		font-size: 20pt;
	}

	hr {
		background: linear-gradient(
			to right,
			rgba(0, 0, 0, 0),
			rgba(0, 0, 0, 0),
			rgba(58, 32, 81, 0.3),
			rgba(0, 0, 0, 0),
			rgba(0, 0, 0, 0)
		);
		height: 1px;
		border: none;
	}
</style>
