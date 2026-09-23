# Santinho

A private note with the candidate numbers someone wants to carry into the voting booth. It is
encrypted on the device, never leaves it, and reports nothing to analytics. Android only.

## Files

- Model: `data/santinho/Santinho.kt` (`Santinho`, `CargoDaUrna`, `CampoDoSantinho`)
- Serialisation: `data/santinho/CodigoDoSantinho.kt` (`paraTexto`, `santinhoDeTexto`, `ORDEM_GRAVADA`)
- Storage: `data/santinho/SantinhoRepository.kt`, `data/santinho/CofreLocal.kt` (expect) plus
  `composeApp/src/androidMain/.../data/santinho/CofreLocal.android.kt` (AES-GCM, Android Keystore)
- Identity check: `composeApp/src/androidMain/.../data/santinho/ConfirmacaoDeIdentidade.android.kt`
- UI: `features/santinho/SantinhoViewModel.kt`, `SantinhoState.kt`, `SantinhoScreen.kt`,
  `SantinhoBanner.kt` (Home card), `AtalhoDoSantinho.kt` (shortcut next to the wordmark)
- Tables: `Santinho.sq` (a single row, `id = 1`, holding an encrypted blob), `Preferencia.sq`
  (whether the banner was dismissed); migrations `16.sqm` and `17.sqm`
- Strings: `santinho_*` in `composeResources/values/strings.xml`

## The offices

`CargoDaUrna`, in the order the voting machine asks for them, with its digit count:
DEPUTADO_FEDERAL (4), DEPUTADO_ESTADUAL (5, labelled "Estadual ou Distrital" for the DF),
SENADOR (3), SEGUNDO_SENADOR (3), GOVERNADOR (2), PRESIDENTE (2).

2026 elects two thirds of the Senate, so the machine asks for a senator twice. In a one-third
year, the second field just stays blank. Election day is 4 Oct 2026, with a possible run-off on
25 Oct for president and governor.

## Rules that are easy to break

- **Storage order is append-only.** The note is fixed-width text with no delimiters, in the order
  of `ORDEM_GRAVADA`, **not** `CargoDaUrna.entries`. Reorder the enum freely for the screen. A new
  office goes on the **end** of `ORDEM_GRAVADA`. Inserting one in the middle shifts every field
  after it and corrupts notes already on phones without any error: that is what would have
  happened when SEGUNDO_SENADOR was added (`095ad52`). `santinhoDeTexto` accepts shorter, older
  texts. There is a test with a legacy 16-character note.
- **Every note is the same length** (19 characters today), padded with spaces. AES-GCM output is
  as long as its input, so a variable length would reveal how much someone wrote down. Padding is
  a space, not zero, because `07` is a real number.
- **Digits only, never validated against candidates.** The app does not know who the candidates
  are, and looking a number up would turn a private note into a record of how someone votes.
  `Santinho.com()` filters to digits and cuts to the office's box count.
- **No analytics, no network, no sync.** Not even "screen opened". Don't add any.
- **The identity check guards saving, not the key.** The key does not use
  `setUserAuthenticationRequired`: if it did, turning off the lock screen would make the note
  unreadable, and every read would ask for a fingerprint. Revealing the numbers asks for
  confirmation; hiding them never does. The prompt accepts `BIOMETRIC_WEAK or DEVICE_CREDENTIAL`,
  so people with only a PIN are covered.
- **A note that won't decrypt comes back empty**, not as an error. The keystore drops the key when
  the lock screen is removed.
- **Two ViewModels are alive at once** (the Home banner and the editing screen), and both observe
  the table. A save only reaches the screen through that flow. Something written elsewhere never
  overwrites digits that are still being typed (`alterado`).
- **The banner, once dismissed, never comes back.** The shortcut next to the wordmark remains the
  way in. Its one pulse is kept in the Home-scoped ViewModel (`brilhoPendente`) so it doesn't fire
  on every return to the Home.
- **A failed migration deletes the santinho.** The Android database fallback
  (`DatabaseDriverFactory.android.kt`) deletes `magna.db` and starts over. See `CLAUDE.md`.
- iOS and desktop: `CofreLocal.disponivel = false`, and the screen says so instead of offering
  the feature.

## Tests

`data/santinho/CodigoDoSantinhoTest.kt`
