import { writable, derived } from 'svelte/store';

const writableProfileStore = writable(null);

export const onLoggedIn = (profile) => writableProfileStore.set(profile);
export const isAuthenticated = derived(writableProfileStore, ($profile) => Boolean($profile?.sub));
export const isAdmin = derived(writableProfileStore, ($profile) => Boolean($profile?.isAdmin));
