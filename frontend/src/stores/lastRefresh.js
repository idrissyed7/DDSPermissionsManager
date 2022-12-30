import { writable } from 'svelte/store';

const lastRefresh = writable(null);

export default lastRefresh;