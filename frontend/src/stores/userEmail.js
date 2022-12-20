import { writable } from 'svelte/store';

const userEmail = writable(null);

export default userEmail;