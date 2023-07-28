// Copyright 2023 DDS Permissions Manager Authors
import { writable } from 'svelte/store';

const permissionsByGroup = writable(null);

export default permissionsByGroup;