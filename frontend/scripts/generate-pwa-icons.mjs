// Regenerate the PWA icon PNGs in public/ from public/favicon.svg.
// Run with: node scripts/generate-pwa-icons.mjs
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import sharp from 'sharp'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const publicDir = path.join(scriptDir, '..', 'public')
const svgPath = path.join(publicDir, 'favicon.svg')

const svg = await readFile(svgPath)

async function writeSquareIcon(outFile, size, { padding = 0, background = { r: 0, g: 0, b: 0, alpha: 0 } } = {}) {
  const logoSize = size - padding * 2
  const logo = await sharp(svg)
    .resize(logoSize, logoSize, { fit: 'contain', background: { r: 0, g: 0, b: 0, alpha: 0 } })
    .toBuffer()
  await sharp({
    create: { width: size, height: size, channels: 4, background },
  })
    .composite([{ input: logo, gravity: 'center' }])
    .png()
    .toFile(path.join(publicDir, outFile))
  console.log(`wrote ${outFile}`)
}

// Standard "any" icons: transparent background, logo fills the canvas.
await writeSquareIcon('pwa-192x192.png', 192)
await writeSquareIcon('pwa-512x512.png', 512)

// Maskable icon: logo must sit inside the ~80% "safe zone" on a solid
// background, since platforms crop the icon to arbitrary shapes.
await writeSquareIcon('pwa-512x512-maskable.png', 512, {
  padding: 51, // ~10% margin per side -> logo occupies the 80% safe zone
  background: { r: 255, g: 255, b: 255, alpha: 1 },
})

// iOS home screen icon: no transparency support, so flatten onto white.
await writeSquareIcon('apple-touch-icon.png', 180, {
  padding: 18,
  background: { r: 255, g: 255, b: 255, alpha: 1 },
})
