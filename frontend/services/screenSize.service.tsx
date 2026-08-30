import viewService from "./view.service";

interface ScreenUtils {
  calculateBuffer(y0: number, y1: number): number;
}

class ScreenSize implements ScreenUtils {
  calculateBuffer(height: number, width: number) {
    console.log(height);
    return height
  }

}

export default new ScreenSize();
