package dev.tkr.logic.strength;

/**
 * 属性上限放宽的执行者。
 *
 * <p>属于<b>逻辑层</b>。目标：把原版属性与本模组属性的上限放到极大，让力量机制不受
 * 原版为平衡而设的窄范围限制。
 *
 * <h2>为什么必须用反射</h2>
 * <p>1.21.1 的 {@code RangedAttribute} 是：
 * <pre>
 *   private final double minValue;
 *   private final double maxValue;
 *   public double sanitizeValue(double v) {
 *       return Double.isNaN(v) ? this.minValue : Mth.clamp(v, this.minValue, this.maxValue);
 *   }
 * </pre>
 * 且 {@code AttributeInstance.getValue()} 最终调用 {@code sanitizeValue(...)}。
 * 也就是说<b>任何超过上限的赋值都会被钳制回去</b>，而这两个字段是 {@code private final}，
 * NeoForge 21.1.232 <b>没有</b>提供修改属性范围的 Hook（已核验：jar 内不存在相关 neoforged 类）。
 *
 * <p>因此只能反射改写字段。可行性已实测确认：MC 类位于<b>未命名模块</b>
 * （{@code RangedAttribute.class.getModule().isNamed() == false}），
 * Java 21 下对未命名模块的类调用 {@code setAccessible(true)} 后写入 final 字段是允许的，
 * <b>不需要</b> {@code --add-opens}。实测结果：改写后 {@code sanitizeValue(1e300)} 返回 {@code 1e300}。
 *
 * <h2>为什么用一个很大的有限值而不是 Double.MAX_VALUE</h2>
 * <p>属性修饰符会做加法与乘法。若上限取 {@code Double.MAX_VALUE}，
 * 两次相乘就溢出为 {@code Infinity}，随后生命值等变成无限，存档与实体逻辑会崩坏。
 * 因此这里用一个<b>足够大且有限</b>的值（默认 {@link StrengthConfig#ATTRIBUTE_MAX}），
 * 既远超正常玩法需要，又不会在常见算术下溢出。
 *
 * <h2>副作用（须知）</h2>
 * <p>放宽的是<b>全局</b>属性上限，因此也会影响非 TKR 内容，包括原版与其他模组对同一属性
 * 的使用。这是「越大越好」这一需求的固有代价；如需只放宽力量，把
 * {@link StrengthConfig#EXPAND_VANILLA_ATTRIBUTE_RANGES} 设为 {@code false} 即可。
 */
public final class AttributeRangeExpander {

    private AttributeRangeExpander() {}

    /**
     * 放宽一个属性实例的范围。
     *
     * <p>只处理 {@code RangedAttribute}；NeoForge 的 {@code PercentageAttribute}
     * 继承自它，因此同样被覆盖（字段查找会沿继承链进行）。
     *
     * <p><b>下限的处理</b>：<b>不会</b>把下限改成负值。
     * 力量等属性语义上不该为负，若把下限设为 {@code -ATTRIBUTE_MAX}，
     * 会让「力量下限不足」这类判定与提示出现无意义的负值。
     * 因此下限保持原样，只放宽上限。
     *
     * @param attribute 目标属性
     * @return 成功改写返回 {@code true}；类型不符或反射失败返回 {@code false}
     */
    public static boolean expand(net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        if (!(attribute instanceof net.minecraft.world.entity.ai.attributes.RangedAttribute ranged)) {
            return false;
        }
        try {
            setFinalDouble(ranged, "maxValue", StrengthConfig.ATTRIBUTE_MAX);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            // 反射失败不应让模组崩掉：记录后继续，属性保持原上限
            System.out.println("[TKR] 警告: 无法放宽属性上限 ("
                    + attribute.getDescriptionId() + "): " + e);
            return false;
        }
    }

    /**
     * 反射改写 {@code private final double} 字段。
     *
     * <p><b>必须沿继承链查找</b>：{@code getDeclaredField} 只看本类，
     * 而 NeoForge 的 {@code PercentageAttribute}（用于
     * {@code generic.knockback_resistance}、{@code generic.movement_speed} 等）
     * 继承自 {@code RangedAttribute}，字段声明在<b>父类</b>上。
     * 实测教训：只用 {@code getDeclaredField} 时这两个属性会抛
     * {@code NoSuchFieldException: maxValue} 而漏改。
     *
     * <p>写完后立即读回校验，避免「以为改了实际没改」这类静默失败。
     *
     * @param target 目标对象
     * @param field  字段名（{@code maxValue} / {@code minValue}）
     * @param value  新值
     * @throws ReflectiveOperationException 整条继承链上都不存在该字段时抛出
     */
    private static void setFinalDouble(Object target, String field, double value)
            throws ReflectiveOperationException {
        java.lang.reflect.Field f = findField(target.getClass(), field);
        f.setAccessible(true);
        f.setDouble(target, value);
        double readBack = f.getDouble(target);
        if (readBack != value) {
            throw new IllegalStateException("字段 " + field + " 写入后读回不一致: 期望 "
                    + value + " 实际 " + readBack);
        }
    }

    /** 沿继承链查找字段，直到 {@code Object} 为止。 */
    private static java.lang.reflect.Field findField(Class<?> type, String name)
            throws NoSuchFieldException {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // 继续向父类查找
            }
        }
        throw new NoSuchFieldException(name + " (在 " + type.getName() + " 的继承链上未找到)");
    }
}
