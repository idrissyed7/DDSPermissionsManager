import { writable } from 'svelte/store';

const users = writable(null);

export default users;