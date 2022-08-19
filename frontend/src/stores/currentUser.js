import { writable } from 'svelte/store';

const currentUser = writable(null);

export default currentUser;