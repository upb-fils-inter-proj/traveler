/** Longest side of an uploaded photo, in pixels. */
export const MAX_PHOTO_SIDE = 1600
export const WEBP_QUALITY = 0.85
/** The admin API refuses larger photos. */
export const MAX_PHOTO_BYTES = 5 * 1024 * 1024

export class PhotoConversionError extends Error {
  readonly reason: 'unreadable' | 'webpUnsupported'

  constructor(reason: 'unreadable' | 'webpUnsupported') {
    super(`Photo conversion failed: ${reason}`)
    this.name = 'PhotoConversionError'
    this.reason = reason
  }
}

/** Scales a size down so the longer side is at most `maxSide`, never up. */
export function scaledSize(
  width: number,
  height: number,
  maxSide = MAX_PHOTO_SIDE,
): { width: number; height: number } {
  const longer = Math.max(width, height)
  if (longer <= maxSide) return { width, height }
  const scale = maxSide / longer
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale)),
  }
}

/** Decodes an image file, scales it down and encodes it as WebP in the browser. */
export async function convertToWebp(file: Blob): Promise<Blob> {
  let bitmap: ImageBitmap
  try {
    bitmap = await createImageBitmap(file)
  } catch {
    throw new PhotoConversionError('unreadable')
  }

  try {
    const { width, height } = scaledSize(bitmap.width, bitmap.height)
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const context = canvas.getContext('2d')
    if (context === null) throw new PhotoConversionError('webpUnsupported')
    context.imageSmoothingQuality = 'high'
    context.drawImage(bitmap, 0, 0, width, height)

    const blob = await new Promise<Blob | null>((resolve) => {
      canvas.toBlob(resolve, 'image/webp', WEBP_QUALITY)
    })
    // Browsers that cannot encode WebP silently fall back to PNG.
    if (blob === null || blob.type !== 'image/webp') {
      throw new PhotoConversionError('webpUnsupported')
    }
    return blob
  } finally {
    bitmap.close()
  }
}

/** Saves a downloaded file under the given name. */
export function saveFile(file: Blob, fileName: string) {
  const url = URL.createObjectURL(file)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.append(link)
  link.click()
  link.remove()
  // Give the browser time to start the download before the URL goes away.
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
}
