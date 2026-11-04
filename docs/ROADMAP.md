# Engineering Roadmap

**Document ID:** IP-ROADMAP-001  
**Version:** 0.1.0  
**Status:** Planned; no milestone completed  
**Last updated:** 2026-11-04

## 1. Roadmap rules

- Milestones are sequential unless a task is explicitly independent.
- A milestone is complete only when its exit gate passes.
- Do not start a later subsystem merely to avoid resolving a foundational defect.
- Benchmarks must identify hardware, OS, build, scene, and test method.
- Performance thresholds may be revised only through a recorded decision.
- Documentation updates are part of every milestone.
- No planned feature should be reported as implemented.

## 2. Milestone summary

| ID | Name | Status | Primary outcome |
|---|---|---|---|
| M0 | Foundation and numerical validation | In progress | Runnable Android shell and tested camera |
| M1 | Input and first drawing renderer | Not started | Natural single-stroke inking |
| M2 | Document and spatial engine | Not started | Large editable vector document |
| M3 | Rendering performance and scale | Not started | Stable viewport rendering under load |
| M4 | Persistence and recovery | Not started | Reliable local documents |
| M5 | Product features and usability | Not started | Usable full application |

## 3. M0 — Foundation and numerical validation

### Scope

- Create Android Studio project.
- Establish package naming, Gradle configuration, SDK policy, and Git repository.
- Create application shell.
- Create a surface-based rendering host.
- Initialize a graphics context and clear the screen.
- Query and log actual graphics API, renderer, vendor, and version.
- Implement pure Kotlin camera mathematics.
- Implement coordinate round-trip, pan, zoom, and viewport tests.
- Verify focal-point-preserving zoom.
- Record device OS build, display mode, and runtime graphics information.

### Tasks

- M0.1 Project skeleton and build reproducibility.
- M0.2 Activity and surface lifecycle.
- M0.3 Minimal clear-screen renderer.
- M0.4 Camera data model and coordinate transforms.
- M0.5 Camera unit tests.
- M0.6 On-device gesture smoke test.
- M0.7 Initial diagnostic log and documentation update.



### Exit gate

- Project builds from a clean checkout.
- App launches on the Tab S10 FE+.
- Graphics context initializes and reports its actual version/vendor/renderer.
- Surface recreation and Activity pause/resume do not leave the app permanently blank.
- Camera unit tests pass.
- Focal-point zoom test passes within a documented pixel tolerance for ordinary test ranges.
- No drawing or persistence features are required yet.

## 4. M1 — Input and first drawing renderer

### Scope

- Add stylus and finger event adapter.
- Add interaction state machine.
- Draw one stroke and a sequence of strokes.
- Add pressure-aware width where supported.
- Implement screen-locked and world-locked width semantics.
- Implement segment geometry and joins/caps.
- Add a basic eraser prototype only after draw-order behaviour is understood.
- Add input latency instrumentation.

### Candidate implementation experiments

1. CPU-expanded triangle mesh.
2. Instanced segment/capsule geometry.
3. Alternative stroke tessellation if both approaches fail quality or performance requirements.

Do not freeze a 32-byte instance layout before measuring actual shader requirements.

### Exit gate

- Continuous stylus stroke has no missing samples under normal input.
- Pressure data is used only when supported and normalized safely.
- Round caps/joins do not show obvious gaps or dark seams in test cases.
- Screen-locked width remains visually stable through zoom.
- World-locked width scales predictably.
- Finger pan and pinch do not accidentally create strokes in default mode.
- Input cancellation and stylus/finger overlap do not leave a stuck active stroke.
- Baseline frame and input-latency measurements are recorded.

## 5. M2 — Document and spatial engine

### Scope

- Define canonical stroke model and stable identifiers.
- Define drawing-order semantics.
- Implement document command application.
- Add undo/redo foundation.
- Add spatial index behind a replaceable interface.
- Benchmark quadtree and spatial hash (and R-tree if useful).
- Implement viewport candidate queries.
- Preserve draw order after spatial filtering.
- Define erase semantics and implement selected behaviour.

### Exit gate

- Deterministic document tests pass.
- Spatial query returns all intersecting strokes for randomized test documents.
- No false negatives in conservative viewport queries.
- Ordering remains correct after query, edit, and erase.
- 100,000-stroke synthetic document can be queried and rendered at a documented baseline.
- Memory and query-time distributions are recorded.
- The 100k workload is defined by stroke count, points per stroke, spatial distribution, and visible fraction.

## 6. M3 — Rendering performance and scale

### Scope

- Add geometry cache and bounded upload queue.
- Add background CPU geometry preparation.
- Add stale-result/version rejection.
- Add GPU resource lifecycle and eviction.
- Add conservative culling.
- Add LOD only where tests demonstrate a need.
- Evaluate sub-pixel aggregation only after image-quality comparison.
- Stress camera precision over large offsets and zoom ranges.
- Evaluate ANGLE and Vulkan only through reproducible comparisons.

### Exit gate

- No unbounded growth in CPU or GPU caches during a long navigation test.
- No stale geometry replaces a newer document revision.
- Context loss/recreation restores the visible document.
- Frame timing distributions are recorded for light, medium, and dense scenes.
- 90 Hz target is evaluated when the device is in 90 Hz mode.
- Dense-scene 60 FPS floor is evaluated against a clearly defined workload.
- No arbitrary “zero jitter” claim; numerical and visual tolerances are documented.

## 7. M4 — Persistence and recovery

### Scope

- Select and document a versioned file format.
- Implement save/load.
- Add atomic or transactional write strategy.
- Add recovery behaviour.
- Add schema/version migration strategy.
- Separate canonical document data from derived GPU cache data.
- Benchmark startup, save latency, load latency, file size, and memory.

### Exit gate

- Save/reopen preserves stroke geometry, style, order, and document metadata.
- Forced process termination during save does not silently destroy the last valid document.
- Corrupt or unsupported files fail safely with useful diagnostics.
- Large-document load and save times are recorded.
- GPU geometry is rebuilt from canonical document data.

## 8. M5 — Product features and usability

Potential scope, ordered by user value and architectural readiness:

- Document browser and multiple documents.
- Tool palette and brush settings.
- Bookmarks/named viewpoints.
- Layers.
- Selection and transformation.
- Image placement.
- PDF import and annotation.
- Export.
- Accessibility and keyboard shortcuts.
- Settings and customization.

Each feature gets its own acceptance criteria and tests. Feature ordering may change based on actual use.

## 9. Cross-cutting work

These tasks run throughout development:

- Unit and property-based tests for camera/document logic.
- Instrumentation and benchmark harness.
- Crash and lifecycle testing.
- Device compatibility notes.
- Security and privacy review for imported/exported files.
- Documentation updates.
- Git review and clean commits.

## 10. Deferred optimizations

The following ideas from the initial Claude plan are retained as candidates, not commitments:

- Off-heap direct ByteBuffer stroke storage.
- Delta-zigzag varint encoding.
- Slab VBO allocator.
- LRU GPU eviction.
- Per-octave geometry rebuild.
- Generation-tagged asynchronous jobs.
- Append-only operation log.
- Per-pen-lift fsync.
- Dust/sub-pixel coverage aggregation.
- Fixed 256-stroke chunk threshold.
- 10^8× zoom as a hard gate.

Adopt an optimization only when a measured bottleneck and a testable benefit justify its complexity.

## 11. Change control

A milestone scope change must include:

1. Reason for change.
2. Evidence (test, benchmark, API constraint, or user requirement).
3. Affected architecture and requirements.
4. Revised exit gate.
5. Entry in `DECISIONS.md`.
