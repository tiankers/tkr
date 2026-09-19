# 更新日志

## [未发布]

### 变更

- 移除 Patchouli 依赖。
  - `build.gradle` 删除 `implementation files('libs/Patchouli-...jar')`。
  - `neoforge.mods.toml` 删除 `patchouli` 依赖声明块（该块曾因版本区间
    `[1.21.1-93-NEOFORGE,)` 被 Maven 版本比较判为不满足，导致 `run/crash-reports/`
    中三次 Mod 加载失败；该问题随依赖移除一并消失）。
  - 删除 `libs/Patchouli-1.21.1-93-NEOFORGE.jar`。
  - 删除手册资源目录 `src/main/resources/patchouli_books/`。
  - 清理 `run/` 下 Patchouli 的生成文件（`config/patchouli-client.toml`、`patchouli_data.json`）。
- 采用「严格领域四层 + 公开 API」结构骨架：`api` → `data` → `logic` → `client` 单向依赖，
  各层约束写在对应的 `package-info.java` 中。

### 规划中

- 文档/引导方案待定。手册能力已随 Patchouli 移除，后续如需游戏内引导，
  候选方案见 `docs/idea.md`。

- 移除 Curios API 依赖。项目现在**没有任何外部模组依赖**。