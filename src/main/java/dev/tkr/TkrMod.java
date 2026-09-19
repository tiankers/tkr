package dev.tkr;

import dev.tkr.data.attribute.TkrAttributes;
import dev.tkr.data.component.TkrComponents;
import dev.tkr.data.item.TkrItems;
import dev.tkr.logic.strength.StrengthAttributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * TKR 模组入口。
 *
 * <p>入口类的职责被刻意压到最小：<b>只做接线</b>（注册表登记 + 事件订阅），
 * 不含任何玩法逻辑。这是「严格领域四层」结构的要求，各层说明见对应
 * {@code package-info.java}。
 *
 * <h2>接线清单</h2>
 * <ul>
 *   <li>{@link TkrAttributes#ATTRIBUTES} —— 注册 {@code tkr:strength} 属性（定义层）</li>
 *   <li>{@link TkrComponents#COMPONENTS} —— 注册 {@code tkr:min_strength} 与
 *       {@code tkr:strength_damage} 组件（定义层）</li>
 *   <li>{@link TkrItems#ITEMS} —— 注册物品（定义层）</li>
 *   <li>{@link StrengthAttributes} —— 把力量注入全体生物（逻辑层，模组总线）</li>
 *   <li>{@code StrengthDamageHandler} —— 力量→伤害结算与武器使用门槛（逻辑层，<b>游戏总线</b>，
 *       通过 {@code @EventBusSubscriber} 自行注册）</li>
 * </ul>
 *
 * <p>注意：NeoForge 21.x 的 {@code @Mod} 注解只接收 {@code value()} 与 {@code dist()}
 * （已核验签名），事件总线通过构造器参数注入，不再是注解参数。
 */
@Mod(TkrMod.MOD_ID)
public final class TkrMod {

    /** 模组 ID，与 {@code gradle.properties} 的 {@code mod_id} 一致。 */
    public static final String MOD_ID = "tkr";

    /**
     * 模组构造器。
     *
     * @param modEventBus 模组事件总线，由 NeoForge 注入
     */
    public TkrMod(IEventBus modEventBus) {
        // 定义层：注册属性、数据组件与物品
        TkrAttributes.ATTRIBUTES.register(modEventBus);
        TkrComponents.COMPONENTS.register(modEventBus);
        TkrItems.ITEMS.register(modEventBus);

        // 逻辑层：订阅【模组总线】事件
        // 注意：游戏总线事件（如 LivingIncomingDamageEvent）不能在这里注册，
        // 必须用 @EventBusSubscriber(bus = Bus.GAME)，否则会抛 IModBusEvent 校验异常。
        modEventBus.register(StrengthAttributes.class);
    }
}
