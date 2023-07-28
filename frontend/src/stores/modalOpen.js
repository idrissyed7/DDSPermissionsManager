// Copyright 2023 DDS Permissions Manager Authors
import { writable } from 'svelte/store';

const modalOpen = writable(false);

export default modalOpen;