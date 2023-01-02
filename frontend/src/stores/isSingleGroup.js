import { writable } from 'svelte/store';

const isSingleGroup = writable(null);
isSingleGroup.set(false);

export default isSingleGroup;