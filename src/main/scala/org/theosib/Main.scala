package org.theosib

//import org.apache.logging.log4j.Logger
//import org.apache.logging.log4j.LogManager
import org.theosib.Adaptors.{RenderAgent, Window}
import org.theosib.Camera.{CameraController, CameraModel}
import org.theosib.GraphicsEngine.{Face, FontAtlas, GLWindow, Mesh, MeshRenderer, Shader, Texture}
import org.theosib.UIElements.Crosshair
import org.theosib.Utils.{Disposer, FileLocator, WindowDimensions}
import org.joml.Vector3d
import org.theosib.Noise.PerlinNoise
import org.theosib.Parser.ConfigParser
import org.theosib.Position.BlockPos
import org.theosib.WorkerThreads.{UpdateRenderThread, UpdateRepaintThread}
import org.theosib.WorldElements.{World, WorldView}

object Main {
//  private final val logger: Logger = LogManager.getLogger(getClass())

  var world : World = null
  var worldView : WorldView = null
  var window : Window = null
  var camera : CameraModel = null
  var cross : Crosshair = null
  var block : TestBlock = null
  var updaterThread : UpdateRepaintThread = null
  var renderThread : UpdateRenderThread = null

  def main(args: Array[String]): Unit = {
    FileLocator.setBaseDir(System.getProperty("user.dir") + "/resources")

//    val f = new FontAtlas("PixeloidSans.ttf");
//    System.exit(0);

    // Setup window
    window = new GLWindow("Voxel Game", 1024, 768)
    window.create()
    window.setFOV(45)

    // Anything with GL properties needs to be disposed in the main thread, so we
    // set up the disposer as a RenderAgent
    window.addRenderer(Disposer)


    // Create the world and the view of it
    world = new World
    worldView = new WorldView(world)

    // Setup camera
    camera = new CameraModel(window, world, worldView).setPos(0, 0, 5).setYaw(-90f).setPitch(0)
    val controller = new CameraController(camera);
    window.addInputter(controller)

    // Resolve circular reference between worldView and camera
    worldView.setCamera(camera)
    worldView.create(window)
    window.addRenderer(worldView)

    camera.getEntity.setGravity(true)
    window.addRenderer(camera)

    println("Updater thread")
    // Thread that processes block updates and repaint events
    updaterThread = new UpdateRepaintThread(world)
    updaterThread.start()

    println("Render thread")
    // Thread that computes the meshes for visible chunks
    renderThread = new UpdateRenderThread(camera, worldView)
    renderThread.start()
    window.addRenderer(renderThread)

    // Overlay the crosshair
    println("Crosshair")
    cross = new Crosshair()
    cross.create(window)
    window.addRenderer(cross)

    println("text test")
    val testText = new TestText(camera)
    testText.create(window)
    window.addRenderer(testText)
    window.renderLoop()

    println("Get block")
    world.getBlock(new BlockPos(0,0,0))

    println("Render loop")
    window.renderLoop()

    updaterThread.quit()
    renderThread.quit()

    Disposer.drainRenderDisposal()
    // Final save, etc
    window.destroy()
  }
}
