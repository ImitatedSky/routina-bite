# Change: cute-widgets

## Why

使用者：「桌面小工具還是不怎麼可愛好看」。

三個小工具功能都對，但長相是「設定頁」的樣子：細細的長條進度條、一排排的字、
奶油底上配家族主色靛藍——擺在 Bite 啟動圖示（奶油底、珊瑚色火焰、藍灰盤子）旁邊，
看起來不像同一家人。

查了 Android 官方的小工具設計指引與一般「可愛小工具」的做法，共通的結論是：
可愛不是多加裝飾，而是**形狀圓、數字大、顏色和圖示一致**。具體幾條：

- 小空間用**圓環**不用長條：環包住一個中心點，正方形的格子放得剛好，中間還能放數字
- 環的兩端用**圓頭**，看起來比切平的柔和
- 圓角用系統給的值（Android 12+ 的 `system_app_widget_background_radius`），內層元件的圓角小 8dp
- 數字放大、標籤縮小，用字級拉出層次
- 顏色走一套，配色要和 App 圖示說同一件事

## What Changes

- **長條進度條全部換成圓頭圓環**，畫成 Bitmap 交給 RemoteViews（`WidgetRings.kt`）
- **熱量改用圖示上那團火焰的珊瑚色**，環中間放一個小火焰；喝水環中間放一滴水
- **營養小工具改成組成環**：三大營養素換算成熱量的佔比，和 App 裡「當日組成」同一個算法
- **按鈕改成帶圖示的膠囊**（淡藍底配水滴圖示的「+250」、淡珊瑚底配餐具圖示的「記一餐」）
- **喝水小工具的底改成淡水藍**，和另外兩塊「點了開 App」的奶油底分得開
- **吃超過時寫「超過了 120」而不是「-120」**，環換成深一點的珊瑚色
- **水喝夠了寫「喝夠了！」**，沒喝夠時寫還差多少
- 圓角：Android 12+ 用系統值，舊版本 24dp；root 用 `@android:id/background`（官方建議，開 App 的轉場比較順）

## Non-goals

- 不換成 Glance（D55 的理由不變）
- 不做動畫（RemoteViews 做不到，也不需要）
- 不改尺寸分桶規則（D77）

## Capabilities

### Modified Capabilities
- `bite-home-widget`: 熱量與喝水改以圓環呈現；超過目標的說法
- `bite-macro-widget`: 改以組成環呈現三大營養素

## Impact

- 新增 `WidgetRings.kt`、`values/dimens.xml`、`values-v31/dimens.xml`、
  `layout/bite_water_widget_wide.xml`、膠囊與色點與三個小圖示的 drawable
- 改三個 provider、八個 layout 中的七個、兩份 colors.xml、strings.xml
- 刪 `bite_widget_button.xml`、`bite_widget_progress_kcal.xml`、`bite_widget_progress_water.xml`
- **零新權限、零新相依**
