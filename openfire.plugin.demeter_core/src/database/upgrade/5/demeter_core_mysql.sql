CREATE TABLE `sBatch` (
  `id` varchar(36) COLLATE utf8_bin NOT NULL COMMENT 'batch id',
  `appId` varchar(36) COLLATE utf8_bin NOT NULL COMMENT 'application id',
  `versionId` varchar(36) COLLATE utf8_bin NOT NULL COMMENT 'application version id',
  `command` tinyint(4) NOT NULL COMMENT 'action of the batch',
  `state` varchar(16) COLLATE utf8_bin NOT NULL COMMENT 'batch task state',
  `nodeId` varchar(64) COLLATE utf8_bin NOT NULL COMMENT 'cluster node',
  `creationTime` bigint(20) UNSIGNED NOT NULL COMMENT 'batch task creation time',
  `updatedTime` bigint(20) UNSIGNED NOT NULL COMMENT 'batch task latest updated time',
  `totalCount` int(10) UNSIGNED NOT NULL COMMENT 'total device count',
  `doneCount` int(10) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'done device count',
  `failedCount` int(10) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'failed device count',
  `data` mediumblob NOT NULL COMMENT 'data of the batch task'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='batch installation tasks';

ALTER TABLE `sBatch`
  ADD PRIMARY KEY (`id`),
  ADD KEY `NODE` (`nodeId`),
  ADD KEY `STATUS` (`state`),
  ADD KEY `APP_ID` (`appId`) USING BTREE,
  ADD KEY `VERSION_ID` (`versionId`) USING BTREE,
  ADD KEY `COMMAND` (`command`) USING BTREE;

UPDATE `ofVersion` SET version=5 WHERE name='demeter_core';