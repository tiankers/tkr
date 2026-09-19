package dev.tkr.data.attribute;

import dev.tkr.TkrMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TKR 自定义属性注册。
 *
 * <p>本类属于<b>定义层</b>：只负责「声明存在这个属性」，不实现任何玩法。
 * 属性如何影响伤害由 {@code dev.tkr.logic.strength} 决定；如何注入到生物由
 * {@code dev.tkr.logic.strength.StrengthAttributes} 决定。
 *
 * <h2>为什么注册为普通属性</h2>
 * <p>注册进 {@code BuiltInRegistries.ATTRIBUTE}（经 {@link Registries#ATTRIBUTE}）后，
 * 其他模组与数据包可以直接用原版 {@code attribute_modifiers} 组件或
 * {@code /attribute} 命令来增减力量，无需为本模组写任何适配代码。
 *
 * <h2>已核验的 API 事实（NeoForge 21.1.232 / 1.21.1）</h2>
 * <ul>
 *   <li>{@code RangedAttribute(String translationKey, double defaultValue, double min, double max)}</li>
 *   <li>链式方法 {@code setSyncable(true)} 与 {@code setSentiment(Attribute.Sentiment)} 可用
 *       （原版 {@code Attributes.java} 中即如此使用，如 {@code generic.armor}）。</li>
 *   <li>属性 id 形如 {@code tkr:strength}（本项目命名空间下无需前缀，前缀是原版自身的历史约定）。</li>
 * </ul>
 */
public final class TkrAttributes {

    /** 属性注册表工厂。 */
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, TkrMod.MOD_ID);

    /**
     * 力量属性的翻译键。
     *
     * <p>惯例与原版一致：{@code attribute.name.<命名空间>.<路径>}（原版为
     * {@code attribute.name.generic.armor} 等）。本模组的路径是 {@code tkr:strength}，
     * 故键为 {@code attribute.name.tkr.strength}。
     * <b>必须在 {@code assets/tkr/lang/*.json} 中提供对应条目</b>，否则界面会显示原始键名。
     */
    public static final String STRENGTH_TRANSLATION_KEY = "attribute.name.tkr.strength";

    /**
     * 力量属性 {@code tkr:strength}。
     *
     * <p>默认值 {@code 0.0}，范围 {@code [0, ATTRIBUTE_MAX]} —— 上限取
     * {@link dev.tkr.logic.strength.StrengthConfig#ATTRIBUTE_MAX}（极大且有限的值），
     * 使力量机制不受原版窄范围限制。整体属性范围还会被
     * {@code AttributeRangeExpander} 在启动时进一步统一放宽。
     *
     * <p>全体生物都会被注入该属性（见 {@code StrengthAttributes}），玩家通过物品的
     * {@code attribute_modifiers} 组件提升它。
     *
     * <p>{@code setSyncable(true)} 是必要的：客户端需要力量值来绘制物品提示中的
     * 「需要力量 X」比较，以及后续可能的 HUD 显示。
     */
    public static final DeferredHolder<Attribute, Attribute> STRENGTH =
            ATTRIBUTES.register("strength", () -> new RangedAttribute(
                    STRENGTH_TRANSLATION_KEY,
                    0.0,                                                  // 默认值：无力量
                    0.0,                                                  // 下限
                    dev.tkr.logic.strength.StrengthConfig.ATTRIBUTE_MAX    // 上限：极大
            ).setSyncable(true));

    private TkrAttributes() {}

    /**
     * 按注册名查找本模组的属性 Holder。
     *
     * <p>供 {@code /tkrattribute} 指令把用户输入的 {@code tkr:strength} 解析成
     * 注册表 Holder。**必须返回注册时的那个 Holder**，不能用
     * {@code Holder.direct(attribute)} 包一个 —— 属性实例是按注册表 Holder 存储的，
     * 身份不同会查不到。
     *
     * @param id 属性注册名
     * @return 匹配的 Holder；命名空间或路径不匹配时返回 {@code null}
     */
    public static net.minecraft.core.Holder<Attribute> holderById(net.minecraft.resources.ResourceLocation id) {
        if (!TkrMod.MOD_ID.equals(id.getNamespace())) return null;
        if (STRENGTH.getId().equals(id)) return STRENGTH;
        return null;
    }

    /**
     * 本模组全部属性的注册名，供指令 Tab 补全使用。
     *
     * <p><b>为什么从注册表查而不是写死列表</b>：写死的列表会在新增属性后忘记更新，
     * 导致新属性无法补全。这里直接扫 {@code BuiltInRegistries.ATTRIBUTE} 并按命名空间过滤，
     * 因此以后新增任何 TKR 属性都会自动出现在补全里。
     *
     * @return 命名空间为 {@code tkr} 的属性 id 集合
     */
    public static java.util.List<net.minecraft.resources.ResourceLocation> tkrAttributeIds() {
        return net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.keySet().stream()
                .filter(id -> TkrMod.MOD_ID.equals(id.getNamespace()))
                .sorted()
                .toList();
    }
}
