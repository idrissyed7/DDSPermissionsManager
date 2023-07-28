<!-- Copyright 2023 DDS Permissions Manager Authors-->
<script>
	export let checked = false;
</script>

<label class="switch">
	<input type="checkbox" bind:checked />
	<span class="slider" />
</label>

<style>
	.switch {
		position: relative;
		display: inline-block;
		width: 60px;
		height: 36px;
		scale: 95%;
	}

	.switch input {
		opacity: 0;
		width: 0;
		height: 0;
	}

	.slider {
		position: absolute;
		cursor: pointer;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		background-color: #ccc;
		-webkit-transition: 0.4s;
		transition: 0.4s;
		border-radius: 34px;
		border-style: solid;
		border-color: rgb(150, 150, 150);
		border-width: 3px;
	}

	.slider:before {
		position: absolute;
		content: url('../icons/false.svg');
		height: 26px;
		width: 26px;
		left: 3px;
		top: 2px;
		bottom: 4px;
		background-color: rgb(150, 150, 150);
		-webkit-transition: 0.4s;
		transition: 0.4s;
		border-radius: 50%;
	}

	input:checked + .slider {
		background-color: #6750a4;
		box-shadow: 0 0 1px #6750a4;
		border-color: transparent;
	}

	input:checked + .slider:before {
		-webkit-transform: translateX(26px);
		-ms-transform: translateX(26px);
		transform: translateX(24px);
		background-color: white;
		content: url('../icons/true.svg');
	}
</style>
