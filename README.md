# InfinitePaper

> An Android-first, offline, vector-based infinite canvas for handwriting, sketching, annotation, and spatial thinking.

**Project status:** Milestone 0 in progress — Android shell and GPU clear-screen prototype implemented 
**Documentation baseline:** 0.1.0  
**Last updated:** 2026-11-04  
**Primary reference device:** Samsung Galaxy Tab S10 FE+ (13.1-inch, Exynos 1580, Xclipse 540, 8 GB physical RAM, 4 GB RAM Plus configured)

---

## 1. Project overview

InfinitePaper is an independent Android application inspired by the interaction model of infinite-paper drawing tools. Its primary objective is to provide a very large, continuously navigable vector workspace with precise stylus input, predictable stroke rendering, and responsive navigation.

The project is not a port of proprietary software. It will implement its own document model, rendering pipeline, interaction model, storage format, and user interface. No proprietary source code, artwork, icons, or other protected assets will be copied.

The first engineering priority is the canvas engine. Product features such as layers, bookmarks, image import, PDF annotation, and document management will be built only after the underlying coordinate, input, rendering, and document systems are validated.

## 2. Product principles

1. **Canvas first.** Navigation, drawing quality, and rendering correctness take priority over interface polish.
2. **Vector-first document model.** User-created strokes are represented as editable geometric data rather than one ever-growing raster bitmap.
3. **Practical scale freedom.** The canvas should support an extremely broad useful zoom range without exposing arbitrary product limits. Numerical limits will be explicit, tested, and documented.
4. **Stylus-native, touch-aware.** A stylus is the primary precision input; fingers provide navigation by default, with optional touch drawing.
5. **Responsive by design.** Input processing, geometry preparation, persistence, and rendering must not block one another unnecessarily.
6. **Bounded resource use.** Caches, temporary buffers, and resident geometry must have explicit budgets and eviction policies.
7. **Offline-first.** Core drawing and document operations must work without network access.
8. **Measured optimization.** Performance claims require reproducible tests on named hardware and software configurations.
9. **Modular engineering.** Camera mathematics, document semantics, spatial indexing, geometry generation, GPU rendering, and persistence should be independently testable.
10. **Documentation is part of the product.** Every meaningful implementation or architectural change must be reflected in the project documentation.

## 3. Current project status

Milestone 0 is in progress. A native Android application and OpenGL ES clear-screen prototype have been implemented and tested on the reference tablet. Camera mathematics, the production stroke renderer, document model, and persistence remain unimplemented.

| Area | Status |
|---|---|
| Product direction | Established |
| Reference device | Established |
| Initial architecture | Proposed; several decisions remain provisional |
| Android project | Created locally; clean GitHub checkout not yet verified |
| Camera implementation | Not implemented |
| GPU renderer | Clear-screen prototype working; stroke renderer not implemented |
| Input pipeline | Not implemented |
| Document model | Not implemented |
| Persistence | Not implemented |
| Automated tests | Not implemented |
| Performance baseline | Not measured |

See [`docs/CURRENT_STATE.md`](docs/CURRENT_STATE.md) for the authoritative next action.

## 4. Target hardware and performance intent

The primary development and validation device is a Samsung Galaxy Tab S10 FE+ with:

- Exynos 1580 system-on-chip.
- Xclipse 540 GPU.
- 13.1-inch display, advertised resolution 2880 × 1800.
- 90 Hz display capability.
- 8 GB physical RAM.
- 4 GB RAM Plus configured.

**RAM Plus is storage-backed virtual memory and must not be counted as 4 GB of additional physical RAM.** The application must be designed and measured against the real memory available to Android and the app process, not an assumed 12 GB physical-memory pool.

The initial performance objective is to make a simple and moderately complex scene feel smooth at the device's 90 Hz refresh mode. A 60 FPS floor is the initial acceptance target for normal interactive drawing under heavier but representative scenes. These are targets, not measured results or guarantees. Actual display mode, frame pacing, thermal state, and workload must be recorded during tests.

## 5. Technology direction

| Area | Initial direction | Status |
|---|---|---|
| Platform | Native Android | Approved |
| Main language | Kotlin | Approved |
| Initial graphics prototype | OpenGL ES 3.0 | Provisional |
| Future graphics option | Vulkan backend | Evaluation required |
| ANGLE | Confirmed active on reference tablet; Vulkan 1.3.279 backend | Observed |
| Rendering host | Android `GLSurfaceView` | Implemented for M0 prototype |
| UI | Native Android UI; keep UI separate from canvas renderer | Approved principle |
| World coordinates | CPU-side `Double` | Approved principle |
| GPU coordinates | Camera-relative `Float` | Approved principle |
| User-created marks | Vector stroke data | Approved principle |
| Spatial index | Quadtree vs spatial hash to be benchmarked | Open |
| Persistence format | Versioned local format; exact design open | Open |

The reference tablet's 2026-11-04 runtime log confirms ANGLE translating OpenGL ES 3.2 to Vulkan 1.3.279 on the Samsung Xclipse 540 GPU. This is an observation for the tested device and configuration, not a guarantee for other devices or a permanent graphics-backend decision.

## 6. Repository structure

```text
InfinitePaper/
├── README.md
├── docs/
│   ├── SPECIFICATION.md
│   ├── ARCHITECTURE.md
│   ├── ROADMAP.md
│   ├── DECISIONS.md
│   ├── BENCHMARKS.md
│   └── CURRENT_STATE.md
└── app/
```

The Android application was generated locally using Android Studio. The project currently uses Kotlin, a native Activity, and GLSurfaceView. The local application ID observed in Logcat is `com.example.infinitepainter`, and the provisional minimum SDK is API 28 (Android 9). The generated project has not yet been verified in a clean checkout from GitHub.

### Documentation authority

- `SPECIFICATION.md` defines what the product must do.
- `ARCHITECTURE.md` defines subsystem boundaries and technical design.
- `ROADMAP.md` defines implementation sequence and exit gates.
- `DECISIONS.md` records accepted and superseded architectural decisions.
- `BENCHMARKS.md` records test methods, device conditions, and measured results.
- `CURRENT_STATE.md` records what exists now and the next concrete task.

If a chat discussion conflicts with a committed document, do not silently choose one. Record the discrepancy and resolve it through a documented decision.

## 7. Development workflow

1. Work in small, independently testable increments.
2. Inspect the current repository and relevant files before proposing edits.
3. Make changes in a focused branch or commit-sized unit.
4. Run the narrowest relevant tests, then broader regression tests.
5. Update documentation in the same change as the implementation.
6. Record benchmark results only when actually measured.
7. Review the diff before committing or merging.
8. Update `CURRENT_STATE.md` at the end of each substantial work session.

Recommended commit style:

```text
docs: establish initial engineering specification
feat(camera): add focal-point-preserving zoom
test(camera): cover coordinate round trips
perf(renderer): add instanced segment benchmark
```

## 8. Definition of done

A task is not complete merely because code compiles. Its completion criteria should include:

- Implementation matches the documented contract.
- Relevant tests pass.
- Failure and lifecycle cases have been considered.
- No unrelated regression is introduced.
- Documentation reflects the actual implementation.
- Performance claims include a reproducible measurement where relevant.

## 9. External technical references

These are starting points for engineering research; verify current guidance when implementation begins.

- [Android OpenGL ES documentation](https://developer.android.com/develop/ui/views/graphics/opengl/about-opengl)
- [Android stylus input documentation](https://developer.android.com/develop/ui/views/touch-and-input/stylus-input)
- [Advanced stylus features](https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/advanced-stylus-features)
- [Android Macrobenchmark frame timing](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics)
- [Android Vulkan documentation](https://developer.android.com/games/develop/vulkan)
- [ANGLE project](https://angleproject.org/)
- [Samsung Galaxy Tab S10 FE series](https://www.samsung.com/)

---

## 10. License and project identity

The project name is a working name. Before public distribution, review naming and trademark considerations. Choose and add an open-source license before accepting outside contributions or publishing source code as an open-source project. No license is asserted by this initial documentation package.
