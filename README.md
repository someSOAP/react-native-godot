# React Native Godot (Fork)

> [!IMPORTANT]
> This fork of the library is powered by [react-native-worklets](https://docs.swmansion.com/react-native-worklets/) (from Software Mansion). It utilizes worklets to ensure high-performance communication between the React Native JavaScript thread and the Godot Engine thread. This package is a drop-in replacement for the original package using `react-native-worklets` instead of `react-native-worklets-core` by Margelo.

This is a fork of the original repository: [borndotcom/react-native-godot](https://github.com/borndotcom/react-native-godot)

### Installation

```bash
yarn add @somesoap/react-native-godot
```

After installation, you must run the following command to download prebuilt binaries:

```bash
yarn download-prebuilt
```

### Peer Dependencies

This package requires the following peer dependency:

- `react-native-worklets` (tested on version `0.10.0`)

To install `react-native-worklets` version `0.10.0`, run:

```bash
yarn add react-native-worklets@0.10.0
```
