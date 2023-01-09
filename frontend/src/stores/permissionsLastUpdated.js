import { writable } from 'svelte/store';

const permissionsLastUpdated = writable(null);

export default permissionsLastUpdated;