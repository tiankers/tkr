# TKR 已知陷阱

> 每条格式：**现象 → 根因 → 正确做法**。新增教训请追加，不要覆盖历史。
> 目的：让下一个会话（可能是完全失忆的我）不重复踩同一个坑。

---

## P-001 依赖 `versionRange` 的行为不能靠推理，必须实跑验证

> **本条曾被修正。原「改用纯数字下界」的建议经实测是错的，详见下方「实测结果」。**

- **现象**：`crash-reports/crash-2026-09-19_18.49.19-fml.txt` 记录
  `Mod tkr requires patchouli Patchouli-1.21.1-93-NEOFORGE.jar / Currently, patchouli is 1.21.1-93-NEOFORGE`
  —— 上报版本与区间下界**逐字相同**，却被判定为不满足。
- **已知事实**：
  1. 当时 `neoforge.mods.toml` 写的是 `versionRange="[1.21.1-93-NEOFORGE,)"`，Patchouli 自报版本是 `1.21.1-93-NEOFORGE`。
  2. **FML 的版本区间解析与 Maven 不同构**：FML 4.0.42 与 maven-artifact 3.9.1 是两套实现，Maven 的结论不能移植到 `versionRange` 上。
  3. 本机**没有** FML 的版本比较类（属 launcher 范畴，不在 `neoforge-21.1.232-merged.jar` 内），**无法离线实测 FML 语义**。
- **实测结果（maven-artifact 3.9.1，仅代表 Maven，不代表 FML）**：
  ```
  [1.21.1-93-NEOFORGE,) contains 1.21.1-93-NEOFORGE = true    ← Maven 认为满足
  [93,)                 contains 1.21.1-93-NEOFORGE = false   ← 纯数字下界反而判不满足
  [9.5.1+1.21.1]        contains 9.5.1+1.21.1       = true
  [9.5.1+1.21.1,)       contains 9.5.1+1.21.1       = true
  [9.5,9.6)             contains 9.5.1+1.21.1       = true
  ```
  → **「改用纯数字下界 `[93,)`」是错误建议**（Maven 语义下直接判不满足）。原建议已作废。
- **正确做法**：
  1. **不要用文本推理或 Maven 复现来改 `versionRange`。** 区间语义以 FML 为准，而 FML 无法离线复现。
  2. 唯一可靠验证方式是**实跑一次客户端/服务端**，看 Mod List 与是否出现 `Mod xxx requires ...` 失败。
  3. 有运行时证据通过的区间**就不要动**。当前 `curios` 的 `versionRange="[9.5.1+1.21.1]"` 已在
     `run/logs/latest.log`（2026-09-19 21:03，FML 4.0.42）实测通过（Mod List 正常、无 requires 失败），**维持不动**。
  4. 拿不准时优先用「精确版本」或官方文档给出的区间，而不是自造区间。
- **状态**：Patchouli 已整体移除，此坑当前无活跃实例；但**任何新依赖都会复现**（Curios 版本含 `+`，同样敏感）。

## P-001b 启动失败 `build/classes/java/main is not a valid mod file`

- **现象**：`crash-2026-09-17_21.39.57-fml.txt` 与 `crash-2026-09-19_18.45.40-fml.txt` 的失败信息都是
  `File C:\Users\26461\Desktop\tkr\build\classes\java\main is not a valid mod file`，`Mod file: <No mod information provided>`。
- **根因**：该目录是编译输出的**类目录**，不是 jar，mod 元数据未就位。通常是 `processResources` 未执行、或直接以类目录当作 mod 路径启动所致。
- **正确做法**：先 `.\gradlew.bat build --offline` 或经由 `runClient`（其已 `dependsOn processResources, classes`）启动，不要手动把类目录当 mod。
- **注意**：这两次崩溃与 P-001 的版本区间问题**无关**，是同一天不同阶段的两个独立故障，排查时不要混为一谈。

## P-002 照抄 1.20.1 时代的 Curios 教程 → 编译失败

- **现象**：`ICurioItem`、`ICuriosHelper`、旧的槽位注册 API 在 9.5.1 中不存在或签名不同。
- **根因**：Curios 在 5.x（MC 1.20.1）→ 9.x（MC 1.21.1）之间做了接口重命名与方法重构。网上教程绝大多数停留在 5.x。
- **正确做法**：**只以 `references/curios-9.5.1-api.md` 为准**，该文件的签名是从本项目实际链接的 jar 用 `javap` 提取的。任何来自博客/论坛/wiki 的代码都必须先比对。

## P-003 `logic` 层引用客户端类 → 服务端专用环境崩溃

- **现象**：单机测试一切正常，开专用服务端或联机时启动即崩。
- **根因**：`net.minecraft.client.*` 及其依赖类在服务端不存在；类加载/校验时抛出。
- **正确做法**：`logic/` 与 `data/` 中**禁止**出现任何 `client` 包 import。需要表现层配合时，走 `logic/event` 发布事件或 `logic/network` 发消息，由 `client/` 主动订阅。

## P-004 用裸 NBT / `CompoundTag` 存物品数据 → 在 1.21 失效

- **现象**：按旧教程写的物品数据读写编译不过或运行时丢失。
- **根因**：1.20.5 起（1.21 延续）物品/方块状态迁移到**数据组件（DataComponent）**体系，属性修饰符、附魔、自定义数据都挂在组件上，并有 `Codec`/`StreamCodec` 要求。
- **正确做法**：自定义数据在 `data/component/` 声明为 `DataComponentType`，编解码放 `data/codec/`。不要退回 `CompoundTag` 手工读写。

## P-005 把 PowerShell 退出码 1 当作构建失败

- **现象**：`.\gradlew.bat build` 输出 `BUILD SUCCESSFUL in 3s`，但工具报 `[exit code: 1]` 并附带 `NativeCommandError`。
- **根因**：PowerShell 把 Gradle 写到 stderr 的内容（例如 `Picked up JAVA_TOOL_OPTIONS: ...`）当作错误记录，`2>&1` 管道进一步放大。
- **正确做法**：**看输出文本里的 `BUILD SUCCESSFUL` / `BUILD FAILED`**，不要只看退出码。这是本机的已知假失败。

## P-006 Gradle 离线镜像的存在与边界

- **现象**：构建日志首行 `[offline-mirror] ACTIVE gradleUserHome=C:\Users\26461\.gradle`，并输出 `[offline-mirror] pinned NeoFormRuntime to 2.0.18`。
- **根因**：`~/.gradle` 下装有 init script，把依赖解析重定向到本地缓存。
- **正确做法**：这是好事 —— 本机构建不依赖网络，加 `--offline` 可确定性避免网络等待。但**不要**在没有镜像的情况下引入新依赖（如升级 NeoForge），会直接失败。

## P-007 项目级技能的发现位置取决于 `.git` 是否存在

- **现象**：技能放在 `.dsh/skills/` 却不被发现（或换个目录启动就找不到）。
- **根因**：DSH 的 `skill-filesystem` 提供者从工作目录向上**寻找 `.git` 目录**来判定项目根，找不到才回退到工作目录本身。技能根为 `<项目根>/.dsh/skills/` 与 `<项目根>/.agents/skills/`。
- **正确做法**：本项目当前**不是 git 仓库**，因此项目根 = 工作目录 `C:\Users\26461\Desktop\tkr`，技能可被正确发现（已验证：`tkr-context` 出现在技能目录中）。**若日后在更高层目录初始化 git 仓库，项目根会上移，本技能会突然失效。** 建议在项目根执行 `git init` 把项目根钉死，同时获得版本历史。
- **技能文件要求**：目录形式的技能必须是 `<技能名>/SKILL.md`；`name` 必须匹配 `^[a-z0-9]+(?:-[a-z0-9]+)*$`（kebab-case，不允许下划线或大写）；YAML frontmatter 必填 `name` + `description`，可选 `whenToUse`（驼峰）。

## P-008 `run/` 目录不是源码，也不是当前状态的证据

- **现象**：`run/logs/`、`run/crash-reports/` 里有历史日志与崩溃报告，容易误判为「当前有故障」。
- **根因**：这是开发环境运行态数据，会累积过期内容；已在 `.gitignore` 中（`run/`）。
- **正确做法**：需要判断当前是否正常，**重新跑一次构建或启动**，不要引用旧日志。`run/` 下的配置与数据文件可以清理，NeoForge 会重新生成。

## P-009 误以为 1.21.1 的属性 id 是裸名（无 `generic.` 前缀）

- **现象**：按「新版本写法」用 `max_health` / `block_break_speed` 去查注册名或翻译键，取不到值或注册出错误 id。
- **根因**：**1.21.1 的属性 id 仍带前缀**。三份本地证据一致：
  1. 反编译源码 `Attributes.java`：`register("generic.max_health", new RangedAttribute("attribute.name.generic.max_health", ...))`
  2. 编译产物 `build/moddev/artifacts/neoforge-21.1.232-merged.jar` 的 `Attributes.class` 常量池字面量 = `generic.max_health`（裸 `max_health` 命中 **0** 次）
  3. `minecraft_1.21.1_client.jar` 的 `en_us.json`：34 个 `attribute.name.*` 键**全部带前缀**
  前缀分布：`generic.` 23 个、`player.` 7 个、`zombie.` 1 个，**无 `horse.`**（已并入 `generic.jump_strength`）。
- **为什么容易错**：扁平化（去掉前缀）在**更晚的版本**才发生，而网上资料常按新版本叙述，且不标注版本。**这个坑真实发生过：本次任务的指令里就写错了这个断言，是提取代理用本地产物证伪后才纠正的。**
- **正确做法**：属性 id 一律以 `references/mc-1.21.1-attributes.tsv` 为准（从 jar 提取）。翻译键形如 `attribute.name.generic.max_health`。

## P-010 用语言文件里的键集合反推游戏版本

- **现象**：在 1.21.1 的中文语言文件里看到 `attribute.name.nameplate_distance`、`waypoint_*`、`bounciness`、`air_drag_modifier` 等键，误判这些属性在 1.21.1 存在。
- **根因**：1.21.1 的 `zh_cn.json` 是一份**超集**文件（8557 键，而同期 `en_us.json` 只有 6881 键），混入了更晚版本才有的前瞻键与旧式遗留键（同时含 `attribute.name.generic.max_health` 与裸键 `attribute.name.max_health`）。它确由 1.21.1 自己的 assetIndex 指向（SHA1 已校验），是 Mojang 资源对象本身的问题。
- **正确做法**：**按精确键名查表，不要靠键集合推断版本。** 判断某属性是否存在，以 `Attributes.java` / `Attributes.class` 为准，语言文件只用来取显示名。
- **附带**：`attribute.name.*_desc` 在 1.21.1 **不存在**（en/zh 命中均为 0），所以属性没有官方描述文本。任何「简介」都是本项目手工撰写的，必须标注来源，不要冒充官方。

## P-011 本机是 Windows PowerShell 5.1，读中文 UTF-8 文件会静默取空值

- **现象**：用 `Get-Content -Raw | ConvertFrom-Json` 读无 BOM 的 UTF-8 中文文件（如 `zh_cn.json`），字段全部取不到值，**且不报错**。
- **根因**：本机 `pwsh` 背后是 PowerShell **5.1**，`Get-Content` 默认按 ANSI/GBK 解码。另外 5.1 的 `ConvertFrom-Json` **没有 `-AsHashtable` 参数**（会报 `NamedParameterNotFound`）。
- **正确做法**：
  ```powershell
  $text = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
  $map  = $text | ConvertFrom-Json          # 5.1 兼容；用 $map.PSObject.Properties.Name 取键
  ```
  需要判断哈希表键时用 `$props -contains $key`，不要用 `.ContainsKey()`。

## P-012 物品「既有耐久又可堆叠」→ 注册期直接抛异常

- **现象**：写 `new Item.Properties().durability(250).stacksTo(16)`，模组加载阶段抛 `IllegalStateException: Item cannot have both durability and be stackable`。
- **根因**：1.21.1 在 `Item.validateComponents()`（`Item.java:455-461`）新增硬校验：
  ```java
  if (map.has(DataComponents.DAMAGE) && map.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1)
      throw new IllegalStateException("Item cannot have both durability and be stackable");
  ```
  而 `durability(int)` 会**一次性设三个组件**（`Item.java:402-406`）：`MAX_DAMAGE=n`、`MAX_STACK_SIZE=1`、`DAMAGE=0`。所以只要之后把堆叠改大于 1 就撞校验。
- **正确做法**：耐久物品天生不可堆叠，不要再调 `stacksTo(n>1)`。旧版本教程没有这条校验，照抄必炸。

## P-013 `Item.getMaxStackSize()` 的兜底是 1，不是 64

- **现象**：自定义构造组件表的物品莫名其妙不可堆叠。
- **根因**：默认 64 并非硬编码在读取处，而来自公共组件集 `DataComponents.COMMON_ITEM_COMPONENTS`（`DataComponents.java:233-240`，其中 `.set(MAX_STACK_SIZE, 64)`）；而读取用的是 `getOrDefault(MAX_STACK_SIZE, 1)`（`Item.java:120`），**兜底为 1**。
- **正确做法**：正常用 `new Item.Properties()` 会拿到公共组件集，无需显式设置；一旦绕过公共组件集自行构造，必须显式 `stacksTo(...)`，否则物品不可堆叠。堆叠取值范围是 `ExtraCodecs.intRange(1, 99)`，硬上限 `Item.ABSOLUTE_MAX_STACK_SIZE = 99`。

## P-014 用 `DeferredRegister.create(...)` 注册数据组件（已弃用），应改用专用工厂

- **现象**：注册自定义 `DataComponentType` 时泛型推断困难或收到弃用警告。
- **根因**：NeoForge 21.1.232 中通用 `create(...)` 路线对 `DATA_COMPONENT_TYPE` 已被标注弃用，且泛型不易推断。正确的专用 API 是：
  ```java
  // DeferredRegister.java:169
  public static DataComponents createDataComponents(ResourceKey<Registry<DataComponentType<?>>> registryKey, String modid)
  // DeferredRegister.java:184 —— @Deprecated(since="1.21.1", forRemoval=true)，1.21.2 移除
  public static DataComponents createDataComponents(String modid)
  ```
  并在 `DeferredRegister.DataComponents`（第 649-670 行）上有专用方法，注释明确写着「use this to avoid inference issues」：
  ```java
  public <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> registerComponentType(
          String name, UnaryOperator<DataComponentType.Builder<D>> builder)
  ```
- **正确做法**：`DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID)` + `registerComponentType(...)`。完整骨架见 `references/item-components-1.21.1.md` §4。

## P-015 组件的「落盘」与「同步」是两件独立的事，且同步是默认开的

- **现象**：以为「没调 `networkSynchronized` → 组件不会同步到客户端」，于是错误地推断联机行为；或以为「没调 `persistent` → 组件既不同步也不落盘」。
- **根因**（逐行核验，`neoforge-21.1.232-sources.jar`）：
  1. `DataComponentType.Builder.build()`（`DataComponentType.java:73-75`）对**每个**组件都产出 stream codec：
     ```java
     StreamCodec<...> streamcodec = Objects.requireNonNullElseGet(
         this.streamCodec, () -> ByteBufCodecs.fromCodecWithRegistries(
             Objects.requireNonNull(this.codec, "Missing Codec for component")));
     ```
     → **不调 `networkSynchronized` 只是走通用回退路径，不是不能同步**；`.networkSynchronized(...)` 的真实价值是性能/格式优化。
  2. 落盘过滤在 `DataComponentPatch.CODEC`（第 46 行）的 `if (!datacomponenttype.isTransient())`；
     而 `DataComponentPatch.STREAM_CODEC`（第 58 行起）走 `type.streamCodec()` 且**无** `isTransient()` 过滤。
     → **transient 组件仍然可以上线**，transient 的确切含义是「**不落盘**」，不是「不同步」。
  3. 两个都不调 → `requireNonNull(this.codec, ...)` 直接 NPE（`Missing Codec for component`）。
- **正确做法**：把两个维度分开决定 —— 要存档 → `.persistent(Codec)`；想省网络开销/自定义格式 → 再加 `.networkSynchronized(StreamCodec)`。**不能把「没写 networkSynchronized」当成「不会同步」来推理行为。**

## P-016 PowerShell 5.1 的 `>` 重定向写出的是 UTF-16LE，不是 UTF-8

- **现象**：`javap ... > out.txt` 之后用 UTF-8 读取内容做正则匹配，**0 命中**，于是误判「目标字符串不存在」（本项目真实发生过：一度以为「57 个组件 id 在编译产物里全部缺失」）。
- **根因**：PS 5.1 的 `>`（即 `Out-File`）默认编码是 **UTF-16LE + BOM**，不是 UTF-8。用 UTF-8 解码只会得到一堆 NUL 字符。
- **正确做法**：
  - 需要 UTF-8 文本落盘时，用 `[System.IO.File]::WriteAllText($p, $t, (New-Object System.Text.UTF8Encoding $false))`；
  - 需要捕获外部命令输出到变量时，直接 `$out = & cmd ...`（内存字符串，不经文件编码）；
  - 若必须经文件，读取时按 UTF-16LE 解码，或改用 `| Out-File -Encoding utf8`。
- **附带**：正则捕获组会带上行尾 `\r`，比较前必须 `.Trim()`，否则 `custom_data\r != custom_data` 这类比较会静默失败。

## P-017 `java.lang.Math` 没有 `acosh` / `asinh` / `atanh`

- **现象**：写 `Math.acosh(x)` 编译失败 —— `找不到符号: 方法 acosh(double)`。
- **根因**：Java 的 `java.lang.Math` **只提供** `sinh` / `cosh` / `tanh`，
  **没有**反双曲函数 `asinh` / `acosh` / `atanh`（这几个是 C99 / Python / Kotlin 等才有的）。
- **正确做法**：自行实现或用恒等式换算：
  ```java
  private static double acosh(double x) {
      if (x < 1.0) return 0.0;
      return Math.log(x + Math.sqrt(x * x - 1.0));
  }
  // asinh(x) = ln(x + sqrt(x² + 1))
  // atanh(x) = 0.5 * ln((1 + x) / (1 - x))
  ```
- **教训**：跨语言写数学代码时，标准库函数集合会变。**依赖编译器把关，不要凭"别的语言有"就写**。

## P-018 曲线规格经过多轮澄清，极易被后来的会话误解

- **现象**：同一套「力量伤害曲线」在讨论中被反复改写过 —— 横轴一度被当成伤害值（实为力量）、
  耐久一度按「伤害/额值」连续扣（实为整数）、比较对象一度是力量（实为伤害值 D）、
  「百分比」一度被理解为「到额值拿满 B×p」（实为线性段每点力量的系数）。
- **根因**：需求方使用「伤害额值」「百分比」这类词时，指代对象在不同轮次发生变化，
  且**给出的示例数字本身并不自洽**（曾出现任何平滑曲线都无法同时满足 4 个点的情况）。
- **正确做法**：
  1. **以最终定稿文档为准**：`.dsh/skills/tkr-context/references/strength-spec.md`。
     改动前必读，不要从代码注释或旧对话推断。
  2. 遇到「示例数字互相矛盾」时，**用连续性反解参数并明确指出差异**，而不是硬凑数字。
     本轮即如此：用户写「力量 40 → 15」，连续性反解给出 16.32，已获用户确认。
  3. 数值类需求**必须在文档中记录"已被否决的方案"**，否则后来的会话可能重新实现被否决的版本。

## P-019 把游戏总线事件注册到模组总线 → 构造器抛异常，模组加载失败

- **现象**（本项目真实发生，2026-09-19 实跑客户端时）：
  ```
  Failed to create mod instance. ModID: tkr, class dev.tkr.TkrMod
  java.lang.IllegalArgumentException: Method ... StrengthDamageHandler.onIncomingDamage(
      net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent)
      has @SubscribeEvent annotation, but takes an argument that is not valid for this bus
  Caused by: This bus only accepts subclasses of interface net.neoforged.fml.event.IModBusEvent,
      which class LivingIncomingDamageEvent is not.
  ```
  随后是一连串 `Cowardly refusing to send event ... to a broken mod state`。
- **根因**：NeoForge 有**两条事件总线**：
  - **模组总线（MOD）** —— 只接受实现 `net.neoforged.fml.event.IModBusEvent` 的事件
    （如 `FMLCommonSetupEvent`、`EntityAttributeModificationEvent`、`RegisterEvent`）。
    通过 `TkrMod` 构造器的 `IEventBus` 参数获取。
  - **游戏总线（GAME）** —— 玩法事件（如 `LivingIncomingDamageEvent`、`ItemTooltipEvent`、
    `PlayerEvent` 系列）。**不能**用构造器的那个 bus 注册。
- **为什么编译期发现不了**：`IEventBus.register(Object)` 的签名对任何对象都成立，
  校验发生在**运行期**。`.\gradlew.bat build` 会通过，只有实跑才会炸。
- **正确做法**：游戏总线事件用注解自动注册：
  ```java
  @EventBusSubscriber(modid = TkrMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)   // bus 默认就是 GAME
  public final class XxxHandler { @SubscribeEvent public static void on(SomeGameEvent e) {...} }
  ```
  仅客户端的事件再加 `value = Dist.CLIENT`。模组总线事件既可以这样写（`bus = MOD`），
  也可以在构造器里 `bus.register(Class)`。
- **教训**：**"编译通过"不等于"能加载"**。涉及注册与总线接线的改动，必须实跑一次
  `runClient` 看日志，不能只靠 `build` 成功就下结论。

## P-020 判断"stdout 有没有输出"要读进程输出，不要只读 latest.log

- **现象**：加了 `System.out.println` 诊断行，在 `run/logs/latest.log` 里搜不到，一度怀疑事件没触发。
- **根因**：本次运行还报了
  `Unable to delete file ...\run\logs\latest.log: 另一个程序正在使用此文件` —— 上一个客户端进程
  尚未完全退出时重启，log4j 无法轮转日志文件，新进程的输出走了**文件描述符被占用的旧文件/标准输出**，
  而 `latest.log` 里只有部分内容。
- **正确做法**：诊断输出优先从**进程输出**（后台任务的 stdout）读取，`latest.log` 作为辅助。
  重启客户端前确认旧进程已退出。
- **附带**：`StatusConsoleListener Advanced terminal features are not available` 与
  `glfwInit took ... seconds` 都是**无害**的噪音，不要当错误。

## P-021 反射改属性上限时用 `getDeclaredField` 会漏掉所有 NeoForge 子类

- **现象**（本项目真实发生，实跑客户端时）：
  ```
  [TKR] 警告: 无法放宽属性上限 (attribute.name.generic.knockback_resistance): NoSuchFieldException: maxValue
  [TKR] 警告: 无法放宽属性上限 (attribute.name.generic.movement_speed): NoSuchFieldException: maxValue
  [TKR] 警告: 无法放宽属性上限 (neoforge.swim_speed): NoSuchFieldException: maxValue
  [TKR] 属性上限放宽: 已处理 31 个属性，改写失败 3 个
  ```
- **根因**：`Class.getDeclaredField(name)` **只看本类**，不查父类。
  `RangedAttribute.maxValue` 声明在 `RangedAttribute` 上，但 NeoForge 的
  `PercentageAttribute`（`generic.knockback_resistance`、`generic.movement_speed`、
  `neoforge.swim_speed` 等都是它）**继承**自 `RangedAttribute`，
  于是这些实例的字段在父类上，`getDeclaredField` 找不到。
- **正确做法**：沿继承链逐级查找：
  ```java
  for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
      try { return c.getDeclaredField(name); } catch (NoSuchFieldException ignored) { }
  }
  throw new NoSuchFieldException(name);
  ```
- **教训**：凡是「反射改原版字段」，都要假定目标可能是子类实例而字段在父类。
  **这也是必须实跑的原因** —— 编译期完全看不出，而且失败是静默的（只打警告、继续运行），
  不实跑就会以为 34 个属性都放宽了。

## P-022 放宽属性下限为负值会污染语义

- **现象**：把 `minValue` 也一并改成 `-ATTRIBUTE_MAX` 后，力量属性范围变成 `[-1.0E12, 1.0E12]`。
- **根因**：属性上限放宽只需要动 `maxValue`。`minValue` 是语义下限
  （生命值最小 1、护甲最小 0），改成负值没有意义，还会让
  「力量下限不足」一类判定与提示出现无意义的负值。
- **正确做法**：**只放宽上限**。参照现有模组 AttributeFix 的配置格式即可确认这一约定：
  ```json
  "minecraft:generic.max_health": {
    "min": { "default": 1,    "value": 1 },        ← 保持原值
    "max": { "default": 1024, "value": 1000000 }   ← 只改这个
  }
  ```
  来源：<https://www.mcmod.cn/class/2264.html>
