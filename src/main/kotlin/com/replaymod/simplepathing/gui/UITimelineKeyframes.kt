package com.replaymod.simplepathing.gui

import com.replaymod.core.ReplayMod
import com.replaymod.core.gui.common.UITexture
import com.replaymod.core.gui.common.timeline.UITimeline
import com.replaymod.core.gui.utils.*
import com.replaymod.core.utils.*
import com.replaymod.core.versions.MCVer
import com.replaymod.replay.ReplayModReplay
import com.replaymod.replay.gui.overlay.GuiMarkerTimeline
import com.replaymod.replaystudio.pathing.change.Change
import com.replaymod.simplepathing.SPTimeline
import de.johni0702.minecraft.gui.utils.lwjgl.Point
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector2f
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.Effect
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.universal.UGraphics
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMatrixStack
import net.minecraft.client.render.Tessellator
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.absoluteValue
import kotlin.time.Duration

class UITimelineKeyframes(
    private val state: KeyframeState,
    replayTimeline: GuiMarkerTimeline,
) : UIContainer() {
    private val rows = listOf(SPTimeline.SPPath.POSITION, SPTimeline.SPPath.TIME).map { path ->
        Row(state, path).constrain {
            y = SiblingConstraint()
            width = ChildBasedRangeConstraint()
            height = 5.pixels
        } childOf this
    }

    init {
        constrain {
            y = 1.pixels
            width = ChildBasedMaxSizeConstraint()
            height = ChildBasedSizeConstraint()
        }

        val speed = pollingState { state.gui.java.overlay.speedSliderValue }

        state.timeKeyframes.zip(state.selectionTimeKeyframes, speed).onSetValueAndNow { (keyframes, selection, speed) ->
            val row = rows[1]
            row.clearChildren()

            var prevTime = Duration.ZERO
            var prevReplayTime = Duration.ZERO
            for ((time, keyframe) in keyframes) {
                val replayTime = keyframe.replayTime
                val lineEffect = LineToReplayTimelineEffect(replayTimeline, replayTime)
                UIKeyframe(time, KeyframeType.TIME, time in selection) effect lineEffect childOf row

                val segmentSpeed = if (prevTime != time) (replayTime - prevReplayTime) / (time - prevTime) else speed

                // Draw red quads on time path segments that would require time going backwards
                if (prevReplayTime > replayTime) {
                    UISegment(prevTime, time, Color.RED) childOf row
                } else if ((segmentSpeed / speed - 1).absoluteValue > 0.01) {
                    // Draw white/green/yellow quads on paused/slower/faster path segments
                    UISegment(prevTime, time, when {
                        segmentSpeed == 0.0 -> Color.WHITE
                        segmentSpeed < speed -> Color.GREEN
                        else -> Color.YELLOW
                    }).addTooltip {
                        addLine("Replay Speed: %.2fx".format(segmentSpeed))
                    } childOf row
                }
                prevTime = time
                prevReplayTime = replayTime
            }
        }

        state.positionKeyframes.zip(state.selectionPositionKeyframes).onSetValueAndNow { (keyframes, selection) ->
            val row = rows[0]
            row.clearChildren()

            var prevTime = Duration.ZERO
            var prevType: KeyframeType? = null
            for ((time, keyframe) in keyframes) {
                val type = if (keyframe.entityId != null) KeyframeType.SPECTATOR else KeyframeType.POSITION
                UIKeyframe(time, type, time in selection) childOf row

                // Draw colored quads on spectator path segments
                if (type == KeyframeType.SPECTATOR && prevType == type) {
                    UISegment(prevTime, time, Color(0x00, 0x88, 0xff)) childOf row
                }
                prevTime = time
                prevType = type
            }
        }
    }

    class Row(state: KeyframeState, path: SPTimeline.SPPath) : UIContainer() {
        var dragging: Dragging? = null

        init {
            onRightMouse { mouseX, _ ->
                val time = parentOfType<UITimeline>()!!.getTimeAt(mouseX)
                val pathObj = state.mod.currentTimeline.getPath(path)
                for (property in pathObj.keyframes.flatMapTo(mutableSetOf()) { it.properties }) {
                    val value = pathObj.getValue(property, time.inWholeMilliseconds).orNull ?: continue
                    property.applyToGame(value, ReplayModReplay.instance.replayHandler)
                }
            }
            onRightClick {
                it.stopImmediatePropagation()
            }

            onMouseDrag { relativeMouseX, _, _ ->
                val dragging = dragging ?: return@onMouseDrag
                val spTimeline = state.mod.currentTimeline ?: return@onMouseDrag

                val diff = (getLeft() + relativeMouseX) - dragging.mouseX
                if (!dragging.passedThreshold) {
                    if (diff.absoluteValue < 5) {
                        return@onMouseDrag
                    } else {
                        dragging.passedThreshold = true
                    }
                }

                // First undo any previous changes
                dragging.change?.undo(spTimeline.timeline)

                // Compute new time
                val deltaTime = parentOfType<UITimeline>()!!.unit.get() * diff.toDouble()

                // Move keyframe to new position and store change for later undoing / pushing to history
                dragging.change = state.moveKeyframes(dragging.selection, deltaTime)

                // Refresh keyframe state (required because we do not yet commit the Change)
                state.refreshKeyframes()
            }

            onMouseRelease {
                val dragging = dragging.also { dragging = null } ?: return@onMouseRelease
                val change = dragging.change ?: return@onMouseRelease
                state.mod.currentTimeline.timeline.pushChange(change)
            }
        }

        data class Dragging(
            val selection: KeyframeState.Selection,
            val mouseX: Float,
            var passedThreshold: Boolean = false,
            /**
             * Change caused by dragging. Whenever the user moves the keyframe further, the previous change is undone
             * and a new one is created. This way when the mouse is released, only one change is in the undo history.
             */
            var change: Change? = null,
        )
    }

    inner class UIKeyframe(
        val time: Duration,
        val type: KeyframeType,
        val selected: Boolean,
    ) : UIContainer(), BoxSelectable {
        val icon by UITexture(ReplayMod.TEXTURE, type.timelineIcon.offset(if (selected) 5 else 0, 0)).constrain {
            width = 100.percent
            height = 100.percent
        } childOf this

        init {
            constrain {
                x = UITimeline.Constraint(time) - (2.5).pixels
                width = 5.pixels
                height = 5.pixels
            }

            // Will be selected on mouse release except if we ended up dragging instead
            var delayedSelect: Pair<KeyframeState.Selection, Row.Dragging>? = null
            onMouseRelease {
                val (selection, dragging) = delayedSelect.also { delayedSelect = null } ?: return@onMouseRelease
                if (!dragging.passedThreshold) {
                    state.selection.set(selection)
                }
            }

            onLeftClick(1) { event ->
                event.stopImmediatePropagation()
                val row = parentOfType<Row>()
                val selection = state.selection.get()
                when {
                    // When Shift or Ctrl (cause Shift moves the camera) is held
                    UKeyboard.isShiftKeyDown() || UKeyboard.isCtrlKeyDown() -> {
                        // flip the selection state of this keyframe
                        state.selection.set(selection.mutate { this[type.path].toggle(time) })
                        row?.dragging = null
                    }
                    // If multiple keyframes are selected and this is one of them
                    selection.size > 1 && time in selection[type.path] -> {
                        // We need delay the selection in case the user wants to drag all of them around
                        delayedSelect = Pair(
                            KeyframeState.Selection.single(type.path, time),
                            Row.Dragging(selection, event.absoluteX).also { row?.dragging = it },
                        )
                    }
                    else -> {
                        // Otherwise, select this keyframe and get ready for dragging
                        val newSelection = KeyframeState.Selection.single(type.path, time)
                        state.selection.set(newSelection)
                        row?.dragging = Row.Dragging(newSelection, event.absoluteX)
                    }
                }
            }
            onLeftClick(2) { event ->
                event.stopImmediatePropagation()
                state.gui.java.openEditKeyframePopup(type.path, time.inWholeMilliseconds)
            }
            onRightClick { event ->
                event.stopImmediatePropagation()
                val keyframe = state.mod.currentTimeline.getKeyframe(type.path, time.inWholeMilliseconds)
                    ?: return@onRightClick
                for (property in keyframe.properties) {
                    val value = keyframe.getValue(property).orNull ?: continue
                    property.applyToGame(value, ReplayModReplay.instance.replayHandler)
                }
            }
        }
    }

    class UISegment(left: Duration, right: Duration, color: Color) : UIBlock(color) {
        init {
            constrain {
                val segmentWidth = UITimeline.Constraint(right) - UITimeline.Constraint(left)
                val actualWidth = basicXConstraint { it.getWidth() }
                x = UITimeline.Constraint(left) + (segmentWidth - actualWidth) / 2
                y = CenterConstraint()
                width = (UITimeline.Constraint(right - left) - 5.pixels).coerceAtLeast(1.pixel)
                height = 3.pixel
            }
        }
    }

    class LineToReplayTimelineEffect(
        private val replayTimeline: GuiMarkerTimeline,
        private val replayTime: Duration,
    ) : Effect() {
        private val scissorEffect by lazy { ScissorEffect(Window.of(boundComponent), scissorIntersection = false) }

        override fun afterDraw(matrixStack: UMatrixStack) {
            val keyframeTimeline = boundComponent.parentOfType<UITimeline>()
                ?: throw IllegalStateException("$boundComponent needs to be the child of a timeline")
            val replayTimelineSize = replayTimeline.lastSize ?: return

            // Determine absolute positions for replay timeline
            val replayTimelinePos = Point(0, 0)
            replayTimeline.container.convertFor(replayTimeline, replayTimelinePos)
            replayTimelinePos.setLocation(-replayTimelinePos.x, -replayTimelinePos.y)

            val replayTimelineLeft = replayTimelinePos.x
            val replayTimelineRight = replayTimelinePos.x + replayTimelineSize.width
            val replayTimelineTop = replayTimelinePos.y
            val replayTimelineBottom = replayTimelinePos.y + replayTimelineSize.height
            val replayTimelineWidth =
                replayTimelineRight - replayTimelineLeft - BORDER_LEFT - BORDER_RIGHT

            val positionXReplayTimeline: Float =
                BORDER_LEFT + replayTime.inWholeMilliseconds / replayTimeline.length.toFloat() * replayTimelineWidth
            val keyframeX = boundComponent.getLeft() + boundComponent.getWidth() / 2

            val color = -0xffff01
            val tessellator = UGraphics.getFromTessellator()
            val buffer = Tessellator.getInstance().buffer
            //#if MC>=11700
            //$$ tessellator.beginWithActiveShader(UGraphics.DrawMode.LINE_STRIP, net.minecraft.client.render.VertexFormats.LINES)
            //#else
            tessellator.beginWithActiveShader(UGraphics.DrawMode.LINE_STRIP, UGraphics.CommonVertexFormats.POSITION_COLOR)
            //#endif

            // Start just below the top border of the replay timeline
            val p1 = Vector2f(replayTimelineLeft + positionXReplayTimeline, (replayTimelineTop + BORDER_TOP).toFloat())
            // Draw vertically over the replay timeline, including its bottom border
            val p2 = Vector2f(replayTimelineLeft + positionXReplayTimeline, replayTimelineBottom.toFloat())
            // Now for the important part: connecting to the keyframe timeline
            val p3 = Vector2f(keyframeX, keyframeTimeline.getTop())
            // And finally another vertical bit (the timeline is already crammed enough, so only the border)
            val p4 = Vector2f(keyframeX, keyframeTimeline.content.getTop())

            MCVer.emitLine(buffer, p1, p2, color)
            MCVer.emitLine(buffer, p2, p3, color)
            MCVer.emitLine(buffer, p3, p4, color)

            //#if MC>=11700
            //$$ com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.render.GameRenderer::getRenderTypeLinesShader)
            //#else
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            //#endif
            GL11.glLineWidth(2f)

            scissorEffect.beforeDraw(matrixStack)
            tessellator.drawDirect()
            scissorEffect.afterDraw(matrixStack)

            //#if MC<11700
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_LINE_SMOOTH)
            //#endif
        }

        companion object {
            private const val BORDER_LEFT = 4
            private const val BORDER_RIGHT = 4
            private const val BORDER_TOP = 4
        }
    }
}
