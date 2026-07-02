# HealthX · 血压记录

一款用于记录血压、查看历史趋势、WebDAV 备份并导出图片分享的安卓 App。

## 功能

- **记录血压**：收缩压 / 舒张压 / 心率 / 状态 / 备注，日期时间可选，状态可自动判定或手动选择。
- **首页概览**：最新一条读数、本周平均、最近记录。
- **历史记录**：按 7/30/90 天/全部筛选，血压趋势折线图 + 列表，支持编辑/删除。
- **统计**：平均值、血压分布环形图、趋势图。
- **WebDAV 备份 / 恢复**：数据以 JSON 上传到 WebDAV（Nextcloud / 坚果云 / 群晖等）。
- **导出图片**：把趋势图 + 记录表渲染成一张图片，保存到相册或分享到微信/QQ 等。
- **导出 CSV**：全部记录导出为 CSV 分享。
- **主题**：跟随系统 / 浅色 / 深色。

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 本地存储 | **Room（SQLite）** — 表 `bp_record` |
| 设置 / WebDAV 配置 | DataStore |
| 图表 | 自绘 Compose Canvas（屏幕） + android.graphics Canvas（图片导出） |
| WebDAV | OkHttp（PROPFIND / MKCOL / PUT / GET） |
| 序列化 | kotlinx.serialization（备份 JSON） |
| 架构 | MVVM：ViewModel + StateFlow + Repository，手写依赖图（`AppGraph`） |

### 为什么用 SQLite / Room
血压是结构化的单表数据，量小、查询简单（按时间范围排序与聚合），SQLite 完全够用。Room 在 SQLite 之上提供编译期 SQL 校验、对象映射与协程/Flow 响应式支持。

## 目录结构

```
app/src/main/java/com/healthx/bp/
├─ HealthXApp.kt            # Application + AppGraph（手写 DI）
├─ MainActivity.kt
├─ data/
│  ├─ db/                   # Room: BpRecord / BpDao / BpDatabase
│  ├─ prefs/                # DataStore 设置与 WebDAV 配置
│  ├─ repository/           # BpRepository
│  └─ webdav/               # WebDavClient + BackupManager
├─ domain/                  # BpStatus 分级、BpStats 统计
├─ ui/
│  ├─ theme/                # 配色 / 排版 / 主题
│  ├─ navigation/           # 路由 + 底部导航
│  ├─ components/           # 折线图 / 环形图 / 卡片 / 时间范围选择
│  ├─ home / record / history / stats / settings / backup / export
└─ util/                    # 格式化、图片导出、分享、ViewModel 工厂
```

## 数据表

`bp_record(id, timestamp, systolic, diastolic, heartRate, status, note)`
- `timestamp`：epoch 毫秒
- `status`：`low | normal | elevated | high`

## 构建运行

1. 用 **Android Studio（Koala 或更新版本）** 打开本项目根目录。
2. 首次打开时 Android Studio 会提示生成 Gradle Wrapper / 下载依赖，按提示同步即可。
   - 若命令行构建：需本机安装 Gradle 8.9，执行 `gradle wrapper` 生成 wrapper，再 `./gradlew assembleDebug`。
3. 在 `local.properties` 中配置 SDK 路径（Android Studio 会自动生成）：
   ```
   sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   ```
4. 运行到设备/模拟器（minSdk 26 / Android 8.0+）。

## 图标

启动图标已从 `icon.png` 中裁出蓝色徽标，生成各密度 `mipmap-*/ic_launcher.png`。

## WebDAV 使用说明

- 服务器地址例：`https://dav.jianguoyun.com/dav/`（坚果云需使用「应用密码」）。
- 远程路径默认 `/healthx`，备份文件名 `healthx-backup.json`。
- 恢复会**覆盖**当前全部数据，请先备份。
