# Product and Engineering Specification

**Document ID:** IP-SPEC-001  
**Version:** 0.1.0  
**Status:** Baseline draft; requirements are subject to validation  
**Last updated:** 2026-11-04

## 1. Purpose

This document defines the intended product behaviour and engineering requirements for InfinitePaper. It is the primary requirements reference for implementation and testing.

The application is an Android-first infinite canvas for freeform drawing, handwriting, sketching, annotation, and spatial organization. The canvas engine must remain independent of optional product features so that rendering and document correctness can be validated before interface expansion.

## 2. Product goals

### 2.1 Primary goals

- Provide a large, continuously navigable two-dimensional workspace.
- Support smooth pinch zoom and panning with stable focal-point behaviour.
- Support natural stylus inking, including pressure when the device reports it.
- Support finger navigation and optional finger drawing.
- Preserve stroke quality and correct visual ordering while zooming and navigating.
- Store user-created marks as vector data.
- Handle documents larger than the current viewport without requiring all geometry to remain GPU-resident.
- Operate offline for core functionality.
- Provide reliable local save, reopen, and recovery behaviour.

### 2.2 Secondary goals

These are intended product capabilities but are not part of the first renderer milestone:

- Multiple documents.
- Bookmarks or named viewpoints.
- Layers.
- Selection and transformation.
- Eraser and undo/redo.
- Image placement.
- PDF import and annotation.
- Export and sharing.
- Customizable tools and interface.

### 2.3 Non-goals for the initial engineering phase

- Real-time collaboration.
- Cloud synchronization.
- Account system.
- OCR or handwriting recognition.
- Full vector-graphics editor compatibility.
- 3D canvas.
- Guaranteed mathematically infinite coordinates or zoom.
- Guaranteed performance for arbitrary document sizes on every Android device.

## 3. Target platform and hardware profile

### 3.1 Primary validation device

| Property | Current project record |
|---|---|
| Device | Samsung Galaxy Tab S10 FE+ |
| Display | 13.1-inch class |
| Advertised resolution | 2880 × 1800 |
| Advertised refresh capability | Up to 90 Hz |
| SoC | Exynos 1580 |
| GPU | Xclipse 540 |
| Physical RAM | 8 GB |
| RAM Plus | 4 GB configured |
| Stylus | S Pen |
| Android version | Must be recorded from device before testing |
| Build fingerprint | Must be recorded before formal benchmarks |
| Graphics driver / API | Must be queried at runtime |

RAM Plus must not be treated as physical RAM. Android, system services, GPU allocations, and other processes share device resources. Application memory budgets must be based on observed process memory and device behaviour.

### 3.2 Compatibility strategy

The Tab S10 FE+ is the primary development target, not the only possible target. The project should avoid unnecessary dependence on Samsung-only APIs. Standard Android input and graphics APIs should be preferred where they meet the requirements.

Minimum Android API level, supported GPU baseline, and fallback policy remain open until the project chooses its distribution target. A graphics feature requirement must be checked at runtime and, if relevant, declared in the manifest.

## 4. Interaction requirements

### 4.1 Default interaction model

| Input | Default action |
|---|---|
| S Pen contact | Draw a stroke |
| S Pen movement | Extend the active stroke |
| S Pen lift | Commit/finish the stroke |
| S Pen eraser end, if reported | Activate erasing behaviour |
| One-finger drag | Pan the canvas |
| Two-finger pinch | Zoom around the gesture focal point |
| Two-finger translation | Pan while zooming |
| Finger drawing mode | Optional user setting |
| Stylus hover, if available | Optional cursor/preview behaviour |

The exact gesture arbitration policy must be tested on the target tablet. A two-finger gesture beginning during an active stylus stroke, pointer cancellation, and stylus/finger overlap require explicit handling.

### 4.2 Stylus data

The input layer should inspect Android `MotionEvent` tool type and available axes. Data may include:

- X/Y position.
- Pressure.
- Tilt.
- Orientation.
- Hover distance.
- Event timestamp.
- Tool type, including stylus and eraser where reported.

The application must not assume every device reports every axis or uses identical pressure calibration. Missing values must have defined defaults. Input processing should avoid unnecessary per-event allocations.

### 4.3 Palm and accidental-touch handling

Palm rejection is a best-effort feature. The app should use platform-provided tool classification and supported stylus-related APIs where appropriate. It must not promise perfect rejection across all devices. Provide a configurable input policy and test it on the target tablet.

### 4.4 Input latency

The active stroke should appear to follow the pen naturally. The system should measure input-to-visible-stroke latency where practical. Motion prediction may be evaluated later, but predicted points must remain transient and be replaced by actual input data. Prediction must never corrupt persisted stroke geometry.

## 5. Camera and coordinate requirements

### 5.1 Coordinate spaces

The engine will distinguish:

1. **Screen space:** physical pixel coordinates in the drawing viewport.
2. **World/document space:** persistent coordinates for document geometry.
3. **Camera-relative space:** coordinates translated relative to a nearby camera origin.
4. **GPU clip space:** normalized device coordinates consumed by the graphics pipeline.

Coordinate conversion must be centralized in the camera module. UI components must not implement their own world/screen conversion formulas.

### 5.2 Precision strategy

- Persistent world coordinates use CPU-side double precision initially.
- Camera position and zoom use double precision initially.
- Geometry submitted to the GPU should use camera-relative coordinates and float precision.
- Large-coordinate handling must be tested for cancellation and precision loss.
- If double precision alone proves insufficient for practical navigation ranges, introduce a hierarchical origin/tile coordinate representation through a documented decision.

The design must not assume that converting large absolute double coordinates directly to GPU floats is safe.

### 5.3 Camera behaviour

- Panning must translate the canvas in the same direction as the user's drag.
- Pinch zoom must preserve the world point beneath the gesture focal point.
- Zoom must be positive, finite, and protected against overflow/underflow.
- Viewport resize and orientation changes must preserve a valid camera state.
- Repeated zoom-in/zoom-out cycles must not produce unexplained camera drift beyond the defined tolerance.
- UI zoom limits should not be arbitrary product restrictions. Internal numerical safety limits are permitted and must be documented.

### 5.4 Zoom policy

The product goal is practical, extremely broad zoom rather than a claim of literal infinity. No fixed user-facing zoom range is approved yet. A usable range will be established through precision and interaction testing. The earlier proposed 10^8× test is an exploratory stress case, not a release requirement or Milestone 0 gate.

## 6. Stroke and document requirements

### 6.1 Conceptual stroke record

A logical stroke should support:

- Stable identifier.
- Stable drawing-order key or sequence identifier.
- Ordered point samples.
- Position.
- Pressure when available.
- Timestamp or sample timing where needed.
- Brush/tool identifier.
- Color and opacity.
- Width and width semantics.
- Bounding box.
- Optional metadata for later features.

This is a conceptual model, not a final serialized schema. Data layout must be profiled before compact binary encoding is selected.

### 6.2 Thickness semantics

Two brush width modes are required:

- **Screen-locked:** apparent width remains approximately constant in screen pixels as the camera zoom changes. Geometry must be derived in a way that preserves this visual width.
- **World-locked:** width is stored in document units and scales with zoom.

The default mode is screen-locked. The user should be able to select the mode when brush settings are implemented.

### 6.3 Stroke geometry

The renderer must eventually handle:

- Segment bodies.
- Rounded caps.
- Continuous joins.
- Pressure-dependent width.
- Stable appearance at different zoom levels.
- Anti-aliasing.
- Clipping at viewport boundaries.
- Overlapping translucent strokes without unintended dark seams or order changes.

A capsule/segment-instancing pipeline is a candidate implementation. Exact instance layout and tessellation strategy are not yet frozen.

### 6.4 Drawing order

Strokes must preserve their logical drawing order, especially when opacity or erasing semantics make compositing order visible. Spatial indexing must not silently reorder content.

Spatial locality and temporal order are separate concerns. The final architecture may use spatial chunks with stable sequence keys or another representation, but any batching optimization must preserve the required compositing result.

### 6.5 Erasing and undo

The user must eventually be able to erase and undo edits. Exact semantics are open:

- Whole-stroke erase.
- Partial/slice erase.
- Mask-based erase.
- Non-destructive operation log.

The implementation must choose semantics before the document format is frozen. If a stroke is split, resulting fragments must retain a well-defined relationship to original drawing order.

## 7. Rendering requirements

- Render only content needed for the current viewport and required margins.
- Avoid uploading or tessellating the entire document for every frame.
- Keep GPU resource ownership on the rendering thread unless the selected API explicitly permits another safe model.
- Bound temporary geometry and GPU cache memory.
- Handle surface recreation and graphics-context loss.
- Avoid blocking input dispatch on expensive geometry generation.
- Maintain correct visual order.
- Provide deterministic test scenes for renderer correctness.
- Record actual frame timing, not only average FPS.

### 7.1 Level of detail

LOD and sub-pixel aggregation may be introduced after the basic renderer is correct. They must be visually validated against a reference rendering. They must not erase semantically important marks or cause unstable flicker during zoom transitions.

### 7.2 Graphics API

OpenGL ES 3.0 is the initial prototype path, subject to a short architecture validation. Vulkan remains an alternative backend. ANGLE is an optional OpenGL ES translation layer whose availability and selected path must be verified on the target device. The renderer should keep document, camera, and geometry logic independent of a specific graphics API.

## 8. Performance requirements

Performance targets must be measured on a named device, OS build, refresh mode, thermal state, and scene.

Initial intent:

- Simple scene: target smooth operation at 90 Hz when the device is in 90 Hz mode.
- Normal interactive drawing: maintain at least 60 FPS in representative test scenes.
- Dense scenes: prioritize stable frame pacing, responsive input, and bounded memory; establish exact thresholds after baseline measurements.
- Avoid long blocking work on the UI or GL thread.
- Bound geometry queues, staging buffers, and GPU cache residency.
- Avoid routine garbage collection caused by the inking hot path.

A claim such as “100,000 strokes at 90 FPS” is not accepted without a reproducible scene definition, viewport distribution, stroke complexity, and measured percentile timings.

## 9. Memory requirements

- 8 GB is the device's physical RAM configuration.
- 4 GB RAM Plus is not additional physical RAM.
- Do not assume the application can use all physical RAM.
- GPU allocations and system memory pressure must be considered separately from Java/Kotlin heap usage.
- Cache sizes must be configurable and bounded.
- Eviction must not discard document data.
- Temporary geometry must be reclaimable.
- Respond appropriately to Android memory pressure callbacks.
- Measure Java/Kotlin heap, native heap, graphics memory where observable, and process PSS.

No fixed application memory budget is approved until a baseline build is profiled.

## 10. Persistence and reliability requirements

- Core documents must be stored locally.
- File format must be versioned.
- Interrupted writes must not silently corrupt the only copy of a document.
- Saving should not block active drawing.
- Recovery behaviour must be tested with forced process termination.
- Document migration must be considered before the format is declared stable.
- The renderer must be able to rebuild GPU resources from persistent document data.

Append-only operation logs, periodic checkpoints, SQLite metadata, and compact binary stroke storage are candidates, not approved implementation details.

## 11. Quality attributes

| Attribute | Requirement |
|---|---|
| Correctness | Coordinate, order, clipping, and input behaviour covered by tests |
| Responsiveness | Input and navigation remain responsive under representative load |
| Reliability | Context loss and interrupted saves handled |
| Maintainability | Subsystems have clear contracts and tests |
| Portability | Avoid unnecessary device-vendor dependencies |
| Privacy | Core drawing data remains local unless user explicitly exports/shares |
| Accessibility | Tool controls and navigation should be usable with Android accessibility services where practical |
| Observability | Diagnostics and benchmark records identify device, build, scene, and test conditions |

## 12. Acceptance principles

A milestone is accepted only when its exit criteria in `ROADMAP.md` pass. “Looks okay on one run” is not a substitute for repeatable tests. Any known limitation must be documented rather than hidden.
