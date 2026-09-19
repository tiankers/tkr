package dev.tkr.data.component;

import com.mojang.serialization.Codec;
import dev.tkr.TkrMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TKR 自定义物品数据组件。
 *
 * <p>属于<b>定义层</b>：只声明组件本身（类型、编解码、是否落盘、是否同步），
 * 不含任何玩法判定。玩法在 {@code dev.tkr.logic.strength}。
 *
 * <h2>为什么用组件而不是 NBT</h2>
 * <p>见 {@code .dsh/skills/tkr-context/references/item-components-1.21.1.md}：
 * 1.21 起物品状态就是组件模型，用 NBT 会与数据包、配方、其他模组脱节。
 *
 * <h2>落盘与同步的准确语义（已核验，勿凭直觉推断）</h2>
 * <ul>
 *   <li><b>落盘</b>：只有调了 {@code .persistent(Codec)} 才会写进存档。不调即 {@code isTransient()}，
 *       会被 {@code DataComponentPatch.CODEC} 跳过。</li>
 *   <li><b>同步</b>：{@code Builder.build()} 对<b>每个</b>组件都会产出 stream codec
 *       （未显式指定时回退 {@code ByteBufCodecs.fromCodecWithRegistries(codec)}），
 *       且 {@code DataComponentPatch.STREAM_CODEC} 不过滤 transient。
 *       所以「没写 {@code networkSynchronized}」<b>不等于</b>「不能同步」；
 *       显式写它主要是拿确定性的编码格式与性能。</li>
 * </ul>
 */
public final class TkrComponents {

    /** 组件注册表工厂（专用工厂写法，见 PITFALLS P-014）。 */
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TkrMod.MOD_ID);

    /**
     * 「力量下限」组件 {@code tkr:min_strength}。
     *
     * <p>语义：使用或穿戴该物品所需的**最低力量**。判定逻辑在
     * {@code dev.tkr.logic.strength.MinStrengthGate}。
     *
     * <p>取值范围 {@code [0, 1024]}。默认（组件缺失）视为 {@code 0}，即无要求。
     * 必须是 {@code double}：力量属性本身是 double。
     *
     * <p>需要同步：客户端要根据它显示「需要力量 X」提示，并按当前力量着色。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> MIN_STRENGTH =
            COMPONENTS.registerComponentType("min_strength", builder -> builder
                    .persistent(nonNegativeDoubleUpTo1024("min_strength"))
                    .networkSynchronized(ByteBufCodecs.DOUBLE));

    /**
     * 「力量伤害」组件 {@code tkr:strength_damage}。
     *
     * <p>语义：持有该组件的武器按使用者力量额外造成伤害，<b>系数 p 与额值 T 由组件自带</b>，
     * 因此每件武器可以不同。数据载体与自动提示见 {@link StrengthDamage}。
     *
     * <p>必须 {@code persistent}：武器参数要跟随物品存档。
     * 需要同步：客户端要显示提示中的系数/额值，且服务端结算需与客户端一致。
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StrengthDamage>> STRENGTH_DAMAGE =
            COMPONENTS.registerComponentType("strength_damage", builder -> builder
                    .persistent(StrengthDamage.CODEC)
                    .networkSynchronized(StrengthDamage.STREAM_CODEC));

    /**
     * {@code [0, ATTRIBUTE_MAX]} 的 double 校验。
     *
     * <p>为什么不直接用现成的：1.21.1 的 {@code ExtraCodecs} 只提供 float 的范围辅助
     * （{@code POSITIVE_FLOAT} 等）与 {@code intRange}，<b>没有</b> double 范围辅助
     * （已核验源码）。故此处自建。
     *
     * <p>上界取 {@link dev.tkr.logic.strength.StrengthConfig#ATTRIBUTE_MAX}，
     * 与属性上限保持一致，避免「属性放得下但组件校验拒绝」的不一致。
     */
    private static Codec<Double> nonNegativeDoubleUpTo1024(String field) {
        double max = dev.tkr.logic.strength.StrengthConfig.ATTRIBUTE_MAX;
        return Codec.DOUBLE.validate(value -> value < 0.0 || value > max
                ? com.mojang.serialization.DataResult.error(
                        () -> field + " must be within [0, " + max + "], got " + value)
                : com.mojang.serialization.DataResult.success(value));
    }

    private TkrComponents() {}
}
