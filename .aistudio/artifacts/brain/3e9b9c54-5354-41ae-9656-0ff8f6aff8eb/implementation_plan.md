# NOVA AI - Full Android Personal Assistant

NOVA AI is a native Android personal intelligence assistant built with Jetpack Compose (Material 3) for Android 16, combining multimodal Gemini reasoning with local device automation, speech synthesis/recognition, a floating overlay interface, and persistent memory.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and aesthetic choices were confirmed through user input:
> - **AI Integration Architecture**: Direct SDK via secure `BuildConfig` secret injection with modular backend proxy abstraction for development and production flexibility.
> - **Visual Identity**: Deep Obsidian Dark with electric cyan (`#00F2FE`) and vivid violet (`#7F00FF`) neon accents.
> - **Home Screen Experience**: Unified conversational feed with intelligent quick-trigger device action chips and persistent system status.
>
> Remaining technical configurations:
> - **Model Defaults**: `gemini-3.8-flash` for high-speed device actions and general conversation; configurable to `gemini-3.1-pro-preview` / `gemini-4-argon` for complex reasoning and multimodal tasks.
> - **Local Data Persistence**: Room SQLite database with reactive Kotlin Flow for chat history, action logs, and assistant preferences.

---

### 1. Overview & Core Concept

- **What It Does**: NOVA AI acts as an on-device personal copilot. Users can interact via text or voice to query AI models, execute native phone operations (launching apps, creating alarms/timers, scheduling calendar events, opening URLs, dialing numbers, composing SMS, checking battery, sharing content, and launching navigation), operate through a floating quick-access overlay, and retain conversation history.
- **Target Audience / Persona**: Power users seeking a unified, responsive Android companion tailored for modern devices like the Vivo T4x 5G on Android 16.
- **Key Value**: Bridges high-level generative AI reasoning with real, verifiable native Android operating system capabilities without mock fallbacks or fabricated responses.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Conversational Assistant & Tool Calling**: User inputs a request (e.g., "Set a timer for 10 minutes and check my battery"). NOVA AI processes the prompt, streams the thought/response, executes the native Android tools with system confirmation, and presents interactive action result cards in the stream.
2. **Push-to-Talk Voice Interaction**: User taps or holds the microphone button; Speech-to-Text transcribes speech with live visual waveforms, streams the Gemini reply, and speaks aloud using Android Text-to-Speech (TTS) with interrupt controls.
3. **Floating Assistant Overlay**: When enabled via system overlay permission, a sleek glowing floating bubble docks to screen edges over any app. Tapping reveals a mini floating console for quick voice or text commands without switching apps.
4. **Action History & Permissions Dashboard**: Users can review executed device automation tasks, grant or revoke granular Android permissions, and configure assistant personality and model parameters.

#### Visual Identity & Theme
- **Aesthetic Direction**: Futuristic Luxury Obsidian — high-contrast dark surfaces, luminous glowing borders, crisp typography, and refined spatial rhythm.
- **Color Palette**:
  - `Background`: Deep Obsidian (`#0B0E14`)
  - `Surface / Cards`: Slate Obsidian (`#141923`) and Elevated Dark (`#1E2433`)
  - `Primary Accent`: Electric Cyan (`#00F2FE` / `#4FACFE`)
  - `Secondary Accent`: Vivid Violet (`#7F00FF` / `#A855F7`)
  - `Success / State`: Emerald Teal (`#10B981`)
  - `Warning / Notice`: Amber Orange (`#F59E0B`)
  - `Text High-Contrast`: Pure White (`#F8FAFC`) and Muted Slate (`#94A3B8`)
- **Typography & Hierarchy**: Clean, modern sans-serif typography with high legibility (`sp` scaled), clear typographic hierarchy for thought reasoning blocks, tool execution tags, and chat bubbles.
- **Interactive Feedback**: Ripple effects on all touchable elements (minimum 48dp target), animated status indicators, smooth staggered message animations, and haptic feedback on action triggers.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct SDK with Modular API Client vs Server-Only Proxy**
  - *Chosen Approach*: Implement a modular repository layer that communicates with Gemini REST/Streaming endpoints using `BuildConfig.GEMINI_API_KEY`, backed by an interchangeable interface (`AiService`). This satisfies immediate development and deployment on AI Studio while permitting a drop-in remote proxy URL in the settings panel.
  - *Why*: Allows zero-friction verification in the AI Studio streaming emulator while adhering to Android security standards and avoiding client-side credential hardcoding.
- **Decision 2: Verifiable Native Device Actions vs Mock Simulation**
  - *Chosen Approach*: Use real Android standard `Intent` actions (`ACTION_SET_TIMER`, `ACTION_SET_ALARM`, `ACTION_INSERT` for calendar, `ACTION_SENDTO` for SMS, `ACTION_DIAL`, `BatteryManager`, `ACTION_VIEW` for web and maps). The action registry returns structured status results (`Executed`, `DispatchedIntent`, `PermissionRequired`, `Failed`).
  - *Why*: Avoids fake simulated states; requires user confirmation or OS handoff for sensitive actions (SMS send, phone calls, calendar inserts).
- **Decision 3: Floating Overlay Architecture with Android 16 Compliance**
  - *Chosen Approach*: Foreground service with `TYPE_APPLICATION_OVERLAY` requiring explicit `Settings.canDrawOverlays` permission check and graceful notification handling.
  - *Why*: Complies with Android 14–16 background service execution limits while delivering genuine multi-tasking utility.

---

### 4. Technical Architecture & Data Strategy

#### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        NOVA AI Android App                             │
├────────────────────────────────────────────────────────────────────────┤
│  UI Layer (Jetpack Compose M3)                                         │
│  ┌───────────────────────┐  ┌──────────────────────┐  ┌──────────────┐ │
│  │ AssistantHomeScreen   │  │ VoiceWaveformSheet   │  │ OverlayPanel │ │
│  └───────────┬───────────┘  └──────────┬───────────┘  └──────┬───────┘ │
│              │                         │                     │         │
│  ViewModel & State Management (NovaAssistantViewModel)                 │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ State: Messages Flow, Listening State, Action Execution State    │  │
│  └───────────┬─────────────────────────┬────────────────────┬───────┘  │
│              ▼                         ▼                    ▼          │
│  ┌───────────────────────┐  ┌──────────────────────┐ ┌───────────────┐ │
│  │  AiOrchestrationRepo  │  │ DeviceActionRegistry │ │ VoiceSubsystem│ │
│  └───────────┬───────────┘  └──────────┬───────────┘ └───────┬───────┘ │
│              │                         │                     │         │
│  Data & Integration Layer              │                     │         │
│  ┌───────────▼───────────┐  ┌──────────▼───────────┐ ┌───────▼───────┐ │
│  │ Gemini REST / SSE API │  │ Android OS Intents & │ │ Android TTS & │ │
│  │ (Retrofit + OkHttp)   │  │ Sensor/Battery APIs  │ │ SpeechRecognizer│
│  └───────────┬───────────┘  └──────────────────────┘ └───────────────┘ │
│              │                                                         │
│  Persistence Layer                                                     │
│  ┌───────────▼───────────────────────────────────────────────────────┐  │
│  │ Room Database: ChatMessageEntity, AutomationLogEntity, Settings   │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

#### Core Entities & Schemas

1. **`ChatMessage`**:
   - `id: Long` (Auto-generated primary key)
   - `sessionId: String`
   - `role: String` ("user" | "model" | "system")
   - `content: String`
   - `toolCallName: String?`
   - `toolCallArgs: String?`
   - `toolResult: String?`
   - `timestamp: Long`

2. **`DeviceAction` Registry**:
   - `OpenApp(packageName, appName)`
   - `WebSearch(query, url)`
   - `SetTimer(durationSeconds, label)`
   - `SetAlarm(hour, minute, message)`
   - `CreateCalendarEvent(title, startTime, endTime, description)`
   - `DraftSms(phoneNumber, message)`
   - `DialPhone(phoneNumber)`
   - `GetBatteryInfo()`
   - `LaunchNavigation(destinationQuery)`
   - `ShareContent(text, subject)`

3. **`AutomationLog`**:
   - `id: Long`, `actionType: String`, `parameters: String`, `status: String`, `timestamp: Long`

#### Delivery Plan (Incremental Phases)
- **Phase 1 (Active)**: Architecture blueprint, design alignment, Gradle dependency verification, and permissions baseline.
- **Phase 2**: Minimal compiling Android application with pristine Deep Obsidian M3 theme, Gemini API integration, and interactive multi-turn chat with quick-action chips.
- **Phase 3**: Device action registry with concrete native Android APIs (intents, battery, timers, alarms, calendar, SMS, dialer, navigation) and permission flow.
- **Phase 4**: Voice subsystem (Speech-to-Text input with waveform animations, Text-to-Speech playback with voice selection and interruption).
- **Phase 5**: Movable floating overlay assistant service (`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`) with compact quick-chat panel.
- **Phase 6**: Room database persistence, action history dashboard, settings/personality customization, and unit/Robolectric test verification.
