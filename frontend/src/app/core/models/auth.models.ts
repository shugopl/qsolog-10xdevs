export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
}

export interface UserResponse {
  id: string;
  email: string;
  username: string;
  role: string;
}
