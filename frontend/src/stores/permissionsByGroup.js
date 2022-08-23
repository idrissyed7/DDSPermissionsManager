import { writable } from 'svelte/store';

const permissionsByGroup = writable(null);

export default permissionsByGroup;