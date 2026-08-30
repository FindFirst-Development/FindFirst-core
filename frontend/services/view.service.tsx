import Bookmark from "@type/Bookmarks/Bookmark";

interface SearchFunctions {

  getNextPage(): Bookmark[];

}


/**
  * Designed to be the query/state manager.
  * A feature complete ViewService should be able
  * to handle the bounding box dimensions
  * and resize events.
  *
  * Fetching content when the user scrolls to a portion of bounding box.
  */
class ViewService {

  private inView: Bookmark[] = [];



}

export default new ViewService();
