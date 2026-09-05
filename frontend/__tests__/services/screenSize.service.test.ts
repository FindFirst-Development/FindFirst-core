import { expect, test } from 'vitest'
import { ScreenSize as screen } from '@services/screenSize.service'



test('calculateBuffer test', () => {

  const width = 800;
  const height = 800;

  const s = new screen();

  // make assumption on card size. 
  // we can use a factitious number for the cards. Lets assume 200 width and 200 height;
  const bufferHeight = s.calculateBuffer(height, width);
  s.setCardSize(200, 200);

})
