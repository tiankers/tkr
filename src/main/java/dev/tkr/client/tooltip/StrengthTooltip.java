package dev.tkr.client.tooltip;

import dev.tkr.TkrMod;
import dev.tkr.data.attribute.TkrAttributes;
import dev.tkr.data.component.StrengthDamage;
import dev.tkr.data.component.TkrComponents;
import dev.tkr.logic.strength.MinStrengthGate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 把 TKR 组件的数值写进物品提示。
 *
 * <p>属于<b>表现层</b>（{@code client}）。判定逻辑在逻辑层
 * （{@link MinStrengthGate} 与 {@code StrengthMath}），这里只负责显示。
 *
 * <h2>为什么必须自己写，而不是让组件实现 TooltipProvider</h2>
 * <p>实测教训（本项目踩过）：{@code TooltipProvider} 对<b>自定义组件无效</b>。
 * 原版 {@code ItemStack.getTooltipLines()} <b>不是</b>遍历所有组件，
 * 而是<b>硬编码调用固定列表</b>：
 * <pre>
 *   this.addToTooltip(DataComponents.JUKEBOX_PLAYABLE, ...);
 *   this.addToTooltip(DataComponents.TRIM, ...);
 *   this.addToTooltip(DataComponents.STORED_ENCHANTMENTS, ...);
 *   this.addToTooltip(DataComponents.ENCHANTMENTS, ...);
 *   this.addToTooltip(DataComponents.DYED_COLOR, ...);
 *   this.addToTooltip(DataComponents.LORE, ...);
 *   this.addToTooltip(DataComponents.UNBREAKABLE, ...);
 * </pre>
 * 自定义组件不在其中，其 {@code addToTooltip} <b>永远不会被调用</b>，
 * 表现为「提示里什么都不显示」且不报错。NeoForge 21.1.232 也<b>没有</b>提供
 * 注册自定义组件提示的钩子（已核验：全 jar 仅 {@code IDataComponentHolderExtension}
 * 两个辅助方法，仍需手动调用）。故改用 {@link ItemTooltipEvent}。
 *
 * <p>详见 {@code PITFALLS.md} P-022 与 {@code DECISIONS.md} D-012 的修正记录。
 *
 * <h2>显示内容</h2>
 * <ul>
 *   <li>有 {@code tkr:strength_damage} 时：伤害系数、伤害额值、弯折点力量（暗灰派生信息）</li>
 *   <li>有 {@code tkr:min_strength} 时：需要力量（满足为绿字，不满足为红字）</li>
 * </ul>
 *
 * <p>注册：该事件属<b>游戏总线</b>，且本类只在物理客户端有意义，
 * 故 {@code value = Dist.CLIENT} 限定，避免在专用服务端加载客户端类。
 */
@EventBusSubscriber(modid = TkrMod.MOD_ID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME)
public final class StrengthTooltip {

    private StrengthTooltip() {}

    /**
     * 一次性诊断开关：第一次为「力量之刃」生成提示时，把实际加入的行打到日志。
     *
     * <p>存在的理由：提示是否真的显示，**无法靠编译或启动日志确认**，必须有人悬停物品。
     * 这个一次性日志让「提示确实生成过」变成可核对的证据，同时不会刷屏。
     * 排查完这类问题后可安全删除。
     */
    private static final java.util.concurrent.atomic.AtomicBoolean DIAGNOSED =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        appendStrengthDamage(event, stack);
        appendMinStrength(event, stack);

        if (stack.has(TkrComponents.STRENGTH_DAMAGE) && DIAGNOSED.compareAndSet(false, true)) {
            System.out.println("[TKR] 提示诊断: 为物品 '" + stack.getHoverName().getString()
                    + "' 生成提示，共 " + event.getToolTip().size() + " 行，内容如下:");
            for (int i = 0; i < event.getToolTip().size(); i++) {
                System.out.println("[TKR]   行" + (i + 1) + ": " + event.getToolTip().get(i).getString());
            }
        }
    }

    /** 显示力量伤害组件的三个数值。 */
    private static void appendStrengthDamage(ItemTooltipEvent event, ItemStack stack) {
        StrengthDamage damage = stack.get(TkrComponents.STRENGTH_DAMAGE);
        if (damage == null) return;

        event.getToolTip().add(Component.translatable("item.tkr.tooltip.damage_ratio",
                        percent(damage.bonusRatio()))
                .withStyle(ChatFormatting.BLUE));
        event.getToolTip().add(Component.translatable("item.tkr.tooltip.damage_threshold",
                        number(damage.threshold()))
                .withStyle(ChatFormatting.BLUE));
        event.getToolTip().add(Component.translatable("item.tkr.tooltip.damage_bend",
                        number(damage.bendStrength()))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** 显示力量下限要求。 */
    private static void appendMinStrength(ItemTooltipEvent event, ItemStack stack) {
        double required = MinStrengthGate.requiredStrength(stack);
        if (required <= 0.0) return;

        Player player = event.getEntity();
        boolean met = MinStrengthGate.meets(player, stack);

        Component line;
        if (met) {
            line = Component.translatable("item.tkr.tooltip.strength_ok", number(required))
                    .withStyle(ChatFormatting.DARK_GREEN);
        } else if (player == null) {
            line = Component.translatable("item.tkr.tooltip.requires_strength", number(required))
                    .withStyle(ChatFormatting.RED);
        } else {
            double current = player.getAttributeValue(TkrAttributes.STRENGTH);
            line = Component.translatable("item.tkr.tooltip.strength_short",
                            number(required), number(current))
                    .withStyle(ChatFormatting.RED);
        }
        event.getToolTip().add(line);
    }

    /** 把系数显示成百分数去掉小数：0.5 → "50"，0.375 → "37.5"。 */
    private static String percent(double ratio) {
        return number(ratio * 100.0);
    }

    /** 整数不带小数点，带小数最多两位并去掉尾随零。 */
    private static String number(double value) {
        if (value == Math.floor(value) && Double.isFinite(value)) {
            return String.valueOf((long) value);
        }
        String text = String.format(java.util.Locale.ROOT, "%.2f", value);
        return text.replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
