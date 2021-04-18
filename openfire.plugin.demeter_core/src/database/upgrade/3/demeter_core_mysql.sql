CREATE TABLE `sDeviceModel` (
  `uuid` varchar(36) COLLATE utf8_bin NOT NULL COMMENT 'uuid',
  `modelName` varchar(32) COLLATE utf8_bin NOT NULL COMMENT 'device model name',
  `status` tinyint(4) NOT NULL COMMENT '0: disabled, 1: enabled',
  `creationTime` bigint(20) UNSIGNED NOT NULL COMMENT 'creation time',
  `updatedTime` bigint(20) UNSIGNED NOT NULL COMMENT 'updated time',
  `script` mediumtext COLLATE utf8_bin NOT NULL COMMENT 'operation interfaces implementation'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='device model info.';

ALTER TABLE `sDeviceModel`
  ADD PRIMARY KEY (`uuid`),
  ADD UNIQUE KEY `MODEL_NAME` (`modelName`),
  ADD KEY `STATUS` (`status`),
  ADD KEY `CREATION_TIME` (`creationTime`),
  ADD KEY `UPDATED_TIME` (`updatedTime`);

UPDATE `ofVersion` SET version=3 WHERE name='demeter_core';