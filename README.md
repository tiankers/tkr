# TKR

- **作者**：tiankers
- **适用版本**：Minecraft 1.21.1 / NeoForge
- **开源协议**：LGPL-2.1-only（可依赖，不允许修改代码）

## 构建 / 运行

```powershell
.\gradlew.bat build        # 产出 build/libs/tkr-<version>.jar
.\gradlew.bat runClient    # 启动客户端
.\gradlew.bat server       # 启动服务端
```

## 依赖

| 依赖 | 版本 | 类型 |
|---|---|---|
| Curios API | 9.5.1+1.21.1 | **必须** |

jar 在 `libs/`，由 `build.gradle` 的 `files(...)` 引入。
依赖声明在 `src/main/resources/META-INF/neoforge.mods.toml`。

## 目录

代码采用四层结构，依赖方向严格单向：`api` → `data` → `logic` → `client`。

- `src/main/java/dev/tkr/TkrMod.java` — mod 入口
- `src/main/java/dev/tkr/api/` — 公开契约；对外稳定，附属可依赖不可改
- `src/main/java/dev/tkr/data/` — 定义层；只声明「有什么」，不写玩法判定
- `src/main/java/dev/tkr/logic/` — 逻辑层；**禁止** import 任何客户端类，须服务端可独立运行
- `src/main/java/dev/tkr/client/` — 表现层；仅物理客户端加载
- `src/main/resources/META-INF/neoforge.mods.toml` — mod 元数据
- `src/main/resources/assets/tkr/`、`data/tkr/` — 资源与数据包内容
- `libs/` — 本地依赖 jar
- `docs/idea.md` — 玩法规格（待填充）
- `CHANGELOG.md` — 变更记录

各层的具体约束见对应目录下的 `package-info.java`。
