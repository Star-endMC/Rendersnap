# Rendersnap
The mod, in it's name, improves on the current (inadequate) rendering engine, by applying aggressive (but subtle) optimizations to cut down the amount of things to render, without causing incompatibility issues.

![Fabric compatible](https://cdn.modrinth.com/data/cached_images/513035d1231a86dff137c74e04a5e64633593f10.png)

## Highlights

### It's simple!

Only simple (yet useful) optimizations are applied, such as block face culling, behind camera draw skipping, section occlusion, and etc.

### It's modular!

All can be controlled, enabled or disabled, aggressive or conservative, through vanilla settings.

### It's compatible!

Fix, not change, so vanilla renderer features such as vanilla shaders are completely supported.

## Benchmarks

![Difference in FPS between Vanilla and Rendersnap](https://cdn.modrinth.com/data/cached_images/9ae214d0598fddf937049788ade1e7df54c660f5.jpeg)

```
Hardware configuration: Intel I5 1035G1 with Nvidia MX330, 8GB DDR4 RAM, 1TB NVMe ssd. 

Software configuration: Mc 26.1.2, Fabric-Api 0.149.1, RenderSnap 1.0. Settings configuration: Fabulous Preset, 32 Chunks, 12 Chunks. 

World configuration: Seed -4562913757330550907, Location -203.83 142.01 647.58 179.26 9.14.
```

## License
Rendersnap is currently licensed under the Star-end License.
