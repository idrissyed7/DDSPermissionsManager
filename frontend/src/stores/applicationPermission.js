import { writable } from 'svelte/store';

const applicationPermission = writable(null);

export default applicationPermission;