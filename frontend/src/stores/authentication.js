import { writable, derived } from 'svelte/store';

const writableProfileStore = writable(null);

export const onLoggedIn = (profile) => writableProfileStore.set(profile);
export const isAuthenticated = derived(writableProfileStore, ($profile) => Boolean($profile?.name));
export let isAdmin = derived(writableProfileStore, ($profile) => Boolean($profile?.roles?.some(role => role === "ADMIN")));