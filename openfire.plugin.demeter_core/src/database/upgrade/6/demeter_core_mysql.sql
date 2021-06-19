CREATE TABLE `sAppInstallation` (
  `id` varchar(36) COLLATE utf8_bin NOT NULL COMMENT 'id',
  `serial` varchar(16) COLLATE utf8_bin NOT NULL COMMENT 'serial number',
  `mac` varchar(12) COLLATE utf8_bin NOT NULL COMMENT 'mac address',
  `appId` varchar(32) COLLATE utf8_bin NOT NULL COMMENT 'application id',
  `versionId` varchar(32) COLLATE utf8_bin NOT NULL COMMENT 'version id',
  `creationTime` bigint(20) UNSIGNED NOT NULL COMMENT 'installed time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='application installation records';

ALTER TABLE `sAppInstallation`
  ADD PRIMARY KEY (`id`),
  ADD KEY `DEVICE_ID` (`serial`,`mac`) USING BTREE,
  ADD KEY `APP_ID` (`appId`) USING BTREE,
  ADD KEY `VERSION_ID` (`versionId`) USING BTREE;

UPDATE `ofVersion` SET version=6 WHERE name='demeter_core';