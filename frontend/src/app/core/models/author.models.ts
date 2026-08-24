export interface AuthorResponse {
  openLibraryId: string;
  name: string;
  bio: string | null;
  birthDate: string | null;
  deathDate: string | null;
  photoUrl: string | null;
  topSubjects: string[] | null;
  workCount: number | null;
}

export interface WorkEntry {
  key: string;
  title: string;
  covers: number[] | null;
}

export interface WorksResponse {
  size: number;
  entries: WorkEntry[] | null;
}
