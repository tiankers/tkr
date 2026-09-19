package dev.tkr.logic.strength;

import dev.tkr.TkrMod;
import dev.tkr.data.attribute.TkrAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 启动期处理：放宽属性上限 + 自检日志。
 *
 * <p>属于<b>逻辑层</b>。承担两件事：
 * <ol>
 *   <li><b>放宽属性上限</b>：调用 {@link AttributeRangeExpander} 把原版全部属性
 *       （以及本模组的 {@code tkr:strength}）的范围放大到
 *       {@link StrengthConfig#ATTRIBUTE_MAX}。属性是否真的进注册表、
 *       上限是否真的改写成功，<b>编译期都验证不了</b>，所以这里打日志留证据。</li>
 *   <li><b>自检输出</b>：把结果写进日志，便于排查「力量不生效」类问题。</li>
 * </ol>
 *
 * <p>属模组总线事件（{@code FMLCommonSetupEvent} 实现 {@code IModBusEvent}），
 * 故 {@code bus = MOD}。此处不修改任何游戏状态，只读注册表并打日志。
 */
@EventBusSubscriber(modid = TkrMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class StrengthStartupCheck {

    private StrengthStartupCheck() {}

    /** 启动自检 + 属性范围放宽。 */
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(StrengthStartupCheck::expandAndReport);
    }

    private static void expandAndReport() {
        // ---- 1. 放宽属性上限 ----
        int expanded = 0;
        int failed = 0;
        if (StrengthConfig.expandVanillaAttributeRanges()) {
            for (Attribute attribute : BuiltInRegistries.ATTRIBUTE) {
                if (AttributeRangeExpander.expand(attribute)) {
                    expanded++;
                } else if (attribute instanceof RangedAttribute) {
                    failed++;   // 是 RangedAttribute 却改写失败，说明反射真的出问题了
                }
            }
        }

        // ---- 2. 自检：确认 tkr:strength 已注册并打印关键信息 ----
        Attribute strength = TkrAttributes.STRENGTH.get();
        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(strength);
        if (id == null) {
            System.out.println("[TKR] 警告: 力量属性未出现在 BuiltInRegistries.ATTRIBUTE 中！");
            return;
        }
        System.out.println("[TKR] 力量属性自检: id=" + id
                + " 翻译键=" + strength.getDescriptionId()
                + " 默认值=" + strength.getDefaultValue()
                + " 范围=" + rangeOf(strength)
                + " 可同步=" + strength.isClientSyncable());
        System.out.println("[TKR] 属性上限放宽: 已处理 " + expanded + " 个属性，改写失败 " + failed
                + " 个，目标上限=" + StrengthConfig.ATTRIBUTE_MAX
                + "，覆盖原版属性=" + StrengthConfig.expandVanillaAttributeRanges());

        // 抽样核对几个关键属性，确认钳制真的失效
        reportClampSample("minecraft:generic.max_health");
        reportClampSample("minecraft:generic.attack_damage");
        reportClampSample("tkr:strength");
    }

    /**
     * 指令树自检：在服务器启动后确认 TKR 的三条指令都已注册。
     *
     * <p>存在的理由：指令是否注册成功，<b>编译期验证不了</b>，而「启动无异常」
     * 只是间接证据（Brigadier 的注册错误会在服务器启动时报错，但不报错不等于注册对了）。
     * 这里直接读指令树并打印，让「已注册」变成可核对的证据。
     */
    @SubscribeEvent
    public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        var dispatcher = event.getServer().getCommands().getDispatcher();
        for (String name : new String[]{"tkr", "tkrattribute", "tkrlist"}) {
            var node = dispatcher.getRoot().getChild(name);
            if (node == null) {
                System.out.println("[TKR] 指令自检: 缺失 /" + name + " ！");
                continue;
            }
            var children = node.getChildren().stream().map(c -> c.getName()).toList();
            System.out.println("[TKR] 指令自检: /" + name + " 已注册"
                    + (children.isEmpty() ? "（无子节点）" : "，子节点=" + children));
        }
    }

    private static String rangeOf(Attribute attribute) {
        if (attribute instanceof RangedAttribute ranged) {
            return "[" + ranged.getMinValue() + ", " + ranged.getMaxValue() + "]";
        }
        return "(非 RangedAttribute)";
    }

    /** 对一个属性做「先设远超原上限的值、再读回」的实测，证明钳制已失效。 */
    private static void reportClampSample(String attributeId) {
        Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.parse(attributeId));
        if (!(attribute instanceof RangedAttribute ranged)) return;
        double probe = StrengthConfig.ATTRIBUTE_MAX;
        double after = ranged.sanitizeValue(probe);
        System.out.println("[TKR]   钳制测试 " + attributeId
                + ": sanitizeValue(" + probe + ") = " + after
                + (after == probe ? "  ✔ 未被钳制" : "  ✘ 仍被钳制到上限"));
    }
}
