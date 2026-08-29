export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  age: number | null;
  roles: string[];
}

export interface ProfileResponse {
  userId: number;
  email: string;
  displayName: string;
  bio: string | null;
  location: string | null;
  avatarUrl: string | null;
  followersCount: number;
  followingCount: number;
  postsCount: number;
  createdAt: string;
  updatedAt: string;
}
