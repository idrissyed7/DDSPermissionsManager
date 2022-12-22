import { writable } from 'svelte/store';

const errorMessageAssociation = writable(null);

export default errorMessageAssociation;