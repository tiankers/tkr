package dev.tkr.logic.strength;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import dev.tkr.TkrMod;
import dev.tkr.data.attribute.TkrAttributes;
import dev.tkr.data.component.StrengthDamage;
import dev.tkr.data.component.TkrComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TKR 全部指令。
 *
 * <p>属于<b>逻辑层</b>（不依赖任何客户端类）。
 *
 * <h2>指令清单</h2>
 * <pre>
 *   /tkr strength_damage &lt;p&gt; [T]          给主手物品赋予力量伤害组件（T 省略时默认 10）
 *   /tkr strength_damage remove           移除该组件
 *   /tkr min_strength &lt;value&gt;            给主手物品设置力量下限
 *   /tkr min_strength remove              移除该组件
 *   /tkrattribute &lt;对象&gt; &lt;属性名&gt; &lt;数值&gt;   设置实体的 TKR 属性基础值
 *   /tkrlist                              列出所有 TKR 指令
 * </pre>
 *
 * <h2>为什么 /tkrlist 从指令树生成而不是写死</h2>
 * <p>写死的列表会在改指令后过期。这里用 Brigadier 的
 * {@code dispatcher.getSmartUsage(node, source)} 直接从<b>已注册的指令树</b>
 * 生成用法字符串，因此永远不会与实现不一致。
 *
 * <h2>为什么用 {@code @EventBusSubscriber(bus = GAME)}</h2>
 * <p>{@code RegisterCommandsEvent} 属游戏总线（不实现 {@code IModBusEvent}）。
 * 详见 {@code PITFALLS.md} P-019。
 */
@EventBusSubscriber(modid = TkrMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class TkrCommands {

    private TkrCommands() {}

    // ------------------------------------------------------------------ 注册

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(buildTkr());
        event.getDispatcher().register(buildAttribute());
        event.getDispatcher().register(buildList());
    }

    /**
     * {@code /tkr ...} —— 给手上物品赋组件。
     *
     * <h2>⚠️ 为什么每个数值参数都必须有上界</h2>
     * <p>实测崩溃（2026-09-20）：{@code tkr:strength_damage} 的 codec 要求值落在
     * {@code (0, ATTRIBUTE_MAX]} 内，但最初这里只写了 {@code doubleArg(0.000001)}（仅下界）。
     * 于是 {@code /tkr strength_damage 9223372036854775807 9223372036854775807}
     * 能把非法值写进物品 —— <b>物品一旦需要序列化就抛 {@code IllegalStateException}</b>：
     * 背包保存失败、网络同步失败、服务器崩溃（存档里的玩家背包会一直带毒）。
     *
     * <p>修复方式：用 Brigadier 的<b>带上界重载</b>
     * {@code DoubleArgumentType.doubleArg(min, max)}，让非法值在<b>解析阶段</b>就被拒绝，
     * 根本进不了物品数据。上界取 {@link StrengthConfig#attributeMax()}，
     * 与组件 codec 的校验上界<b>同源</b>，避免两处各写一个数而漂移。
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildTkr() {
        final double min = 0.000001;
        final double max = StrengthConfig.attributeMax();
        return Commands.literal("tkr")
                .then(Commands.literal("strength_damage")
                        .then(Commands.argument("p", DoubleArgumentType.doubleArg(min, max))
                                .executes(ctx -> applyStrengthDamage(ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "p"),
                                        StrengthConfig.defaultStrengthDamageThreshold()))
                                .then(Commands.argument("T", DoubleArgumentType.doubleArg(min, max))
                                        .executes(ctx -> applyStrengthDamage(ctx.getSource(),
                                                DoubleArgumentType.getDouble(ctx, "p"),
                                                DoubleArgumentType.getDouble(ctx, "T")))))
                        .then(Commands.literal("remove")
                                .executes(ctx -> removeComponent(ctx.getSource(),
                                        TkrComponents.STRENGTH_DAMAGE.get(), "strength_damage"))))
                .then(Commands.literal("min_strength")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, max))
                                .executes(ctx -> applyMinStrength(ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "value"))))
                        .then(Commands.literal("remove")
                                .executes(ctx -> removeComponent(ctx.getSource(),
                                        TkrComponents.MIN_STRENGTH.get(), "min_strength"))));
    }

    /**
     * {@code /tkrattribute <对象> <属性名> <数值>} —— 设置实体属性基础值。
     *
     * <p>属性名要求带 {@code tkr:} 命名空间；写错命名空间会被拒绝并提示，
     * 避免误改原版属性（原版属性请用原版 {@code /attribute}）。
     *
     * <p><b>Tab 补全</b>：{@code ResourceLocationArgument.id()} 自带的补全<b>不限定注册表</b>，
     * 按不到 {@code tkr:} 属性。因此这里显式挂 {@link #SUGGEST_TKR_ATTRIBUTES}，
     * 只列举本模组的属性（从注册表动态取，新增属性会自动出现）。
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildAttribute() {
        return Commands.literal("tkrattribute")
                // hasPermission 是 CommandSourceStack 的实例方法（Commands 没有同名静态方法）
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("attribute", ResourceLocationArgument.id())
                                .suggests(SUGGEST_TKR_ATTRIBUTES)
                                .then(Commands.argument("value",
                                                DoubleArgumentType.doubleArg(-StrengthConfig.attributeMax(),
                                                        StrengthConfig.attributeMax()))
                                        .executes(ctx -> applyAttribute(
                                                ctx.getSource(),
                                                EntityArgument.getEntities(ctx, "targets"),
                                                ResourceLocationArgument.getId(ctx, "attribute"),
                                                DoubleArgumentType.getDouble(ctx, "value"))))));
    }

    /**
     * 属性名参数的 Tab 补全：只列举 {@code tkr:} 命名空间下的属性。
     *
     * <p>用 {@code SharedSuggestionProvider.suggestResource} 而不是手搓
     * {@code Suggestions} —— 它会正确处理「已输入部分匹配」与候选排序。
     *
     * <p>注意 1.21.1 <b>没有</b> {@code net.minecraft.commands.synchronization.SuggestionProvider}
     * 这个类（只有一个遗留的 ClientSuggestionProvider）；这里用的是 Brigadier 自己的
     * {@code com.mojang.brigadier.suggestion.SuggestionProvider} 泛型接口。
     */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TKR_ATTRIBUTES =
            (ctx, builder) -> SharedSuggestionProvider.suggestResource(
                    TkrAttributes.tkrAttributeIds(), builder);

    /** {@code /tkrlist} —— 列出全部 TKR 指令。 */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildList() {
        return Commands.literal("tkrlist")
                .executes(ctx -> listCommands(ctx.getSource()));
    }

    // ------------------------------------------------------------------ /tkr 实现

    private static int applyStrengthDamage(CommandSourceStack source, double p, double threshold) {
        ServerPlayer player = requirePlayerWithItem(source);
        if (player == null) return 0;

        ItemStack stack = player.getMainHandItem();
        stack.set(TkrComponents.STRENGTH_DAMAGE.get(), new StrengthDamage(p, threshold));
        syncHeldItem(player);

        source.sendSuccess(() -> Component.translatable("commands.tkr.strength_damage.success",
                stack.getHoverName(), fmt(p), fmt(threshold)), false);
        return 1;
    }

    private static int applyMinStrength(CommandSourceStack source, double value) {
        ServerPlayer player = requirePlayerWithItem(source);
        if (player == null) return 0;

        ItemStack stack = player.getMainHandItem();
        stack.set(TkrComponents.MIN_STRENGTH.get(), value);
        syncHeldItem(player);

        source.sendSuccess(() -> Component.translatable("commands.tkr.min_strength.success",
                stack.getHoverName(), fmt(value)), false);
        return 1;
    }

    private static <T> int removeComponent(CommandSourceStack source,
                                           net.minecraft.core.component.DataComponentType<T> type,
                                           String label) {
        ServerPlayer player = requirePlayerWithItem(source);
        if (player == null) return 0;

        ItemStack stack = player.getMainHandItem();
        if (!stack.has(type)) {
            source.sendFailure(Component.translatable("commands.tkr.remove.absent", label));
            return 0;
        }
        stack.remove(type);
        syncHeldItem(player);

        source.sendSuccess(() -> Component.translatable("commands.tkr.remove.success",
                stack.getHoverName(), label), false);
        return 1;
    }

    // ------------------------------------------------------------------ /tkrattribute 实现

    /**
     * 把实体的某个 TKR 属性基础值设为指定值。
     *
     * <p>用 {@code setBaseValue} 而不是改修饰符：基础值是「该属性本来的值」，
     * 与装备/药水提供的修饰符分开，符合「设置属性」的直觉。
     *
     * <p><b>为什么按 id 匹配 DeferredHolder 而不是用 {@code Holder.direct(attribute)}</b>：
     * 属性实例是按<b>注册表 Holder</b> 存储的，直接包一个 {@code Holder.direct} 去查会取不到
     * （Holder 的身份不同）。匹配注册名后直接使用注册时的那个 Holder 才是正确做法，
     * 同时也避免了不安全的强制类型转换。
     *
     * @return 成功修改的实体数量
     */
    private static int applyAttribute(CommandSourceStack source, Collection<? extends Entity> targets,
                                      ResourceLocation attributeId, double value) {
        // 命名空间校验：只允许 tkr: 自己的属性（原版属性请用原版 /attribute）
        if (!"tkr".equals(attributeId.getNamespace())) {
            source.sendFailure(Component.translatable("commands.tkrattribute.wrong_namespace",
                    attributeId.toString()));
            return 0;
        }

        net.minecraft.core.Holder<Attribute> holder = TkrAttributes.holderById(attributeId);
        if (holder == null) {
            source.sendFailure(Component.translatable("commands.tkrattribute.unknown",
                    attributeId.toString()));
            return 0;
        }

        int changed = 0;
        int skipped = 0;
        for (Entity target : targets) {
            if (!(target instanceof LivingEntity living)) {
                skipped++;
                continue;
            }
            AttributeInstance instance = living.getAttribute(holder);
            if (instance == null) {
                // 该实体类型没有这个属性（例如未被注入属性的实体）
                skipped++;
                continue;
            }
            instance.setBaseValue(value);
            changed++;
        }

        if (changed == 0) {
            source.sendFailure(Component.translatable("commands.tkrattribute.no_target", skipped));
            return 0;
        }

        final int changedFinal = changed;
        source.sendSuccess(() -> Component.translatable("commands.tkrattribute.success",
                attributeId.toString(), fmt(value), changedFinal), true);
        return changed;
    }

    // ------------------------------------------------------------------ /tkrlist 实现

    /**
     * 从指令树动态列出 TKR 指令。
     *
     * <p>{@code getSmartUsage(node, source)} 返回的是**相对用法**（不含根指令名），
     * 必须自行补上根名才是可直接使用的完整指令。
     *
     * <p>实测教训（本项目踩过两处，均已由离线 Brigadier 复现验证）：
     * <ol>
     *   <li>直接输出 {@code "/" + usage} 会打印成 {@code /strength_damage (<p>|remove)}
     *       —— <b>丢了 tkr 前缀</b>，照抄无法执行。</li>
     *   <li>{@code getSmartUsage} <b>只对子节点生成用法</b>。像 {@code tkrlist} 这种
     *       没有子节点但自身可执行的指令，返回的是<b>空 map</b>，
     *       结果它<b>整个不出现在列表里</b>。因此需要空 map 时回退为「输出自身」。</li>
     * </ol>
     *
     * <p>另外根字面量自身会以带引号的形式（如 {@code "tkr"}）出现在返回值中，需要跳过。
     */
    private static int listCommands(CommandSourceStack source) {
        var dispatcher = source.getServer().getCommands().getDispatcher();
        List<Component> lines = new ArrayList<>();
        for (String rootName : new String[]{"tkr", "tkrattribute", "tkrlist"}) {
            CommandNode<CommandSourceStack> node = dispatcher.getRoot().getChild(rootName);
            if (node == null) continue;

            List<String> usages = new ArrayList<>();
            for (String usage : dispatcher.getSmartUsage(node, source).values()) {
                // 根字面量自身会以带引号的形式出现，跳过
                if (usage.startsWith("\"")) continue;
                // 补上根指令名：getSmartUsage 返回的是相对用法
                usages.add(usage.startsWith(rootName + " ") || usage.equals(rootName)
                        ? usage
                        : rootName + " " + usage);
            }
            // 没有子节点但自身可执行时，getSmartUsage 返回空 —— 回退为输出指令自身
            if (usages.isEmpty()) {
                usages.add(rootName);
            }
            for (String usage : usages) {
                lines.add(Component.translatable("commands.tkrlist.line", "/" + usage));
            }
        }

        source.sendSuccess(() -> Component.translatable("commands.tkrlist.header", lines.size()), false);
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
        return lines.size();
    }

    // ------------------------------------------------------------------ 工具

    /** 校验执行者是玩家且主手非空。 */
    private static ServerPlayer requirePlayerWithItem(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("commands.tkr.not_player"));
            return null;
        }
        if (player.getMainHandItem().isEmpty()) {
            source.sendFailure(Component.translatable("commands.tkr.empty_hand"));
            return null;
        }
        return player;
    }

    /** 把手里物品的变更同步给客户端（否则提示不会立刻更新）。 */
    private static void syncHeldItem(ServerPlayer player) {
        player.inventoryMenu.broadcastChanges();
    }

    /** 数字显示：整数不带小数点，否则最多两位。 */
    private static String fmt(double value) {
        if (value == Math.floor(value) && Double.isFinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
