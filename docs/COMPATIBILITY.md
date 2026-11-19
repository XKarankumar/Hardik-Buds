# Device Compatibility Matrix — Hardik Buds Pro

Hardik Buds Pro classifies connected Bluetooth audio hardware into three transparent tiers:

- **Tier 1 (Standard Bluetooth):** A2DP Stereo Audio, HFP Calls, AVRCP Controls, RSSI, Android AudioEffect EQ & Subwoofer Engine X.
- **Tier 2 (Enhanced Telemetry):** Tier 1 + Bluetooth HFP / BLE GATT (`0x180F`) Battery Reporting & Untethered Metadata where exposed by OS/hardware.
- **Tier 3 (Extended Protocol):** Tier 2 + Google Fast Pair / BLE Device Information (`0x180A`) Firmware Revision readout & OEM companion handoff.

| Brand | Default Tier | Battery Telemetry | Android DSP EQ & Subwoofer X | Native ANC / Touch Remap |
|---|---|---|---|---|
| OnePlus | Tier 2 / Tier 3 | HFP / Fast Pair Metadata | Supported | OEM Protocol Required |
| Samsung | Tier 2 / Tier 3 | HFP / GATT / Metadata | Supported | OEM Protocol Required |
| Apple AirPods | Tier 2 | Standard HFP where exposed | Supported | Proprietary Apple AAP |
| Realme | Tier 2 / Tier 3 | HFP / Fast Pair Metadata | Supported | OEM Protocol Required |
| Oppo | Tier 2 / Tier 3 | HFP / Fast Pair Metadata | Supported | OEM Protocol Required |
| Xiaomi | Tier 2 / Tier 3 | HFP / BLE Telemetry | Supported | OEM Protocol Required |
| Redmi | Tier 2 / Tier 3 | HFP / BLE Telemetry | Supported | OEM Protocol Required |
| Vivo | Tier 2 | HFP Battery | Supported | OEM Protocol Required |
| iQOO | Tier 2 | HFP Battery | Supported | OEM Protocol Required |
| Nothing | Tier 2 / Tier 3 | Fast Pair / HFP Metadata | Supported | OEM Protocol Required |
| JBL | Tier 2 / Tier 3 | HFP / GATT Battery | Supported | OEM Protocol Required |
| Sony | Tier 2 / Tier 3 | HFP / Fast Pair / GATT | Supported | OEM Protocol Required |
| boAt | Tier 2 | HFP Battery | Supported | Standard Bluetooth Only |
| Noise | Tier 2 | HFP Battery | Supported | Standard Bluetooth Only |
| Huawei | Tier 2 / Tier 3 | HFP / BLE Telemetry | Supported | OEM Protocol Required |
| Google Pixel Buds | Tier 3 | Untethered L/R/Case + Fast Pair | Supported | Pixel Companion App |
| Generic Bluetooth | Tier 1 | HFP / GATT if supported | Supported | Not Supported |
