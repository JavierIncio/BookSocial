export type Link = {
  title: string;
  url: string;
};
export type WorkEntry = {
  key: string;
  title: string;
  covers: number[] | null;
  authors: AuthorRole[] | null;
};
export type AuthorRole = { author: AuthorRef };
export type AuthorRef = { key: string };

export interface AuthorDetailResponse {
  key: string;
  name: string;
  bio: string | null;
  birthDate: string | null;
  deathDate: string | null;
  photos: string[] | null;
  links: Link[] | null;
  alternateNames: string[] | null;
}

export interface WorksResponse {
  size: number;
  entries: WorkEntry[];
}
