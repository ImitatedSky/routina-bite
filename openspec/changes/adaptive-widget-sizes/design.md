# Design: adaptive-widget-sizes

## Context

`add-home-widget` 的 Non-goals 寫著「不做多種尺寸的版面切換」，`add-macro-widget` 沿用。
那時的判斷是對的：先有一個能用的 4×2，再談尺寸。現在兩塊都在桌面上跑了一段時間，
暴露出來的是**放不下**——4×2 一塊就佔掉半排，兩塊就是一整排，
而天天按的其實只有「+250 水」那一顆。

## Goals / Non-Goals

**Goals:**
- 兩個既有小工具一路縮到 1×1，每個尺寸都還讀得到「這塊在說什麼」
- 多一個 1×1 的「喝水」，一按就記、不開畫面
- 小工具選單裡三個分得出來

**Non-Goals:**
- 不動兩個完整版面（4×2 已經驗過、使用者在用）
- 不做尺寸設定頁

## Decisions

### D77. 用 `onAppWidgetOptionsChanged` + options 的 dp 分桶，不用 `RemoteViews(Map<SizeF, …>)`
API 31 的 `RemoteViews(Map<SizeF, RemoteViews>)` 讓系統自己挑版面，但 minSdk 是 26，
用了就得為 26–30 再寫一套，等於同一件事寫兩遍。

通用做法只有兩步，而且 26 到今天都一樣：
1. 覆寫 `onAppWidgetOptionsChanged(context, manager, id, newOptions)`，收到就重畫那一個 id
2. 從 options 讀 `OPTION_APPWIDGET_MIN_WIDTH` / `OPTION_APPWIDGET_MIN_HEIGHT`（單位 dp）

分桶（`widgetSizeOf`）：

| 桶 | 條件 | 對應 |
|---|---|---|
| `TINY` | 寬 < 110 **且** 高 < 110 | 約 1×1 |
| `ROW` | 寬 >= 110 **且** 高 < 110 | 2×1 / 4×1 |
| `FULL` | 高 >= 110 | 現有版面 |

門檻取 110dp 是桌面格子的算式：n 格 = 70n − 30，兩格剛好 110dp，
而完整版面本來就是照兩格的高度設計的（D56）。

**讀不到 options（null 或 0）時當 `FULL`**：那是小工具剛放上去、桌面還沒回報尺寸的狀態，
而預設尺寸就是 4×2。寧可先畫完整版面再被 `onAppWidgetOptionsChanged` 糾正，
也不要在 4×2 上先閃一下 1×1。

### D78. `updateWidgets` 改成逐一 id
原本是 `manager.updateAppWidget(ids, 同一份 RemoteViews)`。同一個小工具可以被放好幾份、
各自是不同尺寸，共用一份 RemoteViews 就會有人拿到別人的版面。
逐一 id 的成本是多幾次 `getAppWidgetOptions`（一次 IPC），桌面上最多幾塊，可忽略。

### D79. 1×1 只留一個數字，而且是各自的主角
40dp 見方裡，一個 20sp 的數字已經佔掉大半。標籤、進度條、按鈕不是「擠」而是「放不下」，
硬塞的結果是三樣都看不清楚。

留哪個數字依小工具而定：「今日」留**剩餘熱量**（它回答的問題就是還能吃多少），
「營養」留**已吃熱量**（它回答的是今天吃進去了什麼）。兩個都不是「隨便挑一個」。

`autoSizeTextType` 在 RemoteViews 上不是每個版本都吃，所以字級由 provider 依**位數**決定
（`tinyTextSp`：3 位以內 22sp、4 位 18sp、再多 15sp）。固定 20sp 時「1832」會超過一格的寬度
被裁成「183…」，而 1×1 只剩一個數字，那個數字再被裁掉就什麼都不剩了。
`maxLines=1` + `ellipsize=end` 仍然留著當最後一道防線。

### D80. `ROW` 不放按鈕
一格高（約 40dp）扣掉內距只剩 32dp。放一顆按得到的按鈕就等於把數字整個擠掉，
而使用者把小工具縮成一條，要的就是那個數字。動作留給 `FULL` 與「喝水」那一塊。

`ROW` 裡還有一層寬度門檻：`>= 250dp`（約 4 格）才顯示第二組數字——「今日」的喝水欄、
「營養」的三個營養素。2×1 的寬度硬塞的結果是被裁到看不出那是「蛋白」還是「脂肪」，
不如只留熱量。這是第二層門檻，不是第四個桶——版面同一個，只是 `setViewVisibility`。

### D81. 「喝水」只有一個版面，放大就放大字
這塊的內容是一個動作加一行累計，變大不會多出第二件事可以顯示。
與其為此多養兩個 layout，不如 `setTextViewTextSize` 把字放大、累計那行補上 `ml`。

### D82. 「喝水」整塊就是一顆按鈕
三個小工具裡只有它點下去會改資料。1×1 上沒有空間讓使用者分辨「這裡會加水、那裡會開 App」，
所以只掛一個 `PendingIntent` 在 root 上。底色用按鈕的填色與外框（不是另外兩塊的卡片底），
外觀上就看得出「這塊是按的」。

### D83. `label` 三個各一個字串
`android:label` 就是小工具選單列出來的名字。三個都寫 `@string/app_name` 的結果是
清單上三個「Routina Bite」，使用者只能亂猜。改成「今日」「營養」「喝水」，
加上各自的 `description`，選單上一看就知道拿的是哪一塊。

### D84. 共用的部分抽成 `Widgets.kt`，只抽真的共用的
三個 provider 重複的是：分桶、讀今天的數字、算百分比、開 App 的 `PendingIntent`、
`PendingIntent` 的 flags。抽成一個檔案的五個函式，沒有基底類別、沒有介面——
版面與互動三個各自不同，抽象化只會讓每個 provider 都要先繞過框架才寫得出自己的那一點差異。

## Risks / Trade-offs

- [Android 11 以下的桌面不吃 `targetCellWidth/Height`，改用 `minWidth/minHeight` 決定
  放上去的預設尺寸，所以在那些裝置上新放的小工具會是 1×1 而不是 4×2] → 這是「支援到 1×1」
  的直接代價，沒有第三條路（宣告一個尺寸就只能宣告一個）。1×1 也是完整可用的版面，
  使用者拉大就好，而 12 以上（絕大多數還在用的裝置）拿到的仍是 4×2。
- [`ROW` 在 2×1 上只剩熱量，和 `TINY` 差別不大] → 差別是有單位、左右對齊；
  而測試宿主上實測 150dp 放三個營養素會被裁成「白 0 · 脂肪」，那比少講幾個數字更糟。
- [`getAppWidgetOptions` 在某些第三方桌面回 0] → 落回 `FULL`，是現在的行為，不會更糟。
- [喝水小工具整塊可按，使用者想開 App 時沒有入口] → 桌面上還有 App 圖示與另外兩塊小工具；
  在 1×1 上放第二個點擊區只會讓 +250 變得不敢按。

## Open Questions

- Android 11 以下的 `AppWidgetHostView` 會替小工具加一圈預設內距（實測每邊 8dp），
  所以宿主給 40dp 時真正能用的只有 24dp，`TINY` 的數字會被裁掉。12 以上（provider
  targetSdk >= S）沒有這圈內距，40dp 全是我們的。實測 70dp（真實的一格）以上都正常。
- 三種尺寸在真機桌面上的實際觀感還沒看過（BlueStacks 的 launcher 沒有小工具選單，
  只能用自製的測試宿主，它給的尺寸是我們自己塞進 options 的）。
  1×1 的四位數已改由 `tinyTextSp` 依位數縮字級處理，不再依賴宿主給的寬度剛好夠。
