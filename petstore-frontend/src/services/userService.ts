import { User, UserRegistrationPayload } from '../auth/types';

/**
 * Service managing user registration, authentication, and profiles
 * against the modern microservice backend.
 */
class UserApiService {
  private readonly baseUrl = '/api/v1/users';

  async register(payload: UserRegistrationPayload): Promise<User> {
    const res = await fetch(`${this.baseUrl}/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const errorMsg = await res.text();
      throw new Error(errorMsg || `Registration failed with status ${res.status}`);
    }
    const data = await res.json();
    return {
      username: data.username,
      name: data.name,
      email: data.email,
      role: data.role as any,
      token: data.token,
    };
  }

  async login(username: string, password?: string): Promise<User> {
    const res = await fetch(`${this.baseUrl}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password: password || '' }),
    });
    if (!res.ok) {
      const errorMsg = await res.text();
      throw new Error(errorMsg || `Authentication failed with status ${res.status}`);
    }
    const data = await res.json();
    return {
      username: data.username,
      name: data.name,
      email: data.email,
      role: data.role as any,
      token: data.token,
    };
  }

  async getUser(username: string): Promise<any> {
    const res = await fetch(`${this.baseUrl}/${encodeURIComponent(username)}`);
    if (!res.ok) {
      throw new Error(`User lookup failed with status ${res.status}`);
    }
    return res.json();
  }
}

export const userService = new UserApiService();
