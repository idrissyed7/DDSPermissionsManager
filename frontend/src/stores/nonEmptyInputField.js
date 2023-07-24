import { writable } from 'svelte/store';

const nonEmptyInputField = writable(false);

export default nonEmptyInputField;
