# Change: adaptive-widget-sizes

## Why

兩個小工具都固定 4×2。使用者的桌面第一排本來就滿了，想放一塊「今天還剩多少」就得先騰出
半排位置——結果是兩個小工具都沒放。而真正天天要按的「+250 水」被埋在 4×2 那塊裡，
為了一顆按鈕付四格的代價。

另外，兩個 receiver 的 `android:label` 都是 `@string/app_name`：小工具選單裡兩個都叫
「Routina Bite」，看不出哪個是哪個，只能亂猜一個放上去再看。

## What Changes

- **既有兩個小工具支援縮放到 1×1**：`minWidth` / `minHeight` / `minResize*` 一律降到 40dp，
  `targetCell` 維持 4×2（新放上去仍是完整版面，要小自己縮）。
- **依尺寸換版面**：provider 覆寫 `onAppWidgetOptionsChanged`，從桌面回報的
  `OPTION_APPWIDGET_MIN_WIDTH` / `OPTION_APPWIDGET_MIN_HEIGHT`（dp）分三桶，
  各有一個版面——1×1 只放一個數字，矮而寬放一行字，兩格高以上維持現有版面不動。
- **新增第三個小工具「喝水」**（`BiteWaterWidgetProvider`，預設 1×1）：整塊就是一顆
  「+250」，點一下直接記 250 ml、不開畫面，下面一行是今天的累計。
- **三個 receiver 各給自己的 `android:label`**（「今日」「營養」「喝水」）與 `description`，
  小工具選單裡分得出來。
- `updateWidgets` / `updateMacroWidgets` 改成逐一 id 重畫：同一個小工具可以被放好幾份、
  各自是不同尺寸，一份 RemoteViews 套給所有 id 就不對了。

## Non-goals

- 不用 API 31 的 `RemoteViews(Map<SizeF, RemoteViews>)`（minSdk 26，見 design D77）
- 不做尺寸的使用者設定（尺寸就是使用者拉出來的那個大小，不該再問一次）
- 不動 `bite_widget.xml` 與 `bite_macro_widget.xml` 兩個完整版面

## Capabilities

### New Capabilities
（無）

### Modified Capabilities
- `bite-home-widget`: 尺寸與版面規則；另外多一個「喝水」小工具
- `bite-macro-widget`: 尺寸與版面規則

## Impact

- 新增 `Widgets.kt`（三個 provider 共用）、`BiteWaterWidgetProvider.kt`、
  `layout/bite_widget_tiny.xml`、`layout/bite_widget_row.xml`、
  `layout/bite_macro_widget_tiny.xml`、`layout/bite_macro_widget_row.xml`、
  `layout/bite_water_widget.xml`、`drawable/bite_water_widget_background.xml`、
  `xml/bite_water_widget_info.xml`
- `BiteWidgetProvider.kt`、`BiteMacroWidgetProvider.kt`、`BiteApp.kt`、
  `AndroidManifest.xml`、`proguard-rules.pro`、`strings.xml`、
  `xml/bite_widget_info.xml`、`xml/bite_macro_widget_info.xml`
- **零新權限、零新相依**
