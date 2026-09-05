import viewService from "./view.service";

interface ScreenUtils {
  calculateBuffer(y0: number, y1: number): number;
}

interface CardSize {
  height: number;
  width: number;
}

export class ScreenSize implements ScreenUtils {

  private cardsize: CardSize;

  constructor() {
    this.cardsize = {
      height: -1,
      width: -1
    }
  }

  setCardSize(height: number, width: number) {
    this.cardsize = {
      height: height,
      width: width
    };
  }

  getCardSize(): CardSize {
    return this.cardsize;
  }


  public calculateBuffer(height: number, width: number) {
    console.log(height);
    return height
  }

}

// Anyone who just imports from this class will get the singleton ScreenSize.
export default new ScreenSize();
