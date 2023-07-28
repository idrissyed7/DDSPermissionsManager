// Copyright 2023 DDS Permissions Manager Authors
import axios from 'axios';

axios.defaults.baseURL = import.meta.env.VITE_BACKEND_URL;
const httpAdapter = axios.create({ withCredentials: true});

export { httpAdapter }