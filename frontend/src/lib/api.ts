const API_BASE = '/api';

export const getAuthToken = () => localStorage.getItem('liftoff_token');
export const setAuthToken = (token: string) => localStorage.setItem('liftoff_token', token);
export const removeAuthToken = () => localStorage.removeItem('liftoff_token');

export async function fetchApi(endpoint: string, options: RequestInit = {}) {
  const token = getAuthToken();
  
  const headers = new Headers(options.headers || {});
  
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      removeAuthToken();
      window.location.href = '/login';
      throw new Error('Authentication expired. Please log in again.');
    }

    let errorMessage = 'An error occurred';
    try {
      const errorData = await response.json();
      errorMessage = errorData.message || errorMessage;
    } catch (e) {
      errorMessage = response.statusText;
    }
    throw new Error(errorMessage);
  }

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}
