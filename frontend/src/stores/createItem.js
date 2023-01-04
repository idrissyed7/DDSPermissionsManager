import { writable } from 'svelte/store';

const createItem = writable(null);

export default createItem;