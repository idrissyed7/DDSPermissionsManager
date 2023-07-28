// Copyright 2023 DDS Permissions Manager Authors
import { writable } from 'svelte/store';

const permissionsForAllGroups = writable(null);

export default permissionsForAllGroups;