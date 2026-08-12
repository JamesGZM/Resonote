#!/usr/bin/env node

import { execFileSync } from 'node:child_process';
import { mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const API_COMMIT = '6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb';
const APP_COMMIT = '52c9833afe2e7fedcba8d5b23ff8d1f9731af73a';
const MOBILE_COMMIT = 'ab71195d4cf3297332490fd37704d1ae8973d4c5';
const MOBILE_API_COMMIT = '283f1e97';
const TOP_CARD_PC_COMMIT = 'a86cfefb';
const ANDROID_RETROFIT_ENDPOINTS = new Set([
  'API-DISCOVER-003', 'API-DISCOVER-008', 'API-DISCOVER-009', 'API-DISCOVER-012',
  'API-DISCOVER-013', 'API-DISCOVER-016', 'API-SONG-011', 'API-RANKING-003',
  'API-RANKING-001', 'API-PLAYLIST-001', 'API-PLAYLIST-006', 'API-PLAYLIST-007',
  'API-PLAYLIST-009', 'API-PLAYLIST-010', 'API-SEARCH-001', 'API-SEARCH-002',
  'API-SEARCH-004', 'API-SEARCH-005', 'API-SEARCH-007', 'API-LYRICS-001',
  'API-VIDEO-003', 'API-RECOGNITION-001', 'API-ALBUM-004', 'API-ARTIST-002',
  'API-ARTIST-003', 'API-USER-003', 'API-USER-008', 'API-USER-013',
  'API-CLOUD-003', 'API-LOGIN-008', 'API-LOGIN-010', 'API-YOUTH-008',
  'API-YOUTH-009',
]);
const ANDROID_SPECIAL_PROTOCOL_ENDPOINTS = new Set([
  'API-DEVICE-001', 'API-LOGIN-001', 'API-LOGIN-002', 'API-LOGIN-003',
  'API-LOGIN-004', 'API-LOGIN-015', 'API-CLOUD-001',
]);
const ANDROID_TYPED_WIRE_ENDPOINTS = new Set([
  'API-DISCOVER-003', 'API-DISCOVER-009', 'API-DISCOVER-012', 'API-DISCOVER-013',
  'API-SONG-011', 'API-RANKING-003', 'API-RANKING-001', 'API-PLAYLIST-001',
  'API-PLAYLIST-007', 'API-SEARCH-001', 'API-USER-003', 'API-USER-008',
  'API-USER-013',
]);
const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const DOC_ROOT = resolve(SCRIPT_DIR, '..');
const WORKSPACE_ROOT = resolve(DOC_ROOT, '../..');
const MAIN_CHECKOUT_ROOT = dirname(execFileSync(
  'git',
  ['-C', WORKSPACE_ROOT, 'rev-parse', '--path-format=absolute', '--git-common-dir'],
  { encoding: 'utf8' },
).trim());
const REFERENCE_ROOT = dirname(MAIN_CHECKOUT_ROOT);
const MOEKOE_ROOT = resolve(process.env.MOEKOE_ROOT || join(REFERENCE_ROOT, 'MoeKoeMusic'));
const API_ROOT = join(MOEKOE_ROOT, 'api');
const MOBILE_ROOT = resolve(process.env.MOEKOE_MOBILE_ROOT || join(REFERENCE_ROOT, 'MoeKoeMusic-Mobile'));

const EVIDENCE = Object.freeze({
  source: 'SOURCE_CONFIRMED',
  consumer: 'CONSUMER_CONFIRMED',
  reference: 'REFERENCE_CONFIRMED',
  declared: 'DECLARED',
  fixture: 'FIXTURE_CONFIRMED',
  inferred: 'INFERRED',
  unknown: 'UNKNOWN',
});

const LOGIN_MODULES = new Set([
  'captcha_sent', 'get_verify_info', 'login', 'login_cellphone', 'login_device',
  'login_device_kick', 'login_openplat', 'login_qr_check', 'login_qr_create',
  'login_qr_key', 'login_token', 'login_wx_check', 'login_wx_create', 'sidedt',
  'verify_user_info',
]);

let loginReferenceFields = new Map();

function applyLiteLoginContract(module, requestFields, responseFields) {
  if (!LOGIN_MODULES.has(module)) return requestFields;
  const fields = new Map(requestFields.map((field) => [field.name, field]));
  if (module === 'login' || module === 'login_cellphone') fields.delete('t3');
  const ensure = (name, type, location, defaultValue = null) => {
    const field = fields.get(name) || {
      name, required: false, type, description: '', locations: new Set(),
      evidence: new Set(), default: null,
    };
    field.type = type;
    field.locations.add(location);
    field.evidence.add(EVIDENCE.source);
    if (defaultValue != null) field.default = defaultValue;
    fields.set(name, field);
  };
  if (module === 'captcha_sent') {
    ensure('businessid', 'number', 'body', '5');
    ensure('plat', 'number', 'body', '3');
  }
  if (module === 'get_verify_info') {
    ensure('rtype', 'number', 'body', '1');
    ensure('wasm', 'number', 'body', '1');
    ensure('i', 'string', 'body', "''");
    ensure('sid', 'string', 'body', "''");
    ensure('edt', 'string', 'body', "''");
  }
  if (module === 'login_cellphone') {
    for (const name of ['plat', 'support_multi']) ensure(name, 'number', 'body', '1');
    for (const name of ['t1', 't2', 'dfid', 'dev', 'gitversion', 'key', 'pk', 'params']) ensure(name, 'string', 'body');
    ensure('clienttime_ms', 'number', 'body');
  }
  if (module === 'verify_user_info') {
    ensure('clientver', 'number', 'query', '11510');
    ensure('wasm', 'number', 'body', '1');
    ensure('i', 'string', 'body', "''");
  }
  for (const path of loginReferenceFields.get(module) || []) {
    if (!responseFields.has(path)) responseFields.set(path, EVIDENCE.reference);
  }
  return [...fields.values()].sort((a, b) => a.name.localeCompare(b.name));
}

function applyEndpointRequestContract(module, requestFields) {
  if (module !== 'audio_match') return requestFields;
  const fields = new Map(requestFields.map((field) => [field.name, field]));
  const moduleUserId = fields.get('userid');
  if (moduleUserId) moduleUserId.locations = new Set(['module']);
  fields.set('useid', {
    name: 'useid',
    required: false,
    type: 'unknown',
    description: 'Provider wire spelling for the optional user id.',
    locations: new Set(['query']),
    evidence: new Set([EVIDENCE.source]),
    default: '<source-expression>',
  });
  return [...fields.values()].sort((a, b) => a.name.localeCompare(b.name));
}

const DOMAIN_NAMES = {
  ai: 'AI 推荐', album: '专辑', artist: '歌手', audio: '音频与识曲', brush: '刷刷',
  captcha: '验证码', cloud: '云盘', comment: '评论', device: '设备与验证', discover: '发现与推荐',
  favorite: '收藏统计', fm: '电台', images: '图片', ip: 'IP 内容', login: '登录',
  longaudio: '长音频', lyrics: '歌词', playlist: '歌单', ranking: '排行', recognition: '听歌识曲', scene: '场景音乐', search: '搜索',
  sheet: '曲谱', song: '歌曲', theme: '主题内容', user: '用户', video: '视频',
  youth: '概念版专区', misc: '其他',
};

const DOMAIN_ORDER = Object.keys(DOMAIN_NAMES);
const DEFERRED_PREFIXES = new Set(['audio_match', 'user_cloud', 'video']);
const EXCLUDED_PREFIXES = new Set(['comment', 'youth_vip', 'youth_day_vip', 'youth_month_vip', 'youth_union_vip']);
const MUTATION_WORDS = /(add|del|upload|follow|unfollow|kick|sub|upgrade|login|captcha|verify|playhistory)/;
const SENSITIVE_WORDS = /(token|secret|password|passwd|pwd|authorization|cookie|t1|t2|private|userid|dfid|guid|mid|mac|imei|imsi)/i;
const SEARCH_SONG_RESPONSE_FIELDS = new Set([
  'data.lists[].FileHash', 'data.lists[].HQFileHash', 'data.lists[].SQFileHash',
  'data.lists[].OriSongName', 'data.lists[].SongName', 'data.lists[].FileName',
  'data.lists[].SingerName', 'data.lists[].Image', 'data.lists[].Duration',
]);

function responseFieldCondition(endpoint, path) {
  return endpoint.module === 'search' && SEARCH_SONG_RESPONSE_FIELDS.has(path) ? 'type == "song"' : null;
}

function git(repo, args) {
  return execFileSync('git', ['-C', repo, ...args], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
}

function show(repo, commit, path) {
  return git(repo, ['show', `${commit}:${path}`]);
}

function listTree(repo, commit, prefix) {
  return git(repo, ['ls-tree', '-r', '--name-only', commit, '--', prefix])
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
}

function ensureCommit(repo, commit) {
  const type = git(repo, ['cat-file', '-t', commit]).trim();
  if (type !== 'commit') throw new Error(`${repo} 中 ${commit} 不是 commit`);
}

function extractMobileReferenceEvidence() {
  ensureCommit(MOBILE_ROOT, MOBILE_COMMIT);
  const decoder = [
    show(MOBILE_ROOT, MOBILE_COMMIT, 'src/features/account/auth.ts'),
    show(MOBILE_ROOT, MOBILE_COMMIT, 'src/app/login.tsx'),
  ].join('\n');
  const expected = new Map([
    ['get_verify_info', ['data.v_type', 'data.txappid']],
    ['login_cellphone', [
      'data.info_list', 'data.info_list.userid', 'data.info_list.nickname',
      'data.info_list.pic', 'data.info_list.p_grade',
    ]],
  ]);
  for (const fields of expected.values()) {
    for (const path of fields) {
      const terminal = path.split('.').at(-1);
      if (!decoder.includes(terminal)) throw new Error(`Mobile 固定证据缺少字段：${path}`);
    }
  }
  return expected;
}

function balancedSlice(source, openIndex, open = '{', close = '}') {
  let depth = 0;
  let quote = null;
  let escaped = false;
  let lineComment = false;
  let blockComment = false;
  for (let i = openIndex; i < source.length; i += 1) {
    const char = source[i];
    const next = source[i + 1];
    if (lineComment) {
      if (char === '\n') lineComment = false;
      continue;
    }
    if (blockComment) {
      if (char === '*' && next === '/') { blockComment = false; i += 1; }
      continue;
    }
    if (quote) {
      if (escaped) escaped = false;
      else if (char === '\\') escaped = true;
      else if (char === quote) quote = null;
      continue;
    }
    if (char === '/' && next === '/') { lineComment = true; i += 1; continue; }
    if (char === '/' && next === '*') { blockComment = true; i += 1; continue; }
    if (char === '\'' || char === '"' || char === '`') { quote = char; continue; }
    if (char === open) depth += 1;
    if (char === close) {
      depth -= 1;
      if (depth === 0) return source.slice(openIndex, i + 1);
    }
  }
  return source.slice(openIndex);
}

function splitTopLevel(objectText) {
  const inner = objectText.startsWith('{') ? objectText.slice(1, -1) : objectText;
  const parts = [];
  let start = 0;
  let depth = 0;
  let quote = null;
  let escaped = false;
  let lineComment = false;
  let blockComment = false;
  for (let i = 0; i < inner.length; i += 1) {
    const char = inner[i];
    const next = inner[i + 1];
    if (lineComment) { if (char === '\n') lineComment = false; continue; }
    if (blockComment) { if (char === '*' && next === '/') { blockComment = false; i += 1; } continue; }
    if (quote) {
      if (escaped) escaped = false;
      else if (char === '\\') escaped = true;
      else if (char === quote) quote = null;
      continue;
    }
    if (char === '/' && next === '/') { lineComment = true; i += 1; continue; }
    if (char === '/' && next === '*') { blockComment = true; i += 1; continue; }
    if (char === '\'' || char === '"' || char === '`') { quote = char; continue; }
    if ('{[('.includes(char)) depth += 1;
    if ('}])'.includes(char)) depth -= 1;
    if (char === ',' && depth === 0) { parts.push(inner.slice(start, i).trim()); start = i + 1; }
  }
  const tail = inner.slice(start).trim();
  if (tail) parts.push(tail);
  return parts.filter(Boolean);
}

function objectProperties(objectText) {
  const result = new Map();
  for (const part of splitTopLevel(objectText)) {
    const match = part.match(/^(?:['"]([^'"]+)['"]|([A-Za-z_$][\w$]*))\s*:\s*([\s\S]*)$/);
    if (match) result.set(match[1] || match[2], match[3].trim());
    else if (/^[A-Za-z_$][\w$]*$/.test(part)) result.set(part, part);
  }
  return result;
}

function collectObjectDeclarations(source) {
  const objects = new Map();
  const regex = /\bconst\s+([A-Za-z_$][\w$]*)\s*=\s*\{/g;
  for (const match of source.matchAll(regex)) {
    const open = match.index + match[0].lastIndexOf('{');
    objects.set(match[1], balancedSlice(source, open));
  }
  return objects;
}

function cleanExpression(value, field = '') {
  if (value == null) return null;
  let clean = value.replace(/\s+/g, ' ').trim().replace(/,$/, '');
  if (SENSITIVE_WORDS.test(field) && /['"][^'"]+['"]/.test(clean)) return '<redacted>';
  clean = clean.replace(/(['"])[^'"\n]{48,}\1/g, "'<redacted-long-literal>'");
  return clean.length > 180 ? `${clean.slice(0, 177)}...` : clean;
}

function cleanDefault(value, field = '') {
  if (value == null) return null;
  const clean = cleanExpression(value, field);
  if (clean === '<redacted>') return clean;
  if (/^(?:-?\d+(?:\.\d+)?|true|false|null|undefined|['"][^'"]*['"])$/.test(clean)) return clean;
  return '<source-expression>';
}

function mdCode(value) {
  const escaped = String(value ?? '-')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('|', '&#124;');
  return `<code>${escaped}</code>`;
}

function literalOrDynamic(expression, fallback = 'dynamic') {
  if (!expression) return fallback;
  const clean = expression.trim();
  const literal = clean.match(/^(['"])([\s\S]*?)\1$/);
  if (literal) return literal[2];
  const template = clean.match(/^`([^`]*)`$/);
  if (template && !template[1].includes('${')) return template[1];
  return `dynamic: ${cleanExpression(clean)}`;
}

function extractCallObjects(source, callee) {
  return extractCallEntries(source, callee).map((entry) => entry.text);
}

function extractCallEntries(source, callee) {
  const calls = [];
  const regex = new RegExp(`\\b${callee.replace('.', '\\.') }\\s*\\(\\s*\\{`, 'g');
  for (const match of source.matchAll(regex)) {
    const open = match.index + match[0].lastIndexOf('{');
    calls.push({ index: match.index, text: balancedSlice(source, open) });
  }
  return calls;
}

function parseInterfaces(source) {
  const interfaces = new Map();
  const regex = /export interface\s+(\w+)(?:\s+extends\s+(\w+))?\s*\{/g;
  for (const match of source.matchAll(regex)) {
    const open = match.index + match[0].lastIndexOf('{');
    const interfaceBody = balancedSlice(source, open).slice(1, -1);
    const fields = [];
    for (const line of interfaceBody.split('\n')) {
      const field = line.match(/^\s*(?:readonly\s+)?([\w$]+)(\?)?\s*:\s*([^;]+);(?:\s*\/\/\s*(.*))?/);
      if (field) fields.push({ name: field[1], required: !field[2], type: field[3].trim(), description: field[4] || '' });
    }
    interfaces.set(match[1], { parent: match[2] || null, fields });
  }
  const functions = new Map();
  const functionDescriptions = new Map();
  for (const match of source.matchAll(/export function\s+(\w+)\s*\(\s*params\??\s*:\s*(\w+)/g)) {
    functions.set(match[1], match[2]);
    const prefix = source.slice(Math.max(0, match.index - 800), match.index);
    const comment = [...prefix.matchAll(/\/\*\*([\s\S]*?)\*\//g)].at(-1)?.[1] || '';
    const description = comment.split('\n')
      .map((line) => line.replace(/^\s*\*\s?/, '').trim())
      .find((line) => line && !line.startsWith('@'));
    if (description) functionDescriptions.set(match[1], description);
  }
  function resolveFields(name, visited = new Set()) {
    if (!name || visited.has(name) || !interfaces.has(name)) return [];
    visited.add(name);
    const current = interfaces.get(name);
    return [...resolveFields(current.parent, visited), ...current.fields];
  }
  return { functions, functionDescriptions, resolveFields };
}

function moduleDomain(name) {
  if (/^(login|captcha|verify|get_verify|sidedt|register)/.test(name)) return name === 'register_dev' ? 'device' : 'login';
  if (/^(user_cloud)/.test(name)) return 'cloud';
  if (/^(user|playhistory|lastest)/.test(name)) return 'user';
  if (/^(search)/.test(name)) return 'search';
  if (/^(lyric)/.test(name)) return 'lyrics';
  if (/^(playlist)/.test(name)) return 'playlist';
  if (/^(rank)/.test(name)) return 'ranking';
  if (/^(everyday|recommend|top|personal|yueku|pc_diantai)/.test(name)) return 'discover';
  if (/^(song|audio|krm|kmr|privilege)/.test(name)) return name === 'audio_match' ? 'recognition' : 'song';
  const prefix = name.split('_')[0];
  return DOMAIN_NAMES[prefix] ? prefix : 'misc';
}

function productScope(name) {
  for (const prefix of DEFERRED_PREFIXES) if (name.startsWith(prefix)) return 'deferred';
  for (const prefix of EXCLUDED_PREFIXES) if (name.startsWith(prefix)) return 'excluded';
  return 'candidate';
}

function extractDescription(source, name) {
  const leading = source.split('\n').slice(0, 12)
    .map((line) => line.match(/^\s*\/\/\s*(.+)$/)?.[1]?.trim())
    .filter(Boolean)
    .find((line) => !/^(流程|参数|返回|TODO|const )/.test(line));
  return leading || name.replaceAll('_', ' ');
}

function parseRequestFields(source, declaredFields, objects, callObjects) {
  const byName = new Map();
  for (const field of declaredFields) {
    if (['cookie', 'realIP'].includes(field.name)) continue;
    byName.set(field.name, { ...field, locations: new Set(), evidence: new Set([EVIDENCE.declared]), default: null });
  }
  for (const match of source.matchAll(/params(?:\?\.|\.)([A-Za-z_$][\w$]*)/g)) {
    const name = match[1];
    if (name === 'cookie') continue;
    if (!byName.has(name)) byName.set(name, { name, required: false, type: 'unknown', description: '', locations: new Set(), evidence: new Set(), default: null });
    byName.get(name).evidence.add(EVIDENCE.source);
    const nearby = source.slice(match.index, match.index + 160);
    const defaultMatch = nearby.match(new RegExp(`params(?:\\?\\.|\\.)${name.replace('$', '\\$')}\\s*(?:\\|\\||\\?\\?)\\s*([^,;}\\n]+)`));
    if (defaultMatch) byName.get(name).default = cleanDefault(defaultMatch[1], name);
  }
  for (const call of callObjects) {
    const props = objectProperties(call);
    for (const [callKey, location] of [['params', 'query'], ['data', 'body'], ['headers', 'header'], ['cookie', 'cookie']]) {
      const expression = props.get(callKey);
      if (!expression) continue;
      let fieldObject = null;
      if (objects.has(expression)) fieldObject = objects.get(expression);
      else if (expression.trim().startsWith('{')) fieldObject = expression;
      if (!fieldObject) continue;
      for (const fieldName of objectProperties(fieldObject).keys()) {
        const directParam = objectProperties(fieldObject).get(fieldName)?.match(/params(?:\?\.|\.)(\w+)/)?.[1];
        const name = directParam || fieldName;
        if (!byName.has(name)) byName.set(name, { name, required: false, type: 'unknown', description: '', locations: new Set(), evidence: new Set(), default: null });
        byName.get(name).locations.add(location);
        byName.get(name).evidence.add(EVIDENCE.source);
      }
    }
  }
  for (const field of byName.values()) if (field.locations.size === 0) field.locations.add('module');
  return [...byName.values()].sort((a, b) => a.name.localeCompare(b.name));
}

function parseUpstreamRequests(source, objects) {
  const calls = [
    ...extractCallEntries(source, 'useAxios').map((entry) => ({ ...entry, kind: 'useAxios' })),
    ...extractCallEntries(source, 'http').map((entry) => ({ ...entry, kind: 'native' })),
  ].sort((a, b) => a.index - b.index);
  const result = [];
  for (const [index, call] of calls.entries()) {
    const props = objectProperties(call.text);
    const method = literalOrDynamic(props.get('method'), 'GET').toUpperCase();
    const url = literalOrDynamic(props.get('url'));
    let baseUrl = literalOrDynamic(props.get('baseURL'), call.kind === 'useAxios' ? 'https://gateway.kugou.com' : 'dynamic');
    if (/^https?:\/\//.test(url)) baseUrl = 'absolute-url';
    const headerExpression = props.get('headers') || '';
    const router = headerExpression.match(/['"]x-router['"]\s*:\s*['"]([^'"]+)['"]/i)?.[1] || null;
    const noSignature = /^(true|1)$/.test((props.get('notSignature') || '').trim());
    const encryptType = literalOrDynamic(props.get('encryptType'), call.kind === 'useAxios' ? 'android' : 'none');
    const signing = noSignature ? 'none' : encryptType;
    const responseType = literalOrDynamic(props.get('responseType'), 'json');
    result.push({ sequence: index + 1, kind: call.kind, baseUrl, path: url, method, router, signing, responseType });
  }
  if (result.length === 0) result.push({ sequence: 1, kind: 'dynamic', baseUrl: 'dynamic', path: 'dynamic', method: 'DYNAMIC', router: null, signing: 'unknown', responseType: 'unknown' });
  return result;
}

function extractResponseFields(source) {
  const fields = new Map();
  const patterns = [
    /(?:res|resp|respone|response|answer)\??\.body((?:\??\.[A-Za-z_$][\w$]*|\[['"][^'"]+['"]\])*)/g,
    /\bbody((?:\??\.[A-Za-z_$][\w$]*|\[['"][^'"]+['"]\])*)/g,
  ];
  for (const pattern of patterns) {
    for (const match of source.matchAll(pattern)) {
      const chain = match[1]
        .replaceAll('?.', '.')
        .replace(/\[['"]([^'"]+)['"]\]/g, '.$1')
        .replace(/^\./, '');
      if (!chain || chain === 'toString') continue;
      const parts = chain.split('.').filter(Boolean);
      for (let i = 1; i <= parts.length; i += 1) {
        const path = parts.slice(0, i).join('.');
        if (!['push', 'toString', 'data'].includes(path.split('.').at(-1))) fields.set(path, EVIDENCE.source);
      }
    }
  }
  for (const match of source.matchAll(/(?:res|resp|respone)\.body\[['"]([^'"]+)['"]\]/g)) fields.set(match[1], EVIDENCE.source);
  return fields;
}

function functionRanges(source) {
  const candidates = [];
  const patterns = [
    /(?:async\s+)?function(?:\s+\w+)?\s*\([^)]*\)\s*\{/g,
    /(?:const|let)\s+\w+\s*=\s*(?:async\s*)?\([^)]*\)\s*=>\s*\{/g,
    /(?:async\s*)?\([^)]*\)\s*=>\s*\{/g,
    /(?:async\s+)?[A-Za-z_$][\w$]*\s*\([^)]*\)\s*\{/g,
  ];
  for (const pattern of patterns) {
    for (const match of source.matchAll(pattern)) {
      const name = match[0].match(/^(?:async\s+)?([A-Za-z_$][\w$]*)\s*\(/)?.[1];
      if (['if', 'for', 'while', 'switch', 'catch'].includes(name)) continue;
      const open = match.index + match[0].lastIndexOf('{');
      const body = balancedSlice(source, open);
      candidates.push({ start: open, end: open + body.length });
    }
  }
  return candidates;
}

function containingFunction(ranges, index, sourceLength) {
  return ranges
    .filter((range) => range.start < index && range.end > index)
    .sort((a, b) => (a.end - a.start) - (b.end - b.start))[0] || { start: 0, end: sourceLength };
}

function extractConsumerEvidence() {
  const files = listTree(MOEKOE_ROOT, APP_COMMIT, 'src').filter((path) => /\.(js|vue)$/.test(path));
  const routes = new Map();
  for (const file of files) {
    const source = show(MOEKOE_ROOT, APP_COMMIT, file);
    const allRequestRegex = /(?:\w+\.)?(?:get|post|put|del|patch)\s*\(\s*(['"`])(\/[^'"`$?]+)(?:\?[^'"`]*)?\1/g;
    for (const request of source.matchAll(allRequestRegex)) {
      const route = request[2].replace(/\/$/, '') || '/';
      if (!routes.has(route)) routes.set(route, { files: new Set(), fields: new Set() });
      routes.get(route).files.add(file);
    }
    const regex = /(?:(?:(?:const|let)\s+)?(\w+)\s*=\s*)?await\s+(?:\w+\.)?(get|post|put|del|patch)\s*\(\s*(['"`])(\/[^'"`$?]+)(?:\?[^'"`]*)?\3/g;
    const ranges = functionRanges(source);
    for (const match of source.matchAll(regex)) {
      const route = match[4].replace(/\/$/, '') || '/';
      if (!routes.has(route)) routes.set(route, { files: new Set(), fields: new Set() });
      const evidence = routes.get(route);
      evidence.files.add(file);
      const variable = match[1];
      if (variable) {
        const range = containingFunction(ranges, match.index, source.length);
        const segment = source.slice(match.index + match[0].length, range.end);
        const fieldRegex = new RegExp(`\\b${variable.replace('$', '\\$')}((?:(?:\\?\\.|\\.)[A-Za-z_$][\\w$]*)+)`, 'g');
        for (const fieldMatch of segment.matchAll(fieldRegex)) {
          const parts = fieldMatch[1].replaceAll('?.', '.').replace(/^\./, '').split('.');
          while (['sort', 'map', 'filter', 'forEach', 'find', 'some', 'every', 'includes', 'slice', 'join', 'replace', 'toString'].includes(parts.at(-1))) parts.pop();
          const path = parts.join('.');
          if (path && parts.length <= 6) evidence.fields.add(path);
        }
      }
    }
  }
  const searchEvidence = routes.get('/search');
  if (!searchEvidence) throw new Error('固定 PC 消费端缺少 /search 路由');
  const searchSources = [
    ['src/views/Search.vue', ['FileHash', 'HQFileHash', 'SQFileHash', 'OriSongName', 'SongName', 'FileName', 'SingerName', 'Image', 'Duration']],
    ['src/components/search/SongSearchList.vue', ['OriSongName', 'SongName', 'SingerName', 'Duration']],
  ];
  for (const [file, fields] of searchSources) {
    const source = show(MOEKOE_ROOT, APP_COMMIT, file);
    searchEvidence.files.add(file);
    for (const field of fields) {
      if (!new RegExp(`\\bsong(?:\\?\\.|\\.)${field}\\b`).test(source)) {
        throw new Error(`固定 PC 单曲对象缺少字段消费证据：${file}:${field}`);
      }
      searchEvidence.fields.add(`data.lists[].${field}`);
    }
  }
  const searchView = show(MOEKOE_ROOT, APP_COMMIT, 'src/views/Search.vue');
  for (const evidence of [
    'searchResults.value = response.data.lists',
    `v-else-if="searchType === 'song'" :songs="searchResults"`,
  ]) if (!searchView.includes(evidence)) throw new Error(`固定 PC 单曲搜索数据流证据缺失：${evidence}`);
  return routes;
}

function yamlScalar(value) {
  if (value == null) return 'null';
  if (typeof value === 'boolean' || typeof value === 'number') return String(value);
  return JSON.stringify(String(value));
}

function yamlList(values, indent) {
  if (!values.length) return `${' '.repeat(indent)}[]`;
  return values.map((value) => `${' '.repeat(indent)}- ${yamlScalar(value)}`).join('\n');
}

function catalogYaml(endpoints) {
  const lines = [
    'schema_version: 1',
    `generated_from_api_commit: ${yamlScalar(API_COMMIT)}`,
    `generated_from_app_commit: ${yamlScalar(APP_COMMIT)}`,
    'platform: "lite"',
    `module_count: ${endpoints.length}`,
    'endpoints:',
  ];
  for (const endpoint of endpoints) {
    lines.push(`  - id: ${yamlScalar(endpoint.id)}`);
    lines.push(`    module: ${yamlScalar(endpoint.module)}`);
    lines.push(`    description: ${yamlScalar(endpoint.description)}`);
    lines.push(`    domain: ${yamlScalar(endpoint.domain)}`);
    lines.push(`    wrapper_route: ${yamlScalar(endpoint.wrapperRoute)}`);
    lines.push(`    source: ${yamlScalar(`module/${endpoint.module}.js@${API_COMMIT}`)}`);
    const androidEvidence = homeSliceEvidence(endpoint.id);
    if (androidEvidence.length) {
      lines.push('    android_evidence:');
      for (const evidence of androidEvidence) lines.push(`      - ${yamlScalar(evidence)}`);
    }
    lines.push('    platform: "lite"');
    lines.push(`    authentication: ${yamlScalar(endpoint.authentication)}`);
    lines.push(`    operation: ${yamlScalar(endpoint.operation)}`);
    lines.push(`    product_scope: ${yamlScalar(endpoint.productScope)}`);
    lines.push(`    validation: ${yamlScalar(endpoint.validation)}`);
    lines.push('    response_handling:');
    lines.push(`      transformed: ${endpoint.transforms}`);
    lines.push(`      cookie_writeback: ${endpoint.cookieWriteback}`);
    lines.push(`      risk: ${yamlScalar(endpoint.riskHandling)}`);
    lines.push(`    documentation: ${yamlScalar(`endpoints/${endpoint.domain}.md#${endpoint.anchor}`)}`);
    lines.push('    upstream_requests:');
    for (const request of endpoint.upstreamRequests) {
      lines.push(`      - sequence: ${request.sequence}`);
      lines.push(`        transport: ${yamlScalar(request.kind)}`);
      lines.push(`        base_url: ${yamlScalar(request.baseUrl)}`);
      lines.push(`        path: ${yamlScalar(request.path)}`);
      lines.push(`        method: ${yamlScalar(request.method)}`);
      lines.push(`        router: ${yamlScalar(request.router)}`);
      lines.push(`        signing: ${yamlScalar(request.signing)}`);
      lines.push(`        response_type: ${yamlScalar(request.responseType)}`);
    }
    lines.push('    request_fields:');
    if (endpoint.requestFields.length === 0) lines.push('      []');
    for (const field of endpoint.requestFields) {
      lines.push(`      - name: ${yamlScalar(field.name)}`);
      lines.push(`        type: ${yamlScalar(field.type)}`);
      lines.push(`        required: ${field.required}`);
      lines.push(`        locations: [${[...field.locations].map(yamlScalar).join(', ')}]`);
      lines.push(`        default: ${yamlScalar(field.default)}`);
      lines.push(`        evidence: [${[...field.evidence].map(yamlScalar).join(', ')}]`);
    }
    lines.push('    response_fields:');
    const responseFields = [...endpoint.responseFields.entries()];
    if (responseFields.length === 0) lines.push(`      - path: "*"\n        evidence: ${yamlScalar(EVIDENCE.unknown)}`);
    else for (const [path, evidence] of responseFields) {
      lines.push(`      - path: ${yamlScalar(path)}`);
      lines.push(`        evidence: ${yamlScalar(evidence)}`);
      const condition = responseFieldCondition(endpoint, path);
      if (condition) lines.push(`        condition: ${yamlScalar(condition)}`);
    }
    lines.push(`    consumer_files: [${[...endpoint.consumerFiles].map(yamlScalar).join(', ')}]`);
  }
  while (lines.at(-1) === '') lines.pop();
  return `${lines.join('\n')}\n`;
}

function endpointMarkdown(domain, endpoints) {
  const title = DOMAIN_NAMES[domain];
  const lines = [
    `# ${title} API`, '',
    `本页记录 ${endpoints.length} 个 Lite 包装模块。所有结论均来自固定静态源码；“上游请求”才是 Android 直连需要实现的协议。`, '',
    '[返回 API 文档首页](../README.md)', '',
  ];
  for (const endpoint of endpoints) {
    lines.push(`<a id="${endpoint.anchor}"></a>`);
    lines.push(`## ${endpoint.id} · ${endpoint.description}`, '');
    lines.push('| 属性 | 值 |', '|---|---|');
    lines.push(`| 模块 | ${mdCode(`${endpoint.module}.js`)} |`);
    lines.push(`| Node 包装路由 | ${mdCode(endpoint.wrapperRoute)} |`);
    lines.push(`| 认证 | ${mdCode(endpoint.authentication)}（${EVIDENCE.inferred}，除非响应/源码另有证据） |`);
    lines.push(`| 操作属性 | ${mdCode(endpoint.operation)} |`);
    lines.push(`| 产品范围 | ${mdCode(endpoint.productScope)} |`);
    lines.push(`| 验证 | ${mdCode(endpoint.validation)} |`);
    lines.push(`| 响应转换 | ${endpoint.transforms ? mdCode(EVIDENCE.source) : '未发现模块级转换'} |`);
    lines.push(`| Cookie 回写 | ${endpoint.cookieWriteback ? mdCode(EVIDENCE.source) : '未发现'} |`);
    lines.push(`| 风控 | ${mdCode(endpoint.riskHandling)} |`);
    lines.push(`| 来源 | ${mdCode(`MoeKoeMusic/api@${API_COMMIT}:module/${endpoint.module}.js`)} |`, '');
    const androidEvidence = homeSliceEvidence(endpoint.id);
    if (androidEvidence.length) {
      lines.push('### Android 首页迁移证据', '');
      for (const evidence of androidEvidence) lines.push(`- ${mdCode(evidence)}`);
      lines.push('');
    }
    lines.push('### 上游请求', '', '| 序号 | 传输 | Base URL | Path | Method | x-router | 签名 | 响应 |', '|---:|---|---|---|---|---|---|---|');
    for (const request of endpoint.upstreamRequests) {
      lines.push(`| ${request.sequence} | ${mdCode(request.kind)} | ${mdCode(request.baseUrl)} | ${mdCode(request.path)} | ${mdCode(request.method)} | ${mdCode(request.router || '-')} | ${mdCode(request.signing)} | ${mdCode(request.responseType)} |`);
    }
    lines.push('', '### 请求字段', '');
    if (endpoint.requestFields.length === 0) lines.push(`未发现模块专属请求字段；公共字段见 [PROTOCOL](../PROTOCOL.md)。证据：\`${EVIDENCE.unknown}\`。`);
    else {
      lines.push('| 字段 | 类型 | 必填 | 位置 | 默认值 | 证据 |', '|---|---|---:|---|---|---|');
      for (const field of endpoint.requestFields) {
        lines.push(`| ${mdCode(field.name)} | ${mdCode(field.type)} | ${field.required ? '是' : '否/未知'} | ${[...field.locations].map(mdCode).join(', ')} | ${mdCode(field.default ?? '-')} | ${[...field.evidence].map(mdCode).join(', ')} |`);
      }
    }
    lines.push('', '### 返回值证据', '');
    const responseFields = [...endpoint.responseFields.entries()];
    if (responseFields.length === 0) lines.push(`上游响应由包装层透传，静态证据未确认字段级结构：\`${EVIDENCE.unknown}\`。不得据此生成严格 Kotlin DTO。`);
    else {
      const conditional = responseFields.some(([path]) => responseFieldCondition(endpoint, path));
      lines.push(
        conditional ? '| Body 路径 | 条件 | 证据 |' : '| Body 路径 | 证据 |',
        conditional ? '|---|---|---|' : '|---|---|',
      );
      for (const [path, evidence] of responseFields) {
        const condition = responseFieldCondition(endpoint, path);
        lines.push(conditional
          ? `| ${mdCode(path)} | ${mdCode(condition || '-')} | ${mdCode(evidence)} |`
          : `| ${mdCode(path)} | ${mdCode(evidence)} |`);
      }
      lines.push('', '这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。');
    }
    lines.push('', '### Android 映射', '', '| 项目 | 建议 |', '|---|---|');
    lines.push(`| DataSource 操作 | ${mdCode(endpoint.operationName)} |`);
    lines.push(`| Request DTO | ${mdCode(endpoint.requestDto)} |`);
    const responseMapping = ANDROID_TYPED_WIRE_ENDPOINTS.has(endpoint.id)
      ? `${mdCode(endpoint.responseDto)}；名称为静态候选，现行实现由 internal ${mdCode('@Serializable')} 类型化 wire DTO 直接承接 Retrofit 响应`
      : `${mdCode(endpoint.responseDto)}；含 UNKNOWN 时不得据此生成严格 DTO`;
    lines.push(`| Response DTO | ${responseMapping} |`);
    lines.push(`| 传输实现 | ${mdCode(endpoint.transport)} |`);
    lines.push(`| 协议组件 | ${endpoint.components.map(mdCode).join(', ') || mdCode('none')} |`);
    lines.push(`| 领域映射 | 在 ${mdCode('core:data')} 映射；不得向 UI 暴露 ${endpoint.responseDto} |`, '');
  }
  while (lines.at(-1) === '') lines.pop();
  return `${lines.join('\n')}\n`;
}

function pascal(value) {
  return value.split('_').map((part) => part ? part[0].toUpperCase() + part.slice(1) : '').join('');
}

function camel(value) {
  const p = pascal(value);
  return p ? p[0].toLowerCase() + p.slice(1) : value;
}

function homeSliceEvidence(endpointId) {
  const mobileApi = `MoeKoeMusic-Mobile/api@${MOBILE_API_COMMIT}`;
  const mobileConsumer = `MoeKoeMusic-Mobile@${MOBILE_COMMIT}`;
  const evidence = {
    'API-DISCOVER-003': [`${mobileApi}:module/everyday_recommend.js`, `${mobileConsumer}:src/features/home/load-home-data.ts`],
    'API-DISCOVER-009': [`${mobileApi}:module/top_card.js`, `MoeKoeMusic@${TOP_CARD_PC_COMMIT}:src/components/home/HomeRecommendations.vue`],
    'API-DISCOVER-012': [`${mobileApi}:module/top_playlist.js`, `${mobileConsumer}:src/features/home/load-home-data.ts`],
    'API-DISCOVER-013': [`${mobileApi}:module/top_song.js`, `${mobileConsumer}:src/features/home/load-home-data.ts`],
    'API-SONG-011': [`${mobileApi}:module/song_url.js`, `${mobileConsumer}:src/features/player/song-url.ts`],
    'API-RANKING-003': [`${mobileApi}:module/rank_list.js`, `${mobileConsumer}:src/features/home/load-home-data.ts`],
    'API-RANKING-001': [`${mobileApi}:module/rank_audio.js`, `${mobileConsumer}:src/features/home/load-home-data.ts`],
    'API-PLAYLIST-007': [`${mobileApi}:module/playlist_track_all.js`, `${mobileConsumer}:src/features/playlist/playlist-api.ts`],
  };
  return evidence[endpointId] || [];
}

function staticDocuments(endpoints, consumerRoutes) {
  const domainCounts = new Map();
  for (const endpoint of endpoints) domainCounts.set(endpoint.domain, (domainCounts.get(endpoint.domain) || 0) + 1);
  const unknownCount = endpoints.filter((endpoint) => endpoint.responseFields.size === 0).length;
  const consumerCount = endpoints.filter((endpoint) => endpoint.consumerFiles.size > 0).length;
  const unmatched = [...consumerRoutes.keys()].filter((route) => !endpoints.some((endpoint) => endpoint.wrapperRoute === route));
  const generatedDocuments = {
    'README.md': `# Lite 静态 API 契约\n\n> 状态：静态证据基线，不代表上游接口当前可用或获得服务授权。\n\n## 基线\n\n- PC 消费端：\`MoeKoeMusic@${APP_COMMIT}\`\n- API 协议源：\`MoeKoeMusic/api@${API_COMMIT}\`\n- 平台：概念版 \`lite\`（\`appid=3116\`、\`clientver=11440\`）\n- 模块：${endpoints.length}\n- 验证：仅静态分析，无外部网络请求\n\n## 阅读顺序\n\n1. [公共协议](PROTOCOL.md)\n2. [机器可读目录](catalog.yaml)\n3. [Android/NIA 映射](ANDROID_MAPPING.md)\n4. [验证与缺口](VERIFICATION.md)\n5. [接口领域索引](#接口领域)\n\nNode 包装路由只描述 PC 调用的本地 Express 接口；每个接口章节中的“上游请求”才是 Android 直连契约。字段证据等级为 \`${Object.values(EVIDENCE).join('`、`')}\`。\n\n## 接口领域\n\n${[...domainCounts.entries()].sort((a, b) => DOMAIN_ORDER.indexOf(a[0]) - DOMAIN_ORDER.indexOf(b[0])).map(([domain, count]) => `- [${DOMAIN_NAMES[domain]}](endpoints/${domain}.md)：${count}`).join('\n')}\n\n## 完整性摘要\n\n- 全量模块：${endpoints.length}/${endpoints.length}\n- 固定 PC 消费端直接使用：${consumerCount}\n- 无字段级响应证据：${unknownCount}\n- 未映射的固定 PC 请求路由：${unmatched.length}\n\n完整统计和限制见 [VERIFICATION](VERIFICATION.md)。\n\n## 重新生成与校验\n\n在 Resonote 根目录执行：\n\n\`\`\`shell\nnode docs/api/tools/generate-docs.mjs\nnode docs/api/tools/validate-docs.mjs\n\`\`\`\n\n工具只读取固定 Git 对象；如 MoeKoeMusic 不在默认相邻目录，可通过 \`MOEKOE_ROOT\` 指向仓库。生成器会替换本目录中的领域文档、Schema 和 Fixture 索引。\n`,
    'PROTOCOL.md': `# Lite 公共协议\n\n## 请求管线\n\nAndroid 直连必须复现固定 API 基线的请求上下文：Lite \`appid=3116\`、\`clientver=11440\`、秒级 \`clienttime\`、持久化设备身份，以及按端点选择的签名、请求体和响应解码。默认网关为 \`https://gateway.kugou.com\`；带 \`x-router\` 的请求仍以该网关为传输入口。\n\n## 公共参数与请求头\n\n| 名称 | 位置 | 来源 | 说明 |\n|---|---|---|---|\n| \`dfid\` | Query/Header | DeviceSession | 设备注册结果；未注册时源码可使用占位值 |\n| \`mid\` | Query/Header | DeviceIdentity | 由持久化 GUID 按固定算法派生 |\n| \`uuid\` | Query | Provider | 固定基线默认 \`-\` |\n| \`appid\` | Query | Lite Config | 3116 |\n| \`clientver\` | Query | Lite Config | 11440，个别端点会覆盖 |\n| \`clienttime\` | Query/Header | Clock | 秒级时间戳，必须由可注入时钟提供 |\n| \`token\` / \`userid\` | Query/Body | Session | 登录后按端点注入 |\n| \`x-router\` | Header | Endpoint | 选择网关后端，不能误当作 Retrofit Base URL |\n\n固定源码还注入 User-Agent 与若干 KG 路由头。具体字面量和签名材料不在文档重复，后续实现须从固定 MIT 源码逐项迁移并用 golden test 锁定；凭证型配置不得写入文档或 Fixture。\n\n## 签名模式\n\n- \`android\`：对规范化 Query 和序列化 Body 生成 Android signature。\n- \`web\`：用于二维码等 Web 登录协议。\n- \`register\`：用于设备注册协议。\n- \`none\`：源码显式跳过 signature，可能仍有端点自定义 key。\n- \`unknown\`：无法仅由模块静态确定，实施前必须补证据。\n\n签名器必须依赖可注入 \`Clock\`，并保持参数排序、字符串化、Body 字节和 URL 编码与 Node 基线一致。\n\n## 会话和设备身份\n\n会话至少包含 \`token\`、\`userid\`、\`vip_token\`、\`vip_type\`；设备上下文至少包含 \`dfid\`、GUID、MID、DEV 和平台标识。PC 的 Authorization 拼接只是包装层传输格式，Android 直连不得把它原样发送给上游，而应按端点写入 Query、Body、Header 或 Cookie。敏感值必须持久化加密，日志和 Fixture 一律脱敏。\n\n## 加密与二进制\n\n固定基线出现 AES、RSA 公钥加密、歌单/云盘 AES 封装、KRC 解码、ArrayBuffer 和 PCM/文件二进制。标准 HTTP/JSON 端点使用 Retrofit（动态 URL 可用 \`@Url\` 表达）；二进制、加密或多阶段特殊协议由内部 \`ProtocolTransport\` 使用共享 OkHttp \`Call.Factory\`，不把特殊编排塞入普通 Retrofit 接口或同步 Interceptor。\n\n## 错误模型\n\n必须分别保留 HTTP 失败、Provider 业务失败、签名/设备失败、登录过期、风控验证、解密失败、结构不兼容和网络失败。上游常同时使用 HTTP 状态与 Body 内 \`status\`/\`error_code\`；静态文档没有证明二者存在统一关系。\n\n## 响应兼容策略\n\n\`UNKNOWN\` 或仅 \`CONSUMER_CONFIRMED\` 的响应不得直接转成全字段非空 DTO。初次实现应忽略未知键、对漂移字段使用受控宽容序列化，并在 Network DTO 到领域模型边界完成校验。\n`,
    'ANDROID_MAPPING.md': `# Android / NIA 映射\n\n## 模块边界\n\n\`core:network\` 按 NIA 方式统一拥有共享 OkHttp、Retrofit、序列化、Lite 签名、设备、会话、Cookie、加密、Network DTO、解码与通用风控协调；\`core:data\` 拥有 Repository、缓存与领域映射；Feature/ViewModel 只依赖 Repository。\n\n## NIA 对应方式\n\n- 以 \`ApiNetworkDataSource\` 暴露远端能力，具体 Retrofit/OkHttp 类保持 internal。\n- 每个接口章节给出稳定操作名与静态 DTO 命名候选；候选名称不表示对应类已经实现，已迁移状态以本页纵切片记录和代码为准。\n- 固定 API 包只把响应声明为 \`ApiResponse<T = any>\`，Mobile runtime 进一步暴露 \`MobileApiResult.body: unknown\`，没有可直接复制的九接口 wire response DTO。Mobile 的 \`HomeSong\`、\`PlayerTrack\`、\`PlaylistInfo\` 等是消费模型；其 \`Record<string, unknown>\` 读取路径和 PC 的实际字段访问共同作为 Android wire DTO 的字段证据。\n- Retrofit converter 直接把标准 HTTP/JSON 响应反序列化为 internal \`@Serializable\` wire DTO；\`ignoreUnknownKeys\` 只用于兼容服务端新增字段，已知的字符串/数字变体由字段 serializer 显式处理。DataSource 校验必要字段后映射 Network model，wire DTO 与 Network model 均不得进入 Compose 或公共领域模型。\`JsonObject\` 只保留在加密、二进制或确有多形结构的特殊协议边界。\n- Repository 使用 fake DataSource 测试，不以脆弱的调用顺序 mock 为主。\n- 标准 HTTP/JSON 业务接口由内部 \`MusicApi\` 以 Retrofit 声明；请求签名、公共参数、Session Header 与 Cookie 由 \`ApiProtocolInterceptor\` 基于 \`@Tag\` 请求策略统一注入。\n- \`Call.Factory\` 只作为最底层传输抽象，并由 \`ProtocolTransport\` 用于设备注册、加密登录和风控验证等二进制、加密或多阶段特殊协议；普通业务接口不得用它重新实现一套 Retrofit。\n\n## 首条纵切片\n\n按搜索 → 播放地址 → 歌词 → Media3 播放实施。开始 Kotlin 代码前，先为相应端点补齐签名 golden fixture、脱敏响应 fixture 或明确的类型化 DTO 契约。\n\n## 禁止依赖\n\n- UI/Feature 不直接依赖 Retrofit、OkHttp 或 API DTO。\n- Provider 语义只存在于 \`core:network\` 的内部协议 package，不向 Feature 或领域模型暴露。\n- 领域模型不保留上游字段命名和传输层可空性。\n- 不把 PC → Node 的 Authorization 桥接协议误作上游协议。\n`,
    'VERIFICATION.md': `# 静态验证报告\n\n## 结果\n\n| 检查 | 结果 |\n|---|---:|\n| API 模块覆盖 | ${endpoints.length}/${endpoints.length} |\n| 有 PC 消费证据的接口 | ${consumerCount} |\n| 无字段级响应证据 | ${unknownCount} |\n| 未映射 PC 请求路由 | ${unmatched.length} |\n| 外部请求 | 0 |\n| 实时验证 | 0 |\n\n## 证据优先级\n\n1. API 模块实际构造和转换：\`${EVIDENCE.source}\`。\n2. 固定 PC 应用读取字段：\`${EVIDENCE.consumer}\`。\n3. \`interface.d.ts\` 或现有说明：\`${EVIDENCE.declared}\`。\n4. 固定仓库已有脱敏样例：\`${EVIDENCE.fixture}\`。\n5. 静态推断：\`${EVIDENCE.inferred}\`。\n6. 无证据：\`${EVIDENCE.unknown}\`。\n\n## 固有限制\n\n静态源码通常透传上游 Body，\`interface.d.ts\` 的返回值又多为 \`ApiResponse<any>\`，因此本基线只能完整证明请求构造，不能完整证明所有响应字段、可空性、枚举全集或当前可用性。未列出字段不代表不存在。\n\n## 未映射请求\n\n${unmatched.length ? unmatched.map((route) => `- \`${route}\``).join('\n') : '- 无'}\n\n## Fixture 状态\n\n固定基线没有发现可证明为完整上游响应且已脱敏的 JSON Fixture。本次不制造样例；[fixtures/README](fixtures/README.md) 记录了准入规则。\n`,
    'schemas/requests.yaml': schemaYaml(endpoints, 'request'),
    'schemas/responses.yaml': schemaYaml(endpoints, 'response'),
    'schemas/README.md': `# 静态 Schema 说明\n\n- \`requests.yaml\` 合并固定 \`interface.d.ts\` 声明与模块实际构造字段。\n- \`responses.yaml\` 只列 API 转换代码和固定 PC 消费端能够证明的 Body 路径。\n- \`type: unknown\` 是有意保守结果；在获得合规的脱敏响应 Fixture 前不得收紧。\n- \`path: "*"\` + \`${EVIDENCE.unknown}\` 表示包装层透传响应且没有字段级静态证据。\n`,
    'fixtures/README.md': `# Fixture 准入规则\n\n当前静态基线没有可安全认定为完整上游响应的 JSON Fixture，因此本目录暂不包含伪造样例。后续 Fixture 必须：\n\n- 来自固定源码已提交样例或经批准的只读采样。\n- 旁置来源、提交或采样条件。\n- 删除 token、userid、Cookie、dfid、MID、GUID、设备信息和账号内容。\n- 不以人工拼装 JSON 冒充真实响应。\n- 在响应 Schema 中把相应字段标为 \`${EVIDENCE.fixture}\`。\n`,
  };
  generatedDocuments['ANDROID_MAPPING.md'] = generatedDocuments['ANDROID_MAPPING.md']
    .replace(
      '以 `ApiNetworkDataSource` 暴露远端能力，具体 Retrofit/OkHttp 类保持 internal。',
      '以按业务能力拆分的 Network DataSource 暴露远端能力，具体 Retrofit/OkHttp 类保持 internal；生产代码不提供全能力聚合接口。',
    )
    .replace(
      '`ApiNetworkDataSource` 暴露每日推荐、`top_card`、推荐歌单、新歌速递和歌曲 URL 五个窄操作；',
      '`HomeNetworkDataSource` 暴露每日推荐、`top_card`、推荐歌单和新歌速递；歌曲 URL 由独立 `PlaybackNetworkDataSource` 提供，两者',
    );
  generatedDocuments['ANDROID_MAPPING.md'] = generatedDocuments['ANDROID_MAPPING.md']
    .replace(
      '- 每个接口章节给出稳定操作名与静态 DTO 命名候选；',
      '- 生产 DataSource 按消费者职责拆为 Home、Catalog、Ranking、Playlist、Search、Lyrics、Video、Recognition、UserProfile 与 Library；每个公开 Network port 独立成文件，不保留 catch-all 接口文件。共享歌曲 DTO 解码器独立复用，特殊协议共用的原始响应模型归属 `protocol`，协议层不得反向依赖 `retrofit` 包。\n- 每个接口章节给出稳定操作名与静态 DTO 命名候选；',
    )
    .replace(
      '- 标准 HTTP/JSON 业务接口由内部 `MusicApi` 以 Retrofit 声明；',
      '- 标准 HTTP/JSON 端点分别声明在内部 `ContentApi`、`PlaybackApi`、`SearchApi`、`LyricsApi`、`VideoApi`、`RecognitionApi` 与 `AccountApi`，空的 `MusicApi` 只作为 Retrofit 聚合创建入口；它们共享同一个 Retrofit、OkHttp 和拦截器链。',
    );
  generatedDocuments['ANDROID_MAPPING.md'] = generatedDocuments['ANDROID_MAPPING.md'].replace(
    '固定 API 包只把响应声明为 `ApiResponse<T = any>`，Mobile runtime 进一步暴露 `MobileApiResult.body: unknown`，没有可直接复制的九接口 wire response DTO。Mobile 的 `HomeSong`、`PlayerTrack`、`PlaylistInfo` 等是消费模型；其 `Record<string, unknown>` 读取路径和 PC 的实际字段访问共同作为 Android wire DTO 的字段证据。',
    '固定 API 包的 `ApiResponse<T = any>` 明确定义了泛型响应模式；默认 `any` 只是 TypeScript 对尚未声明端点 Body 类型的退路。Android 的 Retrofit Service 直接返回 internal `ApiResponse<具体 Data DTO>`，对应实测服务端 JSON 的 `status/error_code/data` 信封；没有该信封的播放地址使用独立 DTO。HTTP 状态、Header 与 Cookie 由 Retrofit 异常映射、单次请求上下文和 OkHttp CookieJar/Session 在内部处理，不向 Service 返回类型或 DataSource 暴露 `retrofit2.Response`。各端点 Data DTO 仍需结合 Mobile 消费模型与字段读取、PC 实际字段访问和脱敏实测来收敛。',
  );
  generatedDocuments['ANDROID_MAPPING.md'] = generatedDocuments['ANDROID_MAPPING.md']
    .replace(
      '统一拥有共享 OkHttp、Retrofit、序列化、Lite 签名、设备、会话、Cookie、加密、Network DTO、解码与通用风控协调',
      '统一拥有共享 OkHttp、Retrofit、序列化、Lite 签名、设备、会话、Cookie、加密、Network DTO、解码与风控协议',
    )
    .replace(
      'HTTP 状态、Header 与 Cookie 由 Retrofit 异常映射、单次请求上下文和 OkHttp CookieJar/Session 在内部处理',
      'HTTP 状态由 Retrofit 异常映射，`ssa-code` Header 由受限响应拦截器归一化，Cookie 由 Session/特殊协议在内部处理',
    )
    .replace(
      '请求签名、公共参数、Session Header 与 Cookie 由 `ApiProtocolInterceptor` 基于 `@Tag` 请求策略统一注入。',
      '方法级 `@ApiRequestPolicy` 声明静态策略；`ApiDefaultsInterceptor` 读取已初始化的 Session 内存快照并注入公共参数/Header/Cookie，`ApiSigningInterceptor` 通过 Retrofit `Invocation` Tag 对最终 Query 与序列化 Body 字节签名。',
    );
  generatedDocuments['ANDROID_MAPPING.md'] = generatedDocuments['ANDROID_MAPPING.md'].replace(
    '- Repository 使用 fake DataSource 测试，不以脆弱的调用顺序 mock 为主。',
    '- Retrofit wire DTO 只描述上游传输结构并保持 internal；Network DataSource 校验必要字段后，将不同端点归一化为按稳定业务概念命名的 Network model；Repository 再映射为 `core:model` 领域模型，三层不得复用同一个类。\n- 首页、搜索、榜单和歌单共享歌曲语义时统一映射到 `NetworkSong`，不复制 `NetworkHomeSong`、`NetworkSearchSong` 等页面所有权类型；传输结构确实不同时可以拆 wire DTO。wire DTO 按协议域拆文件，Network/Domain model 按内聚业务概念拆文件，避免 catch-all 模型文件。\n- Repository 使用 fake DataSource 测试，不以脆弱的调用顺序 mock 为主。',
  );
  generatedDocuments['VERIFICATION.md'] = generatedDocuments['VERIFICATION.md'].replace(
    '静态源码通常透传上游 Body，`interface.d.ts` 的返回值又多为 `ApiResponse<any>`，因此本基线只能完整证明请求构造，不能完整证明所有响应字段、可空性、枚举全集或当前可用性。未列出字段不代表不存在。',
    '静态源码通常透传上游 Body；`interface.d.ts` 的 `ApiResponse<T = any>` 可以证明 Node 层统一 HTTP 调用结果（`status/body/headers/cookie`），但默认 `any` 不能证明各端点 Body `T` 的完整字段。因此本基线只能完整证明请求构造与外层传输契约，不能完整证明所有服务端 Body 字段、可空性、枚举全集或当前可用性。未列出字段不代表不存在。',
  );
  generatedDocuments['README.md'] = generatedDocuments['README.md'].replace(
    '工具只读取固定 Git 对象；如 MoeKoeMusic 不在默认相邻目录，可通过 `MOEKOE_ROOT` 指向仓库。',
    '工具只读取固定 Git 对象；默认通过 Git common directory 定位主 checkout 的相邻参考仓库，因此普通 checkout 与 worktree 使用同一规则。如参考仓库不在该位置，可通过 `MOEKOE_ROOT` 和 `MOEKOE_MOBILE_ROOT` 分别覆盖 PC 与 Mobile checkout。',
  );
  generatedDocuments['ANDROID_MAPPING.md'] = generatedDocuments['ANDROID_MAPPING.md'].replace(
    '## 首条纵切片\n\n按搜索 → 播放地址 → 歌词 → Media3 播放实施。开始 Kotlin 代码前，先为相应端点补齐签名 golden fixture、脱敏响应 fixture 或明确的类型化 DTO 契约。',
    '## 通用风控\n\n`core:network` 从 Body 与 Header 统一识别 `20028`/`ssaCode` Challenge，通过不依赖 UI 的 `ApiRiskVerifier` 串行完成验证。普通请求验证成功后重新生成时间戳和签名并最多重试一次；验证接口必须旁路协调器，超时或断网不得触发重试。\n\n## 首页首批纵切片\n\n- `HomeNetworkDataSource` 提供每日推荐、`top_card` 和新歌速递，`CatalogNetworkDataSource` 提供推荐歌单，歌曲 URL 由独立 `PlaybackNetworkDataSource` 提供；它们共同复用设备注册、Session、签名、风控和请求执行器。\n- `HomeRepository` 并发刷新三个首页区块，每日推荐在每次成功请求后重抽 6 首；单区失败保留旧快照，旧代际结果不得覆盖新请求。\n- `loadRadio(mode)` 按需加载 `card_id=1/2/3/4/6`，默认私人好歌为 1。\n- `SongPlaybackRepository` 只返回首个 HTTPS 主/备用地址、时长和扩展名，并类型化区分版权、VIP、网络、协议与风控失败。\n- 本批只落到 `core:network`、`core:data`、`core:model`，不包含 Compose、导航、Media3、Queue 或 Mini Player。\n\n这里的“首批”只覆盖首页首屏内容请求，不等于所有首页入口的目标页面已经可用。Feature 模块的 `api` 是跨功能导航/调用合同，不承载网络接口；首页作为 Tabs Shell 根页面当前使用单一 `:feature:home`，不建立空的 `:feature:home:api`。\n\n## 首页入口可达闭环\n\n- 排行榜快捷入口本身不请求网络，进入发现的榜单子页面后由发现领域加载 `API-RANKING-003`（榜单列表），进入具体榜单后使用 `API-RANKING-001`（榜单歌曲）。两者不得加入首页下拉刷新的并发组。\n- 精选歌单快捷入口进入发现的推荐歌单分类，复用已经实现的 `API-DISCOVER-012`，不复制 PC 固定个人歌单 ID，也不增加一个首页专属接口。\n- 首页 6 个推荐歌单和发现歌单共用详情目的地；点击后使用 `API-PLAYLIST-007` 分页读取歌单信息和歌曲。\n- 上述 `API-RANKING-003`、`API-RANKING-001`、`API-PLAYLIST-007` 是下一批共享发现/歌单数据能力，不扩充 `HomeRepository.refresh()` 的职责。',
  );
  generatedDocuments['ANDROID_MAPPING.md'] = generatedDocuments['ANDROID_MAPPING.md']
    .replace(
      '`core:network` 从 Body 与 Header 统一识别 `20028`/`ssaCode` Challenge，通过不依赖 UI 的 `ApiRiskVerifier` 串行完成验证。普通请求验证成功后重新生成时间戳和签名并最多重试一次；验证接口必须旁路协调器，超时或断网不得触发重试。',
      '`core:network` 将 `ssa-code` Header 归一化进类型化 `ApiResponse<T>`，仅在 `error_code=20028` 时上抛内部 Challenge。`core:data` 将其登记为不透明 `RiskChallengeHandle`，并通过 `RiskVerificationRepository` 暴露验证方式查询和证明提交；Feature/ViewModel 不依赖 Network 类型。验证成功后的单次显式重试仍由原发起流程持有，Interceptor、Authenticator、Network DataSource 和特殊协议传输均不得自动重放。验证接口必须旁路 Challenge 检测以避免递归。',
    )
    .replace('共同复用设备注册、Session、签名、风控和请求执行器。', '共同复用设备注册、Session、签名、类型化风控检测和 Retrofit 调用链。');
  generatedDocuments['ANDROID_MAPPING.md'] = generatedDocuments['ANDROID_MAPPING.md'].replace(
    '`SongPlaybackRepository` 只返回首个 HTTPS 主/备用地址、时长和扩展名，并类型化区分版权、VIP、网络、协议与风控失败。',
    '`SongPlaybackRepository` 只返回服务原生 HTTPS 主/备用地址、时长和扩展名；仅返回 HTTP 时报告 `InsecureMediaUrl` 协议错误，其他非空畸形地址报告 `MalformedResponse`，不通过改写 scheme 伪造安全地址。匿名 VIP 候选实测返回的 `error_code=35104` 与无 URL 的 VIP 响应统一映射为 `PlaybackUnavailableReason.Vip`；其他未知业务码仍保持服务拒绝，从而类型化区分版权、VIP、网络、协议与风控失败。',
  );
  generatedDocuments['ANDROID_MAPPING.md'] = generatedDocuments['ANDROID_MAPPING.md'].replace(
    '- 上述 `API-RANKING-003`、`API-RANKING-001`、`API-PLAYLIST-007` 是下一批共享发现/歌单数据能力，不扩充 `HomeRepository.refresh()` 的职责。',
    '- `API-RANKING-003`、`API-RANKING-001`、`API-PLAYLIST-007` 已由共享 Network DataSource、`RankingRepository` 与 `PlaylistRepository` 实现，并保持类型化错误、取消传播和 Mobile 分页语义：榜单按可消费歌曲是否填满当前页判断，歌单将非正总数视为未知且以原始页大小兜底；它们不扩充 `HomeRepository.refresh()` 的职责。\n- 推荐、榜单和歌单歌曲的音质同时读取显式 HQ/SQ Hash 与 `relate_goods` 可用档位；320K 映射为 `HighQuality`，不冒充 `HighResolution`。缺失歌手在领域层保留为 `null`，由 UI 本地化兜底；仅歌单协议需要的 `fileid` 留在 Network DTO，不进入 `OnlineSong`。',
  );
  generatedDocuments['ANDROID_MAPPING.md'] = generatedDocuments['ANDROID_MAPPING.md'].replace(
    '- `Call.Factory` 只作为最底层传输抽象，并由 `ProtocolTransport` 用于设备注册、加密登录和风控验证等二进制、加密或多阶段特殊协议；普通业务接口不得用它重新实现一套 Retrofit。',
    '- 会话传播使用 `sessionPropagation` 明确区分 `Full/DeviceOnly/None`：可信酷狗 API Host 默认在缓存存在时注入完整会话，登录、验证码和二维码只携带设备身份，非可信 Host 禁止传播 Session。`Full` 原样传播持久化的 Cookie 快照；Cookie 合并与认证字段清理由 Session 存储边界负责。`includeDefaultParams=false` 只关闭默认 Query，不关闭 Header/Cookie，歌词搜索因此仍使用 `Full`。\n- 响应认证分类由共享 verifier 负责。首个经 PC 源码、文档和真实 Canary 共同确认的规则是 `API-SEARCH-001 + error_code 152`，适用于 Mobile `search` 模块的歌曲及 `special/album/author/mv` 类型变体；匿名请求进入 `LoginRequired`，已认证请求进入 `SessionExpired` 并只清除认证字段。App 根级认证状态统一导航到可持续占位的登录门禁页面，不使用瞬时 Snackbar 代替认证流程；Feature 不解析 provider 状态码。\n- 远端认证响应携带请求前 Session revision；本地“接口要求登录”检查、门禁确认、Session 写入和失效清理由 `ApiSessionManager` 使用同一 Mutex 串行化。公开 `authenticationState` 也在该锁内读取 Store 最终值并推进外部变更的 revision，不得发出“认证字段已清但尚无门禁”的匿名中间态；即使没有 Flow 订阅，旧请求也不得清除或遮蔽新登录 Session。\n- `Call.Factory` 只作为最底层传输抽象，并由 `ProtocolTransport` 用于设备注册、加密登录和风控验证等二进制、加密或多阶段特殊协议；普通业务接口不得用它重新实现一套 Retrofit。Retrofit 按固定 NIA 基线通过 `dagger.Lazy<Call.Factory>` 延迟取得共享 Client。\n- 设备注册通过可注入 Provider 按 Mobile 合同读取总内存、品牌、Build ID、型号和厂商，缺失时使用 fallback，存储字段保留 Mobile 固定兼容值；携带 `ssa-code` 的响应按 2 MiB 上限有界读取并在拒绝路径关闭 Body。',
  );
  generatedDocuments['ANDROID_MAPPING.md'] = generatedDocuments['ANDROID_MAPPING.md'].replace(
    '## 首页首批纵切片',
    '## Mobile 39 API 与认证首期\n\n- `MoeKoeMusic-Mobile/src` 实际消费的固定 39 个 API 已全部提供 Network DataSource 与 Repository 能力；总账及逐项离线测试见 [MOBILE_MIGRATION.md](MOBILE_MIGRATION.md)。完整 catalog 的 164 个 Lite 模块不等于本期迁移数量。\n- 手机验证码、密码和二维码登录协议已实现；登录成功先安全持久化 Session，存储失败不得报告成功。完整登录表单仍待后续接入。\n- 用户资料、个人歌单写操作、云盘和每日 VIP 等登录后能力只通过认证 Fake Session、MockWebServer 与 synthetic fixture 验证，尚未使用真实账号联调。\n- 搜索包含单曲、歌单、专辑、歌手、MV、综合搜索、热搜与建议；歌词、视频和听歌识曲使用独立 Network/Data 边界。\n- 所有标准响应统一把非零 `error_code` 视为业务失败；识曲 `status=0` 无匹配和 VIP `131001` 已领取是经 Mobile 消费源码确认的端点特例，不得扩散为全局规则。\n\n## 首页首批纵切片',
  );
  generatedDocuments['README.md'] = generatedDocuments['README.md'].replace(
    `- API 协议源：\`MoeKoeMusic/api@${API_COMMIT}\``,
    `- API 协议源：\`MoeKoeMusic/api@${API_COMMIT}\`\n- 首页 Mobile 消费证据：\`MoeKoeMusic-Mobile@${MOBILE_COMMIT}\`\n- Mobile 内嵌 API 证据：\`MoeKoeMusic-Mobile/api@${MOBILE_API_COMMIT}\`\n- \`top_card\` PC 消费链：\`MoeKoeMusic@${TOP_CARD_PC_COMMIT}\`\n- 其他 Mobile 分支或重写版本不作为证据来源。`,
  );
  generatedDocuments['PROTOCOL.md'] = generatedDocuments['PROTOCOL.md'].replace(
    '## 加密与二进制',
    '## 登录 Origin 与 Lite 条件\n\n- 发送手机验证码：`http://login.user.kugou.com/v7/send_mobile_code`，唯一允许的明文 Origin，只携带 MID 身份。\n- 手机验证码登录：`https://loginserviceretry.kugou.com/v7/login_by_verifycode`。\n- 密码登录：`https://gateway.kugou.com/v9/login_by_pwd`，并设置 `x-router: login.user.kugou.com`。\n- 风控提交：`https://verifyservice.kugou.com/v4/verify_user_info`；扫码端点使用各自独立 HTTPS Origin。\n\nLite 验证码登录固定发送 `t1/t2/dfid/dev/gitversion`，不得发送 Standard 分支的 `t3`。登录成功必须解密 `secu_params`、校验 token/userid、合并响应 Cookie，再由数据层原子提交加密 Session。\n\n## 加密与二进制',
  );
  generatedDocuments['PROTOCOL.md'] = generatedDocuments['PROTOCOL.md'].replace(
    'Android 直连必须复现固定 API 基线的请求上下文：',
    '标准 HTTP/JSON 业务端点由私有 Retrofit Service 直接返回类型化 `ApiResponse<T>`；方法级策略由 OkHttp application interceptor 读取，依次注入公共参数/Session、对最终 Query 与 Body 字节签名，并将已知 `ssa-code` Header 归一化到 JSON 信封。设备注册在 Retrofit 调用前以 suspend single-flight 完成，不允许 Interceptor 发起嵌套请求。\n\nAndroid 直连必须复现固定 API 基线的请求上下文：',
  );
  generatedDocuments['PROTOCOL.md'] = generatedDocuments['PROTOCOL.md'].replace(
    '会话至少包含 `token`、`userid`、`vip_token`、`vip_type`；设备上下文至少包含 `dfid`、GUID、MID、DEV 和平台标识。',
    '会话至少包含 `token`、`userid`、`vip_token`、`vip_type`；设备上下文至少包含 `dfid`、GUID、MID、DEV 和平台标识。设备注册通过可注入 Provider 按 Mobile 合同读取当前 Android 设备的总内存、品牌、Build ID、型号和厂商，缺失时使用固定 fallback，存储字段继续采用 Mobile 的固定兼容值；它优先读取解密后的 `data.dfid`，并与 Mobile 通用 Cookie 合并链一致地接受响应 `Set-Cookie` 中的 `dfid`，两处都缺失时必须报告协议错误，不能带占位值继续业务请求。',
  );
  generatedDocuments['PROTOCOL.md'] = generatedDocuments['PROTOCOL.md'].replace(
    '## 错误模型',
    '## 重试边界\n\n签名 API Client 禁用 OkHttp 连接失败自动重放；HTTP 5xx、业务错误和协议错误均不在 Interceptor 中重试。风控验证成功后只能由原发起流程显式创建一次新请求，使时间戳与签名重新生成；写操作没有幂等保证时不得自动重试。取消必须原样传播。\n\n## 错误模型',
  );
  generatedDocuments['PROTOCOL.md'] = generatedDocuments['PROTOCOL.md'].replace(
    '## 错误模型',
    '## 风控 SID/EDT\n\n固定 PC 包装层在仅收到 `ssa-code` Header 时不会等待上游返回 `sid/edt`，而是使用当前 MID、userid、dfid、进程级 WebGL 指纹和行为事件生成 EDT，并以 RSA-OAEP(SHA-256/MGF1-SHA-256) 封装临时 AES 密钥得到 SID。Android 协议层在 Challenge 已携带完整 `sid/edt` 时保留原值，仅对缺失上下文生成一次，并且只在验证提交请求的协程内存中使用。\n\n## 错误模型',
  );
  generatedDocuments['PROTOCOL.md'] = generatedDocuments['PROTOCOL.md'].replace(
    '上游常同时使用 HTTP 状态与 Body 内 `status`/`error_code`；静态文档没有证明二者存在统一关系。',
    '上游常同时使用 HTTP 状态与 Body 内 `status`/`error_code`；静态文档没有证明二者存在统一关系。`error_code` 缺失或数值零表示无该错误，任何非空且不等价于数值零的值（包括非数字字符串）都按业务拒绝处理。携带 `ssa-code` 的响应只能有界读取，超限和畸形 Body 均关闭后报告协议错误。',
  );
  generatedDocuments['VERIFICATION.md'] = generatedDocuments['VERIFICATION.md'].replace(
    `3. \`interface.d.ts\` 或现有说明：\`${EVIDENCE.declared}\`。`,
    `3. 固定 Mobile 消费端实际读取或测试的行为旁证：\`${EVIDENCE.reference}\`；不得覆盖 Lite 源码。\n4. \`interface.d.ts\` 或现有说明：\`${EVIDENCE.declared}\`。`,
  ).replace('4. 固定仓库已有脱敏样例', '5. 固定仓库已有脱敏样例')
    .replace('5. 静态推断', '6. 静态推断')
    .replace('6. 无证据', '7. 无证据');
  generatedDocuments['VERIFICATION.md'] = generatedDocuments['VERIFICATION.md'].replace(
    '## 证据优先级',
    `以上统计只描述静态文档生成过程。\n\n## Android 运行时 Canary\n\n- \`LiveApiSearchCanaryTest\` 默认跳过，仅在 \`RESONOTE_RUN_LIVE_API_TESTS=true\` 时运行。\n- 首页 Canary 验证每日推荐、私人好歌、推荐歌单和新歌速递均至少返回一个可消费项目；播放地址最多尝试 5 个公开推荐候选，至少一个必须由服务原生返回 HTTPS 地址。\n- 首页入口 Canary 验证排行榜列表非空、前三个公开榜单至少一个返回可消费歌曲，并验证前三个公开推荐歌单至少一个返回详情和歌曲。\n- Canary 不需要账号，不下载或播放音频，也不记录完整响应、Cookie、签名或设备标识。\n- 2026-08-12 类型化 DTO 复测确认每日推荐当次把 \`relate_goods\` 返回为对象；Android 已按 Mobile 的 \`Array.isArray\` 消费语义处理为“非数组即无可用档位”，并增加该真实结构的协议回归。\n- 同日内容复测中，设备注册从解密 Body 或响应 Cookie 取得 \`dfid\`；每日推荐、私人好歌、推荐歌单、新歌速递、排行榜、榜单歌曲、歌单详情和歌单歌曲均返回可消费数据。\n- 同日播放服务对所试候选只返回 HTTP URL；旧实现改写 scheme 后仅断言字符串前缀，不能证明 TLS 或媒体地址可用。该结果现按协议失败处理，不记作 HTTPS Canary 通过。\n- 同日一个匿名 VIP 候选返回 \`error_code=35104\`；该响应按 Mobile 的无地址消费语义映射为候选级 \`PlaybackUnavailableReason.Vip\`，Canary 继续尝试后续歌曲，不把 VIP 限制本身记作协议失败或整组成功。\n- Mobile API 文档明确说明匿名搜索可能返回业务码 \`152\` 并要求认证 Cookie。本批 Canary 不需要账号，因此该已知响应记为搜索用例跳过。\n\n## 证据优先级`,
  );
  return generatedDocuments;
}

function schemaYaml(endpoints, kind) {
  const lines = [
    'schema_version: 1',
    `kind: ${yamlScalar(kind)}`,
    `api_commit: ${yamlScalar(API_COMMIT)}`,
    'schemas:',
  ];
  for (const endpoint of endpoints) {
    lines.push(`  - endpoint_id: ${yamlScalar(endpoint.id)}`);
    lines.push(`    root_type: ${yamlScalar(kind === 'request' ? endpoint.requestDto : endpoint.responseDto)}`);
    lines.push('    fields:');
    if (kind === 'request') {
      if (!endpoint.requestFields.length) lines.push('      []');
      for (const field of endpoint.requestFields) {
        lines.push(`      - path: ${yamlScalar(field.name)}`);
        lines.push(`        type: ${yamlScalar(field.type)}`);
        lines.push(`        required: ${field.required}`);
        lines.push(`        evidence: [${[...field.evidence].map(yamlScalar).join(', ')}]`);
      }
    } else {
      const fields = [...endpoint.responseFields.entries()];
      if (!fields.length) {
        lines.push('      - path: "*"');
        lines.push('        type: "unknown"');
        lines.push(`        evidence: ${yamlScalar(EVIDENCE.unknown)}`);
      } else for (const [path, evidence] of fields) {
        lines.push(`      - path: ${yamlScalar(path)}`);
        lines.push('        type: "unknown"');
        lines.push(`        evidence: ${yamlScalar(evidence)}`);
        const condition = responseFieldCondition(endpoint, path);
        if (condition) lines.push(`        condition: ${yamlScalar(condition)}`);
      }
    }
  }
  return `${lines.join('\n')}\n`;
}

function write(relative, content) {
  const target = join(DOC_ROOT, relative);
  mkdirSync(dirname(target), { recursive: true });
  writeFileSync(target, content, 'utf8');
}

function main() {
  ensureCommit(MOEKOE_ROOT, APP_COMMIT);
  ensureCommit(API_ROOT, API_COMMIT);
  ensureCommit(MOEKOE_ROOT, TOP_CARD_PC_COMMIT);
  ensureCommit(join(MOBILE_ROOT, 'api'), MOBILE_API_COMMIT);
  loginReferenceFields = extractMobileReferenceEvidence();
  const moduleFiles = listTree(API_ROOT, API_COMMIT, 'module')
    .filter((path) => /^module\/[^/]+\.js$/.test(path) && !path.split('/').at(-1).startsWith('_'))
    .sort();
  const declarations = parseInterfaces(show(API_ROOT, API_COMMIT, 'interface.d.ts'));
  const consumerRoutes = extractConsumerEvidence();
  const domainSequences = new Map();
  const endpoints = moduleFiles.map((path) => {
    const module = path.slice('module/'.length, -'.js'.length);
    const domain = moduleDomain(module);
    const sequence = (domainSequences.get(domain) || 0) + 1;
    domainSequences.set(domain, sequence);
    const id = `API-${domain.toUpperCase()}-${String(sequence).padStart(3, '0')}`;
    const source = show(API_ROOT, API_COMMIT, path);
    const objects = collectObjectDeclarations(source);
    const callObjects = extractCallObjects(source, 'useAxios');
    const interfaceName = declarations.functions.get(module);
    const declaredFields = declarations.resolveFields(interfaceName);
    const wrapperRoute = `/${module.replaceAll('_', '/')}`;
    const responseFields = extractResponseFields(source);
    const consumer = consumerRoutes.get(wrapperRoute);
    if (consumer) for (const field of consumer.fields) if (!responseFields.has(field)) responseFields.set(field, EVIDENCE.consumer);
    const upstreamRequests = parseUpstreamRequests(source, objects);
    const hasSession = /params(?:\?\.|\.)cookie|\btoken\b|\buserid\b/.test(source);
    const requiredAuth = /^(user_|playlist_(?:add|del|tracks)|artist_(?:follow|unfollow)|playhistory|youth_.*vip)/.test(module);
    const operation = MUTATION_WORDS.test(module) ? 'write-or-sensitive' : 'read';
    const transforms = /decodeLyrics|crypto[A-Z]|playlistAes|\.body\s*[.=\[]/.test(source);
    const cookieWriteback = /(?:res|resp|response)\.cookie\.push/.test(source);
    const hasNativeRequests = upstreamRequests.some((request) => request.kind === 'native');
    const riskHandling = /^(get_verify_info|verify_user_info|sidedt)$/.test(module)
      ? 'bypass'
      : hasNativeRequests && callObjects.length
        ? 'surface-challenge + native-partial'
        : callObjects.length ? 'surface-challenge' : 'none-or-dynamic';
    const manual = upstreamRequests.length > 1 || upstreamRequests.some((request) => request.kind !== 'useAxios' || request.responseType !== 'json' || request.path.startsWith('dynamic:'));
    const components = new Set();
    if (upstreamRequests.some((request) => !['none', 'unknown'].includes(request.signing))) components.add('ApiRequestSigner');
    if (hasSession) components.add('ApiSession');
    if (/dfid|KUGOU_API_MID|guid|imei|imsi/.test(source)) components.add('ApiDeviceIdentity');
    if (transforms) components.add('ApiResponseDecoder');
    return {
      id, anchor: id.toLowerCase(), module, domain, wrapperRoute,
      description: declarations.functionDescriptions.get(module) || extractDescription(source, module),
      authentication: requiredAuth ? 'required' : hasSession ? 'optional' : 'anonymous',
      operation, productScope: productScope(module), validation: 'static-only',
      upstreamRequests,
      requestFields: applyEndpointRequestContract(
        module,
        applyLiteLoginContract(
          module,
          parseRequestFields(source, declaredFields, objects, callObjects),
          responseFields,
        ),
      ),
      responseFields,
      consumerFiles: consumer?.files || new Set(),
      operationName: camel(module),
      requestDto: `Api${pascal(module)}Request`,
      responseDto: `NetworkApi${pascal(module)}Response`,
      transport: ANDROID_SPECIAL_PROTOCOL_ENDPOINTS.has(id)
        ? 'OkHttp Call.Factory'
        : ANDROID_RETROFIT_ENDPOINTS.has(id) || !manual ? 'Retrofit' : 'OkHttp Call.Factory',
      components: [...components], transforms, cookieWriteback, riskHandling,
    };
  });
  if (endpoints.length !== 164) throw new Error(`预期 164 个模块，实际 ${endpoints.length}`);

  for (const entry of ['endpoints', 'schemas', 'fixtures']) rmSync(join(DOC_ROOT, entry), { recursive: true, force: true });
  write('catalog.yaml', catalogYaml(endpoints));
  const grouped = new Map();
  for (const endpoint of endpoints) {
    if (!grouped.has(endpoint.domain)) grouped.set(endpoint.domain, []);
    grouped.get(endpoint.domain).push(endpoint);
  }
  for (const domain of DOMAIN_ORDER) if (grouped.has(domain)) write(`endpoints/${domain}.md`, endpointMarkdown(domain, grouped.get(domain)));
  for (const [relative, content] of Object.entries(staticDocuments(endpoints, consumerRoutes))) write(relative, content);
  process.stdout.write(`Generated ${endpoints.length} endpoint contracts in ${DOC_ROOT}\n`);
}

main();
