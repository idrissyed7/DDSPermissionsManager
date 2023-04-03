import { writable } from 'svelte/store';

const searchResults = writable(null);

export default searchResults;