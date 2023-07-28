// Copyright 2023 DDS Permissions Manager Authors
import { writable } from 'svelte/store';

const nonEmptyInputField = writable(false);

export default nonEmptyInputField;
