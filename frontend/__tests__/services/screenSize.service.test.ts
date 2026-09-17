import { expect, test } from 'vitest'
import { ScreenSize as screen } from '@services/screenSize.service'

const initPgWidth = 800;
const initPgHeight = 800;


test('Card size not set: no Y buffer', () => {
  const s = new screen();
  expect(() => s.calculateBufferY(initPgHeight, initPgWidth) == 0);
})

test('Calculate initial Y  buffer position test', () => {

  const s = new screen();
  // make assumption on card size. 
  // we can use a factitious number for the cards. Lets assume 200 width and 200 height;
  s.setCardSize(200, 200);
  s.maxHeight = 1200;
  const bufferHeight = s.calculateBufferY(initPgHeight, initPgWidth);
  expect(bufferHeight, 'Should be 2/3 of page height').to.eq(initPgHeight * (2 / 3))

})

test('Calculate Initial Page Size', () => {
  const s = new screen();
  s.pageHeight = 700;
  s.pageWidth = 700;
  s.setCardSize(200, 200);
  s.calculatePageItemSize();
  // should have 3 colums, and 3 rows of cards.
  expect(s.pageItemSize).eq(9);
});
