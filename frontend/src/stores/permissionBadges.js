// Copyright 2023 DDS Permissions Manager Authors
import { writable } from 'svelte/store';

const permissionBadges = writable(null);

export default permissionBadges;