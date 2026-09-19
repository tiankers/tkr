package dev.tkr.data.item;

import dev.tkr.TkrMod;
import dev.tkr.data.component.StrengthDamage;
import dev.tkr.data.component.TkrComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TKR 物品注册。
 *
 * <p>属于<b>定义层</b>：只声明物品及其默认数据组件，不写玩法逻辑。
 *
 * <h2>关于「不用 NBT」</h2>
 * <p>所有数据都通过 {@code Item.Properties.component(...)} 挂载为数据组件，
 * 不使用 {@code CompoundTag}。参见
 * {@code .dsh/skills/tkr-context/references/item-components-1.21.1.md}。
 *
 * <p>当前包含一个用于验证机制的测试武器，玩法物品待玩法规格确定后再添加。
 */
public final class TkrItems {

    /** 物品注册表工厂。 */
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(TkrMod.MOD_ID);

    /**
     * 测试武器「力量之刃」。
     *
     * <p>用途：验证力量伤害曲线与提示显示。默认参数 {@code p = 0.5, T = 10}，
     * 弯折点为力量 20（此时伤害增量恰好 10）。
     *
     * <p>注意它<b>没有</b> {@code min_strength} 组件，因此不会触发使用门槛 ——
     * 便于单独验证曲线。若需测试门槛，给同一物品加该组件即可。
     */
    public static final DeferredItem<Item> STRENGTH_BLADE = ITEMS.register("strength_blade",
            () -> new Item(new Item.Properties()
                    .rarity(Rarity.UNCOMMON)
                    .component(TkrComponents.STRENGTH_DAMAGE.get(), new StrengthDamage(0.5, 10.0))));

    private TkrItems() {}
}
