import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const root = resolve(scriptDir, "..");
const foundation = resolve(root, "approved/foundation");
const componentApproved = resolve(root, "approved/components");
mkdirSync(foundation, { recursive: true });
mkdirSync(componentApproved, { recursive: true });

const W = 1597;
const H = 985;
const font = '-apple-system,BlinkMacSystemFont,"Helvetica Neue",Arial,sans-serif';
const esc = (value) => String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
const text = (x, y, value, cls = "body", anchor = "start") => `<text x="${x}" y="${y}" class="${cls}" text-anchor="${anchor}">${esc(value)}</text>`;
const rect = (x, y, w, h, fill, rx = 0, extra = "") => `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${rx}" fill="${fill}" ${extra}/>`;

function shell({ title, code, dark = false, subtitle = "MATERIAL 3 · BASELINE 1.4.0", body }) {
  const bg = dark ? "#181212" : "#FFF8F7";
  const ink = dark ? "#F5EAEA" : "#201A1B";
  const muted = dark ? "#D7C1C3" : "#524344";
  const rule = dark ? "#6B5A5C" : "#847374";
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">
  <style>
    .mast{font:500 42px ${font};letter-spacing:12px;fill:${ink}}
    .title{font:500 27px ${font};letter-spacing:3px;fill:${ink}}
    .section{font:600 18px ${font};letter-spacing:2px;fill:${ink}}
    .role{font:600 13px ${font};fill:${ink}}
    .meta{font:500 12px ${font};letter-spacing:.5px;fill:${muted}}
    .body{font:500 15px ${font};fill:${ink}}
    .small{font:500 13px ${font};letter-spacing:1px;fill:${muted}}
    .foot{font:500 14px ${font};letter-spacing:2px;fill:${ink}}
  </style>
  ${rect(0, 0, W, H, bg)}
  ${text(30, 60, "RESONOTE", "mast")}
  ${text(1545, 53, `${code} / ${title}`, "title", "end")}
  ${text(1545, 78, subtitle, "small", "end")}
  <line x1="30" y1="96" x2="1567" y2="96" stroke="${rule}" stroke-width="1"/>
  ${body({ bg, ink, muted, rule })}
  <line x1="30" y1="910" x2="1567" y2="910" stroke="${rule}" stroke-width="1"/>
  ${text(35, 946, "RESONOTE DESIGN SYSTEM", "foot")}
  ${text(1565, 946, "MARKDOWN IS THE NORMATIVE SOURCE", "foot", "end")}
  </svg>`;
}

const lightAccent = [
  ["primary", 40, "#AE2A4B"], ["onPrimary", 100, "#FFFFFF"], ["primaryContainer", 90, "#FFD9DD"], ["onPrimaryContainer", 10, "#400012"],
  ["secondary", 40, "#66558F"], ["onSecondary", 100, "#FFFFFF"], ["secondaryContainer", 90, "#E9DDFF"], ["onSecondaryContainer", 10, "#210F47"],
  ["tertiary", 40, "#855300"], ["onTertiary", 100, "#FFFFFF"], ["tertiaryContainer", 90, "#FFDDB8"], ["onTertiaryContainer", 10, "#2A1700"],
  ["error", 40, "#BA1A1A"], ["onError", 100, "#FFFFFF"], ["errorContainer", 90, "#FFDAD6"], ["onErrorContainer", 10, "#410002"],
  ["primaryFixed", 90, "#FFD9DD"], ["primaryFixedDim", 80, "#FFB2BC"], ["onPrimaryFixed", 10, "#400012"], ["onPrimaryFixedVariant", 30, "#8D0D35"],
  ["secondaryFixed", 90, "#E9DDFF"], ["secondaryFixedDim", 80, "#D0BCFE"], ["onSecondaryFixed", 10, "#210F47"], ["onSecondaryFixedVariant", 30, "#4E3D75"],
  ["tertiaryFixed", 90, "#FFDDB8"], ["tertiaryFixedDim", 80, "#FDB965"], ["onTertiaryFixed", 10, "#2A1700"], ["onTertiaryFixedVariant", 30, "#653E00"],
];
const darkAccent = [
  ["primary", 80, "#FFB2BC"], ["onPrimary", 20, "#670022"], ["primaryContainer", 30, "#8D0D35"], ["onPrimaryContainer", 90, "#FFD9DD"],
  ["secondary", 80, "#D0BCFE"], ["onSecondary", 20, "#37265D"], ["secondaryContainer", 30, "#4E3D75"], ["onSecondaryContainer", 90, "#E9DDFF"],
  ["tertiary", 80, "#FDB965"], ["onTertiary", 20, "#472A00"], ["tertiaryContainer", 30, "#653E00"], ["onTertiaryContainer", 90, "#FFDDB8"],
  ["error", 80, "#FFB4AB"], ["onError", 20, "#690005"], ["errorContainer", 30, "#93000A"], ["onErrorContainer", 90, "#FFDAD6"],
  ["primaryFixed", 90, "#FFD9DD"], ["primaryFixedDim", 80, "#FFB2BC"], ["onPrimaryFixed", 10, "#400012"], ["onPrimaryFixedVariant", 30, "#8D0D35"],
  ["secondaryFixed", 90, "#E9DDFF"], ["secondaryFixedDim", 80, "#D0BCFE"], ["onSecondaryFixed", 10, "#210F47"], ["onSecondaryFixedVariant", 30, "#4E3D75"],
  ["tertiaryFixed", 90, "#FFDDB8"], ["tertiaryFixedDim", 80, "#FDB965"], ["onTertiaryFixed", 10, "#2A1700"], ["onTertiaryFixedVariant", 30, "#653E00"],
];
const lightNeutral = [
  ["background", 99, "#FFFBFF"], ["onBackground", 10, "#201A1B"], ["surface", 98, "#FFF8F7"], ["onSurface", 10, "#201A1B"],
  ["surfaceDim", 87, "#E3D7D7"], ["surfaceBright", 98, "#FFF8F7"], ["surfaceContainerLowest", 100, "#FFFFFF"], ["surfaceContainerLow", 96, "#FEF1F1"],
  ["surfaceContainer", 94, "#F8EBEB"], ["surfaceContainerHigh", 92, "#F2E5E5"], ["surfaceContainerHighest", 90, "#ECE0E0"],
  ["surfaceVariant", 90, "#F4DDDF"], ["onSurfaceVariant", 30, "#524344"], ["outline", 50, "#847374"], ["outlineVariant", 80, "#D7C1C3"],
  ["inverseSurface", 20, "#362F2F"], ["inverseOnSurface", 95, "#FBEEEE"], ["inversePrimary", 80, "#FFB2BC"], ["surfaceTint", 40, "#AE2A4B"], ["scrim", 0, "#000000"], ["shadow", 0, "#000000"],
];
const darkNeutral = [
  ["background", 10, "#201A1B"], ["onBackground", 90, "#ECE0E0"], ["surface", 6, "#181212"], ["onSurface", 90, "#ECE0E0"],
  ["surfaceDim", 6, "#181212"], ["surfaceBright", 24, "#3F3738"], ["surfaceContainerLowest", 4, "#120D0D"], ["surfaceContainerLow", 10, "#201A1B"],
  ["surfaceContainer", 12, "#241E1F"], ["surfaceContainerHigh", 17, "#2F2829"], ["surfaceContainerHighest", 22, "#3A3334"],
  ["surfaceVariant", 30, "#524344"], ["onSurfaceVariant", 80, "#D7C1C3"], ["outline", 60, "#9F8C8E"], ["outlineVariant", 30, "#524344"],
  ["inverseSurface", 90, "#ECE0E0"], ["inverseOnSurface", 20, "#362F2F"], ["inversePrimary", 40, "#AE2A4B"], ["surfaceTint", 80, "#FFB2BC"], ["scrim", 0, "#000000"], ["shadow", 0, "#000000"],
];

function semanticBody(accent, neutral) {
  return ({ rule }) => {
    let out = text(32, 132, "ACCENT ROLES", "section");
    accent.forEach(([role, tone, hex], index) => {
      const col = index % 7;
      const row = Math.floor(index / 7);
      const x = 32 + col * 219;
      const y = 148 + row * 88;
      out += rect(x, y, 202, 36, hex, 2, `stroke="${rule}" stroke-opacity=".25"`);
      out += text(x + 4, y + 54, role, "role");
      out += text(x + 4, y + 72, `T${tone} · ${hex}`, "meta");
    });
    out += text(32, 520, "NEUTRAL · SURFACE · INVERSE & SYSTEM", "section");
    neutral.forEach(([role, tone, hex], index) => {
      const col = index % 6;
      const row = Math.floor(index / 6);
      const x = 32 + col * 256;
      const y = 538 + row * 84;
      out += rect(x, y, 238, 30, hex, 2, `stroke="${rule}" stroke-opacity=".3"`);
      out += text(x + 4, y + 47, role, "role");
      out += text(x + 4, y + 64, `T${tone} · ${hex}`, "meta");
    });
    return out;
  };
}

const lightSvg = shell({ title: "LIGHT SEMANTIC SCHEME", code: "01D", body: semanticBody(lightAccent, lightNeutral) });
const darkSvg = shell({ title: "DARK SEMANTIC SCHEME", code: "01E", dark: true, body: semanticBody(darkAccent, darkNeutral) });

const surfaceRoles = new Set(["surface", "surfaceDim", "surfaceBright", "surfaceContainerLowest", "surfaceContainerLow", "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest"]);
const lightSurfaces = lightNeutral.filter(([role]) => surfaceRoles.has(role));
const darkSurfaces = darkNeutral.filter(([role]) => surfaceRoles.has(role));
function surfacesBody({ rule }) {
  let out = text(32, 142, "LIGHT", "section");
  lightSurfaces.forEach(([role, tone, hex], index) => {
    const x = 32 + (index % 4) * 384;
    const y = 162 + Math.floor(index / 4) * 148;
    out += rect(x, y, 360, 82, hex, 2, `stroke="${rule}" stroke-opacity=".3"`);
    out += text(x + 4, y + 104, role, "role") + text(x + 4, y + 123, `T${tone} · ${hex}`, "meta");
  });
  out += text(32, 492, "DARK", "section");
  darkSurfaces.forEach(([role, tone, hex], index) => {
    const x = 32 + (index % 4) * 384;
    const y = 512 + Math.floor(index / 4) * 148;
    out += rect(x, y, 360, 82, hex, 2, `stroke="${rule}" stroke-opacity=".3"`);
    out += text(x + 4, y + 104, role, "role") + text(x + 4, y + 123, `T${tone} · ${hex}`, "meta");
  });
  return out;
}

const surfaceSvg = shell({ title: "SURFACE HIERARCHY", code: "01F", subtitle: "DERIVED FROM 01D / 01E · NOT A SECOND SOURCE", body: surfacesBody });

const contrastPairs = [
  ["LIGHT BODY", "#201A1B", "#FFF8F7", "16.34:1", "PASS / NORMAL TEXT", "#167334"],
  ["LIGHT PRIMARY", "#FFFFFF", "#AE2A4B", "6.50:1", "PASS / NORMAL TEXT", "#167334"],
  ["DARK BODY", "#ECE0E0", "#181212", "14.38:1", "PASS / NORMAL TEXT", "#167334"],
  ["DARK PRIMARY", "#670022", "#FFB2BC", "7.72:1", "PASS / NORMAL TEXT", "#167334"],
  ["AMOLED BODY", "#ECE0E0", "#000000", "16.31:1", "PASS / NORMAL TEXT", "#167334"],
  ["BEAT AMBER / AMOLED", "#855300", "#000000", "3.23:1", "LIMITED / LARGE + GRAPHICS", "#A45D00"],
  ["PULSE ROSE / CONTAINER", "#B83252", "#FFD9DD", "4.49:1", "FAIL / NORMAL TEXT", "#BA1A1A"],
  ["HARMONIC VIOLET / DARK", "#66558F", "#181212", "2.88:1", "FAIL / TEXT + GRAPHICS", "#BA1A1A"],
];
function contrastBody({ rule }) {
  let out = text(35, 140, "NORMAL TEXT ≥ 4.5:1     ·     LARGE TEXT + ESSENTIAL GRAPHICS ≥ 3.0:1", "section");
  contrastPairs.forEach(([name, fg, bg, ratio, verdict, verdictColor], index) => {
    const col = index % 4;
    const row = Math.floor(index / 4);
    const x = 35 + col * 390;
    const y = 185 + row * 330;
    out += rect(x, y, 170, 112, fg, 2, `stroke="${rule}" stroke-opacity=".3"`);
    out += rect(x + 170, y, 170, 112, bg, 2, `stroke="${rule}" stroke-opacity=".3"`);
    out += text(x + 85, y + 138, "FG", "meta", "middle") + text(x + 255, y + 138, "BG", "meta", "middle");
    out += text(x, y + 178, `${String(index + 1).padStart(2, "0")}  ${name}`, "section");
    out += text(x, y + 207, `FG ${fg} / BG ${bg}`, "body");
    out += text(x, y + 248, ratio, "title");
    out += `<text x="${x}" y="${y + 282}" class="role" style="fill:${verdictColor};letter-spacing:1px">${esc(verdict)}</text>`;
  });
  return out;
}
const contrastSvg = shell({ title: "COLOR CONTRAST VALIDATION", code: "01H", subtitle: "WCAG AA · RECALCULATED FROM CANONICAL ROLES", body: contrastBody });

function elevationBody({ rule }) {
  const themes = [
    ["LIGHT", "#FFF8F7", ["#FFF8F7", "#FEF1F1", "#F8EBEB", "#F2E5E5", "#ECE0E0", "#E4D8D8"]],
    ["DARK", "#181212", ["#181212", "#201A1B", "#241E1F", "#2F2829", "#3A3334", "#443D3E"]],
    ["AMOLED", "#000000", ["#000000", "#120D0D", "#181212", "#201A1B", "#2F2829", "#3A3334"]],
  ];
  const levels = [["L0 BASE", "0dp"], ["L1 LOW", "1dp"], ["L2 MEDIUM", "3dp"], ["L3 HIGH", "6dp"], ["L4 OVERLAY", "8dp"], ["L5 HIGHEST", "12dp"]];
  let out = "";
  themes.forEach(([name, base, colors], column) => {
    const x = 35 + column * 520;
    out += text(x, 140, `${String(column + 1).padStart(2, "0")}  ${name}`, "section");
    out += rect(x, 160, 485, 680, base, 8, `stroke="${rule}" stroke-opacity=".35"`);
    levels.forEach(([label, dp], index) => {
      const y = 188 + index * 104;
      const labelColor = column === 0 ? "#201A1B" : "#F5EAEA";
      out += rect(x + 160, y, 300, 78, colors[index], 8, `stroke="${rule}" stroke-opacity=".22"`);
      out += `<text x="${x + 18}" y="${y + 40}" class="role" style="fill:${labelColor}">${label} / ${dp}</text>`;
    });
  });
  return out;
}
const elevationSvg = shell({ title: "ELEVATION", code: "03B", subtitle: "TONAL SURFACE FIRST · LEVEL 5 IS NOT ‘MODAL’", body: elevationBody });

const paths = {
  home: "M240-200h120v-240h240v240h120v-360L480-740 240-560v360Zm-80 80v-480l320-240 320 240v480H520v-240h-80v240H160Z",
  library: "M500-360q42 0 71-29t29-71v-220h120v-80H560v220q-13-10-28-15t-32-5q-42 0-71 29t-29 71q0 42 29 71t71 29ZM320-240q-33 0-56.5-23.5T240-320v-480q0-33 23.5-56.5T320-880h480q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800-240H320Zm0-80h480v-480H320v480ZM160-80q-33 0-56.5-23.5T80-160v-560h80v560h560v80H160Z",
  search: "M784-120 532-372q-30 24-69 38t-83 14q-109 0-184.5-75.5T120-580q0-109 75.5-184.5T380-840q109 0 184.5 75.5T640-580q0 44-14 83t-38 69l252 252-56 56ZM380-400q75 0 127.5-52.5T560-580q0-75-52.5-127.5T380-760q-75 0-127.5 52.5T200-580q0 75 52.5 127.5T380-400Z",
  filter: "M440-160q-17 0-28.5-11.5T400-200v-240L168-736q-15-20-4.5-42t36.5-22h560q26 0 36.5 22t-4.5 42L560-440v240q0 17-11.5 28.5T520-160h-80Zm40-308 198-252H282l198 252Z",
  error: "M440-440h80v-240h-80v240Zm40 160q17 0 28.5-11.5T520-320q0-17-11.5-28.5T480-360q-17 0-28.5 11.5T440-320q0 17 11.5 28.5T480-280Zm0 200q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Z",
  refresh: "M480-160q-134 0-227-93t-93-227q0-134 93-227t227-93q69 0 132 28.5T720-690v-110h80v280H520v-80h168q-32-56-87.5-88T480-720q-100 0-170 70t-70 170q0 100 70 170t170 70q77 0 139-44t87-116h84q-28 106-114 173t-196 67Z",
  settings: "m370-80-16-128q-13-5-24.5-12T307-235l-119 50L78-375l103-78q-1-7-1-13.5v-27q0-6.5 1-13.5L78-585l110-190 119 50q11-8 23-15t24-12l16-128h220l16 128q13 5 24.5 12t22.5 15l119-50 110 190-103 78q1 7 1 13.5v27q0 6.5-2 13.5l103 78-110 190-118-50q-11 8-23 15t-24 12L590-80H370Zm112-260q58 0 99-41t41-99q0-58-41-99t-99-41q-59 0-99.5 41T342-480q0 58 40.5 99t99.5 41Z",
};
const icon = (x, y, size, name, fill = "#201A1B") => `<svg x="${x}" y="${y}" width="${size}" height="${size}" viewBox="0 -960 960 960" fill="${fill}"><path d="${paths[name]}"/></svg>`;

function compactMediumNavigationBody({ rule }) {
  let out = text(35, 138, "01  COMPACT", "section") + text(822, 138, "02  MEDIUM", "section");
  out += text(35, 166, "< 600dp · STANDARD NAVIGATION BAR", "small");
  out += text(822, 166, "600–839dp · NARROW NAVIGATION RAIL", "small");

  // Compact: every icon and label stays inside the 64dp navigation container.
  out += rect(35, 190, 740, 500, "#FFFBFF", 22, `stroke="${rule}"`);
  out += text(65, 235, "LIBRARY", "section");
  out += rect(65, 275, 205, 245, "#FFD9DD", 14) + rect(290, 275, 205, 245, "#F2E5E5", 14) + rect(515, 275, 205, 245, "#F2E5E5", 14);
  out += rect(35, 590, 740, 100, "#F2E5E5", 0);
  const compactItems = [[158, "home", "HOME", true], [405, "library", "LIBRARY", false], [652, "search", "SEARCH", false]];
  compactItems.forEach(([cx, iconName, label, selected]) => {
    if (selected) out += rect(cx - 54, 600, 108, 42, "#FFD9DD", 21);
    out += icon(cx - 15, 606, 30, iconName, selected ? "#AE2A4B" : "#524344");
    out += text(cx, 671, label, "role", "middle");
  });

  // Medium: labels remain vertically grouped with their rail icons.
  out += rect(822, 190, 740, 500, "#FFFBFF", 22, `stroke="${rule}"`);
  out += rect(822, 190, 130, 500, "#F2E5E5", 22);
  const railItems = [[270, "home", "HOME", true], [385, "library", "LIBRARY", false], [500, "search", "SEARCH", false]];
  railItems.forEach(([itemY, iconName, label, selected]) => {
    if (selected) out += rect(845, itemY, 84, 48, "#FFD9DD", 24);
    out += icon(872, itemY + 9, 30, iconName, selected ? "#AE2A4B" : "#524344");
    out += text(887, itemY + 72, label, "role", "middle");
  });
  out += text(992, 235, "LIBRARY", "section");
  out += rect(992, 275, 250, 170, "#FFD9DD", 14) + rect(1262, 275, 250, 170, "#F2E5E5", 14);
  out += rect(992, 465, 520, 165, "#FEF1F1", 14);

  out += text(35, 738, "M3 1.4.0 TOKEN CONTRACT", "section") + text(822, 738, "M3 1.4.0 TOKEN CONTRACT", "section");
  out += text(35, 770, "Container 64dp · Icon 24dp · Icon–Label gap 4dp", "body");
  out += text(35, 798, "LabelMedium · CornerFull active indicator · 3–5 destinations", "small");
  out += text(822, 770, "Narrow rail 80dp · Default collapsed rail 96dp · Icon 24dp", "body");
  out += text(822, 798, "Active indicator 56×32dp · LabelMedium · 3–7 destinations", "small");
  out += `<line x1="35" y1="835" x2="1562" y2="835" stroke="${rule}" stroke-width="1"/>`;
  out += text(35, 873, "WINDOW CHANGE CONTRACT", "section");
  out += text(350, 873, "DESTINATION ID", "role") + text(545, 873, "LABEL + ICON", "role") + text(730, 873, "SELECTED STATE", "role");
  out += text(945, 873, "BACK STACK", "role") + text(1115, 873, "QUERY + FILTER", "role") + text(1335, 873, "SCROLL STATE", "role");
  return out;
}

function expandedNavigationBody({ rule }) {
  const modes = [
    ["EXPANDED", "840–1199dp", "CONTENT USES AVAILABLE WIDTH"],
    ["LARGE", "1200–1599dp", "BODY MAX WIDTH 1200dp"],
    ["EXTRA-LARGE", "≥ 1600dp", "CENTERED MAX-WIDTH BODY"],
  ];
  let out = "";
  modes.forEach(([name, range, layoutNote], index) => {
    const x = 35 + index * 520;
    out += text(x, 138, `${String(index + 1).padStart(2, "0")}  ${name}`, "section");
    out += text(x, 166, range, "small");
    out += rect(x, 190, 485, 520, "#FFFBFF", 22, `stroke="${rule}"`);
    out += rect(x, 190, 190, 520, "#F2E5E5", 22);
    out += rect(x + 12, 265, 166, 56, "#FFD9DD", 28);
    out += icon(x + 28, 279, 28, "home", "#AE2A4B") + text(x + 72, 299, "HOME", "role");
    out += icon(x + 28, 359, 28, "library", "#524344") + text(x + 72, 379, "LIBRARY", "role");
    out += icon(x + 28, 439, 28, "search", "#524344") + text(x + 72, 459, "SEARCH", "role");
    const contentX = x + 215;
    out += text(contentX, 235, "LIBRARY", "section");
    out += rect(contentX, 275, 115, 145, "#FFD9DD", 12) + rect(contentX + 135, 275, 115, 145, "#F2E5E5", 12);
    out += rect(contentX, 445, 250, 150, "#FEF1F1", 12);
    out += `<line x1="${contentX}" y1="635" x2="${contentX + 250}" y2="635" stroke="#847374" stroke-width="1"/>`;
    out += text(contentX + 125, 663, layoutNote, "meta", "middle");
    out += text(x + 242, 758, "PERMANENT DRAWER", "role", "middle");
    out += text(x + 242, 786, "360dp · indicator 336×56dp · CornerFull", "small", "middle");
    out += text(x + 242, 812, "Icon 24dp · LabelLarge · 3–7 destinations", "small", "middle");
  });
  out += `<line x1="35" y1="845" x2="1562" y2="845" stroke="${rule}" stroke-width="1"/>`;
  out += text(35, 880, "LARGE AND EXTRA-LARGE REUSE EXPANDED TOPOLOGY; ONLY MARGINS AND CONTENT MAX WIDTH CHANGE.", "small");
  return out;
}

function recoveryBody({ rule }) {
  let out = text(35, 138, "SEARCH ENTRY", "section");
  out += rect(35, 165, 1527, 92, "#F2E5E5", 46);
  out += icon(62, 187, 48, "search", "#524344") + text(130, 218, "Search albums, artists, and songs", "body");
  out += icon(1482, 187, 48, "filter", "#524344");
  out += rect(35, 284, 220, 52, "#FFD9DD", 8) + text(145, 316, "DOWNLOADED", "role", "middle");
  out += rect(275, 284, 150, 52, "none", 8, `stroke="${rule}"`) + text(350, 316, "CLEAR", "role", "middle");
  out += text(455, 316, "300ms debounce applies to query work only · local input and Clear stay immediate", "small");
  const cards = [
    ["NO RESULTS", "Try clearing filters.", "CLEAR FILTERS", "search", "#524344"],
    ["LIBRARY ERROR", "The request could not finish.", "TRY AGAIN", "error", "#BA1A1A"],
    ["OFFLINE", "Downloaded music is still available.", "VIEW DOWNLOADS", "refresh", "#AE2A4B"],
    ["PERMISSION", "Explain the benefit before asking.", "OPEN SETTINGS", "settings", "#66558F"],
  ];
  cards.forEach(([titleValue, message, action, iconName, color], i) => {
    const x = 35 + i * 390;
    const y = 390;
    out += rect(x, y, 355, 360, "#FEF1F1", 24, `stroke="#D7C1C3"`);
    out += icon(x + 133, y + 42, 88, iconName, color);
    out += text(x + 177, y + 168, titleValue, "section", "middle");
    out += text(x + 177, y + 205, message, "body", "middle");
    out += rect(x + 74, y + 250, 207, 56, i === 0 ? "none" : "#FFD9DD", 28, i === 0 ? `stroke="#AE2A4B"` : "");
    out += text(x + 177, y + 284, action, "role", "middle");
    out += text(x + 177, y + 332, i === 3 ? "RATIONALE → SYSTEM PROMPT" : "RECOVERY PATH REMAINS VISIBLE", "meta", "middle");
  });
  out += `<line x1="35" y1="795" x2="1562" y2="795" stroke="${rule}" stroke-width="1"/>`;
  out += text(35, 835, "STATE CONTRACT", "section");
  out += text(265, 835, "EMPTY ≠ NO RESULT", "role") + text(505, 835, "ERROR ≠ OFFLINE", "role") + text(745, 835, "PERMISSION IS CONTEXTUAL", "role") + text(1050, 835, "PRIMARY NAV STAYS VISIBLE", "role") + text(1325, 835, "EXIT PATH REQUIRED", "role");
  out += text(35, 872, "Every state names what happened, preserves context, and offers one clear recovery or exit action.", "small");
  return out;
}

const compactMediumNavigationSvg = shell({ title: "COMPACT & MEDIUM NAVIGATION", code: "07A-1", subtitle: "NAVIGATION BAR · NAVIGATION RAIL · CONTAINED LABELS", body: compactMediumNavigationBody });
const expandedNavigationSvg = shell({ title: "EXPANDED NAV", code: "07A-2", subtitle: "EXPANDED · LARGE · EXTRA-LARGE · PERMANENT DRAWER", body: expandedNavigationBody });
const recoverySvg = shell({ title: "SEARCH & RECOVERY", code: "07B", subtitle: "SEARCH · FILTER · EMPTY · ERROR · OFFLINE · PERMISSION", body: recoveryBody });

const outputs = new Map([
  ["01d-light-semantic-scheme-source.svg", lightSvg],
  ["01e-dark-semantic-scheme-source.svg", darkSvg],
  ["01f-surface-hierarchy-source.svg", surfaceSvg],
  ["01h-color-contrast-validation-source.svg", contrastSvg],
  ["03b-elevation-source.svg", elevationSvg],
]);

for (const [name, svg] of outputs) writeFileSync(resolve(foundation, name), svg);
writeFileSync(resolve(componentApproved, "07a-1-compact-medium-navigation-source.svg"), compactMediumNavigationSvg);
writeFileSync(resolve(componentApproved, "07a-2-expanded-navigation-source.svg"), expandedNavigationSvg);
writeFileSync(resolve(componentApproved, "07b-search-recovery-source.svg"), recoverySvg);
console.log([...outputs.keys()].join("\n"));
console.log("07a-1-compact-medium-navigation-source.svg\n07a-2-expanded-navigation-source.svg\n07b-search-recovery-source.svg");
