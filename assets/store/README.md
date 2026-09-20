# Play Store listing assets

Everything the Play Console asks for, and what each file satisfies.

| File | Size | Play requirement |
|---|---|---|
| `play_store_512.png` | 512×512 | App icon. 32-bit PNG, no transparency, no rounded corners — Play applies its own mask. |
| `feature_graphic_1024x500.png` | 1024×500 | Feature graphic. Required for every listing. |
| `screenshots/01..08` | 1350×2400 | Phone screenshots. Between 2 and 8; each side 320–3840 px; 9:16. |

## Why the screenshots are 1350 wide and the device is 1080

The Pixel 7a is 1080×2400, which is 9:20 — a ratio of 2.22:1. Play refuses anything past
2:1, so the raw captures would have been rejected on upload. They are matted out to
1350×2400, which is exactly 9:16, in the icon's own background colour (#5A7AA0). Nothing
was cropped; the whole screen is still there.

## What is in each screenshot

1. Home — the four sections, and the santinho shortcut beside the wordmark
2. Deputados — 648 deputados, search and the filter row
3. Deputado — gabinete, contacts, and the expenses tab
4. Deputado — the voting record tab
5. Partidos — the hemicycle with two benches picked out and lifted
6. Partido — the party's own record, composition and deputies by state
7. Comissões — the permanent committees ranked by how much they vote
8. Santinho — empty, showing the privacy notice and none of anybody's numbers

Captured on a Pixel 7a running the build the feature was tested on, with the status bar
in demo mode (10:00, full battery, no notifications) so nothing personal is in frame.

## Still missing, and only Claude Design can supply it

The 512 PNG covers the Play listing but not the app itself. The launcher icon needs:

- `foreground.svg` — 108×108 viewBox, transparent, art inside the centre circle of radius 33
- `monochrome.svg` — same geometry, one flat fill, for Android 13 themed icons
- `ios_1024.png` — 1024×1024, no alpha, no rounded corners, for `iosApp/Assets.xcassets`

Until those arrive, `androidApp/src/main/res/` still carries the placeholder mark.
