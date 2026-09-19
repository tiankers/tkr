package dev.tkr.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 「力量伤害」组件 {@code tkr:strength_damage} 的数据载体。
 *
 * <p>属于<b>定义层</b>，是**纯数据**：只有参数、序列化与派生计算，不含任何显示逻辑。
 * 语义：持有该组件的武器会按使用者的力量属性额外造成伤害。
 *
 * <h2>参数含义</h2>
 * <ul>
 *   <li>{@code bonusRatio}（<b>伤害系数 p</b>）：线性段每点力量贡献的伤害</li>
 *   <li>{@code threshold}（<b>伤害额值 T</b>）：线性段的上限；同时也是伤害趋近的目标
 *       （上限为 {@code 2T}）与耐久消耗的判定分界</li>
 * </ul>
 *
 * <h2>曲线（见 {@code .dsh/skills/tkr-context/references/strength-spec.md}）</h2>
 * <pre>
 *   线性段   D = 力量 × p                       当 力量 ≤ T/p
 *   曲线段   D = 2T − T·e^(−p/T·(力量 − T/p))    当 力量 &gt;  T/p
 * </pre>
 * 因此<b>力量恰好等于 {@code T/p} 时</b>，D 达到 T（弯折点，且左右斜率连续）。
 *
 * <h2>为什么把两个值放在组件上而不是全局配置</h2>
 * <p>用户要求「可以让武器赋予玩家<b>可自定义的</b>百分比与额值」，
 * 即每件武器可以不同。放在物品组件上还能自动获得：数据包可改、配方与命令可直接引用、
 * 并能被原版 {@code give} 的组件语法直接设置。
 *
 * <h2>⚠️ 为什么这里<b>不</b>实现 {@code TooltipProvider}</h2>
 * <p>曾尝试让本类实现 {@code TooltipProvider} 以「自动显示」，<b>实测无效</b>：
 * 原版 {@code ItemStack.getTooltipLines()} 只对<b>硬编码的固定列表</b>调用
 * {@code addToTooltip}（JUKEBOX_PLAYABLE / TRIM / STORED_ENCHANTMENTS / ENCHANTMENTS /
 * DYED_COLOR / LORE / UNBREAKABLE），<b>自定义组件不在其中</b>，
 * 因此 {@code addToTooltip} 永远不会被调用，提示里什么都不显示且不报错。
 * NeoForge 21.1.232 也没有提供注册自定义组件提示的钩子。
 *
 * <p>因此显示改由表现层处理：{@code dev.tkr.client.tooltip.StrengthTooltip}
 * 订阅 {@code ItemTooltipEvent}。这同时满足四层结构的要求 ——
 * 数据层只提供数据，表现层负责显示。
 * 详见 {@code PITFALLS.md} P-023。
 */
public record StrengthDamage(double bonusRatio, double threshold) {

    /** 两个参数都必须为正，否则曲线退化（不产生加成）。 */
    public static final Codec<StrengthDamage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            positiveDouble("bonus_ratio").fieldOf("bonus_ratio").forGetter(StrengthDamage::bonusRatio),
            positiveDouble("threshold").fieldOf("threshold").forGetter(StrengthDamage::threshold)
    ).apply(instance, StrengthDamage::new));

    /**
     * 网络编解码。
     *
     * <p>字段少，直接组合两个 {@code DOUBLE} 即可。
     */
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, StrengthDamage> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.DOUBLE, StrengthDamage::bonusRatio,
                    net.minecraft.network.codec.ByteBufCodecs.DOUBLE, StrengthDamage::threshold,
                    StrengthDamage::new);

    /** {@code (0, ATTRIBUTE_MAX]} 的 double 校验（1.21.1 的 ExtraCodecs 无 double 范围辅助，故自建）。 */
    private static Codec<Double> positiveDouble(String field) {
        double max = dev.tkr.logic.strength.StrengthConfig.ATTRIBUTE_MAX;
        return Codec.DOUBLE.validate(value -> value <= 0.0 || value > max
                ? com.mojang.serialization.DataResult.error(
                        () -> field + " must be within (0, " + max + "], got " + value)
                : com.mojang.serialization.DataResult.success(value));
    }

    /**
     * 曲线弯折点对应的力量值 {@code T/p}。
     *
     * <p>纯派生数据，因此留在本层（{@code data} 不允许依赖 {@code logic}）。
     * 力量达到该值时，{@code D = T}，之后进入 e 型趋近段。
     */
    public double bendStrength() {
        if (bonusRatio <= 0.0) return 0.0;
        return threshold / bonusRatio;
    }

    /** 伤害增量上限 {@code 2T}。 */
    public double maxBonus() {
        return 2.0 * threshold;
    }
}
