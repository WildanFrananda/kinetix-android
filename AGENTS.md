# AGENTS.md — Kinetix Buyer Android Rules (`kinetix-android`)

This file is automatically loaded by the agent whenever working inside `kinetix-android/`. Every rule below is enforced mechanically by `ArchitectureAndLayoutGateTest` during `./gradlew testDebugUnitTest`.

## 1. Hard Code & Style Rules
1. **100% English Only**: All identifiers, resource keys, test names, and markdown files must be in English.
2. **Zero Comments**: No `//`, `/* */`, or KDoc comments in any `.kt` or `.kts` file.
3. **House Layout**:
   - Opening brace `{` stays on the line that opens it.
   - Multiline signatures, calls, and constructors put **every element on its own line** and the closing `)` alone on its own line.
   - Leave a blank line after a closing brace before the next statement, before a final `return`, and between distinct statement groups.
4. **One Declaration Per File**:
   - Every `.kt` file declares **at most one** top-level `class`, `interface`, `object`, `enum class`, `value class`, or `@Composable fun`, and the file name matches that declaration (`SignInScreen.kt` -> `@Composable fun SignInScreen(...)`).
5. **Option A Feature-Grouped Layer Structure**:
   - Every layer package (`core/`, `models/`, `dto/`, `services/`, `repositories/`, `state/`, `viewmodels/`, `ui/screens/`) is organized into feature/domain subfolders (`auth/`, `profile/`, `catalog/`, `pricing/`, `search/`, `review/`, `common/`, `ids/`, `remote/`, `storage/`, `session/`). Never drop files into flat root layer directories.
6. **Retrofit + Central `SafeApiCaller` Only**:
   - Remote HTTP APIs in `services/remote/` are pure Retrofit interfaces (`*ApiService`) returning `Response<T>`.
   - Only `core/network/SafeApiCaller` and `services/session/RetrofitTokenRefreshTransport` may catch `IOException` / `SocketTimeoutException` or map HTTP error codes via `HttpFailureMapper`. Repositories must never duplicate `try/catch (IOException)` or `mapXError` methods.
7. **DTOs & Zero Fallbacks**:
   - DTO properties in `dto/` must use `camelCase` Kotlin identifiers with `@SerialName("snake_case")` when the wire JSON uses `snake_case`.
   - Never add fake default values (`= ""`) on required server fields or fallback empty URLs (`else -> ""`) in DI modules.
8. **Idiomatic Compose Screens & Resource Strings**:
   - Screens in `ui/screens/` must be top-level `@Composable fun XScreen(...)` functions (never `object XScreen { @Composable fun Render }`).
   - Reusable UI widgets belong in `ui/components/`.
   - User-facing copy belongs in `res/values/strings.xml`, never hardcoded in ViewModels. ViewModels expose typed `Failure` instances, and the UI maps them to string resources via `FailureMessageMapper`.
9. **Split Focused Repositories & Shared Form State**:
   - Keep `SessionRepository` (restore/renew/signOut), `AuthRepository` (register/signIn/verify2FA), and `TwoFactorRepository` (setup/enable/disable) separate.
   - Reuse `state/common/AsyncFormUiState` (`FormIdleUiState`, `FormSubmittingUiState`, `FormDelayedUiState`, `FormErrorUiState`) across form screens whose state transitions are identical.
10. **Testing & Gates**:
    - Every Repository and every ViewModel must have unit tests using hand-written fakes in `src/test/`.
    - Always run `./gradlew testDebugUnitTest assembleDebug` and verify `ArchitectureAndLayoutGateTest` passes before finishing a task.
