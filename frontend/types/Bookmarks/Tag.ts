import Bookmark from "./Bookmark";
export const UNTAGGED = "untagged"

export default interface Tag {
  id: number;
  title: string;
}

export interface TagReqPayload {
  id: number;
  title: string;
  bookmarks: Bookmark[];
}

// Tag with count associated bookmarks.
export interface TagWithCnt {
  id?: number;
  title: string;
  associatedBkmks: Bookmark[];
  count: number;
}
