import { writable } from 'svelte/store';

const contextMessage = writable(null);

export default contextMessage;