# Performance and Correctness Benchmarks

**Document ID:** IP-BENCH-001  
**Version:** 0.1.0  
**Status:** Test plan; graphics initialization observation recorded; performance benchmarks pending 
**Last updated:** 2026-11-04

## 1. Purpose

This document defines how InfinitePaper performance and numerical correctness will be measured. It intentionally contains no fabricated results. All result fields remain `NOT MEASURED` until tests are run on the target device.

A benchmark result without device, build, scene, and test conditions is not considered reproducible.

## 2. Reference test device

| Field | Value |
|---|---|
| Device | Samsung Galaxy Tab S10 FE+ |
| SoC | Exynos 1580 |
| GPU | Xclipse 540 |
| Physical RAM | 8 GB |
| RAM Plus | 4 GB configured; not physical RAM |
| Display | 13.1-inch class |
| Advertised resolution | 2880 × 1800 |
| Advertised refresh capability | Up to 90 Hz |
| Android version/build | NOT RECORDED |
| App build/commit | Local development build; commit not recorded |
| Application ID | com.example.infinitepainter |
| GLES version | OpenGL ES 3.2 |
| GL vendor | Samsung Electronics Co., Ltd. |
| GL renderer | ANGLE ((Samsung Xclipse 540) on Vulkan 1.3.279) |
| GL driver version | 24.1.296 |
| GLSL version | OpenGL ES GLSL ES 3.20 |
| ANGLE status | Active in tested configuration |
| ANGLE Git hash | bfdb0b875536 |
| Vulkan support/driver | Vulkan 1.3.279 reported as ANGLE backend; native Vulkan capability not separately queried |
| Observed viewport | 2880 x 1800 |
| Thermal state | NOT RECORDED |

Before formal performance runs, capture the actual runtime values. Do not infer ANGLE use from the presence of Vulkan support.

## 3. Frame-time reference

A 90 Hz display has a nominal frame interval of approximately 11.11 ms. A 60 Hz display has a nominal interval of approximately 16.67 ms.

| Refresh target | Nominal frame interval |
|---|---:|
| 90 Hz | 11.11 ms |
| 60 Hz | 16.67 ms |

The app must record frame-time distributions rather than relying on average FPS alone. At minimum, record P50, P90, P95, and P99 where the measurement API supports them. Also record missed frame deadlines or frame overruns when available.

A 90 Hz panel does not guarantee the system is currently operating at 90 Hz. Record the active display mode and any adaptive refresh behaviour.

## 4. Benchmark environment protocol

For each formal run:

1. Record device model and exact variant.
2. Record Android version, build fingerprint, and security patch level.
3. Record app version, Git commit, build type, and build configuration.
4. Record actual display mode/refresh rate.
5. Record runtime graphics API, GL version, vendor, renderer, and driver information.
6. Record whether ANGLE is active, if this can be determined reliably.
7. Record battery level and thermal state.
8. Allow the device to reach a repeatable thermal condition.
9. Close or record significant background workloads.
10. Run the same deterministic scene and camera path.
11. Repeat enough times to identify run-to-run variation.
12. Preserve raw output, not only a summary number.

Do not compare a cool-device run with a thermally throttled run as if they were equivalent.

## 5. Correctness benchmark suite

### C-001: Coordinate round trip

- Generate world points at ordinary, large, and very large offsets.
- Convert world to screen and back.
- Measure error in screen pixels and world units.
- Include points near viewport edges and center.
- Result: NOT MEASURED.

### C-002: Focal-point zoom invariance

- Choose arbitrary screen focal points.
- Record the world coordinate beneath each point.
- Apply zoom in/out.
- Verify the same world coordinate maps back to the original screen point.
- Include zoom sequences and viewport resize.
- Result: NOT MEASURED.

### C-003: Repeated zoom cycle drift

- Apply a deterministic sequence of zoom-in and zoom-out factors.
- Return to the original scale.
- Measure camera-center and landmark drift.
- Result: NOT MEASURED.

### C-004: Large-offset precision

- Place geometry at large world offsets.
- Move camera by small increments.
- Verify visible geometry remains stable.
- Sweep tested coordinate magnitudes and zoom values.
- Result: NOT MEASURED.

### C-005: Stroke geometry continuity

- Test horizontal, vertical, diagonal, near-zero-length, sharp-turn, and pressure-varying strokes.
- Inspect caps, joins, seams, and clipping.
- Result: NOT MEASURED.

### C-006: Draw-order correctness

- Create overlapping opaque and translucent strokes.
- Query through the spatial index.
- Verify the rendered result matches canonical document order.
- Result: NOT MEASURED.

### C-007: Context loss and surface recreation

- Pause/resume the app.
- Recreate the rendering surface.
- Verify all derived GPU resources can be rebuilt.
- Result: NOT MEASURED.

## 6. Performance benchmark suite

### P-001: Empty frame baseline

Measure renderer overhead with no document geometry.

Record:
- CPU frame time.
- GPU frame time where available.
- Frame duration distribution.
- Power/thermal observations if available.

Result: NOT MEASURED.

### P-002: Single active stroke

Draw a continuous stylus stroke with representative point sampling. Measure:
- Input event processing time.
- Geometry preparation time.
- CPU/GPU frame time.
- Input-to-visible latency where feasible.
- Allocation rate during inking.

Result: NOT MEASURED.

### P-003: Segment scaling

Render increasing segment counts with fixed viewport and shader configuration. Record:
- Segment count.
- Draw calls.
- CPU submission time.
- GPU time.
- Frame-time distribution.

Result: NOT MEASURED.

### P-004: Spatial query scaling

Use deterministic documents at increasing stroke counts. Record:
- Total strokes.
- Points per stroke distribution.
- Spatial distribution.
- Visible fraction.
- Query time.
- Candidate count.
- False-negative count (must be zero).

Result: NOT MEASURED.

### P-005: 100,000-stroke synthetic document

The phrase “100k strokes” is not a complete benchmark definition. Record at least:
- Stroke count.
- Total point count.
- Median/P95 points per stroke.
- Spatial distribution.
- Percentage intersecting viewport.
- Camera path.
- Brush width distribution.
- Alpha/transparency mix.
- Whether geometry is warm or cold in cache.

Result: NOT MEASURED.

### P-006: Continuous pan and pinch

Replay a deterministic camera path for at least several minutes. Record:
- Frame timing distribution.
- Query time.
- Geometry rebuild count.
- Upload bytes/frame.
- Queue depth.
- Cache hit/miss rate.
- Memory trend.

Result: NOT MEASURED.

### P-007: Long-session memory stability

Run continuous drawing and navigation for a defined duration. Record:
- Java/Kotlin heap.
- Native heap.
- Process PSS.
- GPU memory where available.
- Cache resident bytes.
- Temporary buffer bytes.
- GC count and pause time.
- Memory pressure events.

A stable heap alone does not prove stable total memory use.

Result: NOT MEASURED.

### P-008: Save/load and recovery

Once persistence exists, record:
- Document size.
- File size.
- Save latency.
- Load latency.
- Peak memory during save/load.
- Recovery outcome after forced termination.
- Data integrity check.

Result: NOT MEASURED.

## 7. Initial performance goals

These are provisional engineering targets, not benchmark results:

| Workload | Initial goal |
|---|---|
| Empty/simple scene | Smooth at active 90 Hz mode |
| Normal drawing | At least 60 FPS in representative workloads |
| Dense document | Establish measured target after baseline |
| Input hot path | Avoid avoidable allocations and blocking |
| Memory | No unbounded growth during long-session test |
| Spatial query | No false negatives; bounded query cost |
| Cache | Explicit byte budget and observable eviction |

The earlier proposal's “90 FPS” and “p99 below 8.3 ms” were not equivalent requirements: 8.3 ms corresponds to approximately 120 Hz. The project uses the actual target display interval and frame-overrun measurements instead of mixing thresholds.

## 8. Results log

The first runtime graphics capability observation was recorded on 2026-11-04. No formal frame-time, GPU throughput, memory, or latency benchmarks have been completed.

| Date | Commit | Test ID | Device/build | Result | Notes |
|---|---|---|---|---|---|
| — | — | — | — | NOT MEASURED | Baseline pending |

### 2026-11-04 — M0 graphics initialization observation

**Category:** Functional smoke test; not a performance benchmark.

- Device: Samsung Galaxy Tab S10 FE+
- Application ID: com.example.infinitepainter
- Build: Local development build; Git commit not recorded
- Test: Initialize GLSurfaceView, create a GLES 3.0 context, query graphics strings, and clear the framebuffer
- GL vendor: Samsung Electronics Co., Ltd.
- GL renderer: ANGLE ((Samsung Xclipse 540) on Vulkan 1.3.279)
- GL version: OpenGL ES 3.2
- Driver version: 24.1.296
- GLSL version: OpenGL ES GLSL ES 3.20
- ANGLE Git hash: bfdb0b875536
- Observed viewport: 2880 x 1800
- Result: Graphics context initialization confirmed by Logcat; user reports successful on-device execution
- Frame timing: NOT MEASURED
- GPU timing: NOT MEASURED
- Active display refresh rate: NOT RECORDED
- Android OS build: NOT RECORDED
- Thermal state: NOT RECORDED
- Context recreation: NOT TESTED

This observation does not establish a 60 FPS or 90 FPS performance baseline. It confirms only the initial graphics capability and initialization path.

## 9. Result entry template

```markdown
### YYYY-MM-DD — TEST-ID

- Commit:
- App build:
- Device:
- Android build:
- Active refresh mode:
- Graphics API:
- GL vendor / renderer / version:
- ANGLE status:
- Thermal state:
- Scene definition:
- Run count:
- P50:
- P90:
- P95:
- P99:
- Frame overruns / missed deadlines:
- Memory measurements:
- Observations:
- Raw artifact location:
```

## 10. Tooling candidates

- Android Studio Profiler for CPU, memory, and allocation inspection.
- Android Macrobenchmark / FrameTimingMetric for repeatable frame metrics where applicable.
- Perfetto / system tracing for scheduling and frame-pipeline investigation.
- GPU/vendor tooling where available for the target device.
- Custom deterministic geometry and camera-path generator.

Tool choice must be confirmed against current Android and device support when the benchmark harness is implemented.
