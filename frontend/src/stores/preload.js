import { writable } from 'svelte/store';

const preload = writable(null);

preload.set('groups');

export default preload;