import { writable } from 'svelte/store';

const lastActivity = writable(null);

export default lastActivity;