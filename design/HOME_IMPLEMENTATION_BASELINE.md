# 首页实现基准

状态：**已锁定，可进入代码实现**
锁定日期：2026-08-12

本文件冻结首页的内容顺序、滚动关系、固定层级和组件复用关系。当前 PNG 是实现用的结构与密度参考，
不是逐像素最终验收稿；代码阶段发现局部文字、Badge、间距或遮挡细节不一致时，不继续从 PNG 反推规则，
而是回到 `COMPONENT_SYSTEM.md` 和已冻结组件基线实现。

## 1. 规范优先级

1. `PRODUCT_REQUIREMENTS.md` 的首页产品合同与本文的页面结构。
2. `COMPONENT_SYSTEM.md` 08B、08C、09A、09B 的尺寸、测量、状态与可访问性合同。
3. 已冻结组件视觉：
   - `design/approved/components/08-song-item.png`
   - `design/approved/components/08-playlist-item.png`
   - `design/approved/components/09-miniplayer-bottom-navigation.png`
4. 本页三张滚动状态图，只用于确认首页信息密度、区块顺序、滚动截取和层级关系。

组件基线与首页状态图不一致时，以组件基线和 Markdown 合同为准。不得为了复刻状态图中的生成瑕疵，
改变 Music Item、Playlist Item、Mini Player 或 Bottom Navigation。

## 2. 锁定视觉证据

三张图是**同一个首页的三个滚动位置**，不是三个页面，也不是三种备选方案：

- 顶部状态：`design/approved/home/10-home-scroll-top.png`
- 中段状态：`design/approved/home/10-home-scroll-middle.png`
- 底部状态：`design/approved/home/10-home-scroll-bottom.png`

它们共同冻结以下关系：

- Compact 真实手机视口维持同一信息密度，不把全部内容压入一屏。
- Top App Bar 固定；左侧为 Resonote Wordmark，右侧为搜索和听歌识曲入口。
- Bottom Navigation 固定，目的地为“首页、发现、我的”，首页选中。
- Mini Player 是高于滚动内容的独立悬浮卡片；滚动中的 Music Item 或 Playlist Item 可以从其后方经过。
- Mini Player 不结束列表、不切断外层容器，也不要求页面在其上方制造永久空白。
- 列表末尾仍需提供足够的 Bottom Content Padding，使最后一个 Item 能完整滚动到 Mini Player 上方并获得焦点。

## 3. 首页内容顺序

首页滚动内容固定为：

1. 推荐区域：推荐电台主入口、排行榜快捷入口、精选歌单快捷入口。排行榜进入发现的榜单子页面；精选歌单进入发现的推荐歌单分类，不使用 PC 固定个人歌单 ID。
2. 每日推荐：6 个 Music Item，共用一个外层卡片容器；Section Action 为“播放全部”。
3. 推荐歌单：6 个 Playlist Item，Compact 固定两列；标题右侧无 Action。
4. 新歌速递：6 个 Music Item，共用一个外层卡片容器；无分类筛选，Section Action 为“播放全部”。

首页中的 6 项是页面数据合同，不代表同一视口必须同时显示 6 项。滚动截图允许只显示其中一部分，
但实现中的同一区块必须是连续的同一容器，不能拆成两个卡片或用空白 Item 补齐。

## 4. 数据与刷新合同

- 首次进入与下拉刷新都并发请求每日推荐、推荐歌单和新歌速递，三个区块独立完成和报错。
- 每日推荐每次成功获得完整候选池后重新洗牌并选取恰好 6 首；不承诺连续两次必然不同。
- 单区刷新成功只替换该区；单区失败保留该区已有内容并报告类型化问题。首次全部失败时保持空状态。
- 推荐电台不随整页刷新预取，用户触发时按需调用 `top_card`；默认私人好歌为 `card_id=1`，并支持 `2/3/4/6`。
- 排行榜和精选歌单是导航入口，不加入首页刷新并发组。精选歌单目标复用首页推荐歌单已经使用的 `API-DISCOVER-012`；排行榜目标按需使用 `API-RANKING-003` 和 `API-RANKING-001`。
- 点击任一推荐歌单进入统一歌单详情，详情使用 `API-PLAYLIST-007` 分页加载；该接口不属于首页首屏请求。
- 刷新请求必须带代际约束：旧请求即使更晚完成，也不得覆盖新请求已经提交的内容或发布过期问题；数据层以 `Superseded` 明确返回该结果。
- 本批首页数据只保留进程内快照，不引入 Room 或磁盘缓存。

## 5. Music Item 约束

- 首行固定为 `Title → Quality → VIP`；Title 单行 End Ellipsis。
- Artist/Supporting 独占第二行。
- 默认行的 Duration 与 More 使用固定尾部区域，Badge 不得侵入或越过 Duration。
- Playing 行使用低强调 `primaryContainer`、`primary` Title 和均衡器；均衡器替换 Duration，二者互斥，More 保留。
- 首页至少展示一个真实 Playing 状态，并与 Mini Player 的当前媒体保持一致。
- 6 个 Music Item 共享一个外层容器；Item 之间不建立独立 Card，也不加入伪造的空白行。

## 6. 实现与验收边界

当前图片中仍可能存在生成式设计图的细节误差，例如个别文字宽度、封面内容、像素级间距或遮挡位置。
这些问题不阻塞首轮代码实现。首轮实现先验收：

- 页面区块顺序、数量与操作正确。
- 三个滚动位置能在同一页面自然复现。
- 固定 Top App Bar、悬浮 Mini Player、固定 Bottom Navigation 的层级正确。
- 08/09 冻结组件没有发生尺寸、截断、Badge、Playing 或间距回归。
- 最后一个列表项可以滚动到悬浮层上方，TalkBack、键盘与 D-pad 焦点不会停在不可见遮挡区。

完成真实 Compose 页面后，以运行时截图替换或补充本组 ImageGen 状态图，再做像素级设计 QA。

模块边界采用单一 `:feature:home`。Feature `api` 表示跨功能公共导航/调用合同，并非首页网络接口集合；首页作为 Tabs Shell 根页面没有独立外部入口，因此当前不建立 `:feature:home:api`。排行榜、推荐歌单和歌单详情的共享目标由发现/歌单领域拥有，首页只发出类型安全导航意图。
