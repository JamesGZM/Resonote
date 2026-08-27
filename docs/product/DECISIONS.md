# Resonote Product Decisions

> 本文归档已确认的历史产品决策；现行行为合同以 [PRODUCT_REQUIREMENTS](../../design/PRODUCT_REQUIREMENTS.md) 和当前源码/测试为准。

## 决策记录

| 日期 | Decision ID | 决策 | 理由 / 证据 | 影响范围 | 状态 |
|---|---|---|---|---|---|
| 2026-08-11 | P-000 | 先冻结需求与功能文档，再开始产品页面设计 | 避免脱离真实流程提前制造音乐组件 | 产品设计流程 | 已确认 |
| 2026-08-11 | P-001 | Resonote 面向大众，主打无广告的沉浸式音乐体验 | 产品方向 | 定位、内容和交互 | 已确认 |
| 2026-08-11 | P-002 | 同时支持 API 在线音乐、本地音乐和 Android 文件入口导入 | 用户明确需求 | 数据、播放、权限和页面状态 | 已确认 |
| 2026-08-11 | P-003 | 顶层目的地为首页、发现、我的，启动后直接进入首页 | 用户明确需求 | IA、自适应导航和状态恢复 | 已确认 |
| 2026-08-11 | P-004 | 不设置强制首次使用流程，登录、权限和导入按需触发 | 在线 API 是默认入口 | 启动、权限和空错误状态 | 已确认 |
| 2026-08-11 | P-005 | 支持手机验证码与密码登录；导航级登录成功后继续目标页面，原子按钮操作只恢复上下文且必须再次点击 | 用户明确需求 | Auth、导航、错误恢复和 Session | 已确认 |
| 2026-08-11 | P-006 | 首页、发现、我的分别参考 PC Home、Discover、Library 的功能边界 | 用户明确需求 | IA、API 切片和页面清单 | 已确认 |
| 2026-08-11 | P-007 | 外部文件打开复用导入管线，复制到 App 目录并持久加入本地音乐列表 | 用户明确需求 | 系统入口、本地存储、索引和播放 | 已确认 |
| 2026-08-11 | P-008 | “我的”能力范围参考 PC Library；好友资料只读且不可进入；页面独立设计 | 用户明确需求 | Profile、Library、Cloud、Local Music 和 IA | 已确认 |
| 2026-08-11 | P-009 | 验证码为默认登录，密码登录参考 PC 账号/邮箱 + 密码表单；不默认加入扫码登录 | 用户明确需求与 PC 行为 | Auth UI、协议纵切片 | 已确认 |
| 2026-08-11 | P-010 | 本地音乐建模为系统维护的特殊音乐列表并复用歌单逻辑 | 用户明确需求 | Model、Library、Playlist 和 Playback | 已确认 |
| 2026-08-11 | P-011 | 每日 VIP 签到与可选升级是“我的”Must，状态语义参考 MoeKoeMusic-Mobile | 用户明确需求 | Account、VIP API、风控和状态刷新 | 已确认 |
| 2026-08-11 | P-012 | 云盘功能对齐 PC：查看、搜索/排序、播放/队列、上传和删除，不增加下载 | 用户明确需求与 PC 行为 | Cloud、Upload、Playback 和 Storage | 已确认 |
| 2026-08-11 | P-013 | 本地音乐对齐 PC：共享 Queue，但不进入在线歌单、喜欢或账号历史 | 用户明确需求与 PC 行为 | Local Music、Playlist 和 Playback | 已确认 |
| 2026-08-11 | P-014 | 重复导入必须由用户通过冲突弹窗确认，不允许静默处理 | 用户明确需求 | Import、Storage 和 Dialog | 已确认 |
| 2026-08-11 | P-015 | 首页与发现采用 PC 的完整功能合同，Resonote Android 页面与视觉完全独立设计 | 用户明确需求 | Home、Discover、API 和页面清单 | 已确认 |
| 2026-08-11 | P-016 | Mini Player 跨顶层页面常驻，点击直接进入 Full Player | 用户明确需求 | App Scaffold、Player、Navigation 和 Playback | 已确认 |
| 2026-08-11 | P-017 | 所有 PC 参考仅用于功能、规则和状态；Resonote 不复用 PC 页面与视觉实现 | 用户明确边界 | 全部产品页面与设计交付 | 已确认 |
| 2026-08-11 | P-018 | 搜索入口只放首页并进入独立搜索页；话筒进入独立听歌识曲页 | 用户明确需求与 Mobile 行为证据 | Home、Search、Recognition 和 Navigation | 已确认 |
| 2026-08-11 | P-019 | 旧 Player 封面/歌词图降为历史方向稿，按 NIA + MD3 Adaptive 独立更新 | 用户明确需求与现有图片复核 | Player 产品设计和视觉验收 | 已确认 |
| 2026-08-11 | P-020 | 音频焦点提供所有场景、部分场景、不允许三档意图，默认“不允许”；部分场景按公开音频类别适配，兼容降级不改写保存值 | 用户明确需求与 Android 平台约束 | Playback、Settings、验证和文案 | 产品已确认；设备矩阵待验证 |
| 2026-08-11 | P-021 | 播放必须提供标准 MediaSession 媒体通知；岛形/状态栏等系统媒体表面交由 Android 自动呈现，不主动申请 Live Updates | 用户明确需求与 Android 系统行为 | MediaSession、Notification、Playback Service | 已确认 |
| 2026-08-11 | P-022 | 听歌识曲开始前暂停 Resonote，识别结束后保持暂停，由用户明确恢复 | 用户明确需求 | Recognition、Playback 和状态恢复 | 已确认 |
| 2026-08-11 | P-023 | 列表单曲点击沿用旧 Mobile 插播逻辑，不用来源列表替换 Queue；播放全部才替换 | 用户明确选择与本地源码证据 | Song List、Queue 和 Playback | 已确认 |
| 2026-08-11 | P-024 | Queue 支持下一首、队尾追加、跳转、移除、清空、拖拽重排，并持久恢复为暂停状态 | 用户逐项确认 | Queue、Playback Service 和 Persistence | 已确认 |
| 2026-08-11 | P-025 | 普通不可播放项按 PC 逻辑延迟 3 秒自动切换，最多执行 5 次；成功后重置计数 | 用户要求参考 PC；修正其成功后不清零的实现缺陷 | Playback Error、Auth、Queue 和 UX Feedback | 已确认 |
| 2026-08-11 | P-026 | 播放模式保留列表循环、随机、单曲循环、顺序播放到队尾停止四种 | PC 功能证据与用户确认 | Playback State、Queue 和系统媒体控制 | 已确认 |
| 2026-08-11 | P-027 | Player 提供下一首和队尾追加；分享入口作为低频操作进入 Overflow 候选，具体布局后续设计 | 用户明确需求 | Player、Track Actions、Queue 和页面设计 | 已确认 |
| 2026-08-11 | P-028 | 歌词完整保留同步、逐字/逐行、翻译/音译、跳转和个性化设置 | 用户确认与 PC/Mobile 功能证据 | Lyrics、Player、Settings 和 Accessibility | 已确认 |
| 2026-08-11 | P-029 | 在线七档音质、本地/云盘真实信息、六档倍速、响度均衡、睡眠定时和系统音量进入产品范围 | 用户逐项确认 | Playback、Media Source、Settings 和 System Integration | 已确认 |
| 2026-08-11 | P-030 | 支持无缝播放与默认关闭的 3/5/8 秒交叉淡化；不建立均衡器/低音/环绕 | 用户逐项确认 | Playback Pipeline、Settings 和测试矩阵 | 已确认 |
| 2026-08-11 | P-031 | 音频路由使用系统 Output Switcher；耳机/蓝牙支持系统控制，意外断开暂停且重连不自动恢复 | 用户逐项确认 | MediaSession、Audio Route 和设备恢复 | 已确认 |
| 2026-08-11 | P-032 | Android Auto 与 Google Cast 当前均不接入，未来有明确需求时重新评审 | 用户判断无必要 | 产品范围、依赖与系统集成 | Out |
| 2026-08-11 | P-033 | 全局搜索只查在线内容；本地/云盘各自搜索；本机搜索历史最多 20 条；热词与建议按 API 可用性提供 | 用户逐项确认与 PC 搜索行为 | Search、Local Music、Cloud 和 Persistence | 已确认 |
| 2026-08-11 | P-034 | 搜索包含综合、单曲、歌单、专辑、MV、歌手；歌曲插播，集合与作者进入详情，MV 进入独立播放页 | 用户确认并要求参考 PC MV | Search、Details、Video 和 Playback | 已确认 |
| 2026-08-11 | P-035 | 分享保留可见入口但当前不落地，点击明确提示暂未开放 | 用户明确需求 | Player、Overflow 和 Feedback | 入口 Must；能力 Deferred |
| 2026-08-11 | P-036 | MV 获准进入后暂停音乐并自动播放，退出保持音乐暂停；支持全屏和条件式清晰度，不支持后台/PiP/点赞/收藏/下载；方向与门禁由 P-071、P-075、P-076 修订 | 用户逐项确认及后续方向/鉴权澄清 | Video、Playback、Details、Orientation 和 System Integration | 已确认并被后续决策细化 |
| 2026-08-11 | P-037 | 歌单、专辑、歌手详情进入 V1；使用独立目的地与状态模型并复用内部歌曲列表能力 | 用户逐项确认；PC 单页多类型实现仅作功能证据 | Details、Navigation、Search、Home、Discover 和 My | 已确认 |
| 2026-08-11 | P-038 | 只有自建歌单可编辑资料、歌曲和删除；其他歌单只可收藏，专辑不可编辑；批量操作按所有权开放 | 用户明确权限边界 | Playlist、Collection、Auth 和 Batch Actions | 已确认 |
| 2026-08-11 | P-039 | 导航前已知无有效 ID 时禁用详情入口并就地说明；无法预判且进入后才发现缺失/失效 ID、内容删除或解析失败时显示错误页 | 用户确认两层防御逻辑 | Details、Navigation 和 Error Recovery | 已确认 |
| 2026-08-11 | P-040 | 歌曲信息使用 Sheet/Dialog，展示真实元数据，不建立独立页面 | 用户确认 | Track Actions、Metadata 和 Player | 已确认 |
| 2026-08-11 | P-041 | 在线歌曲下载不进入 V1；在线播放使用可淘汰的自动缓存，设置页可查看占用并清除 | 用户逐项确认；PC 无用户下载能力 | Playback Cache、Settings、Storage 和 Offline | Cache Must；Download Deferred |
| 2026-08-11 | P-042 | 本地导入副本不是缓存，清除缓存不得影响本地音乐、Queue、收藏、历史或云盘数据 | 用户确认 | Local Music、Cache、Persistence 和 Data Safety | 已确认 |
| 2026-08-11 | P-043 | 云盘同步 PC 实际使用的列表、上传、删除和播放地址接口；播放 URL 不等于设备下载 | 用户要求同步 PC 端接口与本地源码证据 | Cloud、Network、Playback 和 Storage | 已确认 |
| 2026-08-11 | P-044 | 未来离线下载使用独立“已下载”列表和 App 私有持久目录，不混入本地导入、不导出且删除需确认 | 用户确认保留未来合同 | Download、Storage 和 Library | Deferred 合同 |
| 2026-08-11 | P-045 | 设置从“我的”进入独立页面，并按外观、播放、歌词、缓存、权限、重置和关于组织 | 用户逐项确认 | Settings、My 和 Navigation | 已确认 |
| 2026-08-11 | P-046 | 支持跟随系统、浅色、深色、AMOLED 和 Android 动态取色；不支持动态色时回退品牌色 | 用户确认 | Theme、Design System 和 Compatibility | 已确认 |
| 2026-08-11 | P-047 | 首版只提供简体中文，但工程保留 Android 资源本地化与后续多语言适配能力 | 用户明确范围 | Resources、Formatting 和 Accessibility | 已确认 |
| 2026-08-11 | P-048 | 睡眠定时仅在 Player；歌词偏好可从 Player 与独立歌词设置页访问并共享状态 | 用户确认 | Player、Lyrics 和 Settings | 已确认 |
| 2026-08-11 | P-049 | 权限入口反映并跳转系统设置；重置设置不退出账号或删除媒体、队列、历史与收藏 | 用户确认 | Permissions、Settings、Auth 和 Data Safety | 已确认 |
| 2026-08-11 | P-050 | 不迁移 PC 桌面专属设置和自更新器；关于页提供版本、许可、隐私与项目链接，更新交给分发渠道 | 用户确认 | Settings、About、Distribution 和 Security | 已确认 |
| 2026-08-11 | P-051 | 本地导入统一支持系统单选、多选、目录授权及打开/分享入口；导入前验证真实媒体可读与可解码 | 用户逐项确认 | Local Import、SAF、System Intent 和 Media Validation | 已确认 |
| 2026-08-11 | P-052 | 重复文件使用大小预筛选与 SHA-256 最终确认；重复冲突允许用户取消或以独立副本继续导入 | 用户确认副本模式 | Deduplication、Storage、Dialog 和 Local Model | 已确认 |
| 2026-08-11 | P-053 | 批量导入显示进度与结果并可取消剩余任务；已成功项保留，半成品回滚 | 用户确认 | Import Worker、Progress、Cancellation 和 Error Recovery | 已确认 |
| 2026-08-11 | P-054 | 删除本地歌曲同时删除私有副本和 Queue 引用并要求确认；首版元数据只读 | 用户确认 | Local Music、Playback、Metadata 和 Data Safety | 已确认 |
| 2026-08-11 | P-055 | 使用 Tabs Shell + 单一全局页面栈；三个 Tab 保存根页面状态，但不分别保存详情 Back Stack | 用户明确选择并要求参考 MoeKoeMusic-Mobile | Navigation、App Shell、State Restoration 和 Features | 已确认 |
| 2026-08-11 | P-056 | 发现/我的根页面 Back 回首页，首页根页面 Back 退出；Tab 切换和重复点击不重置、重载或滚动页面 | 用户确认 Mobile 风格返回逻辑 | Navigation、Home、Discover、My 和 Android Back | 已确认 |
| 2026-08-11 | P-057 | 退出登录需确认并返回“我的”匿名状态；账号资料、收藏、历史与云盘清除并在二级页面重新请求 | 用户确认并补充移动端二级页面行为 | Auth、My、Library、Cloud 和 Navigation | 已确认 |
| 2026-08-11 | P-058 | 本地音乐、Queue、进度和设备设置不随账号清除；公开在线歌曲可继续，账号授权内容立即暂停 | 用户逐项确认 | Auth、Playback、Local Music 和 Settings | 已确认 |
| 2026-08-11 | P-059 | 账号专属地址、临时数据和受保护缓存按账号隔离并在退出/换号时失效，公共缓存可保留 | 用户确认 | Auth、Media Cache、Cloud 和 Security | 已确认 |
| 2026-08-11 | P-060 | 上游明确认证状态码统一触发登录门禁；页面级登录后重载，原子操作不重试，后台只标记失效 | 用户明确登录门禁逻辑 | Network、Auth、Navigation 和 Error Mapping | 已确认 |
| 2026-08-11 | P-061 | V1 只保留单一当前账号，不提供已保存账号切换器；多账号选择仅属于单次登录流程 | 用户确认 | Auth、Credential Storage 和 Login | 已确认 |
| 2026-08-11 | P-062 | 最近播放分账号在线历史与设备本机历史；本地/云盘只进入本机历史，不上传普通账号历史 | 用户逐项确认与 PC 行为证据 | History、My、Local Music、Cloud 和 Auth | 已确认 |
| 2026-08-11 | P-063 | 本机媒体累计播放 10 秒后记入历史，短音频完整结束也记录；稳定身份去重并更新次数 | 用户确认 | Playback Events、History 和 Media Identity | 已确认 |
| 2026-08-11 | P-064 | 本机历史上限 500 条并支持单删/清空；账号历史删除能力只在上游 API 支持时提供 | 用户确认 | History、Persistence、API 和 Data Safety | 已确认 |
| 2026-08-11 | P-065 | 历史单曲沿用插播逻辑，播放全部才以当前历史结果替换 Queue | 用户确认 | History、Queue 和 Playback | 已确认 |
| 2026-08-11 | P-066 | 冻结面向大众、无广告沉浸式 Android 音乐应用的定位，统一在线、本地与个人云盘 | 用户全部确认 | Product Positioning、Scope 和 Messaging | 已确认 |
| 2026-08-11 | P-067 | 核心场景按在线播放、Full Player/Queue、系统承接、本地导入、账号音乐库、音频协调排序 | 用户确认 | Product Priorities 和 IA | 已确认 |
| 2026-08-11 | P-068 | 第一条 Vertical Slice 为匿名在线沉浸播放闭环，第二条为系统文件导入与持久本地播放 | 用户确认 | Delivery Plan、Playback、Home 和 Local Import | 已确认 |
| 2026-08-11 | P-069 | V1 以可测试任务、状态保持、系统承接、账号隔离和零广告打断验收，不依赖产品内埋点 | 用户确认 | Acceptance、Privacy 和 Release | 已确认 |
| 2026-08-11 | P-070 | Mini Player 的歌曲主体进入 Full Player，并提供可直接打开同一 Queue 的独立操作 | 用户补充确认 | Mini Player、Queue、Player 和 Accessibility | 已确认 |
| 2026-08-11 | P-071 | V1 仅正式支持 Android 手机；普通页面固定竖屏，MV 显式全屏横屏是唯一方向例外；平板、折叠展开态和桌面宽屏后续扩展 | 用户明确首版范围并进一步澄清 MV 全屏 | Device Scope、Orientation、Player 和 Video | 已确认并由 P-075 细化 |
| 2026-08-11 | P-072 | 触控为主要输入；鼠标/键盘/触控板使用 Android 系统能力，不制作桌面专属交互 | 用户确认 | Input、Accessibility 和 Interaction | 已确认 |
| 2026-08-11 | P-073 | Android TV、Wear OS、ChromeOS/桌面模式、Android Auto 与 Google Cast 不进入 V1 | 用户确认；Auto/Cast 延续 P-032 | Distribution、Platform 和 Dependencies | Out |
| 2026-08-11 | P-074 | 维持 minSdk 26；Adaptive 架构与宽窗口 Design System 保留为未来扩展基础，但不属于 V1 页面验收范围 | 用户确认其他平台原则与当前工程基线 | Compatibility、Design System 和 Future Scope | 已确认 |
| 2026-08-11 | P-075 | MV 仅在用户点击全屏时程序控制切换横屏，退出全屏/页面后恢复竖屏；不支持传感器自动旋转 | 用户明确方向行为 | Video、App Orientation、Lifecycle 和 QA | 已确认 |
| 2026-08-11 | P-076 | MV 是原子播放操作：只有明确认证失败才进入登录门禁；登录后返回来源且需再次点击；VIP、版权、地区和版本限制显示业务错误，晚到认证失败也不得自动重试 | 用户确认自动播放只发生在已获准进入之后 | Video、Auth Gate、Navigation 和 Playback | 已确认 |
| 2026-08-11 | P-077 | 外部文件冷启动完成后 Back 返回文件管理器，前台导入 Back 恢复原 App 页面；Deep Link/系统媒体详情 Back 返回归属 Tab 根页面 | 已确认的移动端页面导航与来源恢复语义 | Intents、Deep Links、Navigation 和 Task Restore | 已确认 |
| 2026-08-11 | P-078 | MV 横屏是同一 Video Player 的显式全屏状态，不建立独立横屏页面或单独视觉稿；播放器功能负责发起全屏，App 统一协调方向并保证恢复 | 用户明确要求依靠 Video Player 能力 | Video UI、Orientation、Lifecycle 和 Design Deliverables | 已确认 |
| 2026-08-11 | P-079 | Full Player 的封面页与歌词页固定使用横向 Pager；旧图只保留该交互方向，视觉、内容层级和组件必须重新设计 | 用户确认既有产品决策 | Player IA、Navigation、Gestures 和 Design Deliverables | 已确认 |
| 2026-08-11 | P-080 | V1 Compact 页面设计稿固定 `390dp` 宽；固定页面使用 `390 × 844dp`，滚动页面使用 `390 × Auto` 长画板；设计证据完整保留顶部 Status Bar 与底部手势安全区，系统区域构成参考 `player-cover-page.png` | 用户确认页面设计交付规范 | Foundation、Page Design、Home、Insets 和 Design Deliverables | 已确认 |
| 2026-08-12 | P-081 | Music Item 首行固定为 Title → Quality → VIP，尾部 Duration 与 More 先保留；长标题只做单行末尾省略，Quality/VIP 不得侵入或越过 Duration。Playlist Item 固定 1:1 Artwork 与单行标题；Mini Player 复用同一信息优先级，并与 Bottom Navigation 保持 16dp 分隔及独立 Queue 入口；底部容器颜色映射由 P-085 修订 | 用户明确要求将已冻结组件写成可跨线程执行的文档合同 | Component System、Home、Lists、Tabs Shell、Player 和 QA | 历史决策；角标布局由 P-086 取代 |
| 2026-08-12 | P-082 | Music Item 的 Playing Indicator 与 Duration 共用固定 Trailing Status Slot 且互斥；播放中以均衡器直接替换时长，More 继续保留 | 用户修正冻结视觉的播放中状态 | Component System、Song Lists、Playback State 和 QA | 已确认 |
| 2026-08-12 | P-083 | Compact 首页锁定为同一页面的三段滚动状态：推荐区域、6 首每日推荐、6 个推荐歌单、6 首无分类新歌速递；Top App Bar 与 Bottom Navigation 固定，Mini Player 以 Overlay 覆盖滚动内容，末尾 Content Padding 保证最后一项可完整滚出遮挡 | 用户确认先锁定设计并进入代码实现；ImageGen 细节留待真实组件校正 | Home、Tabs Shell、Lists、Design QA 和 Implementation | 已确认 |
| 2026-08-12 | P-084 | 首页首次加载与下拉刷新并发更新三个内容区；每日推荐每次成功后重抽 6 首；区块独立提交、失败保留旧内容并受请求代际保护；推荐电台按需使用 `top_card` 五种模式 | 用户确认以 `MoeKoeMusic-Mobile@ab71195d` 的刷新行为为准 | Home API、Repository、Refresh 和 Error Recovery | 已确认 |
| 2026-08-14 | P-085 | 主题以 MD3 语义体系和 NiA 组织方式为骨架，由冻结品牌种子生成完整 Light / Dark Scheme，AMOLED 从 Dark 派生，Android 12+ Dynamic 使用平台完整 Scheme。Bottom Navigation 使用 Material 默认 `surfaceContainer` 且不得单点覆色或手写阴影；Mini Player 使用 `surfaceContainerHigh` 与 Level 3 `6dp` 阴影 | 用户真机验收当前 Bottom Shell 后确认固定为规范 | Theme、Design System、Settings、Tabs Shell 和 QA | 主题决策保留；Mini Player Container 由 P-086 修正 |
| 2026-08-14 | P-086 | 以用户真机调整对应的 `f3c4673` 与 `55a50ca` 为 Music Item / Compact Mini Player 视觉与交互基线，并以当前明确验收修正容器角色：Quality/VIP 合并在封面左下角；Mini Player 与 Bottom Navigation 均使用 `surfaceContainer`，Mini Player 保留 Level 3 Shadow、`shapeLarge`、`artworkShapeStandard`；Compact 只保留 Pause/Play 与 Queue，不显示 Next；整卡点击由带 Shape 的 `Surface(onClick)` 承载 | 用户发现“规范对齐”将已验收实现回归，并明确要求 Mini Player 与 Bottom Navigation 颜色一致 | Component System、Song Lists、Tabs Shell、Mini Player、Roborazzi 和 QA | 已确认；现行合同 |
| 2026-08-15 | P-087 | Compact 首页推荐区域使用三张同宽、同高、同层级的 `1:1` 彩色入口卡，间距为 `8dp`，标题使用加粗 `titleSmall`、辅助文案使用常规 `labelSmall`，并使用语义色派生的轻微渐变；卡内使用已确认设计稿的波形、五柱榜单和同心唱片线性图形，不得用小号通用图标近似；电台卡只显示固定功能文案，不展示歌曲名、歌手或封面。播放操作使用 Compact Overlay Filled Icon Button，保留 `48dp` Target，同时使用 `28dp` 可见容器、`16dp` Glyph，并让可见容器与卡片右、下边缘各保留 `8dp`；排行榜与精选歌单整卡导航 | 用户查看实现与设计稿后明确修正图形造型、视觉占比、文字尺寸和播放按钮规范 | Home、Design System、Accessibility 和 Roborazzi | 已确认；现行合同 |
| 2026-08-15 | P-088 | 点击反馈必须服从控件视觉轮廓：无容器的文字、图标或组合内容统一使用 `ResonotePlainAction`，以内容淡化代替突兀矩形状态层并保留 48dp Target 与焦点指示；有圆角背景或边框的容器使用相同 Shape 的 `Surface(onClick)`；完整矩形状态层仅用于真实的边到边列表行。首页 Tab、Mini Player 播放按钮和标准 Material Button / IconButton 保持各自既有反馈 | 用户指出首页“播放全部”及其他无容器或圆角控件存在生硬点击方块，并明确排除 Tab 与 Mini Player 正常按钮 | Component System、Home、Discover、Login、Player、Accessibility 和 QA | 已确认；现行合同 |
| 2026-08-15 | P-089 | 重新打开 App 时恢复 Mini Player、完整 Queue、当前项、循环模式和最近进度，但固定保持暂停且不复用短时播放 URL；首页先展示设备上的上次缓存并自动刷新，成功区块替换、失败区块保留旧数据；首次无缓存使用保留真实 Top App Bar 的动态骨架屏 | 用户报告冷启动状态丢失并确认恢复、分区替换与骨架屏策略 | Playback、Home、Persistence、Startup 和 Roborazzi | 已确认；现行合同 |
| 2026-08-17 | P-090 | 主 App Secondary Palette 从 Catalog 身份用 Harmonic Violet 改为由 Pulse Rose 同色相派生的低彩度 Echo Rose；Catalog 与已验收首页排行榜入口继续独立使用 Violet，但不再让其控制主 App Tonal Button、选中容器与状态卡片 | 用户指出大量淡紫容器与主 App 主题不协调并明确要求直接修改主题色，同时现行首页视觉不得回归 | Foundation、Theme、Home、Buttons、Chips、Status Surfaces 和 Roborazzi | 已确认；现行合同 |
| 2026-08-24 | P-091 | 设置页使用单页扁平分组行，不使用大卡片；选择项固定为左侧标题、右侧当前值与箭头，音质和倍速使用底部单选弹窗；歌词暂不实现但保留入口，页面末尾内容留白必须兼容全局 Mini Player | 用户明确设置页优化方向并补充歌词、弹窗与 Mini Player 约束 | Settings、Playback、Lyrics、Mini Player 和 Roborazzi | 已确认；现行合同 |
| 2026-08-24 | P-092 | 关于页打开时通过 GitHub 公开 Latest Release 接口检查版本；已是最新、发现新版和失败均提供明确状态，发现新版只打开对应 Release 页面，不静默下载或安装 | 用户明确要求关于页直接使用 GitHub 发布版本检测更新；修订 P-050 的更新承接方式 | Settings、About、GitHub Releases、Network 和 Distribution | 已确认；现行合同 |
| 2026-08-24 | P-093 | 当前版本移除歌曲、Full Player、MV、歌单及其他内容的所有对外分享入口、占位提示与回调；文件管理器将音频分享给 Resonote 仍作为本地导入能力保留 | 用户明确要求去除目前所有分享功能；取代 P-027、P-035 中的分享入口约定 | Player、Song Actions、Content Details、Feedback 和 Documentation | 已确认；现行合同 |
| 2026-08-24 | P-094 | Full Player 右上角 Sheet 直接提供下一首、队尾、添加到歌单、歌曲信息与歌词设置；歌曲信息改为 Material 3 Bottom Sheet，歌曲名和艺人可进入自动搜索并在返回时恢复原 Player | 用户明确要求移除二级歌曲操作并美化信息弹窗、增加搜索跳转 | Player、Song Actions、Search、Navigation、Overlay 和 Roborazzi | 已确认；现行合同 |
| 2026-08-24 | P-095 | 歌词辅助文本中的翻译与音译改为两个独立持久化开关并默认同时开启；两者存在时翻译在前、音译在后，均保持低于主歌词的视觉层级 | 用户明确指出单选不适合辅助文本能力 | Lyrics、Settings、DataStore、Player 和 Roborazzi | 已确认；现行合同 |
| 2026-08-24 | P-096 | 歌曲信息 Sheet 不显示独立 Header 或搜索操作卡片；歌曲名、艺人和专辑仅以蓝色下划线文本表达可搜索性，点击后进入对应关键词搜索 | 用户认为 Header 与搜索卡片视觉过重，并补充专辑搜索 | Player、Song Info、Search、Overlay 和 Roborazzi | 已被 P-097 取代 |
| 2026-08-25 | P-097 | 歌曲信息 Sheet 使用紧凑封面头部与圆角详情容器，移除网页式蓝色下划线；点击歌曲、艺人和专辑时分别自动提交并直接进入单曲、歌手和专辑分类 | 用户真机审查后要求美化信息弹窗，并明确搜索入口应落到对应 Tab | Player、Song Info、Search、Navigation、Overlay 和 Roborazzi | 已确认；现行合同 |
| 2026-08-25 | P-098 | 匿名用户播放支持试听的 VIP 歌曲时直接播放一分钟试听源并显示 VIP 提示；进入试听、提示和试听结束均不得触发登录门禁。只有确实要求账号授权且不存在匿名试听能力的播放操作才能请求登录 | 用户指出现行产品已实现 VIP 一分钟试听，并明确纠正匿名 VIP 播放跳转登录的回归 | Playback、VIP Preview、Auth Gate、Navigation 和 QA | 已确认；现行合同 |
| 2026-08-26 | P-099 | 歌词设置提供完整桌面歌词能力：按需申请显示在其他应用上层权限，以独立前台服务跟随当前播放和同步歌词；支持单/双行、字号、暂停隐藏、拖动定位、锁定触摸穿透、通知解锁/关闭和位置重置，不申请其他应用的通知读取权限 | 用户明确要求实现且应用不发布 Google Play | Lyrics、Settings、Playback Service、Overlay Permission、Notification 和 Roborazzi | 已被 P-100 修订 |
| 2026-08-27 | P-100 | 桌面歌词是悬浮播放控制器而非纯歌词层：开启后即常驻，暂停只冻结同步进度，不隐藏歌词；点击展开锁定、上一首、播放/暂停、下一首、播放模式和关闭操作，无操作 3/5/8 秒后仅收起控制栏；锁定入口和独立解锁入口均位于悬浮层。桌面歌词外观与行为使用歌词设置下的独立子页面承载 | 用户真机审查首版实现后明确修正产品形态、暂停语义、控制能力和设置层级 | Lyrics、Settings、Playback Controller、Overlay、Navigation 和 Roborazzi | 已确认；现行合同 |
| 2026-08-27 | P-101 | 桌面歌词不得使用固定黑色蒙版或独立品牌色；封面可用时复用播放器 Palette 取色，封面不可用时跟随 Resonote Light / Dark / AMOLED 与系统动态强调色。设置页提供 0–100% 浮层透明度，默认 0% 仅显示歌词；接近透明时按 App 明暗主题保证文字和控制可读，有底板时按封面底色计算前景对比度 | 用户真机审查后指出固定黑色浮层、硬编码颜色和主题脱节 | Lyrics、Theme、Artwork Palette、Settings、Overlay 和 Accessibility | 已确认；现行合同 |
| 2026-08-27 | P-102 | 桌面歌词锁定只锁定悬浮窗位置，不关闭触摸或播放控制；展开态顶部左侧为锁定/解锁、右侧为关闭，底部依次为设置、上一首、播放/暂停、下一首、播放模式，播放按钮固定在悬浮窗正中。设置按钮直接打开桌面歌词设置子页面 | 用户真机审查后明确锁定语义、控制器相对位置和设置入口 | Lyrics、Playback Controller、Overlay、Navigation 和 Settings | 已确认；修订 P-100 的锁定与布局描述 |
| 2026-08-27 | P-103 | 桌面歌词播放中以约 60ms 独立插值刷新逐字进度，不提高全局播放状态刷新频率；逐字前沿使用柔和渐变，换行采用短时淡入与位移。透明底板下使用随前景明暗反转的增强描边和阴影，并提高当前行字号与权重、降低辅助行层级，以保证复杂桌面背景上的可读性 | 用户指出逐字切换生硬、悬浮歌词不明显，并要求参考成熟项目优化 | Lyrics Rendering、Overlay、Theme、Performance 和 Accessibility | 已确认；现行合同 |
| 2026-08-27 | P-104 | 桌面歌词位置以歌词区域而非整个展开控制器为锚点，控制显隐不得移动歌词；拖动使用系统窗口真实坐标。顶部左右与底部左右按钮采用相同边距严格对齐，全部控制使用 Resonote Material 3 圆形图标按钮语义，播放为强调 Filled，其余为 Tonal | 用户真机审查后指出拖拽失效、控制显隐导致歌词漂移、四角未对齐且控制器与 App 设计系统不统一 | Lyrics、Overlay、Interaction、Design System 和 Accessibility | 已确认；修订 P-102 的布局细节 |
| 2026-08-27 | P-105 | 桌面歌词控制栏显示与隐藏使用 160ms 纯透明度渐变；收起时先淡出、结束后再缩回窗口，动画被反向触发时从当前透明度继续。圆形按钮尺寸和触摸范围不变，内部图标统一缩放到原视觉尺寸的 76% | 用户真机审查后指出控制显隐仍有跳变且圆形按钮图标过大 | Lyrics、Overlay Animation、Controls 和 Accessibility | 已被 P-106 修订窗口收缩行为；图标尺寸继续有效 |
| 2026-08-27 | P-106 | 桌面歌词悬浮窗始终保持固定尺寸与坐标，控制显隐只做透明度动画，不再在动画前后调整系统窗口。锁定与解锁使用同色同尺寸的标准闭锁/开锁图标，不使用锁定强调色 | 用户真机复查后仍观察到系统窗口缩放导致的歌词抖动，并指出锁定态图标与解锁态不成套 | Lyrics、Overlay Window、Controls 和 Accessibility | 已被 P-107 修订歌词基准变化时的宽度行为；控制显隐固定尺寸继续有效 |
| 2026-08-27 | P-107 | 桌面歌词换行使用 260ms 缓动，主行与辅助行共同交接并抑制描边叠亮；切歌加载时保留旧歌词直至新内容到达。切歌 Palette 保留 A 色直至 B 色就绪，再以 280ms 直接插值，不经过主题默认色。窗口宽度按歌曲歌词测量并限制为 220–260dp，屏幕边缘保留 4dp；上下控制区、圆形按钮和图标同步缩小。闭锁图标严格沿用已验收开锁图标的锁体，只闭合锁梁 | 用户要求继续消除换行闪动、移除切歌默认色中转、缩小控制器并让拖拽边界按歌词宽度计算，同时纠正闭锁图标基准 | Lyrics Rendering、Artwork Palette、Overlay Sizing、Drag、Controls 和 Product Contract | 已确认；修订 P-103、P-105、P-106 |
| 2026-08-27 | P-108 | 桌面歌词的字号设置值是最大字号；当前歌词及辅助歌词必须根据悬浮窗的实际可用宽高自动换行并动态缩小字号，直到全部内容完整显示。不得限制为固定行数，不得省略或裁切；换行后逐字高亮仍按原文偏移连续推进 | 用户指出固定两行仍不能保证歌词完整，明确要求动态字号 | Lyrics Rendering、Responsive Typography、Karaoke Highlight 和 Accessibility | 已确认；修订 P-103、P-107 |
