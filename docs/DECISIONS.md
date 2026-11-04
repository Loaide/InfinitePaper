# Architecture Decision Records

**Document ID:** IP-ADR-INDEX  
**Version:** 0.1.0  
**Status:** Initial decision baseline  
**Last updated:** 2026-11-04

This file records decisions and their rationale. A decision may be approved, provisional, proposed, superseded, or rejected. A conversation statement is not an architectural decision until it is recorded here.

## Decision status vocabulary

- **Approved:** Accepted for the current project baseline.
- **Provisional:** Use for the next experiment, but revisit at the stated gate.
- **Proposed:** Under consideration; not binding.
- **Superseded:** Replaced by a later decision.
- **Rejected:** Considered and not selected.

---

## ADR-001 — Build an independent Android implementation

**Status:** Approved  
**Date:** 2026-11-04

### Context

The product is inspired by the interaction model of an existing infinite-paper application. The project needs its own maintainable implementation.

### Decision

Build an independent Android application. Do not copy proprietary source code, protected assets, or undocumented internals. Reproduce desired user-visible behaviour through original implementation and testing.

### Consequences

- Architecture and code are designed for Android.
- Product behaviour can be used as a reference, but internal implementation is not assumed.
- Project naming and licensing require a later review before public release.

---

## ADR-002 — Kotlin and native Android as the initial application platform

**Status:** Approved  
**Date:** 2026-11-04

### Context

The target is an Android tablet with stylus support. A native platform implementation provides direct access to Android input and lifecycle APIs.

### Decision

Use Kotlin and native Android application APIs for the initial project.

### Consequences

- Android lifecycle, input, and graphics APIs are directly accessible.
- Core mathematical and document modules should remain platform-independent where practical.
- Exact Android Gradle Plugin, Kotlin, compile SDK, target SDK, and minimum SDK versions are selected when the project is created and recorded in the README.

---

## ADR-003 — Vector document model

**Status:** Approved  
**Date:** 2026-11-04

### Context

A single enormous raster bitmap is not appropriate for a large navigable drawing space. It also makes zoom-independent stroke semantics and editing difficult.

### Decision

Represent user-created marks as vector stroke data. GPU geometry and raster caches are derived, disposable representations.

### Consequences

- Persistent document data is independent of GPU buffer layout.
- Renderer caches can be rebuilt after context loss.
- Storage and spatial indexing require deliberate design.
- Raster assets may still be supported as placed objects in later milestones.

---

## ADR-004 — Double-precision CPU camera with camera-relative GPU coordinates

**Status:** Approved principle  
**Date:** 2026-11-04

### Context

Large world coordinates lose precision when represented directly as GPU floats. Using double precision everywhere on the GPU is not assumed to be available or efficient.

### Decision

Use `Double` for initial CPU-side world/camera calculations. Transform visible geometry relative to a nearby camera origin before converting to GPU `Float`.

### Consequences

- Camera mathematics can be unit tested without graphics.
- Absolute world coordinates must not be sent directly to float attributes at extreme magnitudes.
- Double precision still has finite relative precision; large-offset stress tests are required.
- Hierarchical/tiled coordinates remain an option if tests expose limits.

---

## ADR-005 — Stylus-first, finger-navigation default

**Status:** Approved  
**Date:** 2026-11-04

### Context

The primary device is a stylus-capable Android tablet. Precision drawing and comfortable navigation must coexist.

### Decision

Default to stylus drawing and finger navigation. Offer optional finger drawing mode later. Support pressure and eraser identity only when reported by the device.

### Consequences

- Input arbitration must distinguish pointer tool types.
- Palm rejection is best-effort, not guaranteed.
- Multi-pointer transitions and cancellation require explicit tests.

---

## ADR-006 — OpenGL ES 3.0 as the first renderer experiment

**Status:** Provisional  
**Date:** 2026-11-04

### Context

OpenGL ES 3.0 offers a relatively direct path to a first Android GPU renderer and instanced geometry. Vulkan offers more explicit control but introduces additional engineering complexity. ANGLE may translate OpenGL ES to another backend on supported configurations.

### Decision

Use OpenGL ES 3.0 for the first renderer prototype, but keep camera, document, geometry preparation, and rendering contracts independent from the graphics API. Re-evaluate before the renderer becomes difficult to replace.

### Runtime evidence — 2026-11-04

The initial GLSurfaceView prototype successfully initialized on the Samsung Galaxy Tab S10 FE+.

The runtime reported:
- GL vendor: Samsung Electronics Co., Ltd.
- GL renderer: ANGLE ((Samsung Xclipse 540) on Vulkan 1.3.279)
- GLES version: 3.2
- Driver version: 24.1.296
- ANGLE Git hash: bfdb0b875536

ANGLE is confirmed active for this tested configuration. This satisfies the initial graphics capability discovery task but does not resolve the backend performance comparison.

ADR-006 remains provisional until a meaningful stroke renderer and reproducible performance measurements exist.

### Revisit when

- The target device's actual GLES version, renderer, and driver are logged.
- A minimal stroke renderer exists.
- Vulkan or ANGLE can be compared using the same deterministic workload.

### Consequences

- Initial implementation is simpler.
- GLES is not declared the permanent or universally best backend.
- ANGLE must not be assumed available or active.
- Vulkan remains a possible backend if measured benefits justify it.

---

## ADR-007 — Reference hardware is Galaxy Tab S10 FE+

**Status:** Approved  
**Date:** 2026-11-04

### Context

The project needs a real device for stylus, graphics, frame pacing, and memory validation.

### Decision

Use Samsung Galaxy Tab S10 FE+ as the primary reference device. Current user-reported configuration: Exynos 1580, Xclipse 540, 8 GB physical RAM, 4 GB RAM Plus configured, 13.1-inch display, advertised 90 Hz capability.

### Consequences

- Record exact model variant, Android build, refresh mode, GPU renderer string, and thermal state for formal tests.
- RAM Plus is not counted as physical RAM.
- Do not generalize results from this device to all Android tablets.

---

## ADR-008 — Separate spatial organization from temporal drawing order

**Status:** Approved principle  
**Date:** 2026-11-04

### Context

A spatial chunk can contain strokes drawn at different times or in distant areas. Spatial grouping alone must not alter compositing order or reduce culling quality.

### Decision

Keep canonical stroke identity and drawing-order semantics independent from the spatial index. Spatial structures hold references/bounds; render candidates are resolved into valid order before compositing.

### Consequences

- Stable sequence/order keys are likely required.
- A chunk-count threshold alone is insufficient to guarantee good spatial locality.
- Batching must preserve visible compositing semantics.

---

## ADR-009 — No fixed 10^8× zoom acceptance requirement

**Status:** Approved  
**Date:** 2026-11-04

### Context

An earlier proposal used a 10^8× jitter test as an early milestone gate. Such a target is not yet tied to a demonstrated user requirement or a defined coordinate model.

### Decision

Optimize for practical, extremely broad zoom. Use large-scale tests as exploratory stress tests, not release acceptance criteria. Establish product limits from precision, usability, and performance measurements.

### Consequences

- No claim of mathematically infinite zoom.
- Numerical tolerances and tested ranges must be explicit.
- Zoom limits remain internal safety parameters until validated.

---

## ADR-010 — Physical RAM is the memory planning basis

**Status:** Approved  
**Date:** 2026-11-04

### Context

The reference device reports 8 GB physical RAM and 4 GB RAM Plus configured.

### Decision

Treat the device as an 8 GB physical-RAM device. Do not budget RAM Plus as equivalent additional RAM. Measure app process PSS, managed heap, native allocations, and GPU resources where possible.

### Consequences

- Cache and staging memory must be bounded.
- Actual Android memory pressure must inform budgets.
- No fixed app memory ceiling is asserted before profiling.

---

## ADR-011 — Documentation and repository state are the source of truth

**Status:** Approved  
**Date:** 2026-11-04

### Context

The project will use multiple focused conversations. Conversation history alone is not a reliable project database.

### Decision

Maintain the canonical project state in Git. Every substantial implementation change updates relevant documentation, especially `CURRENT_STATE.md`. Architecture changes require a decision record.

### Consequences

- Focused chats must inspect the repository before making assumptions.
- No chat should claim a file was updated unless the resulting repository change is verified.
- Commits and diffs are used to review changes.

---

## ADR-012 — Defer advanced storage and allocator optimization

**Status:** Approved  
**Date:** 2026-11-04

### Context

The initial proposal includes direct buffers, varint compression, slab allocation, LRU eviction, and append-only operation logs before a working renderer exists.

### Decision

Start with simple, testable in-memory structures and a clear persistence abstraction. Adopt off-heap storage, compact encoding, slab allocation, and operation logs only when measurements justify them.

### Consequences

- Initial code is easier to inspect and debug.
- The document model must not depend on the first memory layout.
- Performance work is evidence-driven.

## ADR-013 — Provisional Android API 28 development baseline

**Status:** Provisional
**Date:** 2026-11-04

### Context

The first Android Studio project requires a minimum SDK setting. The project's final distribution target and compatibility requirements have not yet been established.

### Decision

Use Android 9 (API 28) as the provisional minimum SDK for the initial development project.

The M0 renderer requests OpenGL ES 3.0 and declares it as a required feature. The application ID observed in the initial runtime logs is `com.example.infinitepainter`.

### Rationale

This configuration supports development and testing on the primary reference tablet without prematurely establishing the final compatibility policy.

### Consequences

- API 28 is a temporary development baseline, not a permanent product requirement.
- GLES 3.0 is required by the current prototype configuration.
- The application ID should be reviewed before publication.
- The minimum SDK, GPU fallback policy, and distribution compatibility target remain open.

### Revisit when

- The target range of Android devices is defined.
- A meaningful renderer prototype is available.
- Compatibility testing identifies required API or graphics features.

---

## Future ADR template

```markdown
## ADR-NNN — Short decision title

**Status:** Proposed / Approved / Superseded / Rejected
**Date:** YYYY-MM-DD

### Context
What problem or constraint requires a decision?

### Options considered
- Option A
- Option B

### Decision
What are we choosing?

### Rationale
Why is this the best current choice?

### Consequences
What becomes easier, harder, or constrained?

### Revisit when
What evidence would justify changing this decision?
```
