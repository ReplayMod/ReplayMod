/**
 * Based on Elementa's ScrollComponent but with more stuff exposed and certain changes so we can get it to behave just
 * like we need it to:
 * - New scrollTo method
 * - Exposed onScroll method
 * - Fix scrollbars when size or content size changes
 * - Fix NaN as scrollPercentage when the offset range is zero in width
 * - Ignore components going into the negative when calculating actual width/height, fixes scrollbar when zoomed out
 *
 * MIT License
 *
 * Copyright (c) 2021 ReplayMod contributors
 * Copyright (c) 2021 Sk1er LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.replaymod.core.gui.common.elementa

import gg.essential.elementa.components.*

import gg.essential.elementa.UIComponent
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.constraints.resolution.ConstraintVisitor
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.utils.bindLast
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMouse
import gg.essential.universal.UResolution
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max

/**
 * Basic scroll component that will only draw what is currently visible.
 *
 * Also prevents scrolling past what should be reasonable.
 */
class UIScrollComponent @JvmOverloads constructor(
    emptyString: String = "",
    private val innerPadding: Float = 0f,
    private val scrollIconColor: Color = Color.WHITE,
    private val horizontalScrollEnabled: Boolean = false,
    private val verticalScrollEnabled: Boolean = true,
    private val horizontalScrollOpposite: Boolean = false,
    private val verticalScrollOpposite: Boolean = false,
    private val pixelsPerScroll: Float = 15f,
    private val scrollAcceleration: Float = 1.0f,
    customScissorBoundingBox: UIComponent? = null
) : UIContainer() {
    private var animationFPS: Int? = null

    private val actualHolder = UIContainer().constrain {
        x = innerPadding.pixels()
        y = innerPadding.pixels()
        width = RelativeConstraint(1f) - innerPadding.pixels()
        height = RelativeConstraint(1f)
    }

    //Exposed so its position and value can be adjusted by user
    val emptyText = UIWrappedText(emptyString, centered = true).constrain {
        x = CenterConstraint()
        y = SiblingConstraint() + 4.pixels()
    }

    private val scrollSVGComponent = getScrollImage().constrain {
        width = 24.pixels()
        height = 24.pixels()

        color = scrollIconColor.toConstraint()
    }

    var horizontalOffset = innerPadding
        private set
    var verticalOffset = innerPadding
        private set

    private var horizontalScrollBarGrip: UIComponent? = null
    private var horizontalHideScrollWhenUseless = false
    private var verticalScrollBarGrip: UIComponent? = null
    private var verticalHideScrollWhenUseless = false

    private var horizontalDragBeginPos = -1f
    private var verticalDragBeginPos = -1f

    private val horizontalScrollAdjustEvents: MutableList<(Float, Float) -> Unit> =
        mutableListOf(::updateScrollBar.bindLast(true))
    private val verticalScrollAdjustEvents: MutableList<(Float, Float) -> Unit> =
        mutableListOf(::updateScrollBar.bindLast(false))
    private var needsUpdate = true

    private var isAutoScrolling = false
    private var autoScrollBegin: Pair<Float, Float> = -1f to -1f
    private var currentScrollAcceleration: Float = 1.0f

    val allChildren = CopyOnWriteArrayList<UIComponent>()

    /**
     * Difference between the ScrollComponent's width and its contents' width.
     * Will not be less than zero, even if the contents' width is less than the
     * component's width.
     */
    val horizontalOverhang: Float
        get() = max(0f, calculateActualWidth() - getWidth())

    /**
     * Difference between the ScrollComponent's height and its contents' height.
     * Will not be less than zero, even if the contents' height is less than the
     * component's height.
     */
    val verticalOverhang: Float
        get() = max(0f, calculateActualHeight() - getHeight())

    init {
        this.constrain {
            width = ScrollChildConstraint() coerceAtMost 100.percentOfWindow()
            height = ScrollChildConstraint() coerceAtMost 100.percentOfWindow()
        }

        if (!horizontalScrollEnabled && !verticalScrollEnabled)
            throw IllegalArgumentException("ScrollComponent must have at least one direction of scrolling enabled")

        super.addChild(actualHolder)
        actualHolder.addChild(emptyText)
        this.enableEffects(ScissorEffect(customScissorBoundingBox))
        emptyText.setFontProvider(getFontProvider())
        super.addChild(scrollSVGComponent)
        scrollSVGComponent.hide(instantly = true)

        onMouseScroll {
            if (UKeyboard.isShiftKeyDown() && horizontalScrollEnabled) {
                onScroll(it.delta.toFloat(), isHorizontal = true)
            } else if (!UKeyboard.isShiftKeyDown() && verticalScrollEnabled) {
                onScroll(it.delta.toFloat(), isHorizontal = false)
            }

            it.stopPropagation()
        }

        onMouseClick { event ->
            onClick(event.relativeX, event.relativeY, event.mouseButton)
        }
    }

    private var lastHorizontalRange = 0f..0f
    private var lastVerticalRange = 0f..0f
    private var lastActualWidth = 0f
    private var lastActualHeight = 0f

    override fun draw(matrixStack: UMatrixStack) {
        val horizontalRange = calculateOffsetRange(isHorizontal = true)
        val verticalRange = calculateOffsetRange(isHorizontal = false)
        val actualWidth = calculateActualWidth()
        val actualHeight = calculateActualHeight()
        if (lastHorizontalRange != horizontalRange
            || lastVerticalRange != verticalRange
            || lastActualWidth != actualWidth
            || lastActualHeight != actualHeight
        ) {
            lastHorizontalRange = horizontalRange
            lastVerticalRange = verticalRange
            lastActualWidth = actualWidth
            lastActualHeight = actualHeight
            needsUpdate = true
        }

        if (needsUpdate) {
            needsUpdate = false

            // Recalculate our scroll box and move the content inside if needed.
            actualHolder.animate {
                horizontalOffset =
                    if (horizontalRange.isEmpty()) innerPadding else horizontalOffset.coerceIn(horizontalRange)
                verticalOffset = if (verticalRange.isEmpty()) innerPadding else verticalOffset.coerceIn(verticalRange)

                setXAnimation(Animations.IN_SIN, 0.1f, horizontalOffset.pixels())
                setYAnimation(Animations.IN_SIN, 0.1f, verticalOffset.pixels())
            }
            // Run our scroll adjust event, normally updating [scrollBarGrip]
            var percent = abs(horizontalOffset) / horizontalRange
            var percentageOfParent = this.getWidth() / actualWidth
            horizontalScrollAdjustEvents.forEach { it(percent, percentageOfParent) }

            percent = abs(verticalOffset) / verticalRange
            percentageOfParent = this.getHeight() / actualHeight
            verticalScrollAdjustEvents.forEach { it(percent, percentageOfParent) }
        }

        super.draw(matrixStack)
    }

    override fun afterInitialization() {
        super.afterInitialization()

        animationFPS = Window.of(this).animationFPS
    }

    /**
     * Sets the text that appears when no items are shown
     */
    fun setEmptyText(text: String) {
        emptyText.setText(text)
    }

    fun addScrollAdjustEvent(
        isHorizontal: Boolean,
        event: (scrollPercentage: Float, percentageOfParent: Float) -> Unit
    ) {
        if (isHorizontal) horizontalScrollAdjustEvents.add(event) else verticalScrollAdjustEvents.add(event)
    }

    @JvmOverloads
    fun setHorizontalScrollBarComponent(component: UIComponent, hideWhenUseless: Boolean = false) {
        setScrollBarComponent(component, hideWhenUseless, isHorizontal = true)
    }

    @JvmOverloads
    fun setVerticalScrollBarComponent(component: UIComponent, hideWhenUseless: Boolean = false) {
        setScrollBarComponent(component, hideWhenUseless, isHorizontal = false)
    }

    /**
     * A scroll bar component is an optional component that can visually display the scroll status
     * of this scroll component (just like any scroll bar).
     *
     * The utility here is that it is automatically updated by this component with no extra work on the user-end.
     *
     * The hierarchy for this scrollbar grip component must be as follows:
     *  - Have a containing parent being the full height range of this scroll bar.
     *
     *  [component]'s mouse events will all be overridden by this action.
     *
     *  If [hideWhenUseless] is enabled, [component] will have [hide] called on it when the scrollbar is full height
     *  and dragging it would do nothing.
     */
    fun setScrollBarComponent(component: UIComponent, hideWhenUseless: Boolean, isHorizontal: Boolean) {
        if (isHorizontal) {
            horizontalScrollBarGrip = component
            horizontalHideScrollWhenUseless = hideWhenUseless
        } else {
            verticalScrollBarGrip = component
            verticalHideScrollWhenUseless = hideWhenUseless
        }

        component.onMouseScroll {
            if (isHorizontal && horizontalScrollEnabled && UKeyboard.isShiftKeyDown()) {
                onScroll(it.delta.toFloat(), isHorizontal = true)
            } else if (!isHorizontal && verticalScrollEnabled) {
                onScroll(it.delta.toFloat(), isHorizontal = false)
            }

            it.stopPropagation()
        }

        component.onMouseClick { event ->
            if (isHorizontal) {
                horizontalDragBeginPos = event.relativeX
            } else {
                verticalDragBeginPos = event.relativeY
            }
        }

        component.onMouseDrag { mouseX, mouseY, _ ->
            if (isHorizontal) {
                if (horizontalDragBeginPos == -1f)
                    return@onMouseDrag
                updateGrip(component, mouseX, isHorizontal = true)
            } else {
                if (verticalDragBeginPos == -1f)
                    return@onMouseDrag
                updateGrip(component, mouseY, isHorizontal = false)
            }
        }

        component.onMouseRelease {
            if (isHorizontal) {
                horizontalDragBeginPos = -1f
            } else {
                verticalDragBeginPos = -1f
            }
        }

        needsUpdate = true
    }

    fun scrollToLeft(smoothScroll: Boolean = true) {
        scrollTo(horizontalOffset = Float.POSITIVE_INFINITY, smoothScroll = smoothScroll)
    }

    fun scrollToRight(smoothScroll: Boolean = true) {
        scrollTo(horizontalOffset = Float.NEGATIVE_INFINITY, smoothScroll = smoothScroll)
    }

    fun scrollToTop(smoothScroll: Boolean = true) {
        scrollTo(verticalOffset = Float.POSITIVE_INFINITY, smoothScroll = smoothScroll)
    }

    fun scrollToBottom(smoothScroll: Boolean = true) {
        scrollTo(verticalOffset = Float.NEGATIVE_INFINITY, smoothScroll = smoothScroll)
    }

    fun scrollTo(
        horizontalOffset: Float = this.horizontalOffset,
        verticalOffset: Float = this.verticalOffset,
        smoothScroll: Boolean = true
    ) {
        val horizontalRange = calculateOffsetRange(isHorizontal = true)
        val verticalRange = calculateOffsetRange(isHorizontal = false)
        this.horizontalOffset = if (horizontalRange.isEmpty()) innerPadding else horizontalOffset.coerceIn(horizontalRange)
        this.verticalOffset = if (verticalRange.isEmpty()) innerPadding else verticalOffset.coerceIn(verticalRange)

        if (smoothScroll) {
            needsUpdate = true
            return
        }

        actualHolder.setX(this.horizontalOffset.pixels())
        actualHolder.setY(this.verticalOffset.pixels())
        val horizontalFraction = abs(horizontalOffset) / horizontalRange
        val verticalFraction = abs(verticalOffset) / verticalRange
        horizontalScrollAdjustEvents.forEach { it(horizontalFraction, this.getWidth() / calculateActualWidth()) }
        verticalScrollAdjustEvents.forEach { it(verticalFraction, this.getHeight() / calculateActualHeight()) }
    }

    fun filterChildren(filter: (component: UIComponent) -> Boolean) {
        actualHolder.children.clear()
        actualHolder.children.addAll(allChildren.filter(filter).ifEmpty { listOf(emptyText) })
        actualHolder.children.forEach { it.parent = actualHolder }

        needsUpdate = true
    }

    @JvmOverloads
    fun <T : Comparable<T>> sortChildren(descending: Boolean = false, comparator: (UIComponent) -> T) {
        if (descending) {
            actualHolder.children.sortByDescending(comparator)
        } else {
            actualHolder.children.sortBy(comparator)
        }
    }

    fun sortChildren(comparator: Comparator<UIComponent>) {
        actualHolder.children.sortWith(comparator)
    }

    private fun updateGrip(component: UIComponent, mouseCoord: Float, isHorizontal: Boolean) {
        if (isHorizontal) {
            val minCoord = component.parent.getLeft()
            val maxCoord = component.parent.getRight()
            val dragDelta = mouseCoord - horizontalDragBeginPos

            horizontalOffset = if (horizontalScrollOpposite) {
                val newPos = maxCoord - component.getRight() - dragDelta
                val percentage = newPos / (maxCoord - minCoord)

                percentage * calculateActualWidth()
            } else {
                val newPos = component.getLeft() + dragDelta - minCoord
                val percentage = newPos / (maxCoord - minCoord)

                -(percentage * calculateActualWidth())
            }
        } else {
            val minCoord = component.parent.getTop()
            val maxCoord = component.parent.getBottom()
            val dragDelta = mouseCoord - verticalDragBeginPos

            verticalOffset = if (verticalScrollOpposite) {
                val newPos = maxCoord - component.getBottom() - dragDelta
                val percentage = newPos / (maxCoord - minCoord)

                percentage * calculateActualHeight()
            } else {
                val newPos = component.getTop() + dragDelta - minCoord
                val percentage = newPos / (maxCoord - minCoord)

                -(percentage * calculateActualHeight())
            }
        }

        needsUpdate = true
    }

    fun onScroll(delta: Float, isHorizontal: Boolean) {
        if (isHorizontal) {
            horizontalOffset += delta * pixelsPerScroll * currentScrollAcceleration
        } else {
            verticalOffset += delta * pixelsPerScroll * currentScrollAcceleration
        }

        currentScrollAcceleration =
            (currentScrollAcceleration + (scrollAcceleration - 1.0f) * 0.15f).coerceIn(0f, scrollAcceleration)

        needsUpdate = true
    }

    private fun updateScrollBar(scrollPercentage: Float, percentageOfParent: Float, isHorizontal: Boolean) {
        val component = if (isHorizontal) {
            horizontalScrollBarGrip ?: return
        } else {
            verticalScrollBarGrip ?: return
        }

        val clampedPercentage = percentageOfParent.coerceAtMost(1f)

        if ((isHorizontal && horizontalHideScrollWhenUseless) || (!isHorizontal && verticalHideScrollWhenUseless)) {
            if (clampedPercentage == 1f) {
                Window.enqueueRenderOperation(component::hide)
                return
            } else {
                Window.enqueueRenderOperation(component::unhide)
            }
        }

        if (isHorizontal) {
            component.setWidth(RelativeConstraint(clampedPercentage))
        } else {
            component.setHeight(RelativeConstraint(clampedPercentage))
        }

        component.animate {
            if (isHorizontal) {
                setXAnimation(
                    Animations.IN_SIN, 0.1f, basicXConstraint { component ->
                        val offset = (component.parent.getWidth() - component.getWidth()) * scrollPercentage

                        if (horizontalScrollOpposite) component.parent.getRight() - component.getHeight() - offset
                        else component.parent.getLeft() + offset
                    }
                )
            } else {
                setYAnimation(
                    Animations.IN_SIN, 0.1f, basicYConstraint { component ->
                        val offset = (component.parent.getHeight() - component.getHeight()) * scrollPercentage

                        if (verticalScrollOpposite) component.parent.getBottom() - component.getHeight() - offset
                        else component.parent.getTop() + offset
                    }
                )
            }
        }
    }

    private fun calculateActualWidth(): Float {
        if (actualHolder.children.isEmpty()) return 0f

        return actualHolder.children.let { c ->
            c.maxOf { it.getRight() } - c.minOf { it.getLeft() }.coerceAtLeast(actualHolder.getLeft())
        }
    }

    private fun calculateActualHeight(): Float {
        if (actualHolder.children.isEmpty()) return 0f

        return actualHolder.children.let { c ->
            c.maxOf { it.getBottom() } - c.minOf { it.getTop() }.coerceAtLeast(actualHolder.getTop())
        }
    }

    private fun calculateOffsetRange(isHorizontal: Boolean): ClosedFloatingPointRange<Float> {
        return if (isHorizontal) {
            val actualWidth = calculateActualWidth()
            val maxNegative = this.getWidth() - actualWidth - innerPadding
            if (horizontalScrollOpposite) (-innerPadding)..-maxNegative else maxNegative..(innerPadding)
        } else {
            val actualHeight = calculateActualHeight()
            val maxNegative = this.getHeight() - actualHeight - innerPadding
            if (verticalScrollOpposite) (-innerPadding)..-maxNegative else maxNegative..(innerPadding)
        }
    }

    private fun onClick(mouseX: Float, mouseY: Float, mouseButton: Int) {
        if (isAutoScrolling) {
            isAutoScrolling = false
            scrollSVGComponent.hide()
            return
        }

        if (mouseButton == 2) {
            // Middle click, begin the auto scroll
            isAutoScrolling = true
            autoScrollBegin = mouseX to mouseY

            scrollSVGComponent.constrain {
                x = (mouseX - 12).pixels()
                y = (mouseY - 12).pixels()
            }

            scrollSVGComponent.unhide(useLastPosition = false)
        }
    }

    override fun animationFrame() {
        super.animationFrame()

        currentScrollAcceleration =
            (currentScrollAcceleration - ((scrollAcceleration - 1.0f) / (animationFPS ?: 244).toFloat()))
                .coerceAtLeast(1.0f)

        if (!isAutoScrolling) return

        if (horizontalScrollEnabled) {
            val xBegin = autoScrollBegin.first + getLeft()
            val currentX = UMouse.Scaled.x

            if (currentX in getLeft()..getRight()) {
                val deltaX = currentX - xBegin
                val percentX = deltaX / (-getWidth() / 2)
                horizontalOffset += (percentX.toFloat() * 5f)
                needsUpdate = true
            }
        }

        if (verticalScrollEnabled) {
            val yBegin = autoScrollBegin.second + getTop()
            val currentY = UMouse.Scaled.y

            if (currentY in getTop()..getBottom()) {
                val deltaY = currentY - yBegin
                val percentY = deltaY / (-getHeight() / 2)
                verticalOffset += (percentY.toFloat() * 5f)
                needsUpdate = true
            }
        }

        needsUpdate = true
    }

    override fun addChild(component: UIComponent) = apply {
        actualHolder.removeChild(emptyText)

        actualHolder.addChild(component)
        allChildren.add(component)

        needsUpdate = true
    }

    override fun insertChildAt(component: UIComponent, index: Int) = apply {
        if (index < 0 || index > allChildren.size) {
            println("Bad index given to insertChildAt (index: $index, children size: ${allChildren.size}")
            return@apply
        }

        actualHolder.removeChild(emptyText)

        component.parent = actualHolder
        actualHolder.children.add(index, component)
        allChildren.add(index, component)

        needsUpdate = true
    }

    override fun insertChildBefore(newComponent: UIComponent, targetComponent: UIComponent) = apply {
        val indexOfExisting = allChildren.indexOf(targetComponent)
        if (indexOfExisting == -1) {
            println("targetComponent given to insertChildBefore is not a child of this component")
            return@apply
        }

        insertChildAt(newComponent, indexOfExisting)
    }

    override fun insertChildAfter(newComponent: UIComponent, targetComponent: UIComponent) = apply {
        val indexOfExisting = allChildren.indexOf(targetComponent)
        if (indexOfExisting == -1) {
            println("targetComponent given to insertChildAfter is not a child of this component")
            return@apply
        }

        insertChildAt(newComponent, indexOfExisting + 1)
    }

    override fun replaceChild(newComponent: UIComponent, componentToReplace: UIComponent) = apply {
        val indexOfExisting = allChildren.indexOf(componentToReplace)
        if (indexOfExisting == -1) {
            println("componentToReplace given to replaceChild is not a child of this component")
            return@apply
        }

        actualHolder.removeChild(emptyText)

        actualHolder.children.removeAt(indexOfExisting)
        allChildren.removeAt(indexOfExisting)

        newComponent.parent = actualHolder
        actualHolder.children.add(indexOfExisting, newComponent)
        allChildren.add(indexOfExisting, newComponent)

        needsUpdate = true
    }

    override fun removeChild(component: UIComponent) = apply {
        if (component == scrollSVGComponent) {
            super.removeChild(component)
            return@apply
        }

        actualHolder.removeChild(component)
        allChildren.remove(component)

        if (allChildren.isEmpty())
            actualHolder.addChild(emptyText)

        needsUpdate = true
    }

    override fun clearChildren() = apply {
        allChildren.clear()
        actualHolder.clearChildren()
        actualHolder.addChild(emptyText)

        needsUpdate = true
    }

    override fun alwaysDrawChildren(): Boolean {
        return true
    }

    override fun <T> childrenOfType(clazz: Class<T>): List<T> {
        return actualHolder.childrenOfType(clazz)
    }

    override fun mouseClick(mouseX: Double, mouseY: Double, button: Int) {
        actualHolder.mouseClick(mouseX, mouseY, button)
    }

    override fun hitTest(x: Float, y: Float): UIComponent {
        return actualHolder.hitTest(x, y)
    }

    fun searchAndInsert(components: List<UIComponent>, comparison: (UIComponent) -> Int) {
        if (components.isEmpty()) return

        actualHolder.children.remove(emptyText)
        val searchIndex = actualHolder.children.binarySearch(comparison = comparison)

        components.forEach { it.parent = actualHolder }
        allChildren.addAll(components)
        actualHolder.children.addAll(
            if (searchIndex >= 0) searchIndex else -(searchIndex + 1),
            components
        )

        needsUpdate = true
    }

    fun setChildren(components: List<UIComponent>) = apply {
        actualHolder.children.clear()
        actualHolder.children.addAll(components.ifEmpty { listOf(emptyText) })
        actualHolder.children.forEach { it.parent = actualHolder }

        allChildren.clear()
        allChildren.addAll(actualHolder.children)

        needsUpdate = true
    }

    private fun ClosedFloatingPointRange<Double>.width() = abs(this.start - this.endInclusive)
    private fun ClosedFloatingPointRange<Float>.width() = abs(this.start - this.endInclusive)
    private operator fun Float.div(range: ClosedFloatingPointRange<Float>): Float {
        val width = range.width()
        return if (width == 0f) 0f else this / width
    }

    class DefaultScrollBar(isHorizontal: Boolean) : UIComponent() {
        val grip: UIComponent

        init {
            if (isHorizontal) {
                constrain {
                    y = 2.pixels(alignOpposite = true)
                    width = 100.percent()
                    height = 10.pixels()
                }

                val container = UIContainer().constrain {
                    x = 2.pixels()
                    y = CenterConstraint()
                    width = RelativeConstraint() - 4.pixels()
                    height = 4.pixels()
                } childOf this

                grip = UIBlock(Color(70, 70, 70)).constrain {
                    x = 0.pixels(alignOpposite = true)
                    y = CenterConstraint()
                    width = 30.pixels()
                    height = 3.pixels()
                } childOf container
            } else {
                constrain {
                    x = 2.pixels(alignOpposite = true)
                    width = 10.pixels()
                    height = 100.percent()
                }

                val container = UIContainer().constrain {
                    x = CenterConstraint()
                    y = 2.pixels()
                    width = 4.pixels()
                    height = RelativeConstraint() - 4.pixels()
                } childOf this

                grip = UIBlock(Color(70, 70, 70)).constrain {
                    x = CenterConstraint()
                    y = 0.pixels(alignOpposite = true)
                    width = 3.pixels()
                    height = 30.pixels()
                } childOf container
            }
        }
    }

    /**
     * Constrains a scroll component to either the widest/tallest child or to the sum of their widths/heights and
     * padding. If [horizontalScrollEnabled] or [verticalScrollEnabled] are true, they will change the constraint to
     * use the total of their children's corresponding measurements and padding. Otherwise, it will use the maximum
     * measurement for the corresponding direction.
     *
     * This is the default width and height constraint for scroll components.
     *
     * @param padding Pixels of padding to add to each component
     */
    inner class ScrollChildConstraint(val padding: Float = 0f) : WidthConstraint, HeightConstraint {
        override var cachedValue = 0f
        override var recalculate = true
        override var constrainTo: UIComponent? = null

        override fun getWidthImpl(component: UIComponent): Float {
            if (horizontalScrollEnabled) {
                val holder = constrainTo ?: actualHolder
                var totalPadding = (holder.children.size - 1) * padding
                holder.children.forEach { child ->
                    if (child.constraints.y is PaddingConstraint) {
                        totalPadding += (child.constraints.y as PaddingConstraint).getVerticalPadding(child)
                    }
                }
                return (holder.children
                    .sumOf { it.getHeight().toDouble() } + totalPadding).toFloat()

            } else {
                return (constrainTo ?: actualHolder).children.maxByOrNull {
                    if(it.constraints.x is PaddingConstraint)
                        return@maxByOrNull it.getWidth() + (it.constraints.x as PaddingConstraint).getHorizontalPadding(it)
                    it.getWidth() }?.getWidth() ?: 0f
            }
        }

        override fun getHeightImpl(component: UIComponent): Float {
            if (verticalScrollEnabled) {
                val holder = constrainTo ?: actualHolder
                var totalPadding = (holder.children.size - 1) * padding
                holder.children.forEach { child ->
                    if (child.constraints.y is PaddingConstraint) {
                        totalPadding += (child.constraints.y as PaddingConstraint).getVerticalPadding(child)
                    }
                }
                return (holder.children
                    .sumOf { it.getHeight().toDouble() } + totalPadding).toFloat()
            } else {
                return (constrainTo ?: actualHolder).children.maxByOrNull {
                    if(it.constraints.y is PaddingConstraint)
                        return@maxByOrNull it.getHeight() + (it.constraints.y as PaddingConstraint).getVerticalPadding(it)
                    it.getHeight() }?.getHeight() ?: 0f
            }
        }

        override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {
            when (type) {
                ConstraintType.WIDTH -> visitor.visitChildren(ConstraintType.WIDTH)
                ConstraintType.HEIGHT -> visitor.visitChildren(ConstraintType.HEIGHT)
                else -> throw IllegalArgumentException(type.prettyName)
            }
        }

    }

    companion object {

        fun getScrollImage(): UIImage {
            return UIImage.ofResourceCached("/svg/scroll.png")
        }
    }
}
