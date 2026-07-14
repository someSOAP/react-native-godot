# React Native Godot (Fork)

> [!IMPORTANT]
> This fork of the library is powered by [react-native-worklets](https://docs.swmansion.com/react-native-worklets/) (from Software Mansion). It utilizes worklets to ensure high-performance communication between the React Native JavaScript thread and the Godot Engine thread. This package is a drop-in replacement for the original package using `react-native-worklets` instead of `react-native-worklets-core` by Margelo.

This is a fork of the original repository: [borndotcom/react-native-godot](https://github.com/borndotcom/react-native-godot)

### Installation

```bash
npm install @somesoap/react-native-godot
```

After installation, run the package binary to download the prebuilt binaries:

```bash
npx download-prebuilt
```

In this repository (and in the example app), the equivalent command is `npm run download-prebuilt`.

### Peer Dependencies

This package requires the following peer dependency:

- `react-native-worklets` (tested on version `0.10.0`)

To install `react-native-worklets` version `0.10.0`, run:

```bash
npm install react-native-worklets@0.10.0
```

### Transparent Godot view

`RTNGodotView` supports an opt-in `transparent` prop:

```tsx
<View style={{flex: 1}}>
  <ReactNativeContent />

  <RTNGodotView
    style={StyleSheet.absoluteFill}
    transparent
  />
</View>
```

When `transparent={true}`, only Godot-rendered pixels are composited over React Native. Pixels cleared with alpha `0` reveal the React Native content beneath the Godot view.

The default is `false`, which preserves the normal opaque rendering path and its performance characteristics.

#### Godot project setup

The native prop enables alpha composition, but the Godot project must also render a transparent viewport:

1. Enable **Project Settings → Display → Window → Per Pixel Transparency → Allowed**.

   Equivalent project setting:

   ```ini
   [display]

   window/per_pixel_transparency/allowed=true
   ```

2. Enable a transparent viewport/background in Godot:

   ```gdscript
   get_viewport().transparent_bg = true
   ```

   If the project controls the main `Window` directly, ensure its transparent-background setting is also enabled.

3. Do not render an opaque background in the Godot scene:
   - Remove or hide full-screen `ColorRect`, `TextureRect`, sprites, or background meshes.
   - Do not use an opaque sky/environment background.
   - Ensure the renderer clear color has alpha `0` if the project overrides it.

Godot objects, particles, and UI that render with alpha remain visible normally; only untouched/transparent viewport pixels reveal React Native beneath.

### Cancel touches when outside

`RTNGodotView` supports a `cancelTouchWhenOutside` prop that defaults to `false`.

```tsx
<RTNGodotView
  style={styles.godotView}
  cancelTouchWhenOutside={true}
/>
```

When `cancelTouchWhenOutside={true}`, any active touch that moves outside the bounds of the `RTNGodotView` will be automatically canceled in Godot. This prevents "stuck" touch states where Godot continues to process a drag or hold even after the finger has left the view area.
 
### Visibility control

`RTNGodotView` supports a `visible` prop that defaults to `true`.

```tsx
<RTNGodotView
  style={styles.godotView}
  visible={isVisible}
/>
```

When `visible={false}`, the Godot render layer is hidden natively (alpha set to 0 on Android, `hidden` set to `true` on iOS). This can be used to temporarily hide Godot content while keeping the view in the React Native hierarchy. Note that this is different from React Native's `display: 'none'`, as the view still occupies space in the layout.

#### Platform notes

- **Android:** transparent mode uses a translucent `SurfaceView` composition path and places Godot above the underlying React Native content.
- **iOS:** transparent mode configures the embedded Godot render layer for alpha compositing.
- **Input:** touch events continue to be delivered to Godot normally. Since Godot is the upper view, React Native controls beneath it are visual content only and do not receive touches through transparent pixels.

No LibGodot engine fork or modification is required.
