/**
 * 表现层 —— 只在物理客户端加载的一切。
 *
 * <h2>职责</h2>
 * <ul>
 *   <li>{@code render} —— 方块实体渲染器、实体渲染器、饰品槽位渲染。</li>
 *   <li>{@code gui} —— Screen、Menu 界面、HUD 叠加层。</li>
 *   <li>{@code model} —— 模型与材质加载（含自定义 {@code ModelLayer}、颜色着色器）。</li>
 *   <li>{@code input} —— 按键绑定与客户端交互入口。</li>
 * </ul>
 *
 * <h2>依赖方向</h2>
 * <p>本层位于依赖链末端，可以自由依赖 {@code api}、{@code data}、{@code logic}。
 * 反向依赖一律禁止：其他三层不得出现对本层的 import。</p>
 *
 * <h2>生命周期</h2>
 * <p>注册必须挂在客户端专属事件上（如 {@code FMLClientSetupEvent}、{@code RegisterClientTooltipComponentFactoriesEvent}），
 * 且不得在 {@code TkrMod} 的构造器中被直接触发。</p>
 */
package dev.tkr.client;
