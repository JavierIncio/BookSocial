export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  age: number | null;
  roles: string[];
}
