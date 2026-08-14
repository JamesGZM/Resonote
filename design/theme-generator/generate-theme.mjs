import { Hct } from "./node_modules/@material/material-color-utilities/hct/hct.js";
import { TonalPalette } from "./node_modules/@material/material-color-utilities/palettes/tonal_palette.js";
import { argbFromHex, hexFromArgb } from "./node_modules/@material/material-color-utilities/utils/string_utils.js";
import { readFileSync } from "node:fs";

const seeds = Object.freeze({
  primary: "#B83252",
  secondary: "#66558F",
  tertiary: "#855300",
});

const paletteFromSeed = (hex) => TonalPalette.fromInt(argbFromHex(hex));
const primary = paletteFromSeed(seeds.primary);
const secondary = paletteFromSeed(seeds.secondary);
const tertiary = paletteFromSeed(seeds.tertiary);
const primaryHct = Hct.fromInt(argbFromHex(seeds.primary));
const neutral = TonalPalette.fromHueAndChroma(primaryHct.hue, 4);
const neutralVariant = TonalPalette.fromHueAndChroma(primaryHct.hue, 8);

const color = (palette, tone) => hexFromArgb(palette.tone(tone)).toUpperCase();
const scheme = {
  seeds,
  palettes: {
    primary: Object.fromEntries([0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99, 100].map((tone) => [tone, color(primary, tone)])),
    secondary: Object.fromEntries([10, 20, 30, 40, 80, 90].map((tone) => [tone, color(secondary, tone)])),
    tertiary: Object.fromEntries([10, 20, 30, 40, 80, 90].map((tone) => [tone, color(tertiary, tone)])),
    neutral: Object.fromEntries([4, 6, 10, 12, 17, 20, 22, 24, 87, 90, 92, 94, 96, 98, 99, 100].map((tone) => [tone, color(neutral, tone)])),
    neutralVariant: Object.fromEntries([30, 50, 60, 80, 90].map((tone) => [tone, color(neutralVariant, tone)])),
  },
};

const output = `${JSON.stringify(scheme, null, 2)}\n`;
if (process.argv.includes("--check")) {
  const committed = readFileSync(new URL("./generated-theme.json", import.meta.url), "utf8");
  if (committed !== output) {
    throw new Error("generated-theme.json is stale; run npm run generate and update the committed output");
  }
} else {
  process.stdout.write(output);
}
