# Installation [installing]
## Installing the Replay Mod [replaymod]
The **ReplayMod** requires **Minecraft Forge** to be installed.

For the use with the **Replay Mod**, we recommend using Forge Version `1.8-11.14.3.1450`, which can be downloaded [here](http://files.minecraftforge.net/maven/net/minecraftforge/forge/index_1.8.html)
Other Forge Versions might, but don't necessarily work.

If you don't know how to install Forge, follow [this tutorial](https://www.youtube.com/watch?v=4i7-RystzC4).

After installing Forge, simply put the downloaded `ReplayMod.jar` file in the `/mods` folder of your Minecraft directory.

> **Note:** The **Replay Mod** is entirely client side and can not be installed on the servers you play on.

## Installing FFmpeg [ffmpeg]
To use the **Replay Mod**'s [Rendering Feature](#replaying-render), you need to have FFmpeg installed.

### Windows [windows]
Download the latest **FFmpeg Static Build** for your architecture from <http://ffmpeg.zeranoe.com/builds/>.  
Then, extract the downloaded `.7z` file in the folder you want to install FFmpeg in.

Next, download this **Batch Script** to quickly install FFmpeg: <https://replaymod.com/files/ffmpeg-path-installer.bat>

> **Note:** If your browser warns you about the download, dismiss the message. It's a simple Batch Script which won't damage your computer.

Put the downloaded script into the folder where you extracted FFmpeg and run itt by double-clicking it.
If you see a success message in the console, you've sucessfully installed **FFmpeg**!

### Mac OSX [mac]
On OSX, you can install **FFmpeg** with **[Homebrew](http://brew.sh/)** using `brew install ffmpeg`.

Alternatively, you can download the latest static build from <https://ffmpeg.org/> and copy the ffmpeg executable to `/usr/local/bin`:

1. Use <code>CMD &#8984;</code>+`Space` to open Spotlight.
2. Search for `Terminal` and open it.
3. Type in `open /usr/local/bin` and press `Enter`.

### Linux [linux]
On Linux, you can install **FFmpeg** using your system's package manager, for example using `apt install ffmpeg`.
If in doubt, consult the documentation of your distribution.

## Compatibility with other Mods [compatibility]
### General information [general]
In General, the Replay Mod _should_ be compatible with most Forge Mods.
However, due to the way that recording works, many Mods that add new Blocks, Mobs or Game Mechanics are not supported.  
Mods that simply modify the client (like the [PixelCam Mod](http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2327429-pixelcam-camera-studio-for-minecraft-1-8)
or the [CommandGroups Mod](http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2443430-command-groups-mod-order-label-and-color-command)) usually work.

### Optifine [optifine]
The **Replay Mod** is compatible with `Optifine 1.8.0 HD U D5`, which can be downloaded at <https://optifine.net/downloads>.  
Other Optifine versions for Minecraft 1.8.0 might, but don't have to be compatible as well.

> **Warning:** If you have Optifine installed, you **can not render videos** from within the Replay Mod.  
This might change in future Replay Mod versions.

### Shaders Mod [shaders]
You can use the Replay Mod together with [karyonix' Shaders Mod](http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/1286604-shaders-mod-updated-by-karyonix),
but rendering videos results in **possible crashes** and **malformed videos**.
Therefore, you have to **uninstall the Shaders Mod before rendering**.

## Troubleshooting [troubleshooting]
If you need help installing the **Replay Mod** or **FFmpeg**, please read [this forum thread](https://www.replaymod.com/forum/thread/220) 
by [bela333](https://www.replaymod.com/user/bela333) - it covers most of the problems that users encountered so far.

# Recording [recording]
![](img/recording-indicator.jpg)
The **Recording Indicator** which is displayed while recording

Recording Replays is the core feature of the **Replay Mod**.

Recording a Replay is really simple: If enabled in the **Replay Settings**, the Mod will automatically record all of your gaming sessions.  
You will be notified that the Recording started by a **Chat Message**. Chat Messages can be disabled in the **Replay Settings**.  
In the upper left corner, a **Recording Indicator** will be displayed unless disabled in the **Replay Settings** while you are recording.

While Recording, you can set [Event Markers](#replaying-markers) using the `M` key.

Upon exiting your Server/Singleplayer World, you might be asked to wait a couple of seconds until saving the Replay has been finished.  
Congratulations, your Replay is now accessible via the **Replay Viewer**!

# Replaying [replaying]
![](img/replay-viewer.jpg)
The **Replay Viewer** with several recorded Replays

To watch one of your Replays, open up the **Replay Viewer** from the **Main Menu** and select the desired Replay.
Then, press the "Load" button.

In the **Replay Viewer**, you can also **rename**, **delete** and **upload** your **Replay Files**.

## Movement [movement]
### Basic Controls [basic]
While in the Replay, you can freely move around using the movement keys (`WASD, Space, Shift` by default).
You can move through block similar to Spectator Mode.

Using the **Mouse Wheel**, you can modify your flying speed. Scrolling **up** causes you to fly faster, while scrolling **down** slows you down.

### Camera Roll [roll]
![](img/camera-tilted.jpg)
A Screenshot taken with a **tilted camera**

While in a Replay, you can tilt the Camera.

By default, pressing `J` will tilt the screen clockwise, and pressing `L` will tilt the screen counterclockwise.  
Using `K`, you can reset the Camera Roll.

When holding the `Ctrl` key (`Cmd` key on Mac) while tilting, you can tilt slower and more precisely.

> **Hint:** Most of the Keybindings mentioned in this documentation can be changed in Minecraft's **Control Menu**.

### Spectating Entities [spectating]
You can spectate any living entitiy (including players) while in a Replay. When looking at an entity, a crosshair will be visible.

By right-clicking, you will start spectating the entity. If you want to stop spectating the entity, use your Sneak Key (`Shift`).

## Controlling Time [time]
### Replay Speed [speed]
![](img/real-timeline.jpg)
The **Play/Pause button**, **Speed Slider** and **Replay Timeline** hightlighted

In the upper left corner of you screen, there is a **Play/Pause button** (Shortcut: `P`), using which you can entirely pause the time of the Replay.  
All Entities, Particles and Block Updates will be frozen until you resume. You can fly through a paused Replay as usual.

Next to the Play/Pause Button, there is a **Speed Slider**. Using the **Speed Slider**, you can control how fast the time in your Replay passes.  
The minimum Speed value is **0.1 times** the normal Minecraft Tick Speed, and the maximum Speed is **8 times** as fast.

### Jumping in Time [travel]
To the right of the Speed Slider, there is the **Replay Timeline**.
On the Replay Timeline, you see a yellow cursor indicating your current position in the Replay.  
By clicking somewhere on this Timeline, you will travel in time towards the specified point in time.  
Please note that it takes longer to do larger steps in time or to jump backwards in time.

## Camera Paths [paths]
### Introduction [intro]
While in a Replay, you can create controlled **Camera Movements** using the Mods's **Keyframe System**.  
Those Camera Paths can be rendered to a video later (see [Rendering](#replaying-render)).

Camera Paths are inspired by the [PixelCam Mod](http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2327429-pixelcam-camera-studio-for-minecraft-1-8) which is based on the Camera Studio Mod.

The **Replay Mod Keyframe System** is similar to many Video Editing Software's Keyframe Systems.
If you're new to Keyframe Systems, try reading [this Wikipedia article](https://en.wikipedia.org/wiki/Key_frame).

### Keyframe Timeline [timeline]
![](img/keyframe-timeline.jpg)
The **Keyframe Timeline** highlighted

All of the Keyframes which define a Camera Path are placed on the **Keyframe Timeline**.
The Keyframe Timeline is below the [Replay Timeline](#replaying-time-travel).

The **Keyframe Timeline** is 10 Minutes long, meaning a Camera Path can take up to 10 Minutes.
You can zoom in and out on the Keyframe Timeline using the `+` and `-` buttons next to it.

Other than the **Replay Timeline**, the **Keyframe Timeline**'s time values are not relative to the Replay's point of time, but refer to **real time**.  
If two Keyframes are 5 seconds apart, the Camera Path will take 5 seconds to interpolate between those Keyframes.

You can drag Keyframes on the **Keyframe Timeline** by left-clicking them, holding the mouse button and moving your mouse.

### Position Keyframes [place]
The basic components of a Camera Path are **Position Keyframes**.
A Position Keyframe stores a **Camera Position** (x, y, z, yaw, pitch, roll).

You can set a **Position Keyframe** using the "Add Position Keyframe" ![Green Rhomb](img/position-keyframe-button.png) button.  
A **Position Keyframe** initially contains the state the Camera was in when creating the Keyframe.

On the **Keyframe Timeline**, a **Position Keyframe** will appear at the cursors's current position.  
This Keyframe is selected automatically, and the **Position Keyframe Button** will turn red.
By clicking the **Position Keyframe Button** again, the selected **Position Keyframe** will be removed.

By adding multiple **Position Keyframes** on the **Keyframe Timeline**, you can create a simple **Camera Path**.  
Using the [Path Preview](#replaying-paths-place-preview), you can preview your **Camera Path**.

> **Hint:** By right-clicking a **Position Keyframe** on the **Keyframe Timeline**, you can jump to this Keyframe's Position.

#### Path Interpolation [interpolation]
![](img/cubic-path.gif)
A **Camera Path** using **Cubic Interpolation**

![](img/linear-path.gif)
The same **Camera Path** using **Linear Interpolation**

By default, Camera Paths follow a smooth curve through all of the set **Position Keyframes**.
This curve is generated using [Cubic Spline Interpolation](https://en.wikipedia.org/wiki/Spline_interpolation).  
If you wish to disable smooth interpolation and want to make the Path follow straight lines instead, change the **Path Interpolation Setting** in the **Replay Settings**.

You can easily toggle between **Linear** and **Cubic Interpolation** using the `O` key.

#### Path Preview [preview]
![](img/path-preview-icon.jpg)
While the **Path Preview** is enabled, this camera symbol is displayed in the lower right corner of the screen.

![](img/path-preview.jpg)
A normal **Path Preview**

Using the `H` key, you can toggle a **visual representation** of your Camera Path.
If enabled, you will see a **Red Line** in the World following the Camrea Path.

The [Path Interpolation Setting](#replaying-paths-place-interpolation) is respected in this preview.

By moving the cursor on the **Keyframe Timeline**, you can see the Camera's current positon and view direction on the **Path Preview**.

### Time Keyframes [time]
**Time Keyframes** can be used to precisely control time travelling during a **Camera Path**.

> Every Time Keyframe represents a timestamp in the Replay itself.

You can set a **Time Keyframe** using the "Add Time Keyframe" ![Green Hourglass](img/time-keyframe-button.png) button.  
A **Time Keyframe** initially contains the Replay's point in time when creating the Keyframe.

When playing the **Camera Path**, the Replay's time will interpolate between the set **Time Keyframes** in the Keyframes' intervals. Two **Time Keyframes** with the same Timestamp will freeze the Replay time for the duration of their interval.

> **Hint:** By right-clicking a **Time Keyframe** on the **Keyframe Timeline**, you can jump to this Keyframe's Timestamp Value.

> **Note:** Travelling backwards in time is not supported during **Camera Paths**. Do not add a **Time Keyframe** containing an earlier point in time after another **Time Keyframe**.

To play your first **Camera Path**, click the **Play Button** next to the **Keyframe Timeline**.  
By default, the **Camera Path** starts from the Cursor Position on the Keyframe Timeline.
By clicking while holding the `Ctrl` key (`Cmd` key on Mac), you can however start from the beginning regardless of the Cursor Position.

#### Synchronizing Timelines [sync]
Using the `V` key, you can **synchronize** the **Keyframe Timeline** with the time that passed since the last **Time Keyframe**'s timestamp.

This means that pressing `V` moves the **Keyframe Timeline**'s cursor to the position where placing a **Time Keyframe** would result in a **Replay Speed** of the **Speed Slider's current value** between the newly placed and the last **Time Keyframe**.

If holding `Shift` while synchronizing, it will be synchronized as if the **Speed Slider's value** was **1.0**.

### Spectator Keyframes [spectator]
![](img/path-preview-spectator.jpg)
A **Path Preview** with **Spectator Keyframes**

While [spectating an entity](#replaying-movement-spectating), the "Add Position Keyframe" button becomes an "Add Spectator Keyframe" ![Blue Rhomb](img/spectator-keyframe-button.png) button.

To sucessfully spectate an entity, you always need at least two **Spectator Keyframes** created while spectating the same entity.  
On the **Keyframe Timeline**, the periods during which you are spectating an entity are marked with a blue line.

To leave a spectated entity during a camera path, simply add a normal **Position Keyframe**.

### Editing Keyframes [edit]
![](img/keyframe-editor.jpg)
The **Keyframe Editor** for a **Position Keyframe**

You can edit any Keyframe by **double-clicking** it on the Keyframe Timeline.
A new GUI Screen will open which allows you to modify the Keyframe's properties.

On the lower part of the screen, you can precisely set the Keyframe's position on the **Keyframe Timeline**.  
Above that, you can modify Keyframe specific settings, for example its Position value.

### Removing Keyframes [remove]
Using the `DELETE` key, you can remove the currently selected Keyframe.  
Alternatively, while a Keyframe is selected, its connected Button turns red, and clicking it removes the Keyframe as well.

> **Note:** If you use an Apple Computer with a built-in keyboard, you  may need to press `FN + DELETE` instead.

### Keyframe Repository [repository]
![](img/keyframe-repository.jpg)
The **Keyframe Repository** with two **Path Presets**

[YouTube](IYrzbbIzWPw)

Using the `X` key, you can open the **Keyframe Repository**. Every Replay File has a separate Keyframe Repository which can contain several **Keyframe Presets**.

To save the Keyframes currently on your Timeline, click the **"Save current Path"** button.
You can change the Preset's name using the Text Input Field in the upper right corner.  
To load a Keyframe Preset, select a Preset from the list on the left side and click the **"Load"** button.  
Using the **"Remove"** button, you can permanently delete a Keyframe Preset from the **Keyframe Repository**.

## Custom Objects [objects]

[YouTube](_Xdpg828fbE)

### Introduction [introduction]
**Custom Objects** are mainly meant for **video creators**. They allow you to add any image into the Replay World and animate its Position, Opacity and much more.

Instead of using [Motion Tracking](https://en.wikipedia.org/wiki/Match_moving) to add text or pictures to a video, you can directly add these to the rendered file.

### Adding Assets [assets]
![](img/asset-manager.jpg)
The **Asset Manager** with an example Asset

In order to create **Custom Objects**, you first have to add **Assets** to your Replay File.  
Therefore, open the **Asset Manager** using the `G` key.

In the **Asset Manager**, you can add **Image Files (.png, .jpg and more)**, so-called **Assets**, to the Replay.  
When clicking the **"Add"** button, a File Chooser will show up. Select the image file you want to use.  
You can give the Asset a custom name using the Text Input Field in the top right corner.

All of the Assets you added can be used by **Custom Objects**.

### Creating Custom Objects [creating]
![](img/object-manager.jpg)
The **Object Manager** with some **Animation Keyframes**

Using the `F` key, you can open the **Object Manager**. Initially there won't be any Objects in the **Object List**, but you can simply add an Object using the **"Add"** button.

After doing so, you can name the newly created **Custom Object** using the Text Field in the upper right corner.  
To define which **Asset File** (i.e. image) the **Custom Object** should use, select the desired **Asset** from the dropdown beneath the Name Input.

If you leave the **Object Manager**, you should see the image in the World somwhere near your position.

### Animating Custom Objects [animating]
![](img/custom-object-animated.gif)
An animated **Custom Image Object**.

Of course, you don't want the image to stay at that position.
 herefore, open the **Object Manager** again and select the **Custom Object** you want to modify.  
On the lower half of the screen you will see mutliple **Input Fields**, using which you can modify various settings
(so-called **Transformations**), for example the Object's **Position**, **Scale**, **Orientation** and **Opacity**.

When editing these values, you'll notice that on the TImeline to the right, Keyframes appear.
You can set and remove Keyframes for specific settings using its "Add Keyframe" button on the very left of the screen.

The **Object Manager's Keyframe System** is very similar to the **Position Keyframe** and **Time Keyframe** System.

During a **Camera Path**, the **Custom Objects** will interpolate their **Transformation Values** like **Position Keyframes** are interpolated.  
While outside the **Object Manager** you can preview the Objects' position by moving the cursor on the **Keyframe Timeline** - the Objects will interpolate to the position they'll be at that timestamp during the **Camera Path**.

Once you got the hang of it, you'll be able to quickly animate **Custom Objects**.

## Rendering [render]
With the **Replay Mod**, you can render **Camera Paths** to videos **without** using a screen recording tool like Fraps.  
To get started, first [set up your Camera Path](#replaying-paths) as described in the previous chapters.
Then, press the **"Render Camera Path"** button in the upper left corner.

### Rendering Method [method]
The **Replay Mod** does not only allow you to render "normal" vidoes as you see them on YouTube everyday,
you can also render **360 degree videos**, **3D videos** and more.  
To change the rendering method, use the dropdown on the upper half of the **Render Settings Screen**.

#### Default Rendering [default]
![](img/render-default.jpg)
A frame exported using **Default Rendering**

Renders the video in the specified resolution. Fastest Rendering Option.

#### Stereoscopic Rendering [stereoscopic]
![](img/render-stereoscopic.jpg)
A frame exported using **Stereoscopic Rendering**

Renders the video as a [stereoscopic](https://en.wikipedia.org/wiki/Stereoscopy) (side-by-side) 3D move, usable by different 3D technologies. The image for one eye is half the width of the video.

#### Cubic Rendering [cubic]
![](img/render-cubic.jpg)
A frame exported using **Cubic Rendering**

Renders the video with a **360 degree panoramic view**, using [Cubic Projection](http://wiki.panotools.org/Cubic_Projection).
This is usable by several 360 degree video players (and the **Oculus Rift**), for example [VR Player](https://vrplayer.codeplex.com/).  
While **Cubic Videos** can't be used for **YouTube 360° Videos**, it takes less time to render them, so if your player 
is compatible with **Cubic Projection** anyway, you can use this setting.

#### Equirectangular Rendering [equirectangular]
![](img/render-equirectangular.jpg)
A frame exported using **Equirectangular Rendering**

Renders the video with a 360 degree panoramic view, using [Equirectangular Projection](http://wiki.panotools.org/Equirectangular_Projection).
This is usable by [YouTube's new 360 degree video function](https://support.google.com/youtube/answer/6178631?hl=en), and several video players (and the **Oculus Rift**), for example [VR Player](https://vrplayer.codeplex.com/).

> **Note:** In order for **YouTube** to recognize 360 Degree Videos, you have to inject some Metadata. Follow [this guide](https://support.google.com/youtube/answer/6178631?hl=en) for a tutorial.

### Video Settings [settings]
![](img/rendersettings-video.jpg)
The **Video Render Settings** Screen

After you chose the **Rendering Mode** you want to use, you can further customize the rendered video.

#### Video Resolution [resolution]
Below the **Render Method** Dropdown, you can enter the desired **Video Resolution** (Width * Height).
You can enter any resolution there, but make sure to use reasonable values.

Common Video Resolutions for **Default Rendering** are:

- 720p - 1280 * 720
- 1080p - 1920 * 1080
- 4k - 3840 * 2160

---

When using **Stereoscopic Rendering**, please note that the entered **Width** value is the final video's width.
Because **Stereoscopic Videos** have two frames next to each other, you therefore need to **double** the video width,
so if you want a **Full HD Stereoscopic Video**, you have to enter **3840 * 1080**.

---

Videos in **Equirectangular Projection** always need to have an **Aspect Ratio** of **2:1**.

Please note that you should generally use a high resolution when exporting **Equirectangular Videos**, as the Video will be mapped onto a spherical surface in the Viewer you're using. To get a decent looking video, we recommend a resolution of **4320 * 2160**.

> **Note:** If you're re-rendering Equirectangular footage (e.g. using a video editing software like Premiere), make sure to export it at a **2:1** aspect ratio - otherwise it will look strange in 360 degrees.

#### Video Framerate [framerate]

#### Video Encoding and Quality [quality]
![](img/quality-comparison.jpg)
A comparison between the quality of the **MP4 Hight, Default and Potato** Quality Presets.

Below the **Render Method Dropdown**, there is another Dropdown Menu for **Encoding Presets**.

> **Note:** If you simply want to render an MP4 video in good quality, don't change these settings.

There are **7 Encoding Presets** you can choose from:

- **MP4 - Default Quality**  
  Renders an **MP4 video** encoded with the **x264 codec** in **Default Quality**.
  This is default Encoding Preset, as it exports a video with good quality and a relativly low file size.
- **MP4 - High Quality**  
  Renders an **MP4 video** encoded with the **x264 codec** in **High Quality**.
  Rendered videos have a relatively high file size, but their quality is almost lossless. This is useful for video producers who have to encode (render) their footage several times.
- **MP4 - Potato Quality**  
  Renders an **MP4 video** encoded with the **x264 codec** in **Very Low Quality**.
  While the video itself looks really bad, the file size is tiny and therefore this preset can be used for test renders.
- **MP4 - Custom Bitrate**  
  If this Preset is chosen, the **Bitrate Input Field** gets enabled and you can define the video's bitrate yourself.
  The rendered video is an **MP4 video** encoded with the **x264 codec**.
- **WEBM - Custom Bitrate**  
  If this Preset is chosen, the **Bitrate Input Field** gets enabled and you can define the video's bitrate yourself.
  The rendered video is a **webm video** encoded with the **vp8 codec**.  
  **webm video files** are useful if you want to upload footage directly to streaming services like YouTube, 
  as they usually don't have to convert **vp8-encoded videos** when processing them.
- **MKV - Lossless**  
  Renders an **MKV video** encoded with the **x264 codec** in **Lossless Quality**.
  While these video files are of perfect quality, most **non-FFmpeg-based video players and video editing software** (e.g. QuickTime Player, Sony Vegas and Adobe Premiere) can't play these videos. Therefore, you should instead use the **MP4 - High Quality** preset in most cases.
- **PNG Sequence**  
  Exports the sequence as individual frames in the **PNG Format**.  
  **Warning:** This can create a huge amount of files, so make sure to save them in a separate folder.

### Advanced Settings [advanced]
![](img/rendersettings-advanced.jpg)
The **Advanced Render Settings** Screen

By clicking the **"Advanced Settings" Button** in the lower right corner, you can access more settings.

#### Hide Nametags [nametags]
If you don't want to see Entity's Nametags in the rendered video, simply tick the checkbox. Hidden Nametags also include Nametags of **Armor Stands** and **Mobs**, **not only Players**!

#### Stabilize Camera [stabilize]
This setting is only applicable for **Cubic Rendering** and **Equirectangular Rendering**.  
Because you can rotate the camera in **360 degree viewers**, a tilted camera persepctive leads to a strange user experience:
If the user looks around, his view might appear to be tilted because the camera looked up or down.
Therefore, it is recommended to **always stabilize the camera's Pitch and Roll** using the repsective checkboxes.

[Example 360° video **without stabilized Camera Pitch**](https://www.youtube.com/watch?v=lHGSG-0CVZw)  
[Example 360° video **with stabilized Camera Pitch and Roll**](https://www.youtube.com/watch?v=mtBehojUotQ)

#### Chroma Keying [chroma]
![](img/chromasky.jpg)
A screenshot of a video rendered with a **Red Chroma Key Setting**

This is an experimental setting for video producers which allows you to change the sky color to a fixed color.
Using a **Video Editing Software** like **Adobe After Effects** or **Sony Vegas**, you can change the sky color to transparency and add your own footage as sky. Read more about **Chroma Keying** [here](https://en.wikipedia.org/wiki/Chroma_key).

> **Note:** For best results, you should **disable clouds** before rendering, as they are transparent.

### Command Line Settings [commandline]
![](img/rendersettings-commandline.jpg)
The **Command Line Render Settings** Screen


> **Note**: This is for advanced users only. If you do not know what your're doing, leave these settings as they are.

The **Replay Mod** runs [FFmpeg](http://ffmpeg.org/) via the **Command Line** to encode videos.

You can customize both the **executed Command** and the **Command Line Arguments** in the **Command Line Settings** Tag of the **Render Settings Screen**.

#### Custom Command [command]
If you leave the left input field blank, `ffmpeg` will be used as **command**. If you haven't set your **PATH variable** to link to your FFmpeg distribution, simply enter the full path to your FFmpeg executable (e.g. `C:/ffmpeg/ffmpeg.exe` or `/usr/local/bin/ffmpeg`).

#### Command Line Arguments [arguments]
In the right input field, you can input custom **Command Line Arguments** to be used in the console.

Each of the **Encoding Presets** basically represents a set of **Command Line Arugments** which are used with the **ffmpeg Command**.
Customizing these **Command Line Arguments** allows you to use a specific codec or quality setting and much more.  
If you need an introduction to **ffmpeg Command Line Arguments**, read the [FFmpeg documentation](http://ffmpeg.org/ffmpeg.html).

Before passing the given **Command Line Arguments** to the **ffmpeg Command**, the **Replay Mod** replaces the following placeholders with your inputs in the **Render Settings Screen**:
- **%WIDTH%**  
  The video's width
- **%HEIGHT%**  
  The video's height
- **%FPS%**  
  The video's framerate
- **%FILENAME%**  
  The video's filename **without the extension**. This should be surrounded with quotation marks `"`, as the filename might contain spaces.  
  As the **ffmpeg Command** is always executed from the target file's parent directory, %FILENAME% does **not** contain the file's full path.
- **%BITRATE%**  
  The video's bitrate in **kilobytes per second**. As the %BITRATE% placeholder already contains the **value identifier "K"**,
  you don't have to take care of the bitrate value's format.

### Rendering Performance [performance]
![](img/rendering-screen.jpg)
The Screen which is displayed while a video is being rendered

By clicking the **Render Button** in the lower right corner, you start **Default Performance Rendering**. Usually, this is perfectly satisfying regarding rendering speed, but you can increase it slightly using **High Performance Rendering**.

To use **High Performance Rendering**, hold down the `Ctrl` key  (`Cmd` key on Mac) while clicking the **Render Button**.  
**High Performance Rendering** has the following changes to improve rendering speed:
 - Rendering Process, Estimated Time Left and Button Clicks are not recalculated after every rendered frame,
   but only after one rendered seconds (e.g. every 60 frames when rendering with 60fps)
 - Resizing the Minecraft Window does not update the Rendering Gui

### Troubleshooting [troubleshooting]

> If you have trouble with rendering, please first consult the Documentation before asking for help in the Forums.

#### No FFmpeg installation found [ffmpeg]
![](img/ffmpeg-missing.jpg)
The error screen that is displayed when no FFmpeg installation could be found

If the **Replay Mod** tells you to install FFmpeg even though you already have, you have to manually tell the **Replay Mod** where your FFmpeg executable is located.

First, get the full path to your `ffmpeg.exe` (on Windows) or `ffmpeg` executable (on Mac/Linux).  
On Windows, this path  might look like `C:/ffmpeg/ffmpeg.exe`.

Then, open the **"Command Line Settings" Tab** in the Render Settings and paste this path into the **left input field** and retry rendering.

#### Crash while rendering [crash]
If Minecraft crashes after a few frames of rendering, it most likely means the **FFmpeg** didn't like the **Command Line Arguments** you passed.
If you customized the Command Line Arguments manually, re-check them - it's probably your own fault.

> **Hint:** In your .minecraft folder, you'll find a file called `export.log` which contains information about FFmpeg's rendering process.

If you did **not** customize the **Command Line Arguments**, you might have entered some insanely high (or low) values e.g. for Bitrate or Video Resolution. Try again with other, more reasonable values.

## Ambient Lighting [lighting]
![](img/ambient-lighting-comparison.jpg)
The same setting, onace with **Ambient Lighting** enabled, once with **Ambient Lighting** disabled

![](img/eyecon.jpg)
While **Ambient Lighting** is enabled, this eye symbol is displayed in the lower right corner of the screen.

If you have a Replay in a dark setting (for example at nighttime, or in a cave) and **Brightness: Bright** just isn't bright enough for you, you can toogle **Ambient Lighting** using the `Z` key (`Y` on some keyboards).

This works as a replacement for the **Night Vision Potion Effect**, without the side effect of a weird sky color.

## Player Overview [overview]
![](img/player-overview.jpg)
The **Player Overview** Screen

Using the `B` key, you can open the **Player Overview**. In the **Player Overview**, you see a list of all currently loaded **Players** in the Replay.

By clicking on a player's **Name or Head**, you can **spectate** this player.

Next to each player, there's a **checkbox** using which you can toggle the player's **visibility**.
This way, you can hide certain players from the Replay.  
Above the individual checkboxes, there are two checkbox buttons - one to **hide all players** and one to **show all players**.

If you want to **save the player visibility settings**, check **"Remember Hidden Players" checkbox**.
If checked, invisible players will stay invisible after closing the Replay (this can of course be reverted),
otherwise all of the players are visible again after reloading the Replay.

## Thumbnails [thumbnail]
![](img/default-thumb.jpg)
The **Default Thumbnail** which is used if no Thumbnail was created

While in a Replay, you can use the `N` key to create a **Thumbnail** of the current Replay.  
A **Thumbnail** is a Screenshot which should give the viewer a good impression of your Replay's content.

**Thumbnails** are important when uploading a Replay to the **Replay Center**,
as other users are much more likely to download your Replay if they can see a preview of it.  
They also help keeping your **Replay Viewer** clear and structured.

If no **Thumbnail** is set for a Replay, the **Default Thumbnail** will be displayed in the **Replay Viewer** and in the **Replay Center**.

## Event Markers [markers]
![](img/marker-timeline.jpg)
The **Replay Timeline** with several **Event Markers**

Long Replays easily get really clunky and difficult to handle.
Therefore, **Event Markers** are a simple way to mark important events in your Replay.

While [recording a Replay](#recording), you can press the `M` key anytime to set an **Event Marker**.
This **Event Marker** remembers the **Position** where you've added it and will later,
while watching a Replay, be displayed on the **Replay Timeline**.

While viewing a Replay, you can jump to an **Event Marker**'s Position and Timestamp by right-clicking it on the **Replay Timeline**.

You can also add new **Event Markers** while viewing a Replay using the `M` key as well.

You can name **Event Markers** by double-clicking them. 
When hovering over the **Event Marker**, its name is displayed.
This way, it's even simpler to add structure to your Replays.

You can **delete an Event Marker** by clicking it once to select it and then pressing the `DELETE` key.

# Replay Center [center]
The **Replay Center** is where you can **share** your **Minecraft Moments** with others and discover awesome **Replays by other Users**.  
To be able to use the **Replay Center**, you need an **account on ReplayMod.com**.

## Authentication [auth]
![](img/auth-login.jpg)
The **Login Screen** which is displayed upon startup

![](img/auth-register.jpg)
The **Register Screen**

When starting Minecraft with the **Replay Mod** installed, you will be asked to login to **ReplayMod.com**.  
If you don't want to use the **Replay Center**, you can click the **"Skip" Button** to continue without logging in.

If you want to browse other users' **Replays** and **share your own Replays** however, you need to **Register an account**.

> **Note:** By registering an Account on **ReplayMod.com**, you agree to the Website's [Terms of Service](https://www.replaymod.com/legal/terms)

Please note that you can only create **one ReplayMod.com Account per Minecraft Account**, so choose your username wisely.

Once you've registered, you're automatically going to be logged in. The Mod **remembers your login** until you manually log out from the **Replay Center**, so it won't ask you upon every startup if you logged in once.

## Replay Files [files]
![](img/replay-center.jpg)
The **Replay Center** in the Mod

Once you've logged in, you can use the **"Replay Center" Button** in the Main Menu to access the **Replay Center**.

In the upper half of the screen, there are **5 Buttons** for **5 Tabs** of the **Replay Center**:
- **Recent**  
  Shows the most recently uploaded Replays in the **Replay Center**
- **Best**  
  Shows the Replays with the **best rating and most downloads**
- **Downloaded**  
  Shows all of the Replays you've downloaded locally
- **Favorited**  
  Shows all of the Replays you favorited
- **Search**  
  Allows you to search for specific Replays

When you've found an insteresting looking Replay, you can download it using the **"Download" Button** in the lower left corner.
After downloading, you'll automatically join the Replay.

After you've downloaded a Replay, you can rate and favorite it in the **Replay Center** using the respective buttons.

> You are encouraged to rate Replays after you downloaded them to help promoting the best Replays.

## Upload Replays [upload]
![](img/replay-upload.jpg)
The **Replay Upload Screen**

You can share your own **Replay Files** with other users in the **Replay Center**.
Therefore, click the **"Upload" Button** in the **Replay Viewer** after selecting a Replay.

In the **Replay Upload Screen** there are **6 input fields**:
- **Replay Name**  
  The Replay's Name in the Replay Center
- **Replay Description**  
  A description of what happens in the Replay. Try to give the user a good impression why your Replay is worth downloading.
- **Category**  
  The Replay's Category, one of the following: **Survival**, **Build**, **Minigame**, **Miscellaneous**. If none of the other categories fit, use **Miscellaneous**.
- **Tags**  
  One or more tags that fit your replay, spearated by comma. **Example tags:** pvp,battle,redstone,creative
- **Hide Server IP**  
  If you've recorded a Replay on a private Server, you can remove the Server IP by checking the respective checkbox.
- **Thumbnail**  
  While you can't directly edit the Thumbnail when uploading, it is highly recommended that
  you [create a Thumbnail](#replaying-thumbnail) before sharing your Replay.
  Replays without a Thumbnail are much less likely to be downloaded by other users.

When uploading a Replay File, make sure to follow the [Replay Center Rules](https://www.replaymod.com/rules).

# Frequently Asked Questions [faq]

### Do I need a ReplayMod.com Account to use the mod?
You only need a ReplayMod.com Account to access the [Replay Center](#center).
All of the other features are also available offline.

### For how long can I record?
Theoretically, a Replay File can be up to **24 days, 20 hours, 30 minutes and 23 seconds** long - which is a timespan you'll probably never reach.

### How many chunks around me does the Mod record?
If you're playing in **Singleplayer**, all of the loaded chunks within your **Render Distance** are recorded.  
If you're on a server, this depends on the **view-distance** setting in the **server.properties** file, which is set to **11 Chunks** by default.

The Replay Mod of course records every **loaded**, not only the **rendered** chunks. Therefore, chunks behind your back are recorded as well.

### Why can't I start recording in the middle of a session?
Due to the way the **Replay Mod** works, you need to start recording when joining a **Server** or **World** - otherwise some data which the **Replay Files** need is missing.

### How does recording affect my FPS?
Usually, recording a Replay does **_not_** affect your framerate, and it also doesn't lead to lag on servers.  
We've tested the Mod on a low-end notebook with only **3GB of ram**, and everything worked fine.

### How big are the files the Replay Mod records?
The file size of the recorded **.mcpr files** greatly depends on the amount of world data the replay contains.  
A Replay in which you travelled around and discovered a lot of terrain is significantly larger than a Replay which was recorded in a void world.

An average Replay File of **10 Minutes duration** usually is between **2MB and 10MB large**.
Replays recorded on **Minigame Servers** with lots of particle effects and world changes might be larger.
