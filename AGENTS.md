# AGENTS.md — Kinetix Buyer Android Application (`kinetix-android`)

This file is the system design, architectural contract, coding standard, gate specification, backend contract digest, phased plan and decisions log for **`kinetix-android`**: the customer-facing native Android application of Kinetix (display name **`Kinetix`**, package `com.kinetix.shop`), the sibling of `kinetix-ios`. Antigravity loads it automatically inside this repository; `CLAUDE.md` is a pointer to it (AND-D17).

The estate forbids comments in source code, so every design decision, protocol invariant and failure classification lives here and in test names. The file is longer than one tool read returns: read it to the end, in as many reads as that takes. §0 comes first on purpose.

**Authority.** The user in chat; then this file; then the workspace `CONTEXT.md`; then an agent memory note. For what the backend does, a service's source wins, then the published reference built from it (`kinetix-docs`, §4), then this file: where they disagree this file is stale, so stop and correct it before writing Kotlin. For how the gateway behaves, its live configuration (`kinetix-api-gateway/config/envoy/`) wins over the published page, which still describes the previous engine.

**Provenance.** First written by the agent that built Phases 0 to 2. Reviewed and rewritten on 2026-11-19 at the owner's request: four reviewers (backend contracts twice, the Android platform and the code, the estate's rules and the plan) read this file **and the Kotlin built from it** against service source, and every blocker and major finding was re-derived by a second reader before it changed this text. Nothing was built or run during that review (no Gradle, no emulator): a claim about the code is *read*, not *run*, unless it says otherwise.

**Two kinds of statement.** §1 is the design the app must reach. Where the code differs today the text says **Today:** and §5 Phase R schedules the repair. Do not read §1 as a description of what exists.

---

## 0. State and log — read first

### 0.1 Where the repository stands

Read from disk on 2026-11-19.

| Fact | Value |
| :--- | :--- |
| Branches | `development` at `af07edc` (pushed); `main` at `b553cfe`; no tag |
| Build | `minSdk` 26, `targetSdk` and `compileSdk` 37, Java 17 source/target (Gradle and CI run on JDK 25 via foojay resolver; `gradle.properties` sets `android.disallowKotlinSourceSets=false` and suppresses its warning); one `:app` module; dependency locking on for `:app` (the plugin classpath is not locked; Dependabot ignores `com.google.devtools.ksp` with no recorded reason) |
| Source | 247 Kotlin files under `app/src/main/java/com/kinetix/shop` |
| Tests | 30 test classes with **62 `@Test` functions**, 27 hand-written fakes; all JVM unit tests. One instrumented test, which asserts the package name |
| Built | Phase 0 (foundation), Phase 1 (identity, session, 2FA, profile), Phase 2 (catalogue, search, promotions, reading reviews), and the "Step 2" foundation (Room, outbox, WorkManager, a trace header, Firebase adapters) |
| Known broken | 29 confirmed code-defect findings (7 blockers, 22 majors; 11 blockers of every kind in §0.2). **Phase R repairs them before any new feature** |
| Never run | No launch on a device or emulator is recorded for any phase. Nothing Android-specific has a test: the Keystore cipher, Room and its DAOs, WorkManager, the Hilt graph at run time, the messaging service, every composable |
| Firebase | `app/google-services.json` exists nowhere, so the Firebase adapters have never been configured or observed working |
| Release build | R8 optimises only `androidx`, `kotlin` and `kotlinx` (`optimization.packageScope`). The app's own code and every other library are neither shrunk nor renamed: a green `assembleRelease` proves packaging, not the app under R8 |
| Gate | `ArchitectureAndLayoutGateTest` has six checks (§3.2). Several rules earlier marked "(Gate)" were never enforced by it |

### 0.2 What the 2026-11-19 review found

Eleven blockers and 61 majors, all confirmed. The ones a buyer would hit in code that is already written:

- **A weak network signs the buyer out.** Whether a refresh "left the phone" is guessed from the exception class, and a connect timeout, a TLS-handshake timeout and a call timeout all count as sent: the session is wiped though the token was never transmitted.
- **A refresh can be sent twice.** It runs in its caller's coroutine, so rotating the phone or leaving a screen cancels the request after identity may have rotated the token; the next attempt presents a spent token and identity ends the session.
- **The outbox loses edits and says they were saved.** A second offline profile edit replaces the first; an offline save is reported as "updated"; an entry interrupted in flight is never listed again; and the WorkManager worker sends nothing when it starts the process itself, because only the UI restores the session.
- **The store called durable is not.** One Room database, version 1, schema not exported, built with a destructive fallback: the first schema change drops the outbox and the pending-transaction rows on every installed device. That store also keeps a fingerprint instead of the request body and one row per operation, so it cannot do what checkout and top-up need.
- **The device-ownership challenge can never be recognised.** The handler reads `type`, `challengeId`, `challengeToken`; notification sends `kind`, `nonce`, `expires_at`. Its test feeds it the invented keys.
- **A bad configuration crashes instead of showing `ConfigErrorScreen`**: Hilt builds Retrofit while injecting the activity.
- **The search screen's "Price ↓" chip always fails**: search accepts `relevance`, `newest` and `price_asc` only.
- **"No flash sale" is an error internally**, and three DTO fields are required where the service sends `null` or nothing, so a platform flash sale is never shown while checkout would charge it.
- **For most failures the buyer reads server text or a Java exception message**, not a string resource.
- **The feature flags switch nothing** (never fetched, four of five never read); the trace header is read by nothing in the estate; cached answers are shown as live.
- **This file itself was wrong where it mattered**: the cart voucher body, the checkout classification, the gateway's behaviour, and the claim that every contract below was verified.

### 0.3 Log

History is kept; newest last. Corrections made by the review are marked.

**2026-11-17 — Phases 0 to 2 built** (`main`, `b553cfe`, 39 tests). Foundation, identity with 2FA and profile, catalogue, search, promotions, reading reviews.

**2026-11-19 — "Step 1": audit hardening, Navigation Compose, CI hygiene** (`64fb077`, 52 tests). The profile `404` treated as "no profile yet"; `NavHost` with `@Serializable` routes; search debounce; the home reload guard; dependency locking; GitHub Actions CI. *Correction:* `AppRouterState` was replaced by the `NavHost` but is still on disk with its test; the reload guard covers the loaded state only.

**2026-11-19 — Repository governance and CI/CD** (`8024b3b` through `a9e80fb`, 55 tests; dependency updates in `f7f4a7e`). `.coderabbit.yaml`, Dependabot, the pull request template, `.gitlab-ci.yml`, `release.yml`, the `verify` aggregator job, Trivy and an SBOM (`6ceff3e`), the Bouncy Castle substitution for `CVE-2026-8763` (`5d37d2f`). *Correction:* the release workflow does not check that a tag is on `main`, does not take the version from the tag and does not provide `google-services.json`; the GitLab pipeline has never been observed to run.

**2026-11-19 — "Step 2": Room, outbox with WorkManager, trace header, Firebase adapters** (`af07edc`, 62 tests). *Correction, by what the code does:* built and exercised are the catalogue and profile caches and the profile outbox path, with the defects of §0.2. Built and unproven, to be rebuilt with their first consumer, are the pending-transaction store, the push handler and token store, the feature flags and the analytics and crash identity calls. The entry's claims of "durable storage", "permanent 4xx failure isolation", "capturing `DEVICE_VERIFICATION` challenges", "kill switches" and "distributed tracing" do not hold. The "Phase" and "Step" numberings of the old plan are replaced by §5.

**2026-11-19 — Two user rulings that bind both buyer apps.** Multilingual buyer copy (AND-D10) and the notary wallet signing page (AND-D11), recorded in `kinetix-ios/AGENTS.md` as IOS-D9 and IOS-D14.

**2026-11-19 — Reviewed and rewritten.** This file was restructured (state and log first, a system design, rules with honest gate marks, a corrected contract digest, a repair phase, a decisions register). No Kotlin was changed. The section that held the log was §5 and is now §0.3.

**2026-11-20 — The rewrite was rechecked, partly.** Three of five reviewers finished (contracts twice, claims about the code), each blocker and major finding re-derived against files; the other two were cut for cost and never ran. Result: no blocker, four majors, 32 minors, and the rewrite's facts held almost everywhere. Five small corrections were applied at once (the release list now names matching and seal, `email` in the `me` answer, `Retry-After` on identity's per-address `429`, the 2FA `401` wording, the profile `409` without its hedge). Everything else found is listed in §0.5 for whoever continues. No Kotlin was changed.

**2026-11-20 — Open corrections from §0.5 applied.** Applied the 4 major findings, contract minors, and code-claim corrections from the 2026-11-20 recheck to §0.1, §0.3, §1, §2, §3.2, §4, §5, and §7. No Kotlin was changed.

**2026-11-20 — Phase R1 (Start-up) & R2 (Correlation) completed.**
- R1: `MainActivity` and `OutboxFlushWorker` now inject `AppConfigCheckResult` and `dagger.Lazy<SessionRepository>` / `dagger.Lazy<OutboxProcessor>` so Retrofit is never initialized on an invalid config; `AppConfigValidator` catches `URISyntaxException` (test `rejectsMalformedUriSyntaxWithoutThrowing` seen red before fixing); added `network_security_config.xml` for `main` (cleartext refused) and `debug` (trusts user/system CAs for lab TLS); added `verifyReleaseConfig` task hooked to `preReleaseBuild` refusing invalid/missing release URLs.
- R2: Replaced `W3cTraceparentInterceptor` / `TraceIdGenerator` with `RequestCorrelationInterceptor`, `RequestIdGenerator` (`UuidRequestIdGenerator`), and `RequestCorrelationRecorder` in `core/network/` (removing the upward import to `services/telemetry`); injects `X-Request-Id` and `Accept: application/json`, records `last_request_id` and Retrofit `Invocation` route template (never `encodedPath`) as crash breadcrumbs, and attaches `traceId`/`X-Request-Id` to HTTP-mapped `Failure`s.

**2026-11-21 — Phase R3 (Session) completed.**
- Added `@IoDispatcher`, `@DefaultDispatcher`, and `@ApplicationScope` qualifiers in `core/concurrency/` provided by `CoreModule`; moved `EncryptedFileTokenStore` file I/O and cipher calls off the main thread onto `@IoDispatcher` and enabled `StrictMode` for disk/network on the main thread in debug builds (`KinetixApplication`).
- Added four-state `SessionState` (`SessionRestoring`, `SessionSignedOut`, `SessionSignedIn`, `SessionUnreadable`) owned by `SessionRepositoryImpl` on `@ApplicationScope`, updated `AppRootScreen` to render loading on `SessionRestoring`, redirect only on `SessionSignedOut`, and prompt without clearing on `SessionUnreadable` (removed `rememberSaveable sessionRestored`).
- Added per-call `RefreshWireEventListener` and `OkHttpRefreshWireExecutor` (`retryOnConnectionFailure(false)`) so "left the device" is determined strictly from wire events (`requestHeadersStart` or no events) rather than exception classes; classified refresh responses strictly by `(status, errorCode)` including the five client-proof answers (kept and reported) and `403`/`500`/`504` as unknown; made `SingleFlightSessionRenewer` run on `@ApplicationScope` and wipe tokens if persisting a rotated `200` pair fails.
- Updated `SessionRepositoryImpl.restoreSession()` so only a second `401` ends the session, `verifyMe` only shortens expiry and stores `role`, `AuthRepositoryImpl` persists `role` on all session answers, and `signOut()` renews when ≤60s remain, calls `POST /api/v1/auth/logout` via `SafeApiCaller.executeNoContent` inside the 5s budget with one gateway-`401` retry, records an unconfirmed breadcrumb when not `204`, and clears storage.
- Tests seen red first (`restoreSessionKeepsRenewedSessionWhenSecondMeFailsWithNetworkError`, `restoreSessionOnlyShortensExpiryFromMeAndNeverExtendsIt`, `clearsSessionWhenTokenWriteFailsAfter200Refresh`), then verified green alongside new `RefreshWireEventListenerTest` and expanded `RetrofitTokenRefreshTransportTest`, `SingleFlightSessionRenewerTest`, and `SessionRepositoryImplTest`.

### 0.4 Owed

- **By the user:** AND-D1 (Hilt against the estate's no-container rule), AND-D2 (Gradle on the host against everything-in-Docker), AND-D3 (which Firebase products ship, consent, a user id), AND-D12 (two accents and a shape scale), AND-D13 (the second test layer), AND-D14 (warnings as errors), AND-D16 (the map), AND-D17 (`CLAUDE.md`); overruling AND-D6, AND-D7 or AND-D8 if wanted; registering `com.kinetix.shop` in the Firebase project notification sends through.
- **By owning services:** §7.
- **In the workspace:** a `kinetix-android` row in `CONTEXT.md` §2.3 and §2.4.

### 0.5 Open corrections from the 2026-11-20 recheck — to apply, then delete each line

**Not run, still owed.** (1) Lens "Android platform": is §1 implementable with the declared library versions (`gradle/libs.versions.toml`, `app/gradle.lockfile`; sources in `~/.gradle/caches/modules-2/files-2.1/`)? Questions: does OkHttp's default `retryOnConnectionFailure` re-send a refresh or a `POST /cart/items` behind the app's back (§1.7 says a refresh token is never sent twice; §1.8 says an add must not repeat), and what must §1.7 require (a dedicated client, `retryOnConnectionFailure(false)`)? Is `requestHeadersStart` the right "left the device" boundary across retries and HTTP/2? Does `Response<JsonElement>` decode a `200` whose body is `null`, and `Response<Unit>` a `204`? How often does a WorkManager or FCM-service renewal (AND-D8) end "unknown" and sign the buyer out unseen? Keystore failure modes for the "Unreadable" state; Room test layers (AND-D13); the exact Compose names for keyboard options and autofill at the declared BOM; obligations at the declared `targetSdk` the file omits; R8 keep rules (AND-D15). (2) Lens "coverage": for each of the 11 blockers and 61 majors of the first review (JSON, `.result.lenses[].findings[]`, in the session transcript `/Users/super/.claude/projects/-Users-super-Development-real-time-ecommerce-app/570e444a-569f-4ef0-9ff0-0722476896f4.jsonl`; the task output file under `/private/tmp` may be gone) check that the rewrite still prevents the defect; check every reference resolves; check against `kinetix-ios/AGENTS.md`.

**Corrections owed in other files:**
- `kinetix-ios/AGENTS.md`: §6.3 (trace header: Android decided `X-Request-Id` only, AND-D9; "cart writes offline" and "unfinished checkout after a kill": Android now agrees, AND-D6, AND-D7; Hilt vs hand-wired stays) and §7 Q8 (three Android defects, now scheduled in Phase R) are stale; the refresh table's client-proof rows (iOS clears the session on any `400`/`401`; Android keeps it on the five client-proof answers); the 10-a-minute throttles on refresh, logout and `me`; search `sort`, `limit` and 256-byte rules; "order lets any role use the cart"; `FIELD_TOO_LONG`; no `403` on refresh. After editing, `cp AGENTS.md CLAUDE.md` and prove it with `cmp`.
- Workspace `CONTEXT.md` §2.3, §2.4 and §3: rows for `kinetix-ios` and `kinetix-android`. Agent memory: note the review outcome in `kinetix-ios-app-plan.md` and add a `kinetix-android-doc-rewrite` note.
- Git is the user's: nothing here is committed, including this file, `CLAUDE.md` and `.github/PULL_REQUEST_TEMPLATE.md`.

---

## 1. System design

### 1.1 Where the app sits

```text
+------------------------- Kinetix for Android (com.kinetix.shop) -------------------------+
|  Compose screens -> view models -> repositories -> services (HTTP, socket, Room, Keystore) |
+--------+--------------------------------+-----------------------------------+-------------+
         | HTTPS, Bearer JWT              | WSS, Bearer JWT                   ^ FCM
         v                                v                                   |
+--------------- api.wildanfrananda.my.id, 443, TLS only ----------------+    |
|  kinetix-api-gateway (Envoy)                                            |   Firebase Cloud
|    /api/v1/...    -> identity, catalog, search, pricing, order,         |   Messaging
|                      payment, review, notary, notification              |    ^
|    /ws/v1/events  -> kinetix-edge-service (unified event socket)        |    |
+------------------------------+------------------------------------------+   kinetix-notification-service
                               | Redis Pub/Sub (courier positions)             (FCM HTTP v1)
                    kinetix-matching-service
```

One public host serves the API under `/api/v1/` and the event socket at `/ws/v1/events`; port 80 is closed. The gateway engine has been Envoy since 2026-11-14 while the published reference still says Kong: a gateway answer is classified by its status and headers, never by its body.

### 1.2 Boundaries the app never crosses

- **One door for the API.** Every call the app's own code makes to Kinetix goes to the configured API host over TLS. Three kinds of traffic leave by other doors: images, fetched from the absolute `https` URL an answer carried (an empty or non-`https` `image_url` is no image, never a decoding failure); the Firebase SDK's own traffic; chat, when it ships, as MQTT over TLS to EMQX.
- **The backend owns every business rule and every authoritative state**: the price paid, stock, wallet balance, voucher grants, order status. The app shows indications and server answers. It computes no total and no shipping estimate.
- **A client, not a coordinator.** A capability that is missing is built in its owning service first and recorded in §7; it is never approximated here (the "Price ↓" chip is the example of what not to do).
- **Unknown is never a business answer.** "We could not ask" stays its own state all the way to the screen: no cached stock count, no "updated" banner for a write that was not sent.
- **No feature ahead of its backend, and no infrastructure ahead of its feature.** Infrastructure lands in the slice that first uses it, with a test seen red. A handler that answers a fixed failure is a skeleton and is forbidden.
- **No secret in the binary or the repository.**

### 1.3 Layers, packages and dependency direction

Layer packages with feature subfolders in one `:app` module ("Option A"), and **no UseCase layer**: view models depend on repository interfaces for data and on service interfaces only for imperative platform actions, as `kinetix-courier-mobile` does.

```text
ui (screens, components, theme)       ->  viewmodels, state, routes, models, core
viewmodels                            ->  repositories (interfaces), state, models, core (plus service interfaces only for imperative platform actions)
state                                 ->  models, core
routes                                ->  models
repositories (implementations)        ->  services, dto, models, core
services                              ->  dto, models, core
dto, models, core                     ->  nothing of ours (move ApiErrorResponseDto into core/network or name it the one exception)
di, KinetixApplication, MainActivity  ->  may import anything
```

- An arrow is the only allowed import direction. **Today:** three breaches remain: `services/outbox/OutboxProcessor` imports a repository, `core/network` imports `dto.common` (`ApiErrorParser`), and `AppRootScreen` and `MainActivity` import `SessionRepository`; all three are cycles/breaches to remove, and the gate checks folders, never imports.
- Dependency injection is Dagger Hilt today (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, two `SingletonComponent` modules, `HiltWorkerFactory`). The estate's rule is one hand-wired composition root and no container; Hilt here is an **unrecorded exception awaiting the user's ruling** (AND-D1). Until it is ruled: Hilt stays, domain interfaces carry no Hilt annotation, and tests build everything through constructors with fakes.

### 1.4 Concurrency and threading

- **Every suspend function is main-safe.** Blocking work (files, the Keystore, `SharedPreferences`, hashing, large decoding) runs on an injected dispatcher (one qualifier for IO, one for default), never on a hard-coded `Dispatchers` reference. Room and Retrofit suspend calls are already main-safe.
- **One application-scoped coroutine scope (a `SupervisorJob`) owns the session**: restore, refresh, persisting a rotated pair, sign-out. Callers await it and cannot cancel the request or the write.
- Screen work runs in `viewModelScope`; a superseded load is cancelled; a load in flight is joined, never duplicated.
- A cancelled coroutine is never reported as a failure, and `CancellationException` is never swallowed.
- Time is a port (`Clock`); countdowns, backoff and polling never sleep in tests. Debug builds enable `StrictMode` for disk and network on the main thread.

### 1.5 Screens, state, navigation and process death

- A screen is a top-level `@Composable fun XScreen(...)` that renders one sealed UI state with an exhaustive `when`, collected with `collectAsStateWithLifecycle()`.
- **A view model never receives a lambda, a `NavController`, a `Context` or a string resource.** An outcome that must navigate is state, consumed by the route composable, which then tells the view model it was handled. **Today:** three view models take navigation lambdas and call them after the request returns; after a rotation the lambda belongs to a dead composition and the screen does not move.
- **A view model loads once**: content already held is kept on re-entry, a reload is explicit, and a load in flight is joined. **Today:** only `HomeViewModel` guards a reload, and only once loaded; `ProductDetailScreen` and `ProfileScreen` reload on every entry and rotation.
- Navigation is Navigation Compose: `NavHost`, `@Serializable` routes, a view model per back-stack entry. No placeholder route. **A route never carries a credential or a token.** **Today:** the 2FA challenge token is a route argument and is written into saved state.
- **The session is a four-state value** owned by the application-scoped session component: `Restoring`, `SignedOut`, `SignedIn`, `Unreadable`. Whether restore has finished is process state and is never saved. A destination that needs a session shows a neutral loading state while `Restoring` and redirects only on `SignedOut`.
- **What survives process death:** the back stack and route arguments, the pending target of a sign-in, non-secret form text. **What does not, by design:** a password, a code, a token (never in `rememberSaveable`, `SavedStateHandle` or a route). **Today:** the password (`SignInScreen`, `RegisterScreen`, `TwoFactorSettingsScreen`) and the code (`TwoFactorVerifyScreen`, `TwoFactorSettingsScreen`) are `rememberSaveable`. **What is lost today and must be decided per screen:** all view-model state (no `SavedStateHandle` is used anywhere).
- **Every text field declares its keyboard type, IME action and autofill content type.** Password: `KeyboardType.Password`, autocorrect off, the password content type (new password on registration). Email: `KeyboardType.Email` with the username content type. Codes: a numeric keyboard with the one-time-code content type. **Today:** no field sets `KeyboardOptions`, so the keyboard may learn the password.
- Edge-to-edge is on: every screen handles the status bar, the navigation bar and the IME insets, and uses `SystemBarStyle.dark(...)` for both bars. **Today:** the top bar is drawn under the status bar, and `enableEdgeToEdge()` uses the default bar style (following the phone's light or dark setting while the app is dark only).
- **A `Failure` carries a type, an optional machine code and typed fields; it never carries display text.** Server messages and exception messages are diagnostics for the log, never the screen. `FailureMessageMapper` maps type and code to a string resource, exhaustively. **Today:** for twelve of eighteen failure types the mapper shows the server's sentence or the exception's message.

### 1.6 Networking and the classification of failure

- Retrofit interfaces in `services/remote/` return `Response<T>`; `kotlinx.serialization` decodes. OkHttp timeouts are named constants in `NetworkPolicy`.
- Every request carries `Accept: application/json` and **`X-Request-Id`** (a fresh UUID), which is the breadcrumb and crash key and is kept on every typed failure (the body's `traceId` when present, else the response header). `Authorization: Bearer` and `Idempotency-Key` only where §4 says. No `X-Kinetix-Client` today (§7 Q5). **No `traceparent`: nothing in the estate reads it.**
- **Three types may catch a transport error or read a status:** `SafeApiCaller`, the refresh transport, and the event socket client for its handshake and frames. `SafeApiCaller` and the socket client map through `HttpFailureMapper`; the refresh transport shares only `ApiErrorParser`; none repeats the table. A repository may add an endpoint's own answers (for example `409` on register) and nothing else.
- Repositories return `Result<T>` (`Ok` or `Err<Failure>`) or typed flows and never throw to a view model.
- **Classify by (status, the service's error code, whether the body is that service's JSON):** gateway JSON answers carry only `message` (and `request_id` on its `403`); a service's own JSON error carries `error` (or `code` for payment), or is order's `application/problem+json`.

| What happened | Classified as | Never as |
| :--- | :--- | :--- |
| No route, DNS failure, connection refused, TLS failure | network failure, not sent | a refusal |
| Timeout or connection lost after request bytes were written | outcome unknown | success or refusal |
| `502`, `503`, `504` whose body is **not** the service's JSON error (the gateway's) | outcome unknown on a write, unavailable on a read | a refusal, empty, zero |
| A service's own JSON error, whatever its status | what §4 says for that code | a generic failure |
| `429` | wait `Retry-After` when present, else `x-ratelimit-reset` seconds when present, else a named default of 60 s | a wrong password |
| `401` on a route whose contract has a `401` of its own (`login`, `2fa/verify`, `refresh`, the three 2FA management routes, `logout`) when the body is the service's JSON | that endpoint's answer (on 2FA management routes and `logout`, a gateway `{ message: "Unauthorized" }` is an expired/unknown-key token and is renewed once) | session expired |
| `401` on any other route | renew once (§1.7), repeat once; a second `401` is session over | the endpoint's answer |
| `204`, or a `2xx` with no body where none is documented | success | an unreadable answer |
| A `2xx` the DTO cannot decode | unreadable answer | a default value |

  **Today:** every `502`/`503`/`504` is one failure whatever its body; a `500` is not retryable; every `204` is reported as unreadable.
- **Error bodies differ by service.** `ApiErrorParser` reads an optional `error` string, an optional `code` string and an optional `message` that is a string or an array of strings, and treats an empty or non-JSON body as no code.
  - identity (coded refusals), catalog, pricing: `{ error: CODE, message, traceId }` (catalog adds `field`). identity's framework refusals are `{ message, error: <HTTP reason phrase>, statusCode, traceId }`: there `error` is not a code and a validation `message` is an array.
  - search: `{ error: <snake_case code> }` with no message; its `404` is plain text.
  - order: `{ error: CODE, message, … }` with extras per code (`reason`, `orderNumber`, `availableTiers`, `replayed`); `traceId` only on `500 INTERNAL_ERROR` and `503 DATABASE_UNAVAILABLE`; `application/problem+json` for a body it cannot bind; an empty body on its token `401`s.
  - payment: `{ error: <sentence> }`; its machine code is `code`, never `error`.
  - gateway: `{ message: "Unauthorized" }` on `401`, plain text on `429`, `503`, `504`.
- **DTOs.** Kotlin-cased properties with `@SerialName` wherever the wire name differs; identity, order and payment answer camelCase, the others snake_case. A required field has no default. **A field the contract allows to be `null` or absent is nullable with a `null` or empty default; that is not a fake default.** An enumerated wire value the app does not know maps to an `unknown` case.
- **Two Retrofit traps.** A `200` whose JSON body is `null` (no flash sale; a voucher that does not apply) cannot be declared as a nullable body type: declare `Response<JsonElement>` and decode in the repository. A `DELETE` that carries a body (forgetting a handset) needs `@HTTP(method = "DELETE", hasBody = true)`.
- **Money is never `Double` or `Float`, and only search's `price_minor` is an integer.** `price_minor` is in minor units (rupiah × 100; e.g. `8900000` is IDR 89,000; divide by 100 for display as an indication only). catalog `price` and pricing `value`, `flash_price`, `min_spend`, `max_discount` are decimal strings in rupiah (`"89000.00"`); pricing's request field `cart_subtotal` is sent as a decimal string. Order and cart amounts and payment's balances and amounts are JSON numbers in rupiah with up to two decimals (`85000`, `202000.00`); payment's `limit` on a `422` is a string. Decode each into `BigDecimal` (`Long` for `price_minor`) through serializers that never pass through a floating-point type.
- **Timestamps are not uniform.** Most are RFC 3339 in UTC with zero to nine fractional digits (`Instant.parse` reads them). Search sends epoch milliseconds. Payment's `instructions.expiresAt` is Midtrans' own text (`2026-10-23 10:15:00`, no zone), nullable, kept as a string and never used for arithmetic. The notary's `issuedAt` is Unix seconds as a decimal string.
- **The gateway as it runs today** *(read from its Envoy configuration; not run)*: each route a buyer app calls today has **one budget of 100 requests a minute shared by every caller** (other limits: Midtrans 600, map tiles 1200, `POST /api/v1/seal/presentations` 20, `POST /api/v1/seal/reports` 10; checkout shares its budget with every order read, a top-up with every balance and status poll), so a gateway `429` is ordinary and never the buyer's fault; it carries no `Retry-After` (identity's and notification's own `429` carry one; the edge's handshake `429` does not); API routes set no timeout, so the gateway waits Envoy's default **15 seconds** and then answers `504` (a profile `PUT` that places an address, or a checkout, can end that way while the service finishes); and Envoy answers `503` both when it cannot connect and when the upstream resets after the request was delivered. identity also throttles per client address: 5 a minute on `login`, `register`, `2fa/verify` and the 2FA management routes, 10 a minute on `refresh`, `logout` and `me`.

### 1.7 Session and secure storage

- The whole session (`accessToken`, `refreshToken`, expiry instant, `accountId`, `principalId`, `email`, **`role`**) is one encrypted file under `noBackupFilesDir`, written atomically, protected by an AES-GCM key in the Android Keystore. It is never backed up or transferred.
- **Could-not-read is not signed-out.** A missing file is signed out. An unreadable file or a failing Keystore is `Unreadable`: nothing is cleared, nothing is sent, the buyer is told, and signing in again replaces it.
- The expiry is the device time at receipt plus `expiresIn`; `expiresAt` from `GET /api/v1/auth/me` may only shorten it. The lifetime is never a constant in code. The token is renewed when 60 seconds or less remain.
- **The refresh is single-flight and belongs to the app, not to its caller** (§1.4): both tokens are written before any caller resumes, and a write that fails after a `200` ends the session.
- **A refresh token is never sent twice**, so a refresh has three outcomes:

| Outcome | When | Then |
| :--- | :--- | :--- |
| Refused | identity's ordinary refresh refusals (`400`, `401`), `401 CLIENT_MISMATCH` | the session is cleared; sign in |
| Not spent | no request bytes were written; `429`; identity's own `503 DATABASE_UNAVAILABLE`; a gateway `502` or `503` when no request bytes were written; the client-proof answers `400 UNKNOWN_CLIENT`, `400 CLIENT_REQUIRED`, `401 CLIENT_PROOF_REQUIRED`, `401 CLIENT_PROOF_INVALID`, `503 CLIENT_PROOF_UNAVAILABLE` (kept and reported, §7 Q5) | the session is kept; try later |
| Unknown | request bytes were written and no readable answer came back; `500`; `504`; an unreadable `2xx`; any other status | the session is cleared; sign in |

  The refresh transport therefore classifies by error code, not by status alone. Under Envoy a gateway `503` can follow a refresh identity did process; the later attempt is then answered `401` and the session ends there (§7 Q11).
- **"Left the device" is read from the wire, never from an exception class.** The refresh call carries a per-call OkHttp `EventListener`: if the call failed before `requestHeadersStart`, it was not sent; if that event or any later one was seen, the outcome is unknown; if the listener delivered nothing, it counts as sent.
- **At start** with a stored session: `GET /api/v1/auth/me`. `200` keeps it; `401` renews once and asks once more; a second `401` is session over; **any other answer, or none, leaves the session as it is**.
- **Sign-out:** renew first if 60 s or less remain (a refused renewal means the session is already over); forget the handset (Phase 4); `POST /api/v1/auth/logout` through `SafeApiCaller` inside a 5-second budget, renewing once on the gateway's `401`; clear the stored session whatever the answer; record a breadcrumb when identity did not confirm with `204`.

### 1.8 Local persistence: a disposable cache and a durable ledger

Two Room databases with different promises (AND-D5). **Today** there is one, version 1, schema not exported, built with `fallbackToDestructiveMigration`.

| Database | Holds | Promise |
| :--- | :--- | :--- |
| `kinetix_cache.db` | categories, product pages and details (public), the delivery profile (owned) | Disposable; destructive fallback allowed |
| `kinetix_ledger.db` | pending transaction states and outbox entries | Durable. `exportSchema = true` with the schema JSON committed; **never** a destructive fallback; every schema change is a new version with a written `Migration` and a `MigrationTestHelper` test |

- **The cache is served only when the network fails, and every value from it is marked.** The repository returns the data with its origin and age in the type; the screen says "possibly out of date"; stock from the cache is rendered as "could not be checked", never as a count; nothing cached is a basis for adding to a cart or for a total. Rows older than a named number of days are evicted.
- **A pending transaction state is one row per attempt, keyed by its `Idempotency-Key`**, holding the owner, the operation, **the exact request body as sent** (a field that was absent stays absent), a state, timestamps and, for a top-up, its `referenceNumber` and last known status. Order and payment both tie a key to its body, so a repeat needs the body. Many rows per principal. A row is created and saved before the request is sent, in one transaction. A new key is minted only by an explicit new attempt. An unknown operation or status read from disk is an error, never a default. **Today:** one row per operation per principal, a fingerprint instead of the body, a new fingerprint silently replaces the key of an attempt whose outcome is unknown, and `RoomPendingTransactionStore` maps an unknown operation to `CHECKOUT`.
- **Every row has an owner.** Ledger rows and outbox entries carry a `PrincipalId`; the cached profile is keyed by `AccountId`. Only the signed-in principal's rows are read, repeated or drained. When the session is cleared, that buyer's cached profile is deleted; ledger and outbox rows wait for the same buyer.
- If the ledger cannot be opened, checkout and top-up are refused with a stated reason (a single charge cannot be guaranteed); everything else works; the fault is reported.
- **Outbox** (AND-D6). Only writes the contract makes safe to repeat and that no screen waits on: the profile `PUT`, and, from Phase 4, reviews and courier ratings. An operation type exists only when its handler does. **Today:** `RoomOutboxStore` maps an unknown operation to `PROFILE_UPDATE` and an unknown status to `PENDING`; any unrecognised entry must be isolated and reported, never run.
  - One profile entry per principal, **merged** on a later edit: it holds every field changed since the last profile identity confirmed (newest value per field; `avatarUrl: null` survives). Diffing is against the last confirmed profile, never the optimistic one.
  - A queued write returns a distinct "queued" outcome, and the screen says **"saved on this device, not sent yet"** until identity confirms. Checkout pre-fill uses only a confirmed profile.
  - An entry ends one of three ways. **Done:** a `2xx`; for a review or rating also a `409`, whose `review_id` is kept. **Later**, with no attempt cap: not sent, a timeout, `429`, `500`, `502`–`504`, a `409` on the profile `PUT` as the endpoint defines it (read the profile, then send), a `401` after one renewal, and no session to send with. **Refused:** any other `4xx`; the entry is isolated, reported, and the optimistic value withdrawn. An entry found in flight after a process death goes back to pending.
  - Backoff starts at 15 s, doubles to 300 s and honours `Retry-After`.
  - The outbox drains on enqueue, at process start, after sign-in, on returning to the foreground, and from WorkManager with a connected-network constraint. The worker restores the owner from the token store itself: a store that is readable and empty is success (rows wait); an unreadable store is retry.
- **Never through the outbox:** checkout, top-up, and every cart write. `POST /api/v1/cart/items` adds to a quantity, so a blind repeat doubles it; the cart writes that are safe to repeat stay out too, because a cart shown while a write waits differs from the cart checkout prices.

### 1.9 Realtime: the edge event socket

Not built. The design, from the edge's source at v1.4.0:

- One socket client over OkHttp, owned by the application graph, multiplexing subscriptions; its only consumer is live courier tracking (§4.8).
- **Open only while** the tracking destination is at least `STARTED` and the order's `status` is `PAID` or `PROCESSING_FULFILLMENT` (this is the app's own rule: the edge refuses no status; positions can exist only in `PROCESSING_FULFILLMENT`, and `PAID` is kept only so an early subscription starts delivering without another read; `subscribed: true` is no evidence of a delivery in progress). It closes when the app goes to the background, at sign-out (the edge never closes it) and when an order read shows another status. On return the app reads the order over REST first, then reconnects and subscribes. Reconnects use exponential backoff with jitter.
- **A silent subscription is normal.** Positions flow only while matching holds the delivery assigned or picked up, which no answer a buyer can read states (§7 Q13). A buyer's socket carries `courier.location.updated` only, and no frame says picked up, delivered or cancelled (§7 Q19). While the tracking destination is `STARTED`, the app reads `GET /api/v1/orders/{orderId}` on a named interval (long, because `/api/v1/orders` shares one gateway budget with checkout), at once when positions stop for a named time, and closes the socket on any other status, with a test. The screen has a state for "subscribed, no courier position yet".
- **The edge checks the access token once, at the handshake, and keeps it for the life of the connection.** It never closes on expiry. A `subscription.subscribe` sent after that token expired is answered `command.error` `UPSTREAM_UNAVAILABLE` with `retryable: true`, the same frame as an outage. So the client remembers the `exp` of the token it connected with: before a subscribe at or after it, close, renew, reconnect. `retryable` alone is never followed blindly.
- Handshake refusals are classified by status: `401 SESSION_ENDED` renew once then retry; `401 SESSION_REQUIRED` is our own bug and is reported; `429` here carries no `Retry-After` and comes from the edge calling identity's `GET /api/v1/auth/me` per connection (spending the same 10-a-minute per-address bucket as start-up `me`), so bound the reconnect schedule with a named minimum interval and a cap on handshakes a minute well under 10, with a test; `502`/`503` back off.
- **Liveness is any inbound frame.** `system.ping` every 25–30 s keeps inside the edge's 60 s receive timeout; the edge answers one client frame at a time, so a missing `system.pong` counts only when no command awaits its ack.
- A position is last-write-wins by `data.recordedAt`; a repeated `eventId` is dropped.
- What draws the position is undecided: no map library is declared, and the estate serves no map tiles to buyers (AND-D16).

### 1.10 Push on Android

**Today:** the manifest declares the messaging service correctly; nothing else below exists, and the handler that does exist cannot recognise a challenge (§0.2).

- The notification service sends through **FCM HTTP v1 only**; the address it stores is the FCM registration token, valid only for the Firebase project it sends through, so `com.kinetix.shop` must be registered in that project (owed, §0.4).
- **The handset is registered** whenever a session and an FCM token are both present and that pair has not been registered in this run: at a start with a session, after a sign-in or sign-up, and in `onNewToken`. The token is fetched from the SDK. The previous token is kept until its `DELETE` is confirmed. The (token, principal) pair that was registered is stored.
- **The ownership challenge** is a high-priority data message `{ kind: "DEVICE_VERIFICATION", nonce, expires_at }`. It is answered from `onMessageReceived` (or an expedited one-shot worker) with the verify call before `expires_at`, using the stored session of the principal that registered. The handler's test uses a payload captured from the notification service's own encoder.
- **What a buyer can receive today is one notification**: the notary's liability statement, the only template production grants to any caller. The order templates exist in notification's catalogue and nothing is granted to send them. A visible push is an FCM notification message with a title and a body, rendered by the server in Indonesian, and no data: a tap opens the app where it was; nothing routes to an order by guess, and no prompt is shown after checkout (§7 Q3, Q10).
- **Platform obligations.** `POST_NOTIFICATIONS` is a runtime permission since API 33: it is requested in context (notification settings, or the proofs screen), never at first launch, with denied and permanently-denied states. Notification channels are created at start with names from `strings.xml`; the manifest names the default channel and a monochrome small icon. A notification message received while the app is in the foreground reaches `onMessageReceived` and is shown by the app.

### 1.11 Lifecycle and background work

| Moment | What the app does |
| :--- | :--- |
| Process start (UI, worker or push) | validate configuration; no entry point builds Retrofit on an invalid configuration |
| First need of a session | the application-scoped component restores it once per process (§1.7) |
| Session known | register the handset (§1.10); drain the outbox of that principal; show an unfinished checkout or top-up of that principal |
| Returns to the foreground | renew if due; drain the outbox; refresh the visible screen; reopen the socket if a tracking screen is showing |
| Goes to the background | close the socket; stop polling |
| WorkManager flush | restore the owner from the token store; send its entries; never a cart write, a checkout or a top-up |
| Killed mid-checkout or mid-top-up | at the next start the app sends nothing by itself (AND-D7). A top-up that already has a `referenceNumber` is followed by reading its status. A checkout or top-up with no final answer is shown to the buyer as unfinished after the cart and page 1 of the orders are read, and the buyer finishes it (same request, body and key) or drops it (its key is never used again) |

Whether background work may renew an expired session is AND-D8.

### 1.12 Configuration, environments and build

- `HTTP_BASE` (must start with `https://`) and `WS_BASE` (must start with `wss://`) have no default. They are read from a Gradle property, then an environment variable, then `local.properties`; `local.properties.example` is tracked and names the production host, so **a developer's debug build talks to production unless changed**. There is no staging estate and there are no mock services.
- `AppConfigValidator` runs at start; a missing or malformed value renders `ConfigErrorScreen` naming every bad key. For that to be reachable, `MainActivity` injects only the configuration result and reaches the session graph lazily, on the valid branch only; the validator catches `URISyntaxException`. A release variant refuses to build with a missing or invalid value.
- Cleartext is refused by a Network Security Config; the manifest attribute alone is deprecated at this compile SDK. A lab is reached over TLS with its CA trusted by a debug-only configuration (§7 Q6).
- `app/google-services.json` is untracked. The Firebase plugins apply only when it is present, so builds without it stay green; the app then runs with the local adapters of §1.13 and without push. The release workflow's preflight refuses to build without the file for `com.kinetix.shop`. **Today:** the release preflight checks base URLs and signing secrets only. This is a recorded departure from the estate's fail-fast rule and from `kinetix-courier-mobile` (AND-D3). The files at the workspace root are reported to belong to the courier app and must not be used here.
- The session file is under `noBackupFilesDir`; the database and the FCM preferences are excluded by the backup and data-extraction rules. **Today:** the backup rule for the session names a path that does not exist.
- Release: R8's scope is AND-D15; the version comes from the tag; the tagged commit must be on `main` (Phase 6).

### 1.13 Observability and privacy

- **Correlation** is by `X-Request-Id` (§1.6).
- **Never logged or sent to a third party:** tokens, email, phone, address, names, request bodies, identifiers inside paths. Logged: the route template, the status, the request id, the failure type. **Today:** `DefaultPushMessageHandler` still logs the challenge id (Phase 4).
- **Crashes, analytics and flags are interfaces with two production implementations each**: local ones (log-based crash reporter and analytics, bundled flag defaults), which are the behaviour of a build without Firebase and not fakes, and the Firebase ones. **Today:** no local telemetry or flag adapter exists; Hilt binds the Firebase classes directly (`CoreBindingsModule`), which do nothing without Firebase, and flags are constants in Kotlin.
- **Flags.** The keys in `res/xml/remote_config_defaults.xml` are shared with `kinetix-ios`: `checkout_enabled`, `wallet_topup_enabled`, `live_tracking_enabled`, `outbox_sync_enabled`; `minimum_supported_version_code` and `force_update_enabled` are per platform. A flag is fetched at start and on return to the foreground, and read by the screen it governs, which has a state for "disabled". **No backend kill switch exists** (it is the edge's case X8, planned), so the app's flag is the only switch and cannot reach a build that has not fetched it. **Today:** nothing fetches, and only the outbox reads one flag.
- **Privacy.** Until AND-D3 is ruled: the advertising-id permissions Firebase Analytics brings in (`AD_ID`, `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`) are removed in the manifest, advertising-id collection is disabled, and no user id is set on analytics or crash reports. **Today:** the three advertising-id permissions are in the merged manifest and collection is not disabled.

### 1.14 Security posture

- TLS only; no pinning.
- Authorization is the server's. After every session answer the app reads `role`: only a `customer` is a buyer account for the wallet (§4.1).
- Passwords and codes are never stored and never saved in state (§1.5).
- A proof document is kept and exported byte for byte as received.
- Anything that comes from outside the process (a pasted or scanned signature, a restored route, a push payload) is checked for shape before use.
- App integrity (Play Integrity through Firebase App Check) is deferred: production does not enforce it. The rollout that will is §7 Q5, and as written it would lock both buyer apps out.
- App Links need `/.well-known/assetlinks.json` on a host that serves none today (§7 Q4).

### 1.15 Design system, accessibility and localisation

- **`ui/theme/KinetixPalette.kt` and `KinetixTypography.kt` are the buyer brand of both apps** since the user's ruling of 2026-11-19 (IOS-D8): the iOS app reads its tokens from these files, so they are a contract. A change here is a change to both apps.
- Eleven colours, dark only: `ObsidianSurface` `#0A0D12`, `ElevatedSlate` `#131922`, `CardSlate` `#19212D`, `ElectricVolt` `#C6FF00` (primary), `CyanPulse` `#00E5FF` (secondary), `TextPrimary` `#F3F6FA`, `TextSecondary` `#9BA7B8`, `DangerCoral` `#FF5252`, `WarningAmber` `#FFB300`, `SuccessEmerald` `#00E676`, `BorderHairline` `#283242`. Four text styles in the platform faces: sans at 24, 18 and 14 sp, mono at 12 sp.
- Screens read the colour scheme and the typography, never a literal. **Today:** twelve colour roles are set and the rest keep Material's baseline, so chips and the navigation indicator show colours that are not tokens; no shape scale is configured, so buttons are Material's fully rounded default; the hairline token is drawn only as the resting border of the 13 outlined text fields (`outline` role), and no card, button or divider draws it; the platform theme is a light one.
- The estate's taste: no gradients, no glow, no shadows for decoration, no pill buttons, hairline borders. Two accents and the default pill shape are departures the user has not been asked about (AND-D12).
- Every interactive element has a 48 dp target and a content description; state is never conveyed by colour alone; text scales with the system font size without truncating a price.
- **Buyer copy is multilingual** (AND-D10): English in `res/values/strings.xml`, Indonesian in `res/values-in/`, with key parity. Counts use plurals; amounts and dates are formatted for the locale; the app declares a locale configuration so it appears in the system's per-app language settings. Kotlin, identifiers, tests and this file stay English.
- Product images are fetched from `image_url` by one image loader (a decision owed with Phase R); an empty or non-`https` value is no image. **Today:** the catalogue shows no image at all.

### 1.16 Performance

Lists are paged by the server and rendered lazily. Identical in-flight reads are coalesced in the repository. A performance claim is measured on a device and written into §0.3 with the number.

---

## 2. Non-negotiable rules

A rule's gate exists only where a named test in `ArchitectureAndLayoutGateTest` checks it **and §0.3 records that test seen red**. Each rule says what is gated today; everything else is a review rule. Nothing on disk records any current check as seen failing first.

1. **The estate's rules bind here.** Read the workspace `CONTEXT.md` §1 and §2 before the first change and §8 before any claim to the user. Never publish anything from this repository as a web artifact. Never print a secret value to chat, a log or a command's arguments, and never open `google-services.json`, a keystore or a signing secret to quote from it. Never invent a value: an endpoint, a field name, a payload key, a host or a build setting that was not read from a file, a command's output or the user does not exist. (The push handler's keys were invented; that is the defect of §0.2.)
2. **English only.** Kotlin, Gradle scripts, XML, identifiers, test names, Markdown, and commit messages (which the user writes). Buyer-facing copy in another language lives only in `res/values-in/`.
3. **Zero comments**, in every file written here; tool and compiler directives excepted (`@Suppress` with a named rule when unavoidable). *Gate today:* no `//` or `/*` outside string literals, per line, in `.kt` under `src/main` and `src/test` and in the three Gradle scripts. *Review:* XML, TOML, properties, YAML.
4. **House layout (`CONTEXT.md` §2.1).** An opening brace stays on its line. Too long for one line: every element on its own line, the closing parenthesis alone. Blank lines between contexts: after a closing brace before the next statement, before a final `return`, between groups of statements; an assignment stays with the check that reads it; no blank line just inside a brace. *Gate today:* none, and no formatter is configured; 92 places in `src/main` break the closing-brace clause alone (107 to 141 with cuddled returns), plus 66 in `src/test`.
5. **One declaration per file, named after it.** One top-level class, interface, object or screen function per file; no nested enum, DTO or helper class; no grouped-DTO file; sealed variants that carry fields live in their own files. *Gate today:* exactly one column-0 `class`, `interface`, `object` or plain `fun` per file, named after the file. *Review:* nested types and every other top-level form (a `private fun`, an extension, a top-level `val`, a `typealias`), which the check cannot see.
6. **Layers.** One `:app` module. `core`, `models`, `dto`, `services`, `repositories`, `state`, `viewmodels` and `ui/screens` hold feature subfolders, never files. Imports follow §1.3. No UseCase or Interactor layer. *Gate today:* no `.kt` directly in the first seven. *Review:* `ui/screens` (two files sit in its root today), the import direction, the module count.
7. **Failure is a value.** Repositories return `Result<T>` and never throw to a view model. Only the three types of §1.6 catch a transport error or read a status. Unknown is never mapped to a refusal, to empty or to zero. A `Failure` never carries display text. *Gate today:* under `repositories/`, no text `SocketTimeoutException` and no `catch (x: IOException)`. *Review:* everything else.
8. **DTOs and configuration.** `@SerialName` where the wire name differs; no default on a required field; a nullable or absent field is nullable with a `null` or empty default; no host or URL literal outside configuration; no fallback for a missing setting. *Gate today:* no `val snake_case:` under `dto/`; no literal `else -> ""` under `di/`.
9. **Concurrency.** Every suspend function is main-safe; no `Dispatchers.` reference outside `di/`; session work runs in the application scope (§1.4). *Gate today:* none.
10. **Compose, state and navigation.** Top-level `@Composable fun XScreen`; `collectAsStateWithLifecycle()`; typed routes; no credential in a route or in saved state; a view model takes no lambda, `NavController`, `Context` or string resource; copy only from string resources through `FailureMessageMapper`. Keep `SessionRepository`, `AuthRepository` and `TwoFactorRepository` separate. *Gate today:* no text `object …Screen` under `ui/screens`.
11. **Tests.** Hand-written fakes implementing the interfaces; no Mockito, MockK or any mocking library; no test code or test-only branch in `src/main`. **Every DTO has a decode test fed with the endpoint's published sample, its null and absent variants included, through the app's `Json` and the real Retrofit converter.** A fake must not behave better than the thing it replaces. Every new test and every gate check is seen red before it is trusted.
12. **Dependencies.** Listed in `gradle/libs.versions.toml`, locked (regenerate locks with `./gradlew :app:dependencies --write-locks`), each added with its reason in §0.3. None is added silently.
13. **No fake features and no skeletons** (§1.2).
14. **Privacy and secrets** (§1.13): nothing secret in the repository or the binary; `app/google-services.json` and `local.properties` are ignored by git; no pattern covers keystores (`*.jks`, `*.keystore`, `*.p12`), so a keystore is never placed inside the repository (patterns to be added in Phase 6).
15. **Git is the user's.** No `add`, `commit`, `push`, new branch, tag, `stash`, `reset` or `rebase`, and no switching branches; `fetch` and `pull` are allowed. When the user says something is pushed, take it and check the result (a CI run), never the push. Work lands on `development` and reaches `main` by pull request; release tags `vX.Y.Z` are cut from `main` only.
16. **Finishing a task.** Run the gate (§3.2), append to §0.3, and report what was run with its test counts and on what (JVM, emulator, device). A suite that printed no count is no evidence, and unit tests over fakes are not evidence that the app starts.

---

## 3. Structure and gate

### 3.1 Packages, by purpose

```text
com/kinetix/shop/
├── KinetixApplication, MainActivity
├── core/
│   ├── config/        AppConfig, its check result and validator, NetworkPolicy
│   ├── failure/       the sealed Failure, ClientValidationReason, 18 failure types
│   ├── network/       ApiErrorParser, HttpFailureMapper, SafeApiCaller, request-id interceptor
│   ├── result/        Result, Ok, Err, UnitSentinel
│   └── time/          Clock, SystemClock, DelayCountdownTimer
├── di/                CoreModule, CoreBindingsModule
├── models/            ids, auth, profile, catalog, pricing, search, review, notification, outbox, transaction
├── dto/               common, auth, profile, catalog, pricing, search, review
├── services/
│   ├── remote/        Retrofit interfaces, one per backend service
│   ├── session/       the refresh transport, the single-flight renewer
│   ├── storage/       the session cipher and the encrypted token store
│   ├── local/         Room: entities, DAOs, stores
│   ├── outbox/        the processor, the worker, the scheduler
│   ├── notification/  the messaging service, the push handler, the FCM token store
│   ├── telemetry/     crash reporter and analytics, local and Firebase
│   └── config/        the feature flag provider
├── repositories/      auth, profile, catalog, pricing, search, review (interface + Impl)
├── routes/            the sealed AppRoute and its routes
├── state/             common, auth, profile, catalog, search
├── viewmodels/        auth, profile, catalog, search
└── ui/                theme, components, screens/<feature>
```

**Today**, against that tree: `routes/AppRouterState` is dead code with a live test; two screens sit in the root of `ui/screens`; `core/network` holds three trace-header types to be replaced; `services/local` holds one database.

Typed ids (`models/ids/`): `AccountId`, `PrincipalId`, `ProductSku`, `CategorySlug`, `OrderId`, `OrderNumber`, `ReviewId`, `VoucherCode`, `TopUpReference`, `IdempotencyKey` are value classes, so an `OrderNumber` passed where an `OrderId` is required does not compile.

### 3.2 The gate

This repository runs Gradle, the Android SDK and a JDK on the host and in CI, with no pinned toolchain image: a departure from the estate's everything-runs-in-Docker rule that is recorded here and awaits the user's ruling (AND-D2).

One gate command, locally and in CI:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

What it proves and what it does not:

- `ArchitectureAndLayoutGateTest`, six checks: a self-test of the comment scanner; zero comments (§2 rule 3); no file in the root of seven layer packages; one top-level declaration named after its file; no `object …Screen` and no snake_case DTO property; no duplicated transport catch under `repositories/` and no empty-string fallback under `di/`. It reads no document and depends on no wording in this file.
- "Zero warnings" is configured nowhere (AND-D14): the last local lint report ends with 0 errors and 25 warnings.
- `assembleRelease` without signing secrets emits an unsigned APK and runs R8 over three library prefixes only.
- **It runs no Android code.** Room, the Keystore cipher, WorkManager, the Hilt graph, the messaging service and every composable execute in no test (AND-D13).
- In CI, the four tasks run as three invocations across two jobs on JDK 25 against `https://api.kinetix.invalid`, and the required `verify` check also needs a Trivy job that fails on a critical vulnerability. A green local gate is not a green CI.
- `.gitlab-ci.yml` does not run `assembleRelease`, names an image for SDK 35 while the build compiles against 37, and has never been observed to run.

The pull request template and `.coderabbit.yaml` name this file; the log they refer to is §0.3.

---

## 4. Backend contracts

**Before implementing a slice, read its page and its endpoint definitions in full; they win over this digest.** The pages are prose; the fields, bounds, statuses and samples of every endpoint are in `kinetix-docs/src/site/rest/endpoints/<service>.ts` and `sockets/`. Do not invent an endpoint or a field name.

This digest was corrected on 2026-11-19 from service source at the releases production runs (identity v1.7.3, order v1.6.1, catalog v1.2.3, pricing v1.2.2, search v1.2.2, review v1.4.1, payment v1.3.0, edge v1.4.0, matching v1.6.0, notification v1.3.3, notary v1.10.1, seal v1.12.0, gateway v2.2.0). The earlier claim that every line here was verified could not be backed: the cart voucher body was wrong in the very directory it named. `kinetix-ios/AGENTS.md` §4 holds the same digest; when one is corrected, correct the other.

| Slice | Page under `kinetix-docs/src/content/docs/apis/rest/buyer-app/` |
| :--- | :--- |
| Accounts, sign-in, 2FA challenge, stored session | `sign-up-and-sign-in.mdx` |
| Refresh, sign-out | `stay-signed-in.mdx` |
| Managing 2FA | `two-factor.mdx` |
| Delivery profile | `profile.mdx` |
| Catalogue, flash sale, discounts, reading reviews | `browse.mdx` |
| Search | `search.mdx` |
| Wallet and top-up | `wallet-top-up.mdx` |
| Cart and vouchers | `cart.mdx` |
| Checkout | `checkout.mdx` |
| Orders and live tracking | `orders.mdx` |
| Writing reviews and ratings | `reviews.mdx` |
| Notary proofs | `proofs.mdx` |
| Push | `push-notifications.mdx` |
| Hosts, tokens, request ids | `../index.mdx` (its "Limits" and gateway answers describe the previous engine: use §1.6) |

### 4.1 Identity, session, sign-in delay and 2FA

- **Where tokens are checked.** On `/api/v1/auth` (except `login`, `register`, `refresh` and `2fa/verify`, which are public at the gateway), `/api/v1/users`, `/api/v1/payment`, `/api/v1/proofs` and `/api/v1/notifications/devices` the gateway checks issuer, signature and expiry first (its own `401`). Catalog, search, pricing, order and review check the token themselves.
- **Register (`POST /api/v1/auth/register`)**, body `{ email, password }`: email at most 254 characters; password at least 12 characters and at most 72 bytes in UTF-8. `201` is a session: `accessToken`, `refreshToken`, `expiresIn`, and `user` with `id`, `email`, `role`, `principalId`. **Keep both ids**: `user.id` (`AccountId`, an integer) is what identity's own paths take; `user.principalId` (`PrincipalId`, a UUID) is the `sub` every other service knows the buyer by. `409`: the email has an account.
- **Sign in (`POST /api/v1/auth/login`)** accepts a password of 6 to 1,024 characters: **never apply the registration minimum to the sign-in form.** A `200` is one of three shapes: a session; a challenge (`mfaRequired: true`, `challengeToken`, `expiresIn`, `passkeys`); or `mfaEnrolmentRequired` with an `enrolmentToken`, which only an `admin` account receives and which the app refuses, discarding the token without calling logout.
- **Role.** Register always creates a `customer`, but sign-in, the challenge and refresh answer a session for any role (`customer`, `seller`, `courier`, `admin`). Order lets any role use the cart; payment answers `403` on every wallet route for anything but `customer`. After every session answer read and store `role`; for another role say that this app is for buyer accounts and never retry the wallet.
- **Three waits, all `429`.** The gateway's shared budget (no `Retry-After`). identity's per-address throttle (`error: REQUEST_REFUSED`, with `Retry-After`). And `error: "SIGN_IN_DELAYED"`, which counts every check of this account's password or code from any device: after five in a row the wait grows from 1 s to at most 60 s and the account is never locked. Read `retryAfterSeconds` from the body, falling back to `Retry-After`; tick a countdown; keep the screen and any challenge intact.
- **Answer the challenge (`POST /api/v1/auth/2fa/verify`)**, body `{ challengeToken, code }`. `200` is a session. `401 WRONG_CODE` leaves the challenge open. `401 CHALLENGE_EXHAUSTED`, `CHALLENGE_SPENT` or `CHALLENGE_INVALID` can never succeed: return to the password step.
- **Refresh (`POST /api/v1/auth/refresh`)**, body `{ refreshToken }`. Tokens rotate on every use. The outcomes are §1.7's table. Lifetimes today are 900 s and 30 days; the app reads `expiresIn`. A buyer app sends no `X-Kinetix-Client`: true today, and scheduled to change (§7 Q5).
- **Stored session (`GET /api/v1/auth/me`)**: `200` repeats `principalId`, `userId`, `email`, `role`, `expiresAt`. What each answer means at start is §1.7.
- **Sign out**: the order of calls is §1.7. `POST /api/v1/auth/logout` takes the Bearer token and `{ refreshToken }`, which ends this session alone; leaving the field out ends every session of the account; `null` is refused. `204`.
- **Managing 2FA (`POST /api/v1/auth/2fa/{setup,enable,disable}/{id}`)**, `{id}` = `AccountId`. Setup takes `{ password }` and returns `qrCodeUrl` (an `otpauth://` URI), `twoFactorSecret` and `enabled: false`; `409` means it is already on. **Enable and disable both take `{ code, password }`.** Without `password`, setup and enable answer `400 PASSWORD_REQUIRED`; disable answers a validation `400` whose `message` is an array. `422` is a wrong or reused code; `409` on disable means it is not on. A `401` here whose body is identity's, on a token still in date, is a wrong password, **not** an expired session: keep the tokens and ask again. The gateway's own `401` on these routes is an expired token and is renewed once (§1.6). Enabling or disabling ends every other session of the account. These calls count towards `SIGN_IN_DELAYED`.

### 4.2 Delivery profile (`identity`)

- `GET /api/v1/users/{id}/profile`: `404` on an account that never saved one, which is a state, not an error.
- `PUT /api/v1/users/{id}/profile` creates it, then changes only the fields sent. Send only what changed and never `null`, except `avatarUrl: null`, which removes the avatar; `null` anywhere else is a `400`. Bounds: `fullName` at most 120, `phoneNumber` 8 to 15 digits with an optional `+`, `streetAddress` at most 255, `city` at most 100, `postalCode` at most 16, `avatarUrl` an `https` URL; read the endpoint for the rest. A `409` happens only on the first save of a profile, when two requests create it at once: nothing of this request was saved, so read the profile and send the change again (its body is the framework shape, with no code). A changed address is placed on the map before the answer, which can outlast the gateway's 15 s: a `504` there is outcome unknown, settled by reading the profile. The `200` carries the whole profile.
- Checkout does not read the profile: the app pre-fills `recipientName`, `recipientPhone` and `shippingAddress` from a confirmed one.

### 4.3 Catalogue, search, promotions and reading reviews (no token needed)

- `GET /api/v1/categories`: `id`, `name`, `slug`.
- `GET /api/v1/products` with `category` (slug), `q`, `page` (up to 10,000), `page_size` (1 to 100, default 10) answers `{ count, page, page_size, results }`. In each result `weight_grams`, `available_stock` and `is_in_stock` are **nullable** (the last two are `null` when `stock_status` is `unknown`) and `image_url` may be an empty string. `stock_status` is `in_stock`, `out_of_stock` or `unknown`. **Never show `unknown` as out of stock.**
- `GET /api/v1/products/{sku}` (percent-encode the SKU) is keyed by `sku`, never by the numeric `id`. It answers the product with `description` and without the three list stock fields: stock is `warehouse_stock { sku, bin_location, available_quantity, reserved_quantity, stock_status }`, whose middle three are null when `stock_status` is `unknown`.
- `GET /api/v1/pricing/flash-sales/{product_id}` where the id is the `sku`. A `200` with the JSON body `null` means no sale (§1.6, the Retrofit trap). A sale's `merchant_principal_id` is `null` when an admin created it. Checkout uses the flash price only when it is below the catalogue price, and then gives that unit no discount.
- `GET /api/v1/pricing/discounts`: one unpaged list the app matches itself. A discount concerns a product when its `target_product_id` is the product's `sku`, **or** its `target_category_id` is the product's `category_slug`, **or** it names neither; **and** its `merchant_principal_id` is `null` or the product's merchant. Of those, pricing applies only the one that saves most on a unit (`PERCENTAGE`: price × `value` ÷ 100; `FIXED`: `value` rupiah; never more than the price). The indication shows that one and never adds discounts together.
- `GET /api/v1/products/suggest?q=&limit=` returns `{ suggestions }`. `GET /api/v1/products/search?q=&category=&sort=&limit=&offset=` returns `{ total, hits, facets, index }`. **`sort` is `relevance` (the default), `newest` (most recently changed) or `price_asc`; any other value is `400 { "error": "malformed_query" }`.** `limit`: 0 or absent is 24 for search and 10 for suggest, cut to 100; negative values are refused. `q` and each `category` are at most 256 bytes with no control character. `facets` may be absent; a hit's `description` and `categories` may be absent; `index.indexed_through_ms` and `index.age_seconds` may be null. A hit's `id` is the `sku`. When `index.stale` is `true`, show the hits with a non-blocking note.
- `GET /api/v1/reviews/products/{productId}?page=` (`productId` is the `sku`; `page` at least 1, up to `last_page`) and `GET /api/v1/reviews/merchants/{merchantPrincipalId}?page=`. A review's `merchant_principal_id` can be `null`.

### 4.4 Wallet and top-up (`payment`, role `customer`)

- Checkout pays only from the buyer's Kinetix wallet. `GET /api/v1/payment/wallet/customer/balance` returns `availableBalance`; a wallet never funded answers `exists: false` at zero.
- **Start a top-up:** `POST /api/v1/payment/wallet/customer/topup` with `Idempotency-Key` (1 to 128 characters from `A-Z a-z 0-9 . _ : -`; a UUID fits) and body `{ amount, paymentMethod, bank? }`: `amount` a JSON number of whole rupiah, at least 1,000 and at most payment's limit (IDR 2,000,000 as published; it is a configured value); `paymentMethod` `MIDTRANS_VA` or `MIDTRANS_QRIS`; `bank` one of `BCA`, `BNI`, `BRI`, `CIMB`, `PERMATA`, required with VA and refused with QRIS. **Create the key, store key and body in the ledger, then send.**

| Answer | Meaning | Then |
| :--- | :--- | :--- |
| `202` | `referenceNumber` and `instructions` (nullable: `bank`, `virtualAccountNumber` or `qrCodeUrl`, and `expiresAt` as Midtrans' own text) | keep the reference; a replayed `202` may already be past `PENDING` |
| `502` with `referenceNumber` | Midtrans refused; the top-up is `FAILED`, nothing was charged | final for that key; a new attempt needs a new key |
| `503` with `referenceNumber` | still `PENDING` and may still be paid | follow it; do not start another |
| `409` | the key was used with another body | repeat the original body or use a new key |
| `422` with `code` `TOP_UP_AMOUNT_ABOVE_LIMIT` or `WALLET_BALANCE_LIMIT_EXCEEDED` (`limit` is a string) | refused | while one is `PENDING`, take the buyer back to that top-up (its instructions, when it has any) |
| any other `503`, a `500`, an answer without payment's JSON, no answer | outcome unknown | send the same key and body again |

- A `PENDING` top-up can have `instructions: null` (no definite Midtrans answer yet or ever): the screen says it could not be started and is waiting to be concluded, keeps reading the status, and never sends the buyer to instructions; payment concludes it `FAILED` only after an hour (`not-found-grace: 1h`) and until then it holds room under the wallet limit. A top-up may not take the wallet above the limit, every `PENDING` top-up counts against it, several can be pending at once, and a top-up cannot be cancelled.
- **Read it:** `GET /api/v1/payment/wallet/customer/topup/{referenceNumber}`. Statuses are `PENDING`, `SUCCESS`, `FAILED`, `EXPIRED`, `NEEDS_ATTENTION` (set aside for a person; nothing credited). Stop polling on anything but `PENDING`; on `SUCCESS` read the balance again; `404` is no such top-up for this buyer. **No endpoint lists a buyer's top-ups** (§7 Q7): a reference lost with the app's data cannot be found again, and the ledger is excluded from backup.

### 4.5 Cart and vouchers (`order`: a session is required; `pricing`: none)

- One cart per buyer, kept by order under the token's `sub`; there is no guest cart. It expires 14 days after its last change, and reading does not extend it: an empty cart is a state. `GET /api/v1/cart` returns `{ customerPrincipalId, items, appliedVoucherCode, subtotal, updatedAt }`; a line is `{ productId, productTitle, unitPrice, quantity, categoryId, merchantPrincipalId, lineTotal }` with the last two ids nullable.
- `POST /api/v1/cart/items` `{ productId: <sku>, quantity }` **adds** to the line. `PUT /api/v1/cart/items/{productId}` sets the quantity (`0` removes the line; for a product not in the cart it writes nothing and answers `200`). `DELETE /api/v1/cart/items/{productId}` removes it. Each answers the whole cart. A line holds 1 to 999. `409 CART_BUSY`: nothing changed, try again. `503 CART_UNAVAILABLE` on a write is outcome unknown (a Redis timeout of the write itself maps to it: read the cart before the edit is offered again; on `GET /cart` it stays "try later"; §7 Q17). `503 CATALOG_UNAVAILABLE`: nothing changed, try later. An unknown outcome of the add is settled by `GET /api/v1/cart`, never by sending it again. Read the endpoint for the refusals an add can get.
- `DELETE /api/v1/cart` is for an explicit "empty the cart" action only, and **never** after a checkout.
- **The two voucher endpoints take different field names.** pricing's preview, `POST /api/v1/pricing/vouchers/apply`, takes `{ code, cart_subtotal, merchant_principal_id }` and answers `200` with the body `null` when the code does not apply. order's `POST /api/v1/cart/voucher` takes **`{ voucherCode }`** (not blank, at most 50 characters; blank is `400 VOUCHER_CODE_REQUIRED`, longer than 50 is `400 FIELD_TOO_LONG`); `DELETE /api/v1/cart/voucher` removes it. `GET /api/v1/pricing/vouchers/{code}` reads the terms (`value`, `min_spend`, `max_discount` nullable, `expires_at`).
- The cart's `subtotal` is at catalogue prices while checkout compares `min_spend` with the subtotal after flash sales and discounts, so a preview can accept a code checkout will not grant: say so, and trust the order's `appliedVoucher`.

### 4.6 Checkout (`order`)

- Every line must share one `merchantPrincipalId`; warn in the cart.
- **Nothing the app can read before the `201` states what a checkout debits:** cart `unitPrice`/`lineTotal`/`subtotal` are catalogue prices captured at add time; checkout prices again, adds shipping, and debits `FinalTotal` (no quote route exists, §7 Q16). The checkout screen shows the cart subtotal labelled an indication, says shipping and discounts are settled at checkout, and after the `201` shows the order's charged amounts.
- `POST /api/v1/orders/checkout` with `Authorization` and `Idempotency-Key` (at most 100 characters; a fresh UUID), body `{ shippingAddress, recipientName, recipientPhone, shippingServiceTier?, voucherCode? }` (at most 500, 128, 32 and 50 characters). **Order does not refuse a missing key: it runs an unprotected checkout.** The API method must be unable to build a request without one. `voucherCode` replaces the cart's code, an empty string checks out with none, and leaving it out uses the cart's; it is part of what order fingerprints under the key. Without a tier, order takes the cheapest that can carry the parcel; a refused tier comes back with `availableTiers`.
- A `201` is an order, normally **`PAID`**. Read `status` from it: a replayed `201` is the order as it stands now and can be further on, or `CANCELLED`. Keep `id` (`OrderId`) and `orderNumber` (`OrderNumber`).
- **One new key per checkout, kept in the ledger with the body until a final answer.** Classify by order's `error` code whenever the body is order's JSON, and by status alone only when it is not:

| Group | Answers | What the app does |
| :--- | :--- | :--- |
| Key spent | `201`; `409 CHECKOUT_ROLLED_BACK` and `409 CHECKOUT_ROLLBACK_PENDING` (also with `replayed: true`; `rolledBack` indicates given back or still being given back; `reason` can start `voucher:`, `flash sale`, `stock for`, `escrow:` — including a short wallet, whose amount sits in a server sentence the app does not show — or `fulfilment:`, though six fixed reasons carry no prefix so match the prefix only and provide general copy); `503 CHECKOUT_DEPENDENCY_UNAVAILABLE` | Final for that key. Another attempt needs a new key (after topping up if `escrow:`) |
| Outcome unknown | `409 CHECKOUT_IN_PROGRESS`; `500 INTERNAL_ERROR`; `503 DATABASE_UNAVAILABLE`; any `502`, `503` or `504` that is not order's JSON; no answer | Repeat the same request with the same key and body. Order finishes or unwinds a stopped checkout after about 5 minutes and cancels one that never started after about 15 |
| Key reused | `422 IDEMPOTENCY_KEY_REUSED` (carries the earlier `orderNumber`) | Repeat the original body exactly, or use a new key for a new intention |
| Key unused, refused | every other `400`; `404 PRODUCT_NOT_IN_CATALOG`; `409 MERCHANT_NOT_TRADING`; every other `422` (`CART_EMPTY`, `CART_SPANS_TWO_MERCHANTS`, `SHIPPING_ADDRESS_NOT_PLACED` with a `reason`, and the rest on the endpoint); order's own `502 SHIPPING_QUOTE_MALFORMED`, `SHIPPING_FEE_CONTRADICTED`, `ORDER_AMOUNTS_UNCHARGEABLE` | Nothing was written or charged. Fix what the code names and send again; the three `502` codes are a server fault and are never repeated automatically |
| Key unused, could not ask | order's `503` with `CART_UNAVAILABLE`, `CATALOG_UNAVAILABLE`, `IDENTITY_UNAVAILABLE`, `ADDRESS_LOOKUP_UNAVAILABLE`, `SHIPPING_UNAVAILABLE` or `PRICING_UNAVAILABLE` | Repeat unchanged after a wait |

- A second key sent while a first attempt is still running reads the same cart and can create a second order and a second debit; that is why a new attempt is refused while one has no final answer (§1.8).
- After a `201`, order removes from the cart only what that checkout bought. **Read the cart again; never clear it.**

### 4.7 Orders (`order`)

- `GET /api/v1/orders/my-orders?page=&pageSize=&status=` answers `{ items, page, pageSize, totalCount }` (`pageSize` 1 to 100, default 10; pages up to 10,000), newest first, ties by `id`. Pages are cut by position: **deduplicate by `id`**, and read page 1 again to see new orders. An unknown `status` filter is `400 STATUS_UNKNOWN`.
- `status` is `PENDING_PAYMENT`, `PAID`, `PROCESSING_FULFILLMENT`, `DELIVERED`, `COMPLETED` or `CANCELLED` (`SHIPPED` and `REFUNDED` exist and are never set). Order has no assigned or picked-up status.
- `GET /api/v1/orders/{orderId}` takes the UUID `id`, never the `orderNumber`. `driverPrincipalId` stays `null` until a courier has delivered.

### 4.8 Live courier tracking (`edge`)

- `wss://<ws host>/ws/v1/events` with `Authorization: Bearer`; when it opens and closes is §1.9. A buyer connects to the edge alone.
- Subscribe: `{ "event": "subscription.subscribe", "requestId": "<uuid>", "schemaVersion": 1, "data": { "orderId": "<order UUID>" } }`. Heartbeat: the same envelope with `"event": "system.ping"` and `"data": {}`. Leaving: `subscription.unsubscribe` with the subscribe's `data`.
- **Two frame families share the socket.**
  - The edge's own (`command.ack`, `command.error`, `system.pong`): `eventId`, `requestId` (absent when the frame we sent could not be parsed), `schemaVersion`, `occurredAt` and `sentAt` in whole seconds, and `data` or `error`. A subscribe ack's `data` carries `{ subscribed, orderId, orderNumber }`, an unsubscribe ack `{ unsubscribed, orderId }`, and `system.pong` `{}` (each matched to the command by `requestId`); an error sits under `error` as `{ code, reason, message, retryable }`.
  - matching's (`courier.location.updated`), at most one frame every 250 ms per order: `event`, `eventId`, `schemaVersion`, `orderId` (here the **order number**), `sequence` (`null` on this event), `occurredAt`, and `data` with `driverId` (an integer: matching's driver row, never a principal id), `latitude` and `longitude` (always present), `bearingDeg` and `speedKmh` (**number or `null`**), `recordedAt`. The published sample shows numbers only.
- `command.error` codes: `INVALID_ORDER_ID`, `FORBIDDEN`, `ORDER_NOT_FOUND`, `INVALID_JSON`, `INVALID_PAYLOAD`, `UNKNOWN_EVENT`, `UNSUPPORTED_SCHEMA_VERSION`, and `UPSTREAM_UNAVAILABLE`, the only `retryable: true` code, which also covers order refusing the handshake token as expired.
- Handshake refusals, each `{ error, message, requestId }`: `401 SESSION_REQUIRED` or `SESSION_ENDED`, `426 WEBSOCKET_REQUIRED`, `429 TOO_MANY_REQUESTS`, `502 IDENTITY_FAILED`, `503 IDENTITY_UNAVAILABLE`. The gateway can also refuse the same handshake first with its own plain-text `429`, `503` or `504`, classified by status alone.

### 4.9 Writing reviews and courier ratings (`review`)

- Offered only when the order's `status` is `DELIVERED` or `COMPLETED`.
- `POST /api/v1/reviews/products` `{ order_id, product_id, rating, comment }`, one per order line: `order_id` is the order's UUID (`orderNumber` is refused with `422`), `product_id` the line's `productId`. `POST /api/v1/reviews/drivers` `{ order_id, rating, comment }`, one per order, only when the order has a `driverPrincipalId`.
- `rating` is a JSON integer from 1 to 5; `comment` is optional and at most 1,000 characters, with NUL (U+0000) stripped by the app before sending (review strips nothing and refuses NUL in `comment` or `product_id` with `422 VALIDATION_FAILED`). Breaking either is `422 VALIDATION_FAILED`, a definite refusal the outbox never retries.
- Keep the id for the notary in a local table: `id` from a `201`, or `review_id` from a `409`, which means it is already done. A write whose outcome is unknown is sent again unchanged and ends in one of those two answers.

### 4.10 Notary proofs (`notary`)

- `GET /api/v1/proofs/liabilities/{epoch}`, `{epoch}` being `latest` or decimal digits without a leading zero: `{ proofs }`, each document kept whole.
- `GET /api/v1/proofs/evidence/orders/{order}` takes the **`orderNumber`**; while `pending` is above 0, ask again later. `GET /api/v1/proofs/evidence/reviews/{review}` takes the review id from §4.9.
- **Registering a wallet address is not built until the signing page exists** (AND-D11, §7 Q9). The contract, for when it does: `GET /api/v1/proofs/registrations` returns the EIP-712 typed data whole (`types`, `primaryType`, `domain` with `chainId` as a JSON number, `message.principal`); the signing page (not the app) adds `holder` and `issuedAt` to `message` at signing time, `issuedAt` being **the current Unix time in whole seconds as a decimal string**. `POST /api/v1/proofs/registrations` sends exactly `{ holder, issuedAt, signature }`; any other field or shape is `400 invalid_request`; `404 no_account`; `409 registration_not_open`; older than 5 minutes, more than a minute ahead, or signed by another address is `403 registration_refused`. The app shows the signing page's URL as a QR code and a link, takes the result back by scan or paste, checks its shape and posts it. No wallet SDK, no relay.

### 4.11 Push and the device-ownership challenge (`notification`)

- `POST /api/v1/notifications/devices` `{ device_token: <FCM token>, platform: "ANDROID" }`, when §1.10 says; a token the buyer already holds is only refreshed. `200`: done (no proof push works). `202` (with `{ expires_at }`): another principal holds the token; wait for the proof message.
- The proof is an FCM data message `{ kind: "DEVICE_VERIFICATION", nonce, expires_at }` (`expires_at` as `Instant.toString` prints it). Answer before `expires_at` with `POST /api/v1/notifications/devices/verify` `{ device_token, nonce }` and the same buyer's access token.
- On the register call: `429 VERIFICATION_ALREADY_PENDING` (a proof is on its way; verify with the nonce already sent, and register again after `Retry-After` only if it never came), `429 VERIFICATION_LIMIT_REACHED` (register again after `Retry-After`), `422 ADDRESS_GONE` (ask the SDK for a new token and register that), `503 SENDER_UNAVAILABLE` (later), `502 SENDER_REJECTED` (do not repeat unchanged). On the verify call: `422 VERIFICATION_FAILED` (the nonce is not the pending one; five tries in all; never resend the same nonce), `422 VERIFICATION_NOT_PENDING` (register again).
- **Forget:** `DELETE /api/v1/notifications/devices` with the **JSON body `{ device_token }`** (§1.6, the Retrofit trap) and the buyer's access token. `204` whether or not this buyer held the token. Without the body it is `400 MALFORMED_BODY` and nothing is forgotten. Called before sign-out and for the old token whenever the SDK replaces it.

### 4.12 Deferred until the backend ships (do not fake)

- **Chat.** Designed as MQTT 5 to EMQX with HTTP for queries; communication and EMQX are not deployed.
- **Seal verified anonymous reports.** Production already serves the reporter and report routes (seal v1.12.0; gateway limits: `POST /api/v1/seal/presentations` 20/min, `POST /api/v1/seal/reports` 10/min); what a buyer app waits for is a published buyer contract (§7 Q14).
- **Wallet registration, App Links, App Check and a client name, order notifications and push routing.** Each waits on an owner (§7).

---

## 5. Plan

One numbering. A slice is done when §8 holds and §0.3 says so. Infrastructure arrives with its first consumer.

### Phases 0 to 2 — built, with corrections

- [x] Phase 0: build configuration, `core/` (result, failure, config, clock), typed ids, routes, theme, `ConfigErrorScreen`, the token store and the single-flight renewer.
- [x] Phase 1: identity and profile clients, `SessionRepository`, `AuthRepository`, `TwoFactorRepository`, `ProfileRepository`, the five screens.
- [x] Phase 2: catalogue, search, pricing and review clients and repositories, the home, search and product detail screens.

What those boxes do not mean: error envelopes are decoded from JSON text in five test classes, while no test decodes a success body; there is no test for a sign-in that answers a session, a wrong password, `WRONG_CODE` leaving the challenge open, register in the repository, 2FA setup or disable (setup/disable have view-model tests over a fake and no repository test), the wrong-password `401` that keeps the session, a refresh answered `400`, `429`, `500` or `504`, or the `me` `401` path at start; the catalogue shows no image; and the defects of §0.2 are in this code.

### Phase R — repair, before any new feature

In this order; each item ends with tests seen red first.

- [x] **R1 Start-up.** `MainActivity` injects only the configuration result; the session graph is reached lazily on the valid branch; the validator catches `URISyntaxException`; no entry point (the worker included) builds Retrofit on an invalid configuration; a release variant refuses to build without valid values; add Network Security Config (refusing cleartext; debug trusts lab CA).
- [x] **R2 Correlation.** Replace the trace-header interceptor with `X-Request-Id` and send `Accept: application/json`; keep the id on every failure and as the crash key; record the route template (Retrofit's `Invocation` tag), never `encodedPath`, as the breadcrumb with a test seen red.
- [x] **R3 Session.** The application-scoped session component and the four-state session (§1.4, §1.5, §1.7); "left the device" from an OkHttp `EventListener`; the three refresh outcomes classified by error code; restore that only a second `401` ends; expiry from receipt time; `role` stored and checked; sign-out as §1.7; the token store and cipher off the main thread behind injected dispatchers; could-not-read as its own state; enable `StrictMode` for disk and network on the main thread in debug builds.
- [ ] **R4 Network layer.** Classification by (status, code, body origin); `ApiErrorParser` for every envelope of §1.6; `204` as success; `JsonElement` for the nullable-body endpoints; `merchantPrincipalId` nullable in the flash-sale and review DTOs and `facets` optional in search; the money and timestamp rules; a decode test per DTO from the published samples through the real converter.
- [ ] **R5 Search.** Remove the `price_desc` chip; map search's error codes to copy.
- [ ] **R6 Persistence.** Two databases (§1.8) with exported schema and migration tests for the ledger; the cache policy (origin and age in the type, stock from the cache as "could not be checked", eviction); the cached profile deleted when the session is cleared. The pending-transaction store is removed here and rebuilt in Phase 3 with its first consumer.
- [ ] **R7 Outbox.** Merged profile entry; diff against the confirmed profile; the "queued" outcome and its copy; the three ends of an entry; recovery of an entry found in flight; isolate and report unrecognised operation or status entries instead of defaulting or running; drains at start, sign-in and foreground; a worker that restores its owner; removal of the three cart operation types and of the review operation types until Phase 4 builds them; removal of the import cycle against the corrected §1.3 table.
- [ ] **R8 Failure copy.** `Failure` without display text; an exhaustive mapper; `res/values-in/` with key parity; plurals and locale formatting; the locale configuration (AND-D10).
- [ ] **R9 Screens.** No lambda in a view model; keyboard options and autofill on every field; remove `rememberSaveable` on passwords/codes (`SignInScreen`, `RegisterScreen`, `TwoFactorSettingsScreen`, `TwoFactorVerifyScreen`) and remove the 2FA challenge token from route arguments; insets and `SystemBarStyle.dark(...)` for both bars; every view model loads once and no screen triggers a load from `LaunchedEffect` (home load joined and cancelled correctly); delete `AppRouterState`, its test and `requiresAuthentication`; move the two root screens into subfolders; product images with one loader.
- [ ] **R10 Flags, telemetry, privacy.** Fetch and read flags, with a "disabled" state per governed screen, or remove them; local adapters beside the Firebase ones; remove the advertising-id permissions; set no user id (AND-D3).
- [ ] **R11 Gate.** Checks for the corrected §1.3 import direction, `Dispatchers.` outside `di/`, a function-type parameter in `viewmodels/`, a destructive fallback outside the cache builder, a string literal passed to a `Failure`, `rememberSaveable` of a secret; each seen red and recorded; AND-D14; then the layout pass for rule 4.
- [ ] **R12 Tests.** The missing auth branches; the second test layer of AND-D13 for Room, the cipher, WorkManager and an activity launch per start state; delete or replace the package-name test.
- [ ] **R13 Design.** A shape scale; every colour role set from a token; a dark platform theme; removal of the unused template colours (AND-D12).

### Phase 3 — cart, wallet, checkout

Three slices, each starting from its page and endpoint file.

- [ ] **3a Cart and vouchers.** Online only; every answer is the whole cart; `CART_BUSY`; `503 CART_UNAVAILABLE` on write settled by reading the cart; the two voucher field names; the single-merchant warning; the preview caveat. The checkout screen shows the cart subtotal as an indication, states shipping and discounts are settled at checkout, and computes no total.
- [ ] **3b Wallet and top-up.** The ledger row of §1.8 arrives here; every row of §4.4's table; the five statuses; polling that stops in the background; several pending top-ups.
- [ ] **3c Checkout.** The classification of §4.6 as a pure function keyed on (status, code, body origin), tested as a table; a new attempt refused while one has no final answer; the unfinished-checkout prompt after a kill (AND-D7); the cart read again after a `201`. `checkout_enabled` and `wallet_topup_enabled` are read here.

### Phase 4 — orders, tracking, reviews, proofs, push

- [ ] Orders list with cross-page deduplication, order detail.
- [ ] The event socket and the tracking screen (§1.9), after AND-D16.
- [ ] Review and rating writes through the outbox, with a local table for the review ids.
- [ ] Notary: liabilities and the two evidence reads, with byte-exact export.
- [ ] Push (§1.10): registration with the (token, principal) pair, the rebuilt challenge handler, the forget call with its body before sign-out, the runtime permission and channels, the one notification that exists, and a push that arrived (e.g. challenge obtained by registering the same token under a second account) in the definition of done.

### Phase 5 — deferred, each when its owner closes the question

- [ ] Wallet registration (the signing page), chat, seal reports, App Links, App Check and a client name, order notifications and push routing.

### Phase 6 — release engineering

- [ ] `versionName` from the tag and a monotonically increasing `versionCode`; the tag must be strict semver and its commit an ancestor of `origin/main`; a preflight that refuses to build without `google-services.json` for `com.kinetix.shop` and without valid base URLs; add `*.jks`, `*.keystore`, `*.p12` to `.gitignore`; the R8 scope (AND-D15) with its keep rules and a run of the minified build; where artifacts go, who holds the keystore, mapping-file retention; the Play data-safety answers.

---

## 6. Decisions

"Ruled" is the user's decision. "Decided" is an engineering choice made on a clear recommendation and open to overrule. "Owed" is the user's to make. "Recorded" is a choice of the original author that stands until changed.

| Id | Decision | Status |
| :--- | :--- | :--- |
| AND-D1 | Dependency injection. Today: Hilt. The estate's rule is one hand-wired composition root and no DI container, with `dig` in notary and search the one recorded exception; the courier app also uses a container, and nothing records whether the rule binds mobile clients or who allowed Hilt here. For keeping it: compile-time graph validation, a view model per back-stack entry, `HiltWorkerFactory`. For removing it: the rule, and parity with `kinetix-ios` | Owed |
| AND-D2 | Gradle, the Android SDK and a JDK run on the host and in CI, against everything-runs-in-Docker (the courier app does the same with FVM). Record the exception, or build a pinned toolchain image | Owed |
| AND-D3 | Firebase: builds without `google-services.json` stay green and the release preflight refuses, a departure from fail-fast. Which products ship (the user ruled Messaging, Crashlytics, Analytics and Remote Config for iOS as parity with this app), whether analytics needs consent, whether a user id is set. Until ruled: no advertising id, no user id | Owed |
| AND-D4 | One `:app` module with layer packages and feature subfolders, gated | Recorded |
| AND-D5 | Two Room databases: a disposable cache and a durable ledger | Decided 2026-11-19 |
| AND-D6 | The outbox carries profile updates, reviews and ratings. No cart write, checkout or top-up. Same as IOS-D16 | Decided 2026-11-19 |
| AND-D7 | An unfinished checkout or top-up found at start is never sent without the buyer. Silent replay can debit the wallet for a cart that changed since, so choosing it is the user's (money). Same as IOS-D17 | Decided 2026-11-19, the conservative choice |
| AND-D8 | Background work (the WorkManager flush, the push handler) may renew an expired session, through the application-scoped renewer and its three outcomes. Without it a 15-minute token means the outbox almost never drains in the background. The cost: a process killed mid-refresh ends the session unseen. iOS decided the opposite for its platform | Decided 2026-11-19 |
| AND-D9 | Correlation is by `X-Request-Id`; no `traceparent` | Decided 2026-11-19 |
| AND-D10 | Multilingual buyer copy on both buyer apps: English default, Indonesian in `res/values-in/` | Ruled 2026-11-19 (IOS-D9) |
| AND-D11 | The notary wallet registration is signed on a page owned by `kinetix-docs`: QR out, QR or paste back. No wallet SDK, no relay | Ruled 2026-11-19 (IOS-D14) |
| AND-D12 | The palette and type scale are the buyer brand of both apps (ruled, IOS-D8). Still to confirm: two accents where the estate has one, and a shape scale in place of Material's pill buttons | Owed |
| AND-D13 | The second test layer: instrumented tests on an emulator in CI only (never part of the laptop gate), or a JVM Android runtime for Room, migrations and wiring | Owed |
| AND-D14 | Compiler and lint warnings as errors, or drop the zero-warnings claim. Recommended: errors | Owed |
| AND-D15 | R8 over the whole app with keep rules for serializers, Retrofit interfaces and Hilt, replacing the three-prefix scope. Nothing on disk says why the scope was narrowed | Owed (Phase 6) |
| AND-D16 | The map for tracking. No library is declared; the estate serves no tiles to buyers; Google Maps needs a key injected at build time | Owed |
| AND-D17 | `CLAUDE.md` is a pointer to this file. `kinetix-ios` keeps two byte-identical files instead | Owed |

---

## 7. Questions for the owning services

Never worked around in the app. The list is shared with `kinetix-ios/AGENTS.md` §7 and keeps its numbers.

| Id | Owner | Question |
| :--- | :--- | :--- |
| Q1 | edge | The handshake token is reused for every subscribe; an expired one surfaces as `UPSTREAM_UNAVAILABLE`, retryable, though no retry on that socket can succeed. Request: answer an expired token as such, or close the socket at `exp` |
| Q3 | notification, order | A visible push carries no data, and only the notary's liability statement is sent today. Routing a tap needs a data field carrying the order's UUID `id`, and order would have to send notifications at all |
| Q4 | gateway, kinetix-docs | Which host serves `/.well-known/assetlinks.json` for App Links |
| Q5 | identity, infrastructure | The client-proof rollout as written ends by refusing requests that name no client (`400 CLIENT_REQUIRED` on register, login, `2fa/verify` and refresh), and no client name exists for the buyer apps: it would lock both out. Owed before that step: a name in identity's client list and a step for the buyer apps in the rollout document |
| Q6 | infrastructure, docs-internal | An operations page for reaching a lab from the emulator over TLS |
| Q7 | payment | No endpoint lists a buyer's top-ups. A pending top-up whose reference was lost still counts against the wallet cap. Should payment list them? |
| Q9 | kinetix-docs | The wallet signing page of AND-D11 |
| Q10 | notification | Push copy is Indonesian only and rendered by the server. With multilingual apps, does a push follow the buyer's language? |
| Q11 | gateway, infrastructure | Are the shared rate budget, the missing `Retry-After` and the 15-second wait intended? Does the client's address survive the load balancer, so identity's per-address throttles are per client? |
| Q12 | payment | Which zone `instructions.expiresAt` is in |
| Q13 | order, edge | Nothing a buyer can read says a courier has been assigned or has picked up the parcel, while the published page words the tracking condition in those terms |
| Q14 | seal | A published buyer-app contract for verified anonymous reports |
| Q15 | kinetix-docs | The REST reference pins older releases than production runs for the gateway (still Kong), identity, order, review and seal |
| Q16 | order | A quote route that prices cart and shipping for an address without placing an order |
| Q17 | order | `503 CART_UNAVAILABLE` on a write maps a Redis timeout of the write itself, making the outcome unknown |
| Q18 | identity | A distinct error code for the wrong-password `401` on 2FA management routes and logout |
| Q19 | edge, matching | No end-of-delivery frame (`picked up`, `delivered`, `cancelled`) on the buyer socket |

---

## 8. Definition of done for a slice

- The contract page and endpoint definitions were read in full; every documented answer of every endpoint the slice calls is classified and tested, the unknown outcomes first.
- Every DTO has a decode test from the published sample, its null and absent variants included, through the real converter.
- Each new test was seen red. Unit tests ran and printed a count; what is Android (Room, the cipher, WorkManager, a screen) ran in the layer AND-D13 names, or §0.3 says that it has not run.
- **The app was started** on an emulator or a device and the slice was exercised by hand; §0.3 records on what.
- Every state renders: loading, content, empty, each failure, "could not be checked" where the contract has an unknown, and "not sent yet" where a write is queued.
- Killing the process at any point of the slice loses nothing the buyer was promised, and nothing of one buyer is shown or sent as another's.
- Copy is in `strings.xml` in both languages; no text in a view model or a `Failure`; content descriptions and font scaling hold.
- No comment, no fake, no skeleton, no hardcoded value, no fallback; the gate is green.
- §0.3 has an entry: what was built, what was run with its counts and on what, what was measured, what is owed.
