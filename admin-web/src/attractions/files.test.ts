import { describe, expect, it } from 'vitest'
import { scaledSize } from './files.ts'

describe('scaledSize', () => {
  it.each([
    [4000, 3000, 1600, 1200],
    [3000, 4000, 1200, 1600],
    [1600, 900, 1600, 900],
    [800, 600, 800, 600],
  ])('fits %ix%i within 1600 pixels as %ix%i', (width, height, expectedWidth, expectedHeight) => {
    expect(scaledSize(width, height)).toEqual({ width: expectedWidth, height: expectedHeight })
  })
})
