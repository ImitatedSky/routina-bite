# Design: swipe-between-days

## Decisions

### D102. 用 HorizontalPager，不用偵測滑動手勢
另一個做法是在畫面上掛 `detectHorizontalDragGestures`，滑超過一段距離就換天。
那樣放開手指之前畫面不會動，換天是「啪」一下跳過去，看不出往哪個方向。
行事曆、相簿這類一頁一個單位的畫面，大家習慣的是頁面跟著手指走、放開吸附——
這正是 `HorizontalPager`，而且它已經在 compose foundation 裡，不用加相依。

### D103. 頁碼與日期的對照：以打開畫面那天為中心
Pager 需要有限的頁數。給 20000 頁，中間那頁對應「打開今日頁那天」，
往前往後各一萬天（約 27 年），匯入的舊資料與將來都夠。
頁碼 ↔ 日期用 `ChronoUnit.DAYS.between` / `shiftDate` 換算，不另存任何狀態。

### D104. 選中的日期仍放在 ViewModel，Pager 與它雙向同步
新增紀錄、喝水、備註、複製昨天、歷史頁選日期，原本都讀寫 `selectedDate`，
所以不把「現在是哪天」搬進 Pager，而是兩邊對齊：
- Pager 停下來（`settledPage`）→ 把那頁的日期寫回 `selectedDate`
- `selectedDate` 被別處改了（箭頭、回今天、歷史頁）→ 翻到那一頁；超過 3 天直接跳，
  不然從歷史頁點一年前那天會看到一路翻過三百多頁

用 `settledPage` 而不是 `currentPage`：滑到一半時 `currentPage` 已經變了，
這時候按右下角的「＋」不該記到還沒停下來的那一天。

### D105. 每一頁自己算那天的資料
頁裡的摘要、喝水、組成、紀錄列表都用該頁的日期過濾，而不是 `selectedDate`——
滑動中左右兩頁同時在畫面上，兩頁都要顯示自己那天的內容。
旁邊一頁預先組好（`beyondViewportPageCount = 1`），開始滑的時候不會先看到空白。

## Risks / Trade-offs

- [頁內若有橫向捲動元件會和換天搶手勢] → 今日頁目前沒有橫向捲動的東西；之後加的話要留意
- [長時間開著畫面跨過午夜，中心頁仍是昨天] → 只影響頁碼的基準，日期換算是對的；「回今天」照樣回到真正的今天
