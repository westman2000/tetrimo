package com.example.basicxr

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ContentlessEntity
import androidx.xr.scenecore.Dimensions
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialCapabilities.SPATIAL_CAPABILITY_UI
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Session
import androidx.xr.scenecore.getSpatialCapabilities
import com.example.basicxr.TetrisEngine.Companion.BOARD_HEIGHT
import com.example.basicxr.TetrisEngine.Companion.BOARD_WIDTH
import com.example.basicxr.ext.loadGltfModel
import dagger.hilt.android.AndroidEntryPoint
import com.example.basicxr.ui.theme.TetrisTheme
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    //    private var userForward: Pose by mutableStateOf(Pose(Vector3(0.0f, -0.8f, -1.5f))) // for emulator
    private var userForward: Pose by mutableStateOf(Pose(Vector3(-0.5f, -0.1f, -2.0f)))// for device

    private val sceneCoreSession by lazy { Session.create(this) }

    private val viewModel : TetrisViewModel by viewModels()

    private val activeBlocks = Array(BOARD_HEIGHT) {
        Array<GltfModelEntity?>(BOARD_WIDTH) { null }
    }

    private val shadowBlocks = Array(BOARD_HEIGHT) {
        Array<GltfModelEntity?>(BOARD_WIDTH) { null }
    }

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("TAG", "onCreate: ")

        if (sceneCoreSession.getSpatialCapabilities().hasCapability(SPATIAL_CAPABILITY_UI)) {
            sceneCoreSession.mainPanelEntity.setHidden(true)
            createModels()
            createHeadLockedPanelUi()
            game()
        } else
            setContent {
                TetrisTheme {
                    val viewModel: TetrisViewModel by viewModels()
                    TetrisApp(viewModel = viewModel)
                }
            }
    }

    private fun createModels() {
        lifecycleScope.launch {

            // calculate execution time
            val startTime = System.currentTimeMillis()

            val grid = ContentlessEntity.create(sceneCoreSession, "grid")

            val model = sceneCoreSession.loadGltfModel("box.glb") ?: throw RuntimeException("SceneCoreSession loadGltfModel returned null")
            val modelActive = sceneCoreSession.loadGltfModel("blank_block.glb") ?: throw RuntimeException("SceneCoreSession loadGltfModel returned null")
            val modelShadow = sceneCoreSession.loadGltfModel("crate.glb") ?: throw RuntimeException("SceneCoreSession loadGltfModel returned null")

            for (y in 0..19) {
                for (x in 0..9) {
                    GltfModelEntity.create(sceneCoreSession, checkNotNull(model) as GltfModel).apply {
                        setParent(grid)
                        setPose(Pose(Vector3(0f + (x * 0.5f), 7f - (y * 0.5f), -10.0f), Quaternion.Identity))
                    }
                }
            }

            for (y in 0..19) {
                for (x in 0..9) {
                    activeBlocks[y][x] = GltfModelEntity.create(sceneCoreSession, checkNotNull(modelActive) as GltfModel).apply {
                        setParent(grid)
                        setHidden(true)
                        setScale(0.25f)
                        setPose(Pose(Vector3(0f + (x * 0.5f), 7f - (y * 0.5f), -10f), Quaternion.Identity))
                    }
                    shadowBlocks[y][x] = GltfModelEntity.create(sceneCoreSession, checkNotNull(modelShadow) as GltfModel).apply {
                        setParent(grid)
                        setHidden(true)
                        setScale(0.25f)
                        setPose(Pose(Vector3(0f + (x * 0.5f), 7f - (y * 0.5f), -10f), Quaternion.Identity))
                    }
                }
            }

            val endTime = System.currentTimeMillis()

            val executionTime = endTime - startTime
            println("Execution time: $executionTime ms")
        }
    }

    private fun game() {
        lifecycleScope.launch {
            viewModel.board.collect { b ->
                // Draw board grid
                for (y in viewModel.board.value.indices) {
                    for (x in 0 until BOARD_WIDTH) {
                        val cell = viewModel.board.value[y][x]
                        if (cell.filled) {

                            if (cell.color.alpha < 0.5f)
                                shadowBlocks[y][x]?.setHidden(false)
                            else
                                activeBlocks[y][x]?.setHidden(false)

                        } else {
                            activeBlocks[y][x]?.setHidden(true)
                            shadowBlocks[y][x]?.setHidden(true)
                        }
                    }
                }
            }
        }
    }

    private fun createPanelView(
        activity: Activity,
        contentScreen: @Composable () -> Unit
    ) : View {
        return ComposeView(activity).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                contentScreen()
            }
            setViewTreeLifecycleOwner(activity as LifecycleOwner)
            setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
        }
    }

    private fun createPanelUi(
        session: Session,
        view: View,
        surfaceDimensionsPx : Dimensions,
        dimensions : Dimensions,
        panelName : String,
        pose: Pose
    ) : PanelEntity {
        return PanelEntity.create(
            session = session,
            view = view,
            surfaceDimensionsPx = surfaceDimensionsPx,// ,
            dimensions = dimensions,
            name = panelName,
            pose = pose
        ).apply {
            setParent(session.activitySpace)
        }
    }


    private fun createHeadLockedPanelUi() {
        val headLockedPanelView = createPanelView(this) {
            GameControls(viewModel)
        }
        val headLockedPanel = createPanelUi(
            session = sceneCoreSession,
            view = headLockedPanelView,
            surfaceDimensionsPx = Dimensions(600f, 600f),
            dimensions = Dimensions(10f, 10f),
            panelName = "headLockedPanel",
            pose = userForward
        )
        headLockedPanelView.postOnAnimation {
            updateHeadLockedPose(headLockedPanelView, headLockedPanel)
        }
    }

    private fun updateHeadLockedPose(view: View, panelEntity: PanelEntity) {
        sceneCoreSession.spatialUser.head?.let { projectionSource ->
            projectionSource.transformPoseTo(userForward, sceneCoreSession.activitySpace).let {
                panelEntity.setPose(it)
            }
        }
        view.postOnAnimation { updateHeadLockedPose(view, panelEntity) }
    }
}
