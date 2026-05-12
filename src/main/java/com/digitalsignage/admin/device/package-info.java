/**
 * 设备侧 HTTP：激活、拉取生效配置、播放日志上报。HTTP 心跳由 Player 服务写库，本服务仅通过 WebSocket/读库做在线与疑似判定（见 docs/Use_Case.md 用例 10、16–17）。
 */
package com.digitalsignage.admin.device;
