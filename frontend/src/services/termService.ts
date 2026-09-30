import api from './api';
import type { CreateTermRequest, Term } from '../types';
import type { AxiosResponse } from 'axios';

const termService = {
  // Every term, newest first. Readable by any logged-in user.
  list: (): Promise<AxiosResponse<Term[]>> =>
    api.get<Term[]>('/terms'),

  // ADMIN only.
  create: (data: CreateTermRequest): Promise<AxiosResponse<Term>> =>
    api.post<Term>('/terms', data),
};

export default termService;
