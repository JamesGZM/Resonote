#!/usr/bin/env node

import { execFileSync } from 'node:child_process';
import { mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const API_COMMIT = '6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb';
const APP_COMMIT = '52c9833afe2e7fedcba8d5b23ff8d1f9731af73a';
const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const DOC_ROOT = resolve(SCRIPT_DIR, '..');
const WORKSPACE_ROOT = resolve(DOC_ROOT, '../..');
const MOEKOE_ROOT = resolve(process.env.MOEKOE_ROOT || join(WORKSPACE_ROOT, '..', 'MoeKoeMusic'));
const API_ROOT = join(MOEKOE_ROOT, 'api');

const EVIDENCE = Object.freeze({
  source: 'SOURCE_CONFIRMED',
  consumer: 'CONSUMER_CONFIRMED',
  declared: 'DECLARED',
  fixture: 'FIXTURE_CONFIRMED',
  inferred: 'INFERRED',
  unknown: 'UNKNOWN',
});

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
      lines.push('| Body 路径 | 证据 |', '|---|---|');
      for (const [path, evidence] of responseFields) lines.push(`| ${mdCode(path)} | ${mdCode(evidence)} |`);
      lines.push('', '这里只列出源码或固定 PC 消费端能够证明的字段；未列出的字段不代表不存在。');
    }
    lines.push('', '### Android 映射', '', '| 项目 | 建议 |', '|---|---|');
    lines.push(`| DataSource 操作 | ${mdCode(endpoint.operationName)} |`);
    lines.push(`| Request DTO | ${mdCode(endpoint.requestDto)} |`);
    lines.push(`| Response DTO | ${mdCode(endpoint.responseDto)}；含 UNKNOWN 时先使用宽容中间结构 |`);
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

function staticDocuments(endpoints, consumerRoutes) {
  const domainCounts = new Map();
  for (const endpoint of endpoints) domainCounts.set(endpoint.domain, (domainCounts.get(endpoint.domain) || 0) + 1);
  const unknownCount = endpoints.filter((endpoint) => endpoint.responseFields.size === 0).length;
  const consumerCount = endpoints.filter((endpoint) => endpoint.consumerFiles.size > 0).length;
  const unmatched = [...consumerRoutes.keys()].filter((route) => !endpoints.some((endpoint) => endpoint.wrapperRoute === route));
  return {
    'README.md': `# Lite 静态 API 契约\n\n> 状态：静态证据基线，不代表上游接口当前可用或获得服务授权。\n\n## 基线\n\n- PC 消费端：\`MoeKoeMusic@${APP_COMMIT}\`\n- API 协议源：\`MoeKoeMusic/api@${API_COMMIT}\`\n- 平台：概念版 \`lite\`（\`appid=3116\`、\`clientver=11440\`）\n- 模块：${endpoints.length}\n- 验证：仅静态分析，无外部网络请求\n\n## 阅读顺序\n\n1. [公共协议](PROTOCOL.md)\n2. [机器可读目录](catalog.yaml)\n3. [Android/NIA 映射](ANDROID_MAPPING.md)\n4. [验证与缺口](VERIFICATION.md)\n5. [接口领域索引](#接口领域)\n\nNode 包装路由只描述 PC 调用的本地 Express 接口；每个接口章节中的“上游请求”才是 Android 直连契约。字段证据等级为 \`${Object.values(EVIDENCE).join('`、`')}\`。\n\n## 接口领域\n\n${[...domainCounts.entries()].sort((a, b) => DOMAIN_ORDER.indexOf(a[0]) - DOMAIN_ORDER.indexOf(b[0])).map(([domain, count]) => `- [${DOMAIN_NAMES[domain]}](endpoints/${domain}.md)：${count}`).join('\n')}\n\n## 完整性摘要\n\n- 全量模块：${endpoints.length}/${endpoints.length}\n- 固定 PC 消费端直接使用：${consumerCount}\n- 无字段级响应证据：${unknownCount}\n- 未映射的固定 PC 请求路由：${unmatched.length}\n\n完整统计和限制见 [VERIFICATION](VERIFICATION.md)。\n\n## 重新生成与校验\n\n在 Resonote 根目录执行：\n\n\`\`\`shell\nnode docs/api/tools/generate-docs.mjs\nnode docs/api/tools/validate-docs.mjs\n\`\`\`\n\n工具只读取固定 Git 对象；如 MoeKoeMusic 不在默认相邻目录，可通过 \`MOEKOE_ROOT\` 指向仓库。生成器会替换本目录中的领域文档、Schema 和 Fixture 索引。\n`,
    'PROTOCOL.md': `# Lite 公共协议\n\n## 请求管线\n\nAndroid 直连必须复现固定 API 基线的请求上下文：Lite \`appid=3116\`、\`clientver=11440\`、秒级 \`clienttime\`、持久化设备身份，以及按端点选择的签名、请求体和响应解码。默认网关为 \`https://gateway.kugou.com\`；带 \`x-router\` 的请求仍以该网关为传输入口。\n\n## 公共参数与请求头\n\n| 名称 | 位置 | 来源 | 说明 |\n|---|---|---|---|\n| \`dfid\` | Query/Header | DeviceSession | 设备注册结果；未注册时源码可使用占位值 |\n| \`mid\` | Query/Header | DeviceIdentity | 由持久化 GUID 按固定算法派生 |\n| \`uuid\` | Query | Provider | 固定基线默认 \`-\` |\n| \`appid\` | Query | Lite Config | 3116 |\n| \`clientver\` | Query | Lite Config | 11440，个别端点会覆盖 |\n| \`clienttime\` | Query/Header | Clock | 秒级时间戳，必须由可注入时钟提供 |\n| \`token\` / \`userid\` | Query/Body | Session | 登录后按端点注入 |\n| \`x-router\` | Header | Endpoint | 选择网关后端，不能误当作 Retrofit Base URL |\n\n固定源码还注入 User-Agent 与若干 KG 路由头。具体字面量和签名材料不在文档重复，后续实现须从固定 MIT 源码逐项迁移并用 golden test 锁定；凭证型配置不得写入文档或 Fixture。\n\n## 签名模式\n\n- \`android\`：对规范化 Query 和序列化 Body 生成 Android signature。\n- \`web\`：用于二维码等 Web 登录协议。\n- \`register\`：用于设备注册协议。\n- \`none\`：源码显式跳过 signature，可能仍有端点自定义 key。\n- \`unknown\`：无法仅由模块静态确定，实施前必须补证据。\n\n签名器必须依赖可注入 \`Clock\`，并保持参数排序、字符串化、Body 字节和 URL 编码与 Node 基线一致。\n\n## 会话和设备身份\n\n会话至少包含 \`token\`、\`userid\`、\`vip_token\`、\`vip_type\`；设备上下文至少包含 \`dfid\`、GUID、MID、DEV 和平台标识。PC 的 Authorization 拼接只是包装层传输格式，Android 直连不得把它原样发送给上游，而应按端点写入 Query、Body、Header 或 Cookie。敏感值必须持久化加密，日志和 Fixture 一律脱敏。\n\n## 加密与二进制\n\n固定基线出现 AES、RSA 公钥加密、歌单/云盘 AES 封装、KRC 解码、ArrayBuffer 和 PCM/文件二进制。凡目录标记 \`arraybuffer\`、多阶段请求或动态 URL 的端点，优先使用共享 OkHttp \`Call.Factory\`，不强行套用普通 Retrofit JSON 接口。\n\n## 错误模型\n\n必须分别保留 HTTP 失败、Provider 业务失败、签名/设备失败、登录过期、风控验证、解密失败、结构不兼容和网络失败。上游常同时使用 HTTP 状态与 Body 内 \`status\`/\`error_code\`；静态文档没有证明二者存在统一关系。\n\n## 响应兼容策略\n\n\`UNKNOWN\` 或仅 \`CONSUMER_CONFIRMED\` 的响应不得直接转成全字段非空 DTO。初次实现应忽略未知键、对漂移字段使用受控宽容序列化，并在 Network DTO 到领域模型边界完成校验。\n`,
    'ANDROID_MAPPING.md': `# Android / NIA 映射\n\n## 模块边界\n\n\`core:network\` 提供共享 OkHttp、Retrofit、序列化和通用传输错误；\`platform:api\` 拥有 Lite 签名、设备、会话、Cookie、加密、Network DTO 和解码；\`core:data\` 拥有 Repository、缓存与领域映射；Feature/ViewModel 只依赖 Repository。\n\n## NIA 对应方式\n\n- 以 \`ApiNetworkDataSource\` 暴露远端能力，具体 Retrofit/OkHttp 类保持 internal。\n- 每个接口章节给出稳定操作名和 DTO 根类型建议。\n- Network DTO 使用 kotlinx.serialization，默认忽略未知键；不得进入 Compose 或公共领域模型。\n- Repository 使用 fake DataSource 测试，不以脆弱的调用顺序 mock 为主。\n- Retrofit 只用于单阶段、稳定 Host、JSON 请求；动态路径、二进制、加密或多阶段流程使用共享 \`Call.Factory\`。\n\n## 首条纵切片\n\n按搜索 → 播放地址 → 歌词 → Media3 播放实施。开始 Kotlin 代码前，先为相应端点补齐签名 golden fixture、脱敏响应 fixture 或明确的宽容 DTO 决策。\n\n## 禁止依赖\n\n- UI/Feature 不直接依赖 Retrofit、OkHttp 或 API DTO。\n- \`core:network\` 不包含 dfid、MID、KRC、x-router 等 Provider 语义。\n- 领域模型不保留上游字段命名和传输层可空性。\n- 不把 PC → Node 的 Authorization 桥接协议误作上游协议。\n`,
    'VERIFICATION.md': `# 静态验证报告\n\n## 结果\n\n| 检查 | 结果 |\n|---|---:|\n| API 模块覆盖 | ${endpoints.length}/${endpoints.length} |\n| 有 PC 消费证据的接口 | ${consumerCount} |\n| 无字段级响应证据 | ${unknownCount} |\n| 未映射 PC 请求路由 | ${unmatched.length} |\n| 外部请求 | 0 |\n| 实时验证 | 0 |\n\n## 证据优先级\n\n1. API 模块实际构造和转换：\`${EVIDENCE.source}\`。\n2. 固定 PC 应用读取字段：\`${EVIDENCE.consumer}\`。\n3. \`interface.d.ts\` 或现有说明：\`${EVIDENCE.declared}\`。\n4. 固定仓库已有脱敏样例：\`${EVIDENCE.fixture}\`。\n5. 静态推断：\`${EVIDENCE.inferred}\`。\n6. 无证据：\`${EVIDENCE.unknown}\`。\n\n## 固有限制\n\n静态源码通常透传上游 Body，\`interface.d.ts\` 的返回值又多为 \`ApiResponse<any>\`，因此本基线只能完整证明请求构造，不能完整证明所有响应字段、可空性、枚举全集或当前可用性。未列出字段不代表不存在。\n\n## 未映射请求\n\n${unmatched.length ? unmatched.map((route) => `- \`${route}\``).join('\n') : '- 无'}\n\n## Fixture 状态\n\n固定基线没有发现可证明为完整上游响应且已脱敏的 JSON Fixture。本次不制造样例；[fixtures/README](fixtures/README.md) 记录了准入规则。\n`,
    'schemas/requests.yaml': schemaYaml(endpoints, 'request'),
    'schemas/responses.yaml': schemaYaml(endpoints, 'response'),
    'schemas/README.md': `# 静态 Schema 说明\n\n- \`requests.yaml\` 合并固定 \`interface.d.ts\` 声明与模块实际构造字段。\n- \`responses.yaml\` 只列 API 转换代码和固定 PC 消费端能够证明的 Body 路径。\n- \`type: unknown\` 是有意保守结果；在获得合规的脱敏响应 Fixture 前不得收紧。\n- \`path: "*"\` + \`${EVIDENCE.unknown}\` 表示包装层透传响应且没有字段级静态证据。\n`,
    'fixtures/README.md': `# Fixture 准入规则\n\n当前静态基线没有可安全认定为完整上游响应的 JSON Fixture，因此本目录暂不包含伪造样例。后续 Fixture 必须：\n\n- 来自固定源码已提交样例或经批准的只读采样。\n- 旁置来源、提交或采样条件。\n- 删除 token、userid、Cookie、dfid、MID、GUID、设备信息和账号内容。\n- 不以人工拼装 JSON 冒充真实响应。\n- 在响应 Schema 中把相应字段标为 \`${EVIDENCE.fixture}\`。\n`,
  };
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
    const riskHandling = /verify|sidedt|captcha/.test(module)
      ? 'module-specific + common-ssa'
      : hasNativeRequests && callObjects.length
        ? 'partial-common-ssa'
        : callObjects.length ? 'common-ssa' : 'none-or-dynamic';
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
      requestFields: parseRequestFields(source, declaredFields, objects, callObjects),
      responseFields,
      consumerFiles: consumer?.files || new Set(),
      operationName: camel(module),
      requestDto: `Api${pascal(module)}Request`,
      responseDto: `NetworkApi${pascal(module)}Response`,
      transport: manual ? 'OkHttp Call.Factory' : 'Retrofit',
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
