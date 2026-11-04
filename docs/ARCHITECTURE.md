# System Architecture

**Document ID:** IP-ARCH-001  
**Version:** 0.1.0  
**Status:** Proposed architecture; not yet implemented  
**Last updated:** 2026-11-04

## 1. Architectural objectives

The architecture is designed around a high-frequency interactive canvas. Its main risks are numerical precision, input latency, rendering throughput, large-document traversal, memory pressure, and reliable persistence.

The architecture must make these concerns independently testable. A camera test should not require a GPU. A document-order test should not require Android. A renderer benchmark should be able to use synthetic geometry without a real document file.

## 2. High-level subsystem map

```text
Android Activity / Window
        |
        +---------------- UI controls (future)
        |
   Canvas Surface / Input Adapter
        |
        +---------------- Input Pipeline
        |                       |
        |                       v
        |                 Interaction State
        |                       |
        v                       v
     Camera <------------ Document Commands
        |                       |
        |                       v
        |                 Document Model
        |                       |
        |                       v
        |                 Spatial Query
        |                       |
        +-----------------------+
                                |
                         Visible Content Set
                                |
                         Geometry Preparation
                                |
                         Render Queue / Cache
                                |
                         Graphics Backend
                                |
                              Display

Persistence <---- Document Model / Operations
Diagnostics  <---- Input, Camera, Query, Geometry, Renderer, Storage
```

## 3. Module responsibilities

### 3.1 Android application shell

Responsibilities:

- Own Activity/window lifecycle.
- Host the drawing surface.
- Route lifecycle events.
- Host non-canvas controls.
- Coordinate permissions and Android document picker integration when those features exist.

The application shell must not contain coordinate mathematics, stroke tessellation, or document indexing logic.

### 3.2 Input adapter

Responsibilities:

- Receive Android `MotionEvent` and relevant generic motion events.
- Identify pointer/tool types.
- Extract position, pressure, tilt, orientation, timestamps, and eraser identity when available.
- Normalize device-specific or missing values into an internal event model.
- Handle pointer IDs and cancellation.
- Avoid unnecessary allocations in high-frequency paths.

The adapter should not directly call OpenGL functions. It emits input data or commands to the interaction layer.

### 3.3 Interaction state machine

Responsibilities:

- Decide whether an input sequence means drawing, panning, zooming, erasing, or another gesture.
- Track active pointer identities.
- Resolve stylus/finger arbitration.
- Handle interrupted sequences and cancellation.
- Convert completed interactions into document commands.

Gesture rules should be explicit and testable. Do not scatter gesture decisions across Activity, View, and renderer classes.

### 3.4 Camera module

Responsibilities:

- Store camera center and zoom.
- Convert between screen, world, and camera-relative coordinates.
- Implement pan and focal-point-preserving zoom.
- Handle viewport changes.
- Enforce finite values and numerical safety.
- Provide pure functions where possible for unit testing.

The camera module must not depend on Android UI classes or OpenGL types.

### 3.5 Document model

Responsibilities:

- Own logical strokes and other document objects.
- Maintain stable identifiers and ordering semantics.
- Apply document commands.
- Support undo/redo when implemented.
- Expose read-only snapshots or query interfaces to rendering and persistence.
- Keep document semantics independent from GPU buffer layouts.

The document model is the source of truth. GPU buffers are disposable derived data.

### 3.6 Spatial engine

Responsibilities:

- Organize document objects for efficient viewport queries.
- Return candidate strokes/chunks intersecting a world-space query region.
- Update or invalidate spatial entries after edits.
- Avoid losing objects that cross spatial boundaries.
- Preserve a link to stable document ordering.

Candidate structures include quadtree, R-tree, or a hybrid. Choose by workload benchmark, not preference alone.

Important distinction: spatial organization and temporal drawing order are separate. A spatial query can identify candidates, but the final visible list must preserve required compositing order.

### 3.7 Geometry preparation

Responsibilities:

- Convert logical stroke samples into renderable primitives.
- Build screen-locked geometry at the relevant camera scale.
- Build world-locked geometry where appropriate.
- Generate joins, caps, pressure widths, and bounds.
- Support clipping and LOD.
- Produce immutable or safely transferable geometry batches.

Geometry preparation should be separable from GPU submission. Background workers may prepare CPU-side geometry; only the graphics thread should perform API operations unless the backend explicitly supports a safe alternative.

### 3.8 Graphics backend

Initial implementation: OpenGL ES 3.0-compatible clear-screen prototype using Android GLSurfaceView. The reference tablet reports OpenGL ES 3.2 through ANGLE backed by Vulkan 1.3.279. This is a provisional rendering approach; no stroke geometry or production rendering pipeline has been implemented.

Responsibilities:

- Own graphics resources.
- Compile and validate shaders.
- Upload prepared geometry.
- Submit visible batches.
- Manage blending, clipping, viewport, and frame state.
- Handle surface/context recreation.
- Expose diagnostics and timing information.

Keep the backend boundary narrow enough that Vulkan can be evaluated without rewriting the document model or camera.

Do not prematurely design a large cross-API abstraction. Abstract only the operations actually required by the first renderer.

### 3.9 Persistence

Responsibilities:

- Serialize document state.
- Version the file format.
- Save safely without blocking drawing.
- Recover from interrupted writes.
- Rebuild derived spatial and GPU data as needed.
- Migrate older document versions when format evolution begins.

The initial format is not selected. Avoid coupling serialized data to an in-memory object layout or GPU vertex format.

### 3.10 Diagnostics and benchmark harness

Responsibilities:

- Record app version, device, OS build, GPU/driver, display mode, thermal state, and scene identity.
- Measure frame timing distributions.
- Measure input-to-render latency where feasible.
- Record memory usage and cache residency.
- Run deterministic synthetic workloads.
- Export results to `docs/BENCHMARKS.md`.

## 4. Coordinate and precision model

### 4.1 Spaces

Let the camera center be `(Cx, Cy)`, zoom be `Z`, and viewport dimensions be `(W, H)` in physical pixels.

World to screen:

```text
screenX = W / 2 + (worldX - Cx) * Z
screenY = H / 2 + (worldY - Cy) * Z
```

Screen to world:

```text
worldX = Cx + (screenX - W / 2) / Z
worldY = Cy + (screenY - H / 2) / Z
```

The sign convention assumes screen Y increases downward and world Y is defined consistently with that choice. This convention must be tested and documented in code.

### 4.2 Focal-point-preserving zoom

Before changing zoom, convert the gesture focal point from screen to world coordinates. After changing zoom, adjust the camera center so the same world point maps back to the same screen point.

This must be tested for focal points at the center, corners, and arbitrary off-center positions.

### 4.3 Precision limits

Double precision provides substantially more mantissa precision than float, but it does not provide uniform absolute precision over an unbounded coordinate range. Subtracting nearby values after both have become extremely large can still lose detail.

Initial strategy:

1. Keep document and camera calculations in `Double`.
2. Subtract a nearby camera origin on the CPU.
3. Convert only small camera-relative values to `Float`.
4. Stress-test at large world offsets and zoom extremes.
5. Introduce hierarchical origin rebasing or tiled coordinates if the tests reveal precision loss.

Do not send huge absolute world coordinates to float vertex attributes or uniforms.

### 4.4 Screen-locked stroke geometry

For a screen-locked brush, the logical centerline remains in world space while the visible width is defined in screen pixels. Geometry generation must account for current zoom. A cached screen-space tessellation becomes invalid when camera scale or orientation changes beyond its validity policy.

For world-locked brushes, width is defined in world units and scales with zoom.

## 5. Rendering pipeline proposal

### 5.1 First renderer

The first meaningful renderer should draw simple diagnostic geometry, then one stroke, then a sequence of segments.

Candidate pipeline:

1. Query visible document candidates.
2. Resolve their required drawing order.
3. Prepare or retrieve geometry.
4. Upload missing geometry within a bounded budget.
5. Submit batches.
6. Present the frame.
7. Record timing and counters.

### 5.2 Instanced segment/capsule candidate

Claude's original plan proposes a static unit quad and per-segment instance attributes. This is a reasonable candidate because it can reduce per-segment CPU submission overhead.

However:

- A single rectangle per segment is insufficient by itself for seamless round joins and caps.
- Pressure changes may require per-point widths or additional geometry.
- Overlapping translucent capsules can produce dark seams if blending is not designed carefully.
- The instance record must be aligned with actual shader needs and measured; a 32-byte layout is not yet a requirement.
- Very long or nearly degenerate segments need robust handling.

The first implementation should compare a simple CPU-expanded mesh with instanced segment geometry on the target device before committing to a complex pipeline.

### 5.3 Batching and order

Batching may combine adjacent compatible primitives, but must not reorder content where alpha compositing or eraser semantics make order observable. A chunk is not automatically a safe draw-order unit merely because it is spatially close.

### 5.4 Culling

Use conservative bounds. A stroke whose bounds intersect the viewport must not disappear due to an underestimated bound. Include brush radius, antialiasing fringe, and any required prediction or tessellation margin.

## 6. Spatial organization

The document's canonical objects should have stable identity and order independent of spatial index layout.

Potential design:

- Canonical stroke storage in document order or indexed by stable ID.
- Spatial index stores references/IDs and conservative bounds.
- Query returns candidate IDs.
- Candidates are resolved into correct compositing order.
- Edits update or invalidate affected index entries.

A stroke crossing multiple spatial cells may be referenced by multiple cells, but it should have one canonical logical identity. Avoid duplicating the authoritative stroke payload in every cell.

Chunking policy should consider spatial locality, memory footprint, stroke count, and edit frequency. The proposed “256 strokes” threshold is only an experiment parameter. A chunk sealed solely by stroke count may have a very large bounding box and poor culling efficiency.

## 7. Concurrency and thread ownership

Initial ownership model:

| Work | Preferred owner |
|---|---|
| Android lifecycle | Main/UI thread |
| Input event reception | Android input/UI dispatch path |
| Gesture state | Input/interaction owner |
| Document mutation | Serialized document command owner |
| CPU tessellation | Worker executor/coroutines |
| OpenGL context operations | GL/render thread |
| File serialization | I/O dispatcher |
| Metrics aggregation | Diagnostics component |

The final ownership model must avoid concurrent mutation of stroke data while a renderer or serializer reads it. Use immutable snapshots, versioned data, or explicit synchronization. Do not rely on “it usually happens in this order.”

`GLSurfaceView` can simplify initial GL-thread management. If later requirements demand explicit control over EGL lifecycle, frame scheduling, or threading, evaluate a custom `SurfaceView`/EGL host through a documented decision.

## 8. Memory and resource management

- CPU document data is authoritative.
- GPU geometry is derived and evictable.
- Caches have explicit byte budgets.
- Upload queues have bounded capacity.
- Workers should coalesce obsolete rebuild requests.
- Version/generation tags may prevent stale geometry from replacing newer geometry.
- Evicted GPU data must be reproducible from document data.
- Resource destruction and context recreation must be tested.
- Avoid allocating new objects for every pointer sample or every frame where practical.

Do not implement off-heap storage, slab allocators, or LRU GPU caches before measurements show they are needed. These techniques remain possible optimizations.

## 9. Persistence architecture options

Not yet selected:

- Versioned binary document file.
- Append-only operation log plus checkpoints.
- SQLite metadata/index with binary stroke blobs.
- Hybrid model.

Selection criteria:

- Crash consistency.
- Incremental save cost.
- Random access to visible regions.
- Format migration.
- File size.
- Implementation complexity.
- Recovery time.
- Compatibility with future features.

## 10. Graphics API strategy

Initial prototype: OpenGL ES 3.0, with an explicit review before renderer complexity grows.

ANGLE may translate OpenGL ES calls to another backend such as Vulkan, but its presence and use are device/runtime dependent. It is not the GPU. Do not make ANGLE a correctness dependency.

Vulkan should be evaluated if the prototype reveals a concrete need for lower-level resource control, explicit synchronization, or performance. Its added complexity is not automatically justified for a 2D canvas.

### 10.1 M0 graphics implementation observation

The initial Android implementation contains three principal classes:

- `MainActivity`: owns the Activity lifecycle, checks reported GLES 3.0 support, creates the canvas view, and forwards pause/resume.
- `CanvasView`: creates a GLSurfaceView, requests a GLES 3.0 context, installs the renderer, and uses render-on-demand mode.
- `CanvasRenderer`: handles surface creation, viewport changes, framebuffer clearing, and graphics capability logging.

The prototype uses GLSurfaceView's dedicated rendering thread. OpenGL calls are confined to renderer callbacks.

Observed on 2026-11-04:
- GL vendor: Samsung Electronics Co., Ltd.
- Renderer: ANGLE ((Samsung Xclipse 540) on Vulkan 1.3.279)
- GLES version: 3.2
- Viewport: 2880 x 1800

The rendering context initialized successfully. Surface recreation, context loss, frame timing, and input-to-render latency remain unverified.

The graphics backend must remain separate from the future pure Kotlin camera, document, and geometry modules.

## 11. Key architecture risks

| Risk | Mitigation |
|---|---|
| Precision loss at large coordinates | Camera-relative coordinates; stress tests; hierarchical origin if needed |
| Poor spatial locality | Separate spatial indexing from temporal order; benchmark index structures |
| Alpha compositing changes due to batching | Preserve order; renderer correctness tests |
| Stylus lag | Avoid allocation/blocking in input path; measure latency |
| GPU context loss | Rebuild derived resources from document data |
| Memory pressure | Bounded caches; PSS and graphics memory measurements |
| Excessive architecture complexity | Implement only the next validated subsystem |
| Storage corruption | Versioned format; atomic/transactional save strategy; recovery tests |
| Renderer lock-in | Keep camera/document/geometry independent of graphics API |

## 12. Open architecture questions

1. Should the first renderer use `GLSurfaceView` or a custom EGL surface host?
2. Does the target tablet expose OpenGL ES 3.0/3.1/3.2 and which driver path is active?
3. Is ANGLE available and selectable for this app/device configuration?
4. Does Vulkan offer a measurable advantage for the initial 2D workload?
5. Which spatial index performs best for realistic stroke distributions?
6. What geometry representation produces the best quality/performance trade-off?
7. What eraser semantics best preserve order and support undo?
8. Which persistence model provides acceptable save/recovery behaviour?
9. What coordinate representation is required for the tested practical zoom range?
