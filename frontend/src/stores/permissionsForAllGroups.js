import { writable } from 'svelte/store';

const permissionsForAllGroups = writable(null);

export default permissionsForAllGroups;