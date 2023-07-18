import { writable } from 'svelte/store';

const userValidityCheck = writable(false);

export default userValidityCheck;
