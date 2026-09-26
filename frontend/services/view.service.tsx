import Bookmark from "@type/Bookmarks/Bookmark";
import authService from "./auth.service";
import api from "@api/Api";

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

  private _inView: Bookmark[] = [];

  public get inView(): Bookmark[] {
    return this._inView;
  }
  public set inView(value: Bookmark[]) {
    this._inView = value;
  }
  private _allRetrieved: Bookmark[] = [];

  public get allRetrieved(): Bookmark[] {
    return this._allRetrieved;
  }
  public set allRetrieved(value: Bookmark[]) {
    this._allRetrieved = value;
  }


  constructor() {
    console.log('ViewService loaded')
    if (authService.getAuthorized()) {
      console.log('constructing view service')
      api.getAllBookmarks().then((resp) => {
        console.log(resp)
        // dispatch({ type: "add", bookmarks: resp.data as Bookmark[] });
        // setIsLoading(false);
      });
    }
  }


}

export default new ViewService();
