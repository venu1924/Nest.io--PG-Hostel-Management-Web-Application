import api from './api';
import { persistSession } from './authStorage';

export async function createPg(payload) {
  const { data } = await api.post('/auth/register', payload);
  persistSession(data.token, data.admin);
  return data;
}
