"use client"
import CardSize from '@/types/ControllerTypes/CardSize';

/**
 * Interface to explain some top level utilities for the ScreenSize Service.
 */
interface ScreenUtils {
  /**
   *
   * The idea here is we want to know the point in which when the user scrolls that we should then 
   * page for more content.
   
   * @param height the max Y of inner screen. This will grow as the more things loaded into view.
   * @param width the max X of the screen. This should stay 
   *   mostly consistent unless the user chooses to resize the window. 
   *   We will recalculate to determine how many new things we can fit. 
  */
  calculateBufferY(height: number, width: number): number;
}

const unsetCardsize: CardSize = {
  height: -1,
  width: -1
}

export class ScreenSize implements ScreenUtils {

  private _cardsize: CardSize;
  private _yPos: number;
  private _maxHeight: number;
  private _lastBuffer: number;
  private _pageHeight: number;
  private _pageWidth: number;
  private _pageItemSize: number;

  constructor() {
    this._cardsize = unsetCardsize
    this._yPos = 0;
    this._maxHeight = 0;
    this._lastBuffer = 0;
    this._pageHeight = 0;
    this._pageWidth = 0;

    // borrowed from https://www.webdevtutor.net/blog/typescript-window-is-not-defined
    if (typeof window !== 'undefined') {
      (window as any).addEventListener('load', () => {
        console.log("loaded")
        this._pageHeight = window.innerHeight;
        this._pageWidth = window.innerWidth;
        this._maxHeight = window.innerHeight;

        console.log("constructor", this.pageHeight)
      });
    };

    this._pageItemSize = 0;
  }

  public get lastBuffer(): number {
    return this._lastBuffer;
  }
  public set lastBuffer(value: number) {
    this._lastBuffer = value;
  }

  public get cardsize(): CardSize {
    return this._cardsize;
  }

  public set cardsize(value: CardSize) {
    this._cardsize = value;
  }

  public get yPos(): number {
    return this._yPos;
  }
  public set yPos(value: number) {
    this._yPos = value;
  }

  public get maxHeight(): number {
    return this._maxHeight;
  }

  public set maxHeight(value: number) {
    this._maxHeight = value;
  }

  public get pageHeight(): number {
    return this._pageHeight;
  }
  public set pageHeight(value: number) {
    this._pageHeight = value;
  }

  public get pageItemSize(): number {
    return this._pageItemSize;
  }
  public set pageItemSize(value: number) {
    this._pageItemSize = value;
  }
  public get pageWidth(): number {
    return this._pageWidth;
  }
  public set pageWidth(value: number) {
    this._pageWidth = value;
  }

  setCardSize(height: number, width: number) {
    this._cardsize = {
      height: height,
      width: width
    };
  }

  public calculatePageItemSize() {
    if (!(this.pageHeight && this.pageWidth)) {
      throw new Error("Page demensions are not set");
    }

    const ch = this.cardsize.height;
    const cw = this.cardsize.width;

    const rowCards = Math.floor(this.pageHeight / ch);
    const colCards = Math.floor(this.pageWidth / cw);

    this.pageItemSize = rowCards * colCards;
  }

  public calculateBufferY(height: number, width: number = 0) {

    // Error gaurds at start of the function, preventing wasted and wrong compute.
    if (this._cardsize == unsetCardsize) {
      // edge case the cardsize is not set yet.
      throw new Error("Cardsize should be set.");
    }

    if (!this.pageHeight) {
      throw new Error("The height is not set.");
    }

    if (!this.maxHeight) {
      throw new Error("The maxHeight is not set.");
    }

    const ch = this.cardsize.height;
    const rowCards = Math.floor(this.maxHeight / ch);
    if (width) {
      // convert cards to pixel space.
      this.lastBuffer = rowCards * this.cardsize.height / 3;
      return this.lastBuffer;
    }

    if (this.lastBuffer && this.yPos > this.lastBuffer) {
      return this.lastBuffer + height;
    }

    if (!this.lastBuffer) {
      // edge case small screen.
      if (rowCards == 1) {
        return this.cardsize.height;
      }
      // The last buffer is unset, we're setting the
      // initial buffer.
      this.lastBuffer = rowCards * this.cardsize.height / 3;
    }
    return this.lastBuffer;
  }

}

// Anyone who just imports from this class will get the singleton ScreenSize.
export default new ScreenSize();
