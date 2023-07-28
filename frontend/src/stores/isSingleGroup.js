// Copyright 2023 DDS Permissions Manager Authors
import { writable } from 'svelte/store';

const isSingleGroup = writable(null);
isSingleGroup.set(false);

export default isSingleGroup;