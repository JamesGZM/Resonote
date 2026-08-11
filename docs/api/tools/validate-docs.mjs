#!/usr/bin/env node

import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const API_COMMIT = '6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb';
const APP_COMMIT = '52c9833afe2e7fedcba8d5b23ff8d1f9731af73a';
const DOC_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const WORKSPACE_ROOT = resolve(DOC_ROOT, '../..');
const MOEKOE_ROOT = resolve(process.env.MOEKOE_ROOT || join(WORKSPACE_ROOT, '..', 'MoeKoeMusic'));
const API_ROOT = join(MOEKOE_ROOT, 'api');
const ALLOWED_EVIDENCE = new Set(['SOURCE_CONFIRMED', 'CONSUMER_CONFIRMED', 'DECLARED', 'FIXTURE_CONFIRMED', 'INFERRED', 'UNKNOWN']);

function git(repo, args) {
  return execFileSync('git', ['-C', repo, ...args], { encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 });
}

function fail(message) {
  throw new Error(message);
}

function markdownFiles(root) {
  const result = [];
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    const path = join(root, entry.name);
    if (entry.isDirectory()) result.push(...markdownFiles(path));
    else if (entry.name.endsWith('.md')) result.push(path);
  }
  return result;
}

function main() {
  const catalogPath = join(DOC_ROOT, 'catalog.yaml');
  if (!existsSync(catalogPath)) fail('缺少 catalog.yaml');
  const catalog = readFileSync(catalogPath, 'utf8');
  const modules = [...catalog.matchAll(/^    module: "([^"]+)"$/gm)].map((match) => match[1]);
  const ids = [...catalog.matchAll(/^  - id: "([^"]+)"$/gm)].map((match) => match[1]);
  const sourceModules = git(API_ROOT, ['ls-tree', '-r', '--name-only', API_COMMIT, '--', 'module'])
    .split('\n')
    .filter((path) => /^module\/[^/_][^/]*\.js$/.test(path))
    .map((path) => path.slice(7, -3))
    .sort();
  if (modules.length !== 164 || sourceModules.length !== 164) fail(`模块数错误：catalog=${modules.length}, source=${sourceModules.length}`);
  if (new Set(modules).size !== 164 || new Set(ids).size !== 164) fail('模块或 ID 存在重复');
  if (modules.slice().sort().join('\n') !== sourceModules.join('\n')) fail('catalog 与固定 API commit 的模块集合不一致');
  const wrapperRoutes = new Set([...catalog.matchAll(/^    wrapper_route: "([^"]+)"$/gm)].map((match) => match[1]));
  const appFiles = git(MOEKOE_ROOT, ['ls-tree', '-r', '--name-only', APP_COMMIT, '--', 'src'])
    .split('\n')
    .filter((path) => /\.(?:js|vue)$/.test(path));
  const appRoutes = new Set();
  for (const file of appFiles) {
    const source = git(MOEKOE_ROOT, ['show', `${APP_COMMIT}:${file}`]);
    for (const match of source.matchAll(/(?:\w+\.)?(?:get|post|put|del|patch)\s*\(\s*(['"`])(\/[^'"`$?]+)(?:\?[^'"`]*)?\1/g)) {
      appRoutes.add(match[2].replace(/\/$/, '') || '/');
    }
  }
  const unmappedAppRoutes = [...appRoutes].filter((route) => !wrapperRoutes.has(route));
  if (unmappedAppRoutes.length) fail(`固定 PC 请求未映射：${unmappedAppRoutes.join(', ')}`);
  for (const required of ['platform', 'wrapper_route', 'source', 'authentication', 'operation', 'product_scope', 'validation', 'documentation', 'response_handling', 'transformed', 'cookie_writeback', 'risk', 'method', 'signing']) {
    const count = [...catalog.matchAll(new RegExp(`^\\s+${required}:`, 'gm'))].length;
    const expected = ['method', 'signing'].includes(required) ? 164 : 164;
    if (count < expected) fail(`${required} 条目不足：${count}`);
  }
  for (const match of catalog.matchAll(/^\s+evidence:\s*(?:\[([^\]]*)\]|"([A-Z_]+)")/gm)) {
    const values = match[2] ? [match[2]] : (match[1].match(/[A-Z_]+/g) || []);
    for (const value of values) if (!ALLOWED_EVIDENCE.has(value)) fail(`非法证据等级：${value}`);
  }
  const docs = [...catalog.matchAll(/^    documentation: "([^"]+)"$/gm)].map((match) => match[1]);
  for (const target of docs) {
    const [relative, anchor] = target.split('#');
    const absolute = join(DOC_ROOT, relative);
    if (!existsSync(absolute)) fail(`文档不存在：${relative}`);
    if (!readFileSync(absolute, 'utf8').includes(`<a id="${anchor}"></a>`)) fail(`文档锚点不存在：${target}`);
  }
  for (const markdown of markdownFiles(DOC_ROOT)) {
    const content = readFileSync(markdown, 'utf8');
    for (const match of content.matchAll(/\]\(([^)]+)\)/g)) {
      const target = match[1];
      if (/^(?:https?:|#)/.test(target)) continue;
      const relative = target.split('#')[0];
      if (!existsSync(resolve(dirname(markdown), relative))) fail(`Markdown 链接不存在：${markdown} -> ${target}`);
    }
  }
  const responseSchema = readFileSync(join(DOC_ROOT, 'schemas/responses.yaml'), 'utf8');
  const schemaEntries = [...responseSchema.matchAll(/^  - endpoint_id:/gm)].length;
  if (schemaEntries !== 164) fail(`响应 Schema 数量错误：${schemaEntries}`);
  const consumerFields = [...responseSchema.matchAll(/^        evidence: "CONSUMER_CONFIRMED"$/gm)].length;
  if (consumerFields < 20) fail(`PC 消费字段提取异常：仅 ${consumerFields} 个`);
  const fixtureDir = join(DOC_ROOT, 'fixtures');
  const fixtureFiles = readdirSync(fixtureDir).filter((name) => name.endsWith('.json'));
  const secretPatterns = [
    /"(?:token|userid|cookie|dfid|mid|guid|imei|imsi|authorization)"\s*:\s*"?(?!<redacted>)[^",}\s]+/i,
    /(?:Bearer\s+|token=)[A-Za-z0-9._-]+/i,
  ];
  for (const file of fixtureFiles) {
    const content = readFileSync(join(fixtureDir, file), 'utf8');
    JSON.parse(content);
    const metadata = join(fixtureDir, `${file.slice(0, -'.json'.length)}.source.yaml`);
    if (!existsSync(metadata)) fail(`Fixture 缺少来源元数据：${file}`);
    for (const pattern of secretPatterns) if (pattern.test(content)) fail(`Fixture 可能包含敏感值：${file}`);
  }
  const sourceFields = [...catalog.matchAll(/^        evidence: "([A-Z_]+)"$/gm)].map((match) => match[1]);
  for (const field of sourceFields) if (!ALLOWED_EVIDENCE.has(field)) fail(`响应字段证据无效：${field}`);
  if (!catalog.includes(`generated_from_api_commit: "${API_COMMIT}"`) || !catalog.includes(`generated_from_app_commit: "${APP_COMMIT}"`)) fail('固定提交记录不正确');
  process.stdout.write(`Validated 164 modules, ${appRoutes.size} PC routes, ${docs.length} document links, ${schemaEntries} response schemas, ${consumerFields} consumer fields, ${fixtureFiles.length} JSON fixtures.\n`);
}

main();
