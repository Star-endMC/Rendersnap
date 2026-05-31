# Rendersnap
The mod, in it's name, improves on the current (inadequate) rendering engine, by applying aggressive (but subtle) optimizations to cut down the amount of things to render, without causing incompatibility issues.

![.](https://cdn.modrinth.com/data/cached_images/f6a97386786796587059baec6a3457ed4e82dae4.png)

## Highlights

### It's simple!

Only simple (yet useful) optimizations are applied, mostly cutting out things you can't see, including:
- **Multi-render** - Renders chunks efficiently by keeping chunk rebuilds on the worker queue instead of forcing nearby rebuilds on the render thread.
- **Section occlusion** - Uses the section visibility graph to skip rendering terrain hidden behind other terrains.
- **Block face culling** - Skips rendering faces between full solid blocks.
- **Distance entity culling** - Non-player entities will not be rendered behind the player or beyond the fog.
- And more!

Other visual features are also added, including zooming and hiding some visual features.

### It's modular!

All can be controlled, enabled or disabled, aggressive or conservative, through vanilla settings.

### It's compatible!

Fix, not change, so vanilla renderer features such as vanilla shaders are completely supported. Any compatibility issues arising with vanilla or other mods will be fixed.

## Installation

- Download `Fabric API` (it is a required dependency) and the mod from the latest snapshot available. (You may ignore the Minecraft version for both mods, as snapshot versions from the same update usually do not break compatibility.)

- Install the mod as you normally would in your launcher. If your launcher does not have a mod installer, manually move the downloaded `.jar` file into the `mods` folder of your instance.

## Benchmarks

![Difference in FPS between Vanilla and Rendersnap](https://cdn.modrinth.com/data/cached_images/9ae214d0598fddf937049788ade1e7df54c660f5.jpeg)

```
Hardware configuration:
- CPU: Intel Core i5 1035G1
- GPU: Nvidia MX330
- RAM: 8GB DDR4 RAM
- Storage: 1TB NVMe SSD 

Software configuration:
- Minecraft 26.1.2
- Fabric API 0.149.1
- Rendersnap 1.0

Settings configuration: Fabulous preset (32 chunks render distance, 12 chunks simulation distance)

World configuration:
- Seed: -4562913757330550907
- Location: -203.83 142.01 647.58 179.26 9.14
```

## Extras

It is recommended to use complementary mods like [ModernFix](https://modrinth.com/mod/modernfix-mvus) and [Ixeris](https://modrinth.com/mod/ixeris). The mod only fills the gaps that are left by those mods.

This mod is not a one-size-fits-all renderer, and it benefits most on integrated GPUs that cannot take advantage of complex optimizations. Consider other renderer mods that work best on your hardware.

## License
Rendersnap is currently licensed under the Star-end License.
