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
  const rowCards = Math.floor(s.maxHeight / 200);
  const bufferHeight = s.calculateBufferY(initPgHeight);
  expect(bufferHeight, 'Should be 1/3 of page height').to.eq(rowCards * s.cardsize.height / 3);

})

test('Calculate Initial Page Size', () => {
  const s = new screen();
  s.pageHeight = 700;
  s.pageWidth = 700;
  s.maxHeight = 700;
  s.setCardSize(200, 200);
  s.calculatePageItemSize();
  // should have 3 colums, and 3 rows of cards.
  expect(s.pageItemSize).eq(9);
});

test('Calculate resize event: leaving bookmark size fixed for maths', () => {
  const s = new screen();
  s.pageHeight = 700;
  s.pageWidth = 700;
  s.maxHeight = 700;
  s.setCardSize(200, 200);
  s.calculatePageItemSize();

  // lets use the page size as our standard here. 
  const nmbk = s.pageItemSize;

  console.log(nmbk)
  let bufferHeight = s.calculateBufferY(initPgHeight);
  const rowCards = Math.floor(s.maxHeight / 200);
  expect(bufferHeight, 'Should be roughly 2/3 of page height').to.eq(rowCards * s.cardsize.height / 3);
  // simulate resize:
  s.pageWidth = 500;
  s.maxHeight = 1000;
  bufferHeight = s.calculateBufferY(initPgHeight, 500);
  expect(bufferHeight, 'The new page should be 5 rows of cards, 200 pxs per row = 1000 / 3').to.eq(1000 / 3);
});

test('Handling errors', () => {
  const s = new screen();
  let bufferHeight = s.calculateBufferY(initPgHeight);


})
