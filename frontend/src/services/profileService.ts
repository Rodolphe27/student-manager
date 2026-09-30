import api from './api';
import type { Profile, UpdateProfileRequest, ChangePasswordRequest } from '../types';
import type { AxiosResponse } from 'axios';

// Every call acts on the logged-in user (taken from the session server-side) — there
// is deliberately no id parameter.
const profileService = {
  get: (): Promise<AxiosResponse<Profile>> =>
    api.get<Profile>('/profile'),

  update: (data: UpdateProfileRequest): Promise<AxiosResponse<Profile>> =>
    api.put<Profile>('/profile', data),

  changePassword: (data: ChangePasswordRequest): Promise<AxiosResponse<void>> =>
    api.put('/profile/password', data),
};

export default profileService;
