# AGENTS.md — Kinetix Buyer Android Application (`kinetix-android`)

This file is automatically loaded by Antigravity whenever working inside `kinetix-android/`. It is the authoritative architectural contract, coding standard, mechanical gate specification (`ArchitectureAndLayoutGateTest`), domain reasoning log, and phased implementation plan for **`kinetix-android`** (the customer-facing native Android application for Kinetix, display name **`Kinetix`**, package `com.kinetix.shop`).

Because the Kinetix estate strictly forbids comments in source code (`CONTEXT.md` §1.1), **every design decision, protocol invariant, error classification rule, and implementation plan lives in this document and in test names**. Read this entire file before writing or modifying any file in this repository.

---

## 1. Non-Negotiable Estate, Repository & Mechanical Gate Rules

Always read the **Living Progress & Decisions Log** (§5 at the bottom of this file) before starting any work, and append/update it after completing any step or decision.

Rules marked **(Gate)** below are enforced mechanically by `ArchitectureAndLayoutGateTest` during `./gradlew testDebugUnitTest`; all other rules are mandatory code-review contracts:

1. **100% English Only**: All Kotlin code, Gradle scripts, XML resources (except localized `values-in/` buyer-facing strings if added later), identifiers, test names, commit messages, and markdown files in this repository MUST be written strictly in English.
2. **Zero Comments in Source Code (Gate)**: No `//`, `/* */`, or KDoc comments in `.kt` or `.kts` files (both leading and trailing `//` comments are checked). The only permitted exceptions across the estate are tool/compiler directives when strictly required (e.g., `@Suppress` annotations with a specific rule name when unavoidable). Reasoning belongs in this `AGENTS.md` or in descriptive test method names.
3. **House Code Layout (`CONTEXT.md` §2.1)**:
   - **Same-line opening braces (`{`)**: An opening brace always stays on the line that opens it (K&R / OTBS).
   - **Multiline wrapping**: When a function signature, constructor, call, or composite literal does not fit on one line, place **every element on its own line** and the closing parenthesis `)` alone on its own line. Never put some arguments on the first line and wrap the rest; never share a wrapped line between two arguments.
   - **Blank lines between contexts**: Leave a blank line after a closing brace before the next statement, before a final `return`, and between groups of statements doing different things. Keep an assignment cuddled with the check that reads it. Never place a blank line immediately inside opening or closing braces.
4. **One Declaration Per File (Gate, `CONTEXT.md` §2.2)**:
   - Every `.kt` file declares **exactly one** top-level `class`, `data class`, `value class`, `enum class`, `sealed interface`, `sealed class`, `interface`, `object`, or `fun` (for top-level `@Composable fun` screens/components), and the file name MUST match that exact `PascalCase` declaration name (`OrderSummary.kt`, `AuthRepository.kt`, `SignInScreen.kt` -> `@Composable fun SignInScreen(...)`). Do not place extra top-level helper functions in a file that already declares a type or composable.
   - Never nest an `enum class`, DTO, or helper data class inside another class or group multiple DTOs into a single file (`AuthDtos.kt` is forbidden).
   - Sealed hierarchy variants that carry fields (`data class`) or represent distinct state/failure/route/event types live in their own files and implement the `sealed interface` in the same package.
5. **Option A Feature-Grouped Layer Structure (Gate), Hilt DI & No UseCase Layer**:
   - Do not split `:app` into dozens of `:feature:*`, `:domain:*`, and `:data:*` Gradle subprojects. Keep a single `:app` module under `app/src/main/java/com/kinetix/shop/`.
   - Every layer package (`core/`, `models/`, `dto/`, `services/`, `repositories/`, `state/`, `viewmodels/`, `ui/screens/`) MUST be organized into feature/domain subfolders (`auth/`, `profile/`, `catalog/`, `pricing/`, `search/`, `review/`, `common/`, `ids/`, `remote/`, `storage/`, `session/`). Never drop `.kt` files directly into flat root layer directories.
   - **Dependency Injection uses Dagger Hilt (`com.google.dagger.hilt.android` with KSP)**:
     - Annotate the `Application` class with `@HiltAndroidApp` and `MainActivity` with `@AndroidEntryPoint`.
     - Annotate ViewModels with `@HiltViewModel` and `@Inject constructor(...)`.
     - Annotate `RepositoryImpl` and `Service` classes with `@Singleton` (or unscoped where appropriate) and `@Inject constructor(...)`, binding each `<Name>RepositoryImpl` to its `<Name>Repository` interface via `@Binds` in a Hilt `@Module` under `di/`.
     - Keep domain interfaces (`Repository` and `Service` interfaces) free of Hilt annotations. In unit tests (`src/test/`), instantiate ViewModels and `RepositoryImpl`s directly via their constructors using hand-written fakes.
   - **There is no `UseCase` / `Interactor` layer** (matching `kinetix-courier-mobile`). ViewModels depend on `Repository` interfaces for domain data and on `Service` interfaces only for imperative platform actions.
6. **Retrofit + Central `SafeApiCaller` (Gate) & Expected Failure as Value (`Result<T>` & Sealed `Failure`)**:
   - Remote HTTP APIs in `services/remote/` are pure Retrofit interfaces (`*ApiService`) returning `Response<T>`.
   - Only `core/network/SafeApiCaller` and `services/session/RetrofitTokenRefreshTransport` may catch `IOException` / `SocketTimeoutException` or map HTTP error codes via `HttpFailureMapper`. Repositories must never duplicate `try/catch (IOException)` or `mapXError` methods.
   - Repositories return `Result<T>` (`Ok<T>` or `Err<Failure>`) or typed `Flow`s and **never throw exceptions to a ViewModel**.
   - **Unknown is never a business refusal** (the house failure mode): a `502`, `503`, `504`, DNS/connection error, or timeout means the server could not answer; map it to `UnavailableFailure` / `NetworkFailure` / `UnknownOutcomeFailure`, never to `UnauthorizedFailure`, `Empty`, or `0`.
7. **DTOs (Gate), Zero Hardcoded Fallbacks (Gate) & Fail-Fast Configuration**:
   - DTO properties in `dto/` must use `camelCase` Kotlin identifiers with `@SerialName("snake_case")` when the wire JSON uses `snake_case`.
   - Never add fake default values (`= ""`) on required server fields or fallback empty URLs (`else -> ""`) in DI modules.
   - `HTTP_BASE` and `WS_BASE` have **no default fallback values** in Kotlin code or Gradle. They are injected at build time and validated by `AppConfigValidator` at startup.
   - If `HTTP_BASE` (must start with `https://`) or `WS_BASE` (must start with `wss://`) is missing or malformed, the app renders `ConfigErrorScreen` naming every missing/invalid key instead of starting normal navigation.
   - Cleartext HTTP traffic is disabled in `AndroidManifest.xml` (`android:usesCleartextTraffic="false"`).
8. **Idiomatic Compose Screens (Gate), Navigation Compose, Shared Form State & Resource Strings**:
   - Screens in `ui/screens/` must be top-level `@Composable fun XScreen(...)` functions (never `object XScreen { @Composable fun Render }`).
   - Collect `StateFlow` in Compose via `collectAsStateWithLifecycle()`.
   - Navigation uses `Navigation Compose` (`NavHost` + `@Serializable` routes + per-`NavBackStackEntry` `hiltViewModel()` scoping) so rotation, system Back, and process death preserve navigation cleanly. Never add placeholder routes pointing to unrelated screens.
   - Reusable UI widgets belong in `ui/components/`.
   - User-facing copy belongs in `res/values/strings.xml`, never hardcoded in ViewModels. ViewModels expose typed `Failure` instances (including `ClientValidationFailure(ClientValidationReason)`), and the UI maps them to string resources via `FailureMessageMapper`.
   - Keep `SessionRepository` (restore/renew/signOut), `AuthRepository` (register/signIn/verify2FA), and `TwoFactorRepository` (setup/enable/disable) separate.
   - Reuse `state/common/AsyncFormUiState` (`FormIdleUiState`, `FormSubmittingUiState`, `FormDelayedUiState`, `FormErrorUiState`) across form screens whose state transitions are identical.
9. **Hand-Written Fakes Only, No Mocking Frameworks**:
   - Unit tests live in `app/src/test/java/com/kinetix/shop/` and use hand-written fakes implementing repository or service interfaces (`FakeAuthRepository`, `FakeCatalogRepository`, `FakeTokenStore`).
   - Do not add Mockito, MockK, or any reflection/bytecode mocking library.
   - No test code or test-only branches inside `src/main/`.
   - Always run `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease` and verify `ArchitectureAndLayoutGateTest` passes before finishing a task.

---

## 2. Layered Package Structure (Option A: Layer Packages + Feature/Domain Subfolders)

All production sources live under `app/src/main/java/com/kinetix/shop/` and are organized by layer at the top level with mandatory feature/domain subfolders inside each layer (`ArchitectureAndLayoutGateTest` fails the build if any `.kt` file is placed directly in the root of `core/`, `models/`, `dto/`, `services/`, `repositories/`, `state/`, or `viewmodels/`):

```text
com/kinetix/shop/
├── core/
│   ├── config/     # AppConfig, AppConfigCheckResult, AppConfigValidator, NetworkPolicy
│   ├── failure/    # Sealed Failure interface, ClientValidationReason, and 18 concrete Failure types
│   ├── network/    # SafeApiCaller, HttpFailureMapper, ApiErrorParser
│   ├── result/     # Result, Ok, Err, UnitSentinel
│   └── time/       # Clock, SystemClock, DelayCountdownTimer
├── di/             # Hilt @Module classes (CoreBindingsModule, CoreModule)
├── models/
│   ├── ids/        # @JvmInline value class IDs (AccountId, PrincipalId, ProductSku, etc.)
│   ├── auth/       # StoredSession, SignInOutcome, TwoFactorSetupSecret
│   ├── profile/    # DeliveryProfile, UpdateDeliveryProfileInput
│   ├── catalog/    # Category, StockStatus, ProductSummary, ProductPage, ProductDetail
│   ├── pricing/    # DiscountType, FlashSale, PromotionDiscount, ProductPriceIndication
│   ├── search/     # SearchFacetCount, SearchHit, SearchIndexState, ProductSearchResult
│   └── review/     # ProductReview, ReviewPage
├── dto/            # @Serializable wire DTOs (camelCase properties + @SerialName for snake_case)
│   ├── common/     # ApiErrorResponseDto
│   ├── auth/
│   ├── profile/
│   ├── catalog/
│   ├── pricing/
│   ├── search/
│   └── review/
├── services/
│   ├── remote/     # Retrofit API interfaces (*ApiService.kt returning Response<T>)
│   ├── session/    # TokenRefreshTransport, RetrofitTokenRefreshTransport, SingleFlightSessionRenewer
│   └── storage/    # SessionCipher, KeyStoreSessionCipher, TokenStore, EncryptedFileTokenStore
├── repositories/
│   ├── auth/       # SessionRepository, AuthRepository, TwoFactorRepository + Impls
│   ├── profile/    # ProfileRepository + Impl
│   ├── catalog/    # CatalogRepository + Impl
│   ├── pricing/    # PricingRepository + Impl
│   ├── search/     # SearchRepository + Impl
│   └── review/     # ReviewRepository + Impl
├── routes/         # Sealed AppRoute, concrete routes, AppRouterState
├── state/
│   ├── common/     # Shared AsyncFormUiState (FormIdle, FormSubmitting, FormDelayed, FormError)
│   ├── auth/       # TwoFactorSettingsUiState + variants
│   ├── profile/    # ProfileUiState + variants
│   ├── catalog/    # HomeCatalogUiState, ProductDetailUiState + variants
│   └── search/     # SearchUiState + variants
├── viewmodels/
│   ├── auth/       # SignInViewModel, RegisterViewModel, TwoFactorVerifyViewModel, TwoFactorSettingsViewModel
│   ├── profile/    # ProfileViewModel
│   ├── catalog/    # HomeCatalogViewModel, ProductDetailViewModel
│   └── search/     # SearchViewModel
└── ui/
    ├── theme/      # KinetixPalette, KinetixTypography, KinetixTheme
    ├── components/ # FailureMessageMapper, LoadingIndicatorBlock, ErrorBannerBlock, PaginationControlsRow, ProductSummaryCard
    └── screens/    # Top-level @Composable fun XScreen(...) functions grouped by feature
```

### Layer Rules
- **UI (`ui/screens/`)**: Stateless top-level `@Composable fun XScreen(...)` functions (never `object XScreen`). Render `UiState` with an exhaustive `when` expression. User-facing copy comes from `res/values/strings.xml` and `FailureMessageMapper`.
- **ViewModels (`viewmodels/`)**: Hold screen state in `StateFlow<UiState>`, use `DelayCountdownTimer` for `429 SIGN_IN_DELAYED` timers, and expose typed `Failure` values instead of hardcoded English strings.
- **Repositories (`repositories/`)**: Focused single-responsibility interfaces (`SessionRepository`, `AuthRepository`, `TwoFactorRepository`, `ProfileRepository`, `CatalogRepository`, `PricingRepository`, `SearchRepository`, `ReviewRepository`) delegating transport execution and error classification to `SafeApiCaller`.
- **Services (`services/`)**: Own raw transport and platform APIs (`services/remote/*ApiService.kt` using Retrofit + `kotlinx.serialization` converter, `AndroidKeyStore` encrypted file storage).
- **Value Class IDs (`models/ids/`)**: Use `@JvmInline value class` for domain identifiers (`AccountId`, `PrincipalId`, `ProductSku`, `CategorySlug`, `OrderId`, `OrderNumber`, `ReviewId`, `VoucherCode`, `TopUpReference`, `IdempotencyKey`) so passing an `OrderNumber` where an `OrderId` UUID is required fails at compile time.
- **Mechanical Architecture Gate (`ArchitectureAndLayoutGateTest.kt` + `AGENTS.md`)**: Every `./gradlew testDebugUnitTest` run mechanically verifies zero comments in `.kt`/`.kts`, subfolder organization under layer packages, 1 top-level declaration per file matching the filename, top-level `@Composable fun` screens, camelCase DTO fields, no duplicated `IOException`/`SocketTimeoutException` blocks in repositories, and no empty-string URL fallback in DI.

---

## 3. Verified Backend Contracts & Domain Decisions

Every endpoint path, status code, and protocol rule below is verified against `kinetix-docs/src/content/docs/apis/rest/buyer-app/` and `kinetix-docs/src/site/rest/endpoints/`. Do not invent endpoints or alter field names.

### 3.1 Identity, Session Lifecycle, Sign-In Delay & 2FA
- **No `X-Kinetix-Client` header on buyer calls**: Unlike the courier app, a buyer app sends no `X-Kinetix-Client` header on register, login, `2fa/verify`, or `refresh`; identity accepts buyer requests that name no client.
- **Account Creation (`POST /api/v1/auth/register`)**:
  - Request body: `{ "email": "...", "password": "..." }`. Password minimum length is **12 characters** (verified in `kinetix-identity-service/src/application/dto/register_user_input.dto.ts` `@MinLength(12)`).
  - `201`: Returns a full session (`accessToken`, `refreshToken`, `expiresIn`, `tokenType`, and `user: { id, email, role: "customer", principalId }`).
  - Keep **both** IDs: `user.id` (`AccountId`, integer) is required by identity's own routes (`/api/v1/users/{id}/profile`, `/api/v1/auth/2fa/*/{id}`, `/api/v1/auth/passkeys/{id}`); `user.principalId` (`PrincipalId`, UUID string) is the JWT `sub` used by all other services.
  - `409`: Email already registered; direct the buyer to sign in.
- **Sign-In (`POST /api/v1/auth/login`)**:
  - Request body: `{ "email": "...", "password": "..." }`.
  - `200` has three distinct shapes:
    1. Standard session (`accessToken`, `refreshToken`, `user`) when 2FA is off.
    2. Second-factor challenge (`mfaRequired: true`, `challengeToken`, `expiresIn`, `passkeys: boolean`) when 2FA is enabled. Transition to the 2FA verification step.
    3. Admin enrolment (`mfaEnrolmentRequired: true`, `enrolmentToken`): only returned for `admin` accounts without 2FA. Refuse with `AdminAccountNotSupportedFailure` ("This app is for buyer accounts") and discard the token without calling logout.
- **Sign-In Delay Policy (`429 SIGN_IN_DELAYED`)**:
  - Starting from the 5th failed attempt on an account, identity answers `429` with `error: "SIGN_IN_DELAYED"`, `retryAfterSeconds` in the JSON body, and the `Retry-After` HTTP header (from 1 s up to 60 s) before checking the password or TOTP code.
  - Read `retryAfterSeconds` from the body (falling back to the `Retry-After` header), tick a 1-second countdown in the ViewModel, disable the submit button until the timer reaches zero, and keep the current screen/challenge intact.
- **Two-Factor Challenge (`POST /api/v1/auth/2fa/verify`)**:
  - Request body: `{ "challengeToken": "...", "code": "123456" }`.
  - `200`: Returns a full session.
  - `401` with `error: "WRONG_CODE"`: Keep the challenge open so the buyer can type the next 6-digit code.
  - `401` with `error: "CHALLENGE_EXHAUSTED"` (5 attempts used), `"CHALLENGE_SPENT"`, or `"CHALLENGE_INVALID"`: The challenge can never succeed; return the buyer to the password screen to issue a new challenge.
- **Single-Flight Refresh Token Rotation (`POST /api/v1/auth/refresh`)**:
  - Access tokens live 15 minutes (`expiresIn: 900`); refresh tokens live 30 days and **rotate on every use**.
  - Presenting a spent refresh token a second time is treated by identity as token theft and revokes the entire session family (`401 "This refresh token has already been used; the session has been revoked"`).
  - `SessionRenewer` MUST guard refreshes with a single-flight `Mutex`: concurrent callers that see an expired access token await the in-flight refresh result rather than sending a second refresh request.
  - `TokenStore` persists the entire session (`accessToken`, `refreshToken`, `expiresAtEpochSeconds`, `accountId`, `principalId`, `email`) as a single atomic encrypted payload protected by an `AndroidKeyStore` AES-GCM master key so process death can never leave half of a rotated pair on disk.
  - If a refresh request was sent and the connection dropped after transmit, or returned `500`/`504` or an unreadable `2xx`, identity may have spent the refresh token: clear the stored session and require sign-in rather than re-sending a potentially spent token. If the request never left the phone or answered `429`/`502`/`503`, keep the stored session and retry later.
- **Stored Session Check (`GET /api/v1/auth/me`)**:
  - Called on app startup when a stored session exists. `200` confirms `{ principalId, userId, role, expiresAt }`. `401` when expired triggers `SessionRenewer` once.
- **Sign-Out (`POST /api/v1/auth/logout`)**:
  - Before calling logout, if the handset has a registered FCM device token, call `DELETE /api/v1/notifications/devices` first while the access token is still valid (renewing once if expired).
  - Then send `POST /api/v1/auth/logout` with `Authorization: Bearer <accessToken>` and body `{ "refreshToken": "<refreshToken>" }` so identity revokes only this device's session (omitting `refreshToken` revokes all sessions across all devices; sending `null` is refused with `400`). Discard local tokens even if the network call times out (5 s budget).
- **Managing Account 2FA (`/api/v1/auth/2fa/{setup,enable,disable}/{id}`)**:
  - `{id}` is `AccountId` (`user.id`). Every call requires `{ "password": "..." }` in the body in addition to the Bearer access token.
  - A `401` on these endpoints for a wrong password means the password was wrong, **not** that the session expired: keep the session and prompt for the password again.
  - Enabling or disabling 2FA revokes all *other* sessions of the account while keeping the current session alive.

### 3.2 Delivery Profile (`identity`)
- **Endpoints**:
  - `GET /api/v1/users/{id}/profile` (`{id}` = `AccountId`). Returns `404` on a brand-new account that has never saved a profile.
  - `PUT /api/v1/users/{id}/profile`. Creates the profile on first call and updates only the sent fields on subsequent calls.
- **Rules**:
  - Never send `null` for any field except `avatarUrl: null` (which removes the avatar); `null` on any other field causes a `400` refusal.
  - When the address changes, identity geocodes it onto the road network before answering, which can take several seconds; use an appropriate read timeout and clear loading state.
  - Checkout does not fetch the profile automatically from identity; the app pre-fills `recipientName`, `recipientPhone`, and `shippingAddress` on the checkout screen from this profile.

### 3.3 Browse Catalogue, Search, Promotions & Reviews Read (`catalog`, `search`, `pricing`, `review`)
None of the read endpoints in this section require an access token, so guests can browse and search freely before signing in.
- **Categories (`GET /api/v1/categories`)**:
  - Returns all categories with `id`, `name`, and `slug`. Keep `slug` (`CategorySlug`) for product filtering and search facets.
- **Product List (`GET /api/v1/products`)**:
  - Query parameters: `category` (slug), `q`, `page`, `page_size` (max 100).
  - Each item carries `id`, `sku`, `title`, `category`, `category_slug`, `price`, `currency`, `image_url`, `merchant_principal_id`, `weight_grams`, `available_stock`, `is_in_stock`, and `stock_status` (`"in_stock"`, `"out_of_stock"`, or `"unknown"` when warehouse could not be reached over gRPC).
  - Never display `"unknown"` stock as `"Out of stock"`; show that stock availability could not be checked right now.
- **Product Detail (`GET /api/v1/products/{sku}`)**:
  - Keyed by `sku` (`ProductSku`), never by numeric `id`. Returns `description` and `warehouse_stock`.
- **Flash Sale & Active Discounts (`pricing`)**:
  - `GET /api/v1/pricing/flash-sales/{product_id}` where `{product_id}` is the product's `sku`. A `null` body (`200`) means no active flash sale. Checkout uses `flash_price` only when it is strictly below the catalog price, and when a flash sale applies, regular discounts do not stack on that unit.
  - `GET /api/v1/pricing/discounts` returns the unpaged list of active discounts. A discount matches a product when:
    1. `target_product_id == product.sku`, OR `target_category_id == product.category_slug`, OR both `target_product_id` and `target_category_id` are `null`; AND
    2. `merchant_principal_id == null` OR `merchant_principal_id == product.merchant_principal_id`.
- **Search (`search`)**:
  - `GET /api/v1/products/suggest?q={text}&limit={n}`: Returns `{ "suggestions": [...] }`. Blank `q` returns an empty list.
  - `GET /api/v1/products/search?q={text}&category={slug}&sort={sort}&limit={n}&offset={o}`: Returns `{ total, hits, facets, index: { indexed_through_ms, age_seconds, stale } }`.
  - Each hit's `id` is the product's `sku`. Opening a hit navigates to Product Detail using `GET /api/v1/products/{sku}`.
  - When `index.stale == true`, render the search results normally along with a non-blocking banner indicating the search index may be slightly out of date.
- **Product & Merchant Reviews (`review`)**:
  - `GET /api/v1/reviews/products/{productId}?page={p}` (`{productId}` is `sku`, page >= 1).
  - `GET /api/v1/reviews/merchants/{merchantPrincipalId}?page={p}`.

### 3.4 Kinetix Wallet & Midtrans Top-Up (`payment`)
Checkout pays exclusively from the buyer's Kinetix Wallet balance held by `kinetix-payment-service`.
- **Read Balance (`GET /api/v1/payment/wallet/customer/balance`)**:
  - Requires `customer` role Bearer token. Returns `availableBalance` (and `exists: false` with balance `0` if never funded).
- **Start a Top-Up (`POST /api/v1/payment/wallet/customer/topup`)**:
  - Requires `Idempotency-Key` header (generate and persist per top-up attempt before sending; reuse on network timeout/no-answer).
  - Returns `202` with `referenceNumber` and `instructions` (`virtualAccountNumber` for bank transfer or `qrCodeUrl` for QRIS, plus `expiresAt`).
  - A `503` that still carries `referenceNumber` means the top-up was created as `PENDING` and can still be paid: track that `referenceNumber` in step 5 instead of creating a new one.
  - **IDR 2,000,000 Wallet Cap**: A top-up cannot bring the wallet above IDR 2,000,000, and every `PENDING` top-up counts toward that cap until it leaves `PENDING` (`TopUpService.java`). Top-ups cannot be cancelled. Persist active `PENDING` `referenceNumber`s locally; if `422 WALLET_BALANCE_LIMIT_EXCEEDED` is returned while a top-up is still `PENDING`, direct the buyer back to that pending top-up's payment instructions (`GET /api/v1/payment/wallet/customer/topup/{referenceNumber}`).
- **Poll Top-Up Status (`GET /api/v1/payment/wallet/customer/topup/{referenceNumber}`)**:
  - Poll while `status == "PENDING"`. When `status == "SUCCESS"`, refresh the wallet balance (`GET /api/v1/payment/wallet/customer/balance`).

### 3.5 Redis Shopping Cart & Vouchers (`order`, `pricing`)
- **Authenticated Server Cart Only**: `kinetix-order-service` keeps one cart per buyer in Redis under `KinetixOrder:cart:principal:{principalId}` with a 14-day TTL. All `/api/v1/cart` routes require a Bearer token; there is no unauthenticated guest cart on the backend.
- **Cart Endpoints**:
  - `GET /api/v1/cart`: Returns `{ items, appliedVoucherCode, subtotal }`.
  - `POST /api/v1/cart/items`: Body `{ productId: "<sku>", quantity: <int> }`. Adds to existing line quantity and returns the updated cart.
  - `PUT /api/v1/cart/items/{productId}`: Sets line quantity (`0` removes the line) and returns the updated cart.
  - `DELETE /api/v1/cart/items/{productId}`: Removes the line.
  - `DELETE /api/v1/cart`: Explicit "Empty Cart" action only. **Never** call `DELETE /api/v1/cart` after checkout.
- **Voucher Preview & Attachment**:
  - `GET /api/v1/pricing/vouchers/{code}`: Reads voucher terms (`value`, `min_spend`, `max_discount`, `expires_at`).
  - `POST /api/v1/pricing/vouchers/apply`: Preview with `{ code, cart_subtotal, merchant_principal_id }`. Note that `cart_subtotal` uses catalog prices, whereas checkout evaluates `min_spend` against the post-flash-sale/post-discount subtotal.
  - `POST /api/v1/cart/voucher`: Attaches `{ code }` to the cart.
  - `DELETE /api/v1/cart/voucher`: Removes `appliedVoucherCode` from the cart.

### 3.6 Idempotent Checkout (`order`)
- **Single-Merchant Constraint**: All lines in the cart must share the same `merchantPrincipalId`; otherwise `POST /api/v1/orders/checkout` answers `422 CART_SPANS_TWO_MERCHANTS`. Warn the buyer in the Cart/Checkout UI if lines span multiple merchants.
- **Placing the Order (`POST /api/v1/orders/checkout`)**:
  - Headers: `Authorization: Bearer <token>`, `Idempotency-Key: <UUID>`.
  - Body: `{ shippingAddress, recipientName, recipientPhone, shippingServiceTier? }`. If `shippingServiceTier` is omitted, `order` picks the cheapest tier that fits the parcel weight; if a requested tier is refused, `availableTiers` is returned so the buyer can choose.
  - A `201` means the checkout saga reserved warehouse stock, calculated pricing, debited the buyer's Kinetix Wallet in escrow, and created the order in **`PAID`** status. Keep both `id` (`OrderId` UUID, for reading/reviewing/subscribing to the order) and `orderNumber` (`OrderNumber`, for notary evidence proofs).
- **Four-Way `Idempotency-Key` Outcome Classification**:
  1. **Key Spent (Final Decision)**: `201`, `409 CHECKOUT_ROLLED_BACK` (e.g., `reason: "escrow:..."` when wallet balance is insufficient, or stock reservation failed), `409 CHECKOUT_ROLLBACK_PENDING`, and `503 CHECKOUT_DEPENDENCY_UNAVAILABLE`. The key is now permanently bound to that order attempt; starting a new checkout attempt requires generating a new `Idempotency-Key`.
  2. **Outcome Unknown (Must Retry With Same Key)**: `409 CHECKOUT_IN_PROGRESS`, `500`, `503 DATABASE_UNAVAILABLE`, Gateway `502`/`504`, or network timeout/drop after send. Retry the exact same request with the **same `Idempotency-Key`** so the buyer is never charged twice.
  3. **Key Reused on Different Payload**: `422 IDEMPOTENCY_KEY_REUSED`. Either replay the original body or generate a fresh key for a new intention.
  4. **Pre-Order Validation Refusal (Key Unused)**: `422 CART_EMPTY`, `422 CART_SPANS_TWO_MERCHANTS`, or other `503`s before order row creation. The key was not recorded and may be reused once the precondition is fixed.
- **Post-Checkout Cart Refresh**:
  - On `201`, `order` removes from Redis only the items and quantities bought by that checkout. Always call `GET /api/v1/cart` after a `201` to refresh local cart state; never call `DELETE /api/v1/cart`.

### 3.7 Orders & Live Courier Tracking (`order`, `edge`)
- **My Orders (`GET /api/v1/orders/my-orders?page={p}&pageSize={s}&status={status}`)**:
  - Ordered newest first, tie-broken by `id`. Because pagination is offset-based (`page` up to `10000`), a newly placed order shifts page boundaries: deduplicate items across pages by `id` (`OrderId`), and refresh page 1 to see newly created orders.
- **Order Detail (`GET /api/v1/orders/{orderId}`)**:
  - `{orderId}` MUST be the order's UUID `id`, never `orderNumber`.
- **Live Courier Location Tracking via Edge X21 WebSocket (`wss://<ws-host>/ws/v1/events`)**:
  - Opened only while viewing an active delivery (`PAID` / `PROCESSING_FULFILLMENT` / assigned / picked up) and while the screen is in the foreground.
  - Authenticate with the buyer's Bearer access token.
  - Subscribe frame:
    `{ "event": "subscription.subscribe", "requestId": "<uuid>", "schemaVersion": 1, "data": { "orderId": "<orderUuid>" } }`
    Edge verifies ownership against `GET /api/v1/orders/{orderId}` and replies with `command.ack` (`{ subscribed: true, orderId, orderNumber }`) or `command.error` (`INVALID_ORDER_ID`, `FORBIDDEN`, `ORDER_NOT_FOUND`, `UPSTREAM_UNAVAILABLE`).
  - Receive coalesced courier telemetry frames (at most 1 frame per 250 ms):
    `{ "event": "courier.location.updated", "eventId": "...", "schemaVersion": 1, "orderId": "<orderNumber>", "occurredAt": "...", "data": { "driverId": 17, "latitude": -6.1781, "longitude": 106.8235, "bearingDeg": 87.5, "speedKmh": 21.6, "recordedAt": "..." } }`.
  - Send heartbeat `{ "event": "system.ping", "requestId": "<uuid>", "schemaVersion": 1, "data": {} }` every 25–30 seconds (well inside edge's 60-second idle timeout) and expect `system.pong`.
  - Send `{ "event": "subscription.unsubscribe", "requestId": "<uuid>", "schemaVersion": 1, "data": { "orderId": "<orderUuid>" } }` when leaving the tracking screen, and close the socket when backgrounded.

### 3.8 Product Reviews & Courier Ratings (`review`)
- Offered only when an order's `status` is `DELIVERED` or `COMPLETED`.
- **Product Review (`POST /api/v1/reviews/products`)**:
  - Body: `{ "order_id": "<orderUuid>", "product_id": "<sku>", "rating": 1..5, "comment": "..." }`.
  - `order_id` must be the UUID `id` (`orderNumber` is refused with `422`). Strip any `U+0000` (NUL) characters from `comment` before sending.
  - Keep the returned review ID (`id` from `201`, or `review_id` from `409` which means the buyer already reviewed that line) for notary evidence verification.
  - If a write returns `503` or loses connection before an answer, resending the exact same payload is idempotent and settles to `201` or `409`.
- **Courier Rating (`POST /api/v1/reviews/drivers`)**:
  - Offered only when the order carries a non-null `driverPrincipalId`.
  - Body: `{ "order_id": "<orderUuid>", "rating": 1..5, "comment": "..." }`. Same UUID, NUL-stripping, and `201`/`409` rules apply.

### 3.9 Notary Proofs & On-Chain Statement Registration (`notary`)
- **Liability Proofs (`GET /api/v1/proofs/liabilities/{epoch}`)**:
  - `{epoch}` is `"latest"` or an integer epoch string without leading zeros. Returns `{ proofs: [...] }` proving the buyer's Kinetix Wallet balance committed on chain. Preserve proof JSON documents verbatim for export/verification.
- **Ethereum Wallet Registration for On-Chain Statements**:
  - `GET /api/v1/proofs/registrations`: Fetches the EIP-712 typed-data template (`types`, `domain`, `message.principal`).
  - The buyer signs `{ holder: "<0xAddress>", principal: "<principalId>", issuedAt: "<unixSecondsString>" }` in an external Ethereum wallet using `eth_signTypedData_v4`.
  - `POST /api/v1/proofs/registrations`: Sends `{ holder, issuedAt, signature }` within 5 minutes of `issuedAt` (notary refuses timestamps >60 s ahead of its clock).
- **Order & Review Evidence**:
  - `GET /api/v1/proofs/evidence/orders/{order}`: `{order}` is `orderNumber` (NOT `orderId` UUID). If `pending > 0`, some records await batch anchoring.
  - `GET /api/v1/proofs/evidence/reviews/{review}`: `{review}` is the `ReviewId` from `201` or `409`.

### 3.10 Push Notifications & Device Ownership Challenge (`notification`)
- **Register Handset (`POST /api/v1/notifications/devices`)**:
  - Body: `{ "device_token": "<fcmToken>", "platform": "ANDROID" }`.
  - `200`: Registration complete.
  - `202`: Another principal previously held this FCM token on the handset; `notification` sends a high-priority data push `{ "kind": "DEVICE_VERIFICATION", "nonce": "...", "expires_at": "..." }`.
  - When the FCM service receives `kind == "DEVICE_VERIFICATION"`, immediately call `POST /api/v1/notifications/devices/verify` with `{ "device_token": "<fcmToken>", "nonce": "<nonce>" }` using the same buyer's access token.
  - Handle `429 VERIFICATION_ALREADY_PENDING` (wait for existing nonce), `429 VERIFICATION_LIMIT_REACHED` (wait `Retry-After`), and `422 ADDRESS_GONE` (request a fresh FCM token from Firebase SDK and re-register).
- **Forget Handset (`DELETE /api/v1/notifications/devices`)**:
  - Called before `POST /api/v1/auth/logout` on sign-out, and for the old token whenever `onNewToken` rotates the handset's FCM token.

### 3.11 Deferred Backend Integrations (Do Not Fake)
- **Chat over MQTT 5 (`kinetix-communication-service` + EMQX on `:8883`)**: Design is settled (`kinetix-communication-service/CLAUDE.md`), where messages are published/subscribed over MQTT 5 (`kinetix/communication/principals/{me}/{outbox,replies,messages}`) and queried via HTTP (`/api/v1/conversations`). However, `kinetix-communication-service` and EMQX are not deployed to production yet (`identity.proto` C++ `has_location` collision and pending compose/CI). Do not build fake chat screens or mock chat backends; integrate only when `communication` is live.
- **Seal Verified Anonymous Reports (S3, `kinetix-seal-service`)**: Uses the 1×1 px hidden `WebView` holder runtime (`/api/v1/seal/holder/<digest>/index.html`) once S3c is tagged in production.

---

## 4. Phased Implementation Plan

Work proceeds in strict vertical slices. Every slice must pass `./gradlew testDebugUnitTest assembleDebug` with zero warnings/errors and hand-written fake unit tests before moving to the next phase.

### Phase 0 — Boilerplate Cleanup & Core Foundation
- [x] Replace `KINETIX_ANDROID_AI_CONTEXT.md` with `CLAUDE.md` / `AGENTS.md`.
- [x] Fix `app/build.gradle.kts`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `strings.xml`, and `AndroidManifest.xml`:
  - `minSdk = 26`, `targetSdk = 37`, `compileSdk = 37`, `JavaVersion.VERSION_17`.
  - App name `"Kinetix"`, `rootProject.name = "kinetix-android"`, `namespace = "com.kinetix.shop"`, `applicationId = "com.kinetix.shop"`.
  - Add dependencies in `libs.versions.toml`: Dagger Hilt (`hilt-android`, `hilt-compiler` via KSP, `androidx-hilt-navigation-compose`), `kotlinx-serialization-json`, `kotlinx-coroutines-android`, `retrofit`, `retrofit2-kotlinx-serialization-converter`, `okhttp`, `androidx-lifecycle-viewmodel-compose`, `junit`, `kotlinx-coroutines-test`.
  - Set `android:usesCleartextTraffic="false"` and configure `data_extraction_rules.xml` / `backup_rules.xml` to exclude encrypted session storage.
- [x] Implement `core/` (`Result`, `Ok`, `Err`, `UnitSentinel`, `Failure` sealed hierarchy, `AppConfig`, `AppConfigValidator`, `Clock`).
- [x] Implement `models/` value class IDs (`AccountId`, `PrincipalId`, `ProductSku`, `CategorySlug`, `OrderId`, `OrderNumber`, `ReviewId`, `VoucherCode`, `TopUpReference`, `IdempotencyKey`) and `StoredSession`.
- [x] Implement `routes/` (`AppRoute` sealed interface, concrete routes, `AppRouterState`) and `ui/theme/` + `ConfigErrorScreen` + `AppRootScreen`.
- [x] Implement `services/TokenStore` (atomic AES-GCM encrypted file backed by `AndroidKeyStore`) and `services/SessionRenewer` (single-flight `Mutex` token rotation).
- [x] Verify Phase 0 unit tests (fakes for `Clock`, `SessionCipher`, `TokenStore`, `TokenRefreshTransport`; 14/14 passing after red-gate verification) and Gradle gates.

### Phase 1 — Identity, Session, 2FA & Delivery Profile
- [x] Implement `IdentityApiService` (Retrofit interface) + central `SafeApiCaller` / `HttpFailureMapper` and DTOs for `/api/v1/auth/{register,login,2fa/verify,refresh,logout,me}`, `/api/v1/auth/2fa/{setup,enable,disable}/{id}`, and `/api/v1/users/{id}/profile`.
- [x] Implement split focused repositories: `SessionRepository`, `AuthRepository`, and `TwoFactorRepository` (`repositories/auth/`) handling all 3 login 200 shapes, `429 SIGN_IN_DELAYED` countdown extraction, `WRONG_CODE` vs spent challenge, single-flight refresh, and keeping session on wrong password in 2FA management.
- [x] Implement `ProfileRepository` & `ProfileRepositoryImpl` (`repositories/profile/`, `GET`/`PUT` profile, partial field diffing, never sending `null` except `avatarUrl: null`).
- [x] Build top-level `@Composable` screens `SignInScreen`, `RegisterScreen`, `TwoFactorVerifyScreen`, `TwoFactorSettingsScreen`, and `ProfileScreen` with their ViewModels, shared `AsyncFormUiState`, and `strings.xml` + `FailureMessageMapper`.
- [x] Write unit tests with hand-written fakes covering every auth branch, `SIGN_IN_DELAYED` timer lock, refresh token rotation/revocation, and profile update.

### Phase 2 — Browse Catalogue, Search, Promotions & Read Reviews
- [x] Implement `CatalogApiService`, `SearchApiService`, `PricingApiService`, and `ReviewApiService` (Retrofit interfaces under `services/remote/`).
- [x] Implement `CatalogRepository`, `SearchRepository`, `PricingRepository` (including client-side discount matching rules and flash-sale priority), and `ReviewRepository` under Option A feature subfolders.
- [x] Build top-level `@Composable` screens `HomeScreen` (categories, paged product feed, stock status handling including `"unknown"`), `SearchScreen` (debounced suggestions, faceted search, `index.stale` notice), and `ProductDetailScreen` (warehouse stock, flash sale/discount indicator, product & merchant reviews).
- [x] Enforce structural quality via `kinetix-android/AGENTS.md` + `ArchitectureAndLayoutGateTest` and verify all 39/39 unit tests + `assembleDebug` passing cleanly with zero compiler warnings.

### Phase 3 — Kinetix Wallet Top-Up, Redis Cart, Vouchers & Idempotent Checkout
- [ ] Implement `PaymentApiService` and `WalletRepository` (`GET /api/v1/payment/wallet/customer/balance`, `POST /api/v1/payment/wallet/customer/topup` with persistent `Idempotency-Key`, `GET /api/v1/payment/wallet/customer/topup/{referenceNumber}`, and IDR 2,000,000 cap / pending top-up redirection).
- [ ] Implement `OrderCartApiService` and `CartRepository` (`/api/v1/cart`, `/api/v1/cart/items`, `/api/v1/cart/voucher`, voucher terms & preview, single-merchant check).
- [ ] Implement `CheckoutRepository` (`POST /api/v1/orders/checkout` with the 4-group `Idempotency-Key` state machine, profile pre-fill, shipping tier selection, and post-201 `GET /api/v1/cart` refresh without `DELETE /api/v1/cart`).
- [ ] Build `WalletScreen` (balance + top-up VA/QRIS instructions), `CartScreen`, and `CheckoutScreen`.
- [ ] Write unit tests verifying all 4 `Idempotency-Key` outcome categories, `CART_SPANS_TWO_MERCHANTS`, `CHECKOUT_ROLLED_BACK` (`escrow:` insufficient balance), and pending top-up recovery on `WALLET_BALANCE_LIMIT_EXCEEDED`.

### Phase 4 — Orders, Live Courier Tracking (Edge X21), Write Reviews, Notary Proofs & Push Notifications
- [ ] Implement `OrderRepository` (`GET /api/v1/orders/my-orders` with cross-page ID deduplication, `GET /api/v1/orders/{orderId}`).
- [ ] Implement `EdgeEventSocketService` & `OrderTrackingRepository` (`wss://<ws-host>/ws/v1/events`, `subscription.subscribe`, `system.ping`/`system.pong` heartbeat, `courier.location.updated`, `subscription.unsubscribe`, foreground-only lifecycle).
- [ ] Implement write methods on `ReviewRepository` (`POST /api/v1/reviews/products`, `POST /api/v1/reviews/drivers` with UUID enforcement, NUL character stripping, and `201`/`409` `review_id` capture).
- [ ] Implement `NotaryApiService` & `NotaryRepository` (`/api/v1/proofs/liabilities/{epoch}`, `/api/v1/proofs/registrations`, `/api/v1/proofs/evidence/orders/{orderNumber}`, `/api/v1/proofs/evidence/reviews/{reviewId}`).
- [ ] Implement `NotificationApiService` & `PushDeviceRepository` (`POST /api/v1/notifications/devices`, `202` `DEVICE_VERIFICATION` FCM data message handler -> `POST /api/v1/notifications/devices/verify`, and `DELETE /api/v1/notifications/devices`).
- [ ] Build `OrderListScreen`, `OrderDetailScreen`, `OrderTrackingScreen`, `WriteReviewScreen`, and `NotaryProofsScreen`.
- [ ] Write unit tests for WebSocket subscribe/heartbeat/location frames, review `409` idempotency, notary proof fetching, and FCM `202` challenge verification.

### Phase 5 — Deferred Ecosystem Features (When Backend Ships)
- [ ] **MQTT 5 Chat (`kinetix-communication-service` + EMQX `:8883`)**: Implement once `identity.proto` C++ fix and EMQX deployment are live.
- [ ] **Seal S3 Verified Anonymous Reports (`kinetix-seal-service`)**: Implement hidden WebView holder runtime once S3c is tagged in production.

---

## 5. Living Progress & Architectural Decisions Log

Always read this section before starting any task and append an entry after every completed milestone.

### 2026-11-19 — Production Roadmap Agreed & Step 1 (Audit Hardening + Navigation Compose + CI/Hygiene) Completed
- **Agreed Production Roadmap**:
  1. **Step 1 (DONE)**: Fix all critical audit defects, migrate to Navigation Compose, add Gradle dependency locking, verify `lintDebug` + `assembleRelease` (R8), add GitHub Actions CI, and unify `CLAUDE.md` -> `AGENTS.md`.
  2. **Step 2 (NEXT)**: Production foundation — Room DB (read cache for catalog/products/profile/orders + durable storage for active checkout `Idempotency-Key` & pending Midtrans top-up `referenceNumber`) + Room Outbox + `WorkManager` (for safe offline mutations: profile updates, reviews, cart sync; checkout & top-up remain online-only), OkHttp W3C `traceparent` interceptor for distributed tracing, and Hybrid Firebase (`Crashlytics`, `Remote Config`, `Analytics`, `FCM` setup — requires `app/google-services.json`).
  3. **Step 3**: Phase 3 — Kinetix Wallet Top-Up, Redis Cart, Vouchers & Idempotent Checkout built directly on Navigation Compose + Room + Outbox + Firebase.
  4. **Step 4**: Phase 4 — Orders, Edge X21 Live Courier Tracking, Write Reviews (via Outbox), Notary Proofs, FCM Device Ownership Challenge & App Links.
- **Completed in Step 1**:
  - **Profile `404` bug fixed**: `ProfileViewModel` treats `NotFoundFailure` from `GET /api/v1/users/{id}/profile` as `ProfileLoadedUiState(profile = null)` so new buyers can create their first delivery profile via `PUT`.
  - **Navigation Compose (`NavHost` + `@Serializable` routes)**: Replaced manual `remember { AppRouterState }` in `AppRootScreen` with `rememberNavController()` + `NavHost` + `rememberSaveable` pending auth target + Material icons + per-`NavBackStackEntry` `hiltViewModel()` scoping + `collectAsStateWithLifecycle()` across all screens; removed placeholder `CartRoute`/`WalletRoute`/`OrdersRoute`/`OrderDetailRoute`.
  - **Search, Home, Logout, Discount & Fallback fixes**: Added `200ms` debounce + job cancellation in `SearchViewModel`; guarded `HomeCatalogViewModel.loadInitialCatalog()` against redundant reloads on back navigation (with explicit `refresh()`); added `5s` `withTimeoutOrNull` on logout and preserved `CancellationException`; added `appliedDiscount` to `ProductPriceIndication` so badge matches effective price; introduced typed `ClientValidationFailure(ClientValidationReason)` and `NetworkPolicy.DEFAULT_RETRY_AFTER_SECONDS`; verified `@MinLength(12)` against `kinetix-identity-service`.
  - **Build, Lint, R8 & CI Hygiene**: Added `androidTestImplementation(platform(libs.androidx.compose.bom))` (fixing `lintDebug`), enabled Gradle dependency locking (`app/gradle.lockfile`), moved machine-specific Homebrew JDK paths from repo `gradle.properties` to `~/.gradle/gradle.properties`, updated `.gitignore` to ignore `.idea/`, added `.github/workflows/ci.yml`, tightened `ArchitectureAndLayoutGateTest` to catch trailing `//` comments, and collapsed `CLAUDE.md` to a pointer to `AGENTS.md`.
  - **Verification**: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease` passed cleanly (52/52 unit tests passing, zero compiler warnings, `lintDebug` green, R8 `assembleRelease` green).

### 2026-11-19 — Estate Repo Governance & CI/CD Added
- Added `.coderabbit.yaml` (assertive auto-review enforcing `AGENTS.md` rules on `main` & `development`), `.github/dependabot.yml` (weekly Gradle & GitHub Actions updates targeting `development`), `.github/PULL_REQUEST_TEMPLATE.md`, `.gitlab-ci.yml` (for GitLab mirror), `.github/workflows/release.yml` (tag-triggered `v*.*.*` signed release build & GitHub Release), and upgraded `.github/workflows/ci.yml` to the estate `verify` aggregator job (`test-and-lint` + `build` -> `verify`) so the `main` branch ruleset requires a single check (`verify`).
- Addressed all 14 CodeRabbit findings on PR #1 (`development -> main`): scoped workflow permissions (`contents: read` / `{}`) and `persist-credentials: false`, added conditional release signing config in `app/build.gradle.kts` + mandatory secret check and `apksigner`/`jarsigner` verification in `release.yml`, guarded `ProfileRoute`/`TwoFactorSettingsRoute` against cleared sessions after `restoreSession()`, fixed `SearchScreen` process-death initialization guard (`state is SearchIdleUiState`), invalidated pending suggestions on query change in `SearchViewModel`, separated `PASSWORD_REQUIRED` from `INVALID_TOTP_CODE` in `TwoFactorSettingsViewModel`, removed hardcoded English detail on malformed MFA `UnreadableAnswerFailure`, replaced regex comment check with literal-aware scanner + generalized `IOException` catch regex in `ArchitectureAndLayoutGateTest`, and expanded unit tests (verified via `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`: 55/55 unit tests passing, 0 compiler warnings, `lintDebug` and R8 `assembleRelease` green).
- Added Trivy filesystem vulnerability scanning (`aquasecurity/trivy-action@v0.36.0`, `CRITICAL`, `ignore-unfixed: true`) and SPDX SBOM (`sbom.spdx.json`) generation to `.github/workflows/ci.yml` (as a parallel `security` job gated by `verify`, with 30-day artifact retention) and `.github/workflows/release.yml` (attached alongside signed APK and AAB on GitHub Releases).
- Mitigated Trivy `CRITICAL` finding `CVE-2026-8763` (`org.bouncycastle:bcprov-jdk18on:1.80.2`, pulled transitively by `androidLintTool`) by substituting `org.bouncycastle:*:1.80.2` -> `1.85` in `app/build.gradle.kts` and regenerating `app/gradle.lockfile` (verified via `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`).
- Applied the 4 valid Gradle updates from Dependabot PR #7 (`composeBom` `2026.02.01` -> `2026.09.00`, `navigationCompose` `2.9.6` -> `2.10.2`, `hiltNavigationCompose` `1.3.0` -> `1.4.0`, `coreKtx` `1.10.1` -> `1.19.1`), kept `ksp` at `2.3.2` (added `com.google.devtools.ksp` to Gradle `ignore` in `.github/dependabot.yml`), regenerated `app/gradle.lockfile`, and verified all 55/55 unit tests, `lintDebug`, `assembleDebug`, and R8 `assembleRelease` passing.
