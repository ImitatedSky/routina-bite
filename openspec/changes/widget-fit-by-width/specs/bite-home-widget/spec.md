# bite-home-widget

## ADDED Requirements

### Requirement: Widgets show as much as fits the space they are given
Each Bite widget SHALL decide what to show from how much room its text actually needs rather than
from a fixed number of cells, SHALL fall back step by step to shorter forms before leaving anything
out, and SHALL centre what it shows when only one group of figures fits.

#### Scenario: Three cells wide, one cell high
- **WHEN** the today widget is three cells wide and one cell high on a phone whose cells are about 76dp
- **THEN** it shows both the calorie ring with its figure and the water ring with today's water

#### Scenario: Two cells wide, one cell high
- **WHEN** only the calorie group fits
- **THEN** that group is centred in the widget rather than left-aligned

#### Scenario: Narrow and tall
- **WHEN** a widget is one cell wide and several cells high, or two by two
- **THEN** it shows a single ring sized to the shorter side with its figure inside, not the two-column layout

#### Scenario: Android 12 and later
- **WHEN** a single-cell widget is drawn on Android 12 or later
- **THEN** its centre figure is sized for the whole cell, without allowing for padding only older home screens add
