# Tasks: adaptive-widget-sizes

## 1. 共用的部分

- [x] 1.1 `Widgets.kt`：`WidgetSize` 三桶與 `widgetSizeOf(options)`（110dp 門檻）
- [x] 1.2 `widgetToday(context)`：今天的熱量、三大營養素、喝水與目標，三個 provider 共用
- [x] 1.3 `widgetPercent` 與 `openAppIntent`（含 `FLAG_IMMUTABLE`）搬進共用檔

## 2. 既有兩個小工具依尺寸換版面

- [x] 2.1 兩個 provider 覆寫 `onAppWidgetOptionsChanged`，收到就重畫那一個 id
- [x] 2.2 `updateWidgets` / `updateMacroWidgets` 改成逐一 id、各自讀 options
- [x] 2.3 `bite_widget_tiny.xml`：只有剩餘熱量，置中
- [x] 2.4 `bite_widget_row.xml`：一行「剩餘 N」＋進度條，寬 >= 250dp 才顯示喝水
- [x] 2.5 `bite_macro_widget_tiny.xml`：只有已吃熱量
- [x] 2.6 `bite_macro_widget_row.xml`：一行熱量＋三個營養素（各自顏色），無進度條；
      寬 < 250dp 時只留熱量（硬塞會被裁到看不懂）
- [x] 2.7 兩個 info xml：`minWidth`/`minHeight`/`minResize*` 一律 40dp，`targetCell` 維持 4×2
- [x] 2.8 兩個完整版面（`bite_widget.xml`、`bite_macro_widget.xml`）一個字都沒動

## 3. 喝水小工具

- [x] 3.1 `BiteWaterWidgetProvider`：整塊掛一個 `PendingIntent`，requestCode 21
- [x] 3.2 `bite_water_widget.xml`：「+250」＋累計；字級由程式依尺寸調整
- [x] 3.3 `bite_water_widget_background.xml`：按鈕的填色與外框，和另外兩塊分得開
- [x] 3.4 `bite_water_widget_info.xml`：`targetCell` 1×1、可放大
- [x] 3.5 manifest 第三個 receiver、`proguard-rules.pro` 加 `-keep`
- [x] 3.6 `BiteApp` 的資料流一併重畫三種小工具

## 4. 選單裡分得出來

- [x] 4.1 三個 receiver 的 `android:label` 各一個字串（今日／營養／喝水）
- [x] 4.2 喝水小工具補 `android:description`

## 5. 驗證與提交

- [x] 5.1 `assembleDebug` 與 `assembleRelease` 綠燈
- [x] 5.2 版本 15 / 0.13.0
- [x] 5.3 `dumpsys appwidget` 三個 provider 都在、label 是今日／營養／喝水
- [x] 5.4 自製測試宿主渲染 1×1、2×1、4×1、4×2 四種尺寸
- [x] 5.5 喝水小工具點一下真的 +250，App 與小工具的數字都跟著變
- [x] 5.6 在 App 裡記一筆，三個小工具都即時更新
- [x] 5.7 `DATE_CHANGED` 廣播不崩
- [x] 5.8 release（R8）版安裝，三個 provider 與五個 layout 都沒被 shrink 掉
- [x] 5.9 `logcat -b crash` 乾淨、既有資料沒動到
- [x] 5.10 1×1 的數字依位數縮字級（`tinyTextSp`），四位數不會被 ellipsize
- [ ] 5.11 真機（Pixel）確認：小工具選單的三個名字、Android 12 以上新放上去是不是 4×2、
      跨午夜的 `DATE_CHANGED`（BlueStacks 發不出 protected broadcast）
