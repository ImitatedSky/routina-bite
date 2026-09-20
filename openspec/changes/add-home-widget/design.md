# Design: add-home-widget

## Context

v0.7.0 加了桌面捷徑（長按圖示的選單＋可釘選的獨立圖示），解決的是「快速記一筆」。
小工具解決的是另一半：**不用開 App 就看得到今天的狀態**。兩者互補，不是替代。

小工具的管線大部分在 v0.7.0 就有了：`ShortcutActivity`（隱形跳板、記完 toast）與
`MainActivity` 的 `EXTRA_OPEN_ADD`／`EXTRA_OPEN_TODAY`。這一版只是把它們接到 `RemoteViews` 的按鈕上。

## Goals / Non-Goals

**Goals:**
- 一眼看到「今天還能吃多少」與「水喝了多少」
- 不開 App 就能記一杯水
- 跨日自己換、App 裡改了資料就即時反映

**Non-Goals:**
- 多尺寸版面切換、三大營養素、歷史趨勢（那些開 App 才看）
- 不用 Glance（D55）

## Decisions

### D55. 用 `RemoteViews` + `AppWidgetProvider`，不用 Glance
`androidx.glance` 會再拉進一整套 `org.jetbrains.compose.*` 相依，與 `add-bite-charts` 的 D1
否決 Vico 是同一個理由：這個專案的 Compose 版本現在只有一個真值（BOM），多一個來源就多一個
版本衝突的入口。而這個小工具的內容就是幾個 TextView、兩條 ProgressBar、兩顆按鈕，
`RemoteViews` 剛好夠用，零新相依。

### D56. 版面是兩欄不是四列，而且照 2 格的高度設計
第一版是四列堆疊（熱量標題／熱量進度／喝水標題／喝水進度／按鈕），實測在小高度下
**被裁掉的剛好是中間的進度條與喝水那列**，只剩標題和按鈕——最沒用的兩樣留下來了。

改成左右兩欄之後高度砍一半。實際量出來的預算：
內距 20dp ＋ 首行 28dp ＋ 進度條 8dp（含 4dp 間距）＋ 按鈕列 40dp（含 6dp 間距）**≈ 106dp**，
壓在桌面 2 格（70n − 30，n=2 即 110dp）以內。

**每欄只放「一行字 + 一條進度條」是刻意的**：塞進「2400 / 2000 大卡」這種明細會讓高度超標，
而它回答的問題（吃了多少）已經被「剩餘」隱含。明細開 App 就看得到。

兩欄的首行都固定 28dp，兩條進度條才會落在同一條水平線上——不固定的話字級不同（20sp vs 15sp）
會讓左右兩條差幾個 dp，看起來像沒對齊。

### D57. 按鈕列固定高度，上半部吸收多餘空間
按鈕列用 `layout_height="40dp"`（固定）、上半部用 `weight=1`。反過來（按鈕用 weight）時，
高度不夠就會把按鈕壓成 0，結果是**看得到數字但按不到任何東西**。
寧可上面擠一點，也要留住按得到的按鈕。

### D58. 更新走三條路
1. **`BiteApp` 觀察資料流**（`entries` / `water` / `targets`）：在 App 裡記一筆、按小工具的
   「+250 水」、匯入備份，小工具都即時變。與動態捷徑同一個位置、同一個寫法。
2. **`DATE_CHANGED` / `TIMEZONE_CHANGED` 廣播**：過了午夜要換成新的一天。
   沒有這條的話，小工具會一直顯示昨天的數字直到有人動它。
3. **`updatePeriodMillis` 30 分鐘**：前兩條都沒發生時的保險（系統的最小值）。

`onUpdate` 直接讀 `BiteApp.repository`：`AppWidgetProvider` 是 App 行程裡的 receiver，
被叫醒時 `BiteApp.onCreate` 會先跑、repository 是同步載入，所以拿得到值。

### D59. 一律讀 `todayDate()`
App 裡有「正在看哪一天」的概念，小工具沒有——它永遠是「現在」。
與 `ShortcutActivity` 的 D53 同一個理由。

### D60. 顏色走資源檔，且有 `values-night`
`RemoteViews` 由桌面那支 App 畫，解不到 Bite 的 Compose 主題。小工具長在別人的桌面上，
桌布深淺不由我們決定，所以兩套顏色都要備好，由桌面的設定決定用哪一組。
喝水用一個低飽和的偏青藍（`#4C87A8`），與主色（靛藍）和三大營養素色都分得開。

## Risks / Trade-offs

- [不合規的桌面給的高度小於宣告的 `minHeight`，進度條會被裁掉] → 版面已經按 2 格的 110dp
  設計並留 4dp 餘裕；真的遇到更小的宿主時，被裁的是進度條而不是數字與按鈕。
- [`setDynamicShortcuts` 之外又多一條觀察資料流的路，App 啟動時會多做一次重畫] → 成本是
  一次 `RemoteViews` 建構，可忽略。
- [沒有孤兒清理：小工具被移除後那條觀察仍在跑] → `updateWidgets` 在沒有任何實例時是 no-op。

## Open Questions

- 真機上 4×2 的實際觀感還沒看過（BlueStacks 的桌面沒有小工具選單，只能用自製的測試宿主，
  而它給的高度比任何真實桌面都小）。若真機上覺得太擠，最便宜的調整是把宣告改成 4×3。
