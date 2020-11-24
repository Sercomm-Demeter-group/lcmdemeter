Demeter
===
About
---
[Demeter] is a DSL package delivery server for OpenWRT platform, and it was constructed in the form of [Openfire](https://github.com/igniterealtime/Openfire) plugins.

Features
---
- Packages repository for DSL devices of OpenWRT platform
- Packages remote installation for DSL devices of OpenWRT platform
- Packages remote uninstallation for DSL devices of OpenWRT platform
- Packages remote control for DSL devices of OpenWRT platform

Project Components
---
- `Demeter Commons ID (demeter-commons-id)`
- `Demeter Commons UMEi (demeter-commons-id)` 
- `Demeter Commons Utilities (demeter-commons-util)` 
- `Demeter C2C API (demeter-c2c-api)` 
- `Openfire Plugin - Demeter Core (demeter_core)`
- `Openfire Plugin - Demeter Service API (demeter_service_api)`
- `Openfire Plugin - Demeter C2C API (demeter_c2c_api)`
- `Demeter Portal (demeter_portal)`

Architecture
---
![](https://github.com/sercomm-cloudwu/lcmdemeter/blob/main/resources/demeter-architecture.jpg)

- `MySQL`: Currently Demeter supports MySQL database schema only
- `Openfire`: With [Demeter] plugins, Openfire handles the RESTful API requests and WebSocket communication with DSL devices
- `NGINX`: In additional to host Demeter Portal, it plays the role of reverse proxy to forward Demeter C2C API requests and WebSocket communication from DSL devices to Openfire
