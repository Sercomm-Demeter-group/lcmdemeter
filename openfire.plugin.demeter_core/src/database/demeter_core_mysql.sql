CREATE TABLE IF NOT EXISTS `sApp` (
  `id`                  VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'MD5(publisher+name) ',
  `publisher`           VARCHAR(255) COLLATE utf8_bin NOT NULL COMMENT 'publisher',
  `name`                VARCHAR(255) COLLATE utf8_bin NOT NULL COMMENT 'app name',
  `catalog`             VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'sAppCatalog.name',
  `model`               VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'hardware model',
  `price`               VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'U.S.D.',
  `publish`             TINYINT(4) UNSIGNED NOT NULL DEFAULT '0' COMMENT '0: false, 1: true',
  `description`         TEXT COLLATE utf8_bin NOT NULL COMMENT 'description',
  `creationTime`        BIGINT(20) UNSIGNED NOT NULL COMMENT 'creation time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNIQUE_VAL` (`publisher`,`name`,`model`) USING BTREE,
  KEY `CATEGORY` (`catalog`) USING BTREE,
  KEY `PUBLISH` (`publish`) USING BTREE,
  KEY `CREATION_TIME` (`creationTime`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='app info';

CREATE TABLE IF NOT EXISTS `sAppCatalog` (
  `id`                  INT(10) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'catalog id',
  `name`                VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'app catalog name',
  `creationTime`        BIGINT(20) UNSIGNED NOT NULL COMMENT 'creation time',
  PRIMARY KEY (`id`),
  KEY `CATALOG_NAME` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='app catalog';

CREATE TABLE IF NOT EXISTS `sAppEventRecord` (
  `id`                  BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'id',
  `appId`               VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'sApp.id',
  `appName`             VARCHAR(255) COLLATE utf8_bin NOT NULL COMMENT 'sApp.name',
  `serial`              VARCHAR(16) COLLATE utf8_bin NOT NULL COMMENT 'device serial',
  `mac`                 VARCHAR(12) COLLATE utf8_bin NOT NULL COMMENT 'device mac',
  `userId`              VARCHAR(255) COLLATE utf8_bin NOT NULL COMMENT 'sEndUser.id',
  `eventType`           VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'event type',
  `unixTime`            BIGINT(20) UNSIGNED NOT NULL COMMENT 'unix time',
  `partitionIdx`        DATETIME(3) NOT NULL COMMENT 'for partition by datetime, UTC',
  PRIMARY KEY (`id`,`partitionIdx`),
  KEY `DEVICE_ID` (`serial`,`mac`) USING BTREE,
  KEY `APP_ID` (`appId`) USING BTREE,
  KEY `UNIX_TIME` (`unixTime`) USING BTREE,
  KEY `USER_ID` (`userId`) USING BTREE,
  KEY `EVENT_TYPE` (`eventType`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='app event record';

CREATE TABLE IF NOT EXISTS `sAppIcon` (
  `appId`               VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'sApp.id',
  `iconId`              VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'MD5(data)',
  `size`                BIGINT(20) UNSIGNED NOT NULL COMMENT 'icon data length',
  `updatedTime`         BIGINT(20) UNSIGNED NOT NULL COMMENT 'updated time',
  `data`                blob NOT NULL COMMENT 'icon binary data',
  PRIMARY KEY (`appId`),
  UNIQUE KEY `ICON_ID` (`iconId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='app icon info';

CREATE TABLE IF NOT EXISTS `sAppSubscription` (
  `appId`               VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'sApp.id',
  `userId`              VARCHAR(255) COLLATE utf8_bin NOT NULL COMMENT 'subscribed by',
  `creationTime`        BIGINT(20) UNSIGNED NOT NULL COMMENT 'creation time',
  PRIMARY KEY (`appId`,`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='end user subscribe apps information';

CREATE TABLE IF NOT EXISTS `sAppVersion` (
  `id`                  VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'MD5(IPK file)',
  `appId`               VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'sApp.id',
  `version`             VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'version',
  `filename`            VARCHAR(255) COLLATE utf8_bin NOT NULL COMMENT 'IPK filename',
  `installedCount`      BIGINT(20) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'installed count',
  `removedCount`        BIGINT(20) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'removed count',
  `creationTime`        BIGINT(20) UNSIGNED NOT NULL COMMENT 'creation time',
  `ipkFilePath`         VARCHAR(255) COLLATE utf8_bin NOT NULL COMMENT 'IPK file path',
  `ipkFileSize`         BIGINT(20) UNSIGNED NOT NULL COMMENT 'bytes',
  `releaseNote`         TEXT COLLATE utf8_bin NOT NULL COMMENT 'version release note',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNIQUE_KEY` (`appId`,`version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='app version and file Info';

CREATE TABLE IF NOT EXISTS `sDeviceFOTARecord` (
  `serial`              VARCHAR(16) COLLATE utf8_bin NOT NULL COMMENT 'serial',
  `mac`                 VARCHAR(12) COLLATE utf8_bin NOT NULL COMMENT 'mac',
  `begin`               BIGINT(20) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'begin unix time in millisecond',
  `end`                 BIGINT(20) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'end unix time in millisecond',
  PRIMARY KEY (`serial`,`mac`),
  KEY `begin` (`begin`) USING BTREE,
  KEY `end` (`end`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='device FOTA record';

CREATE TABLE IF NOT EXISTS sDeviceProp (
  `serial`              VARCHAR(16)     COLLATE utf8_bin NOT NULL COMMENT 'serial number',
  `mac`                 VARCHAR(12)     COLLATE utf8_bin NOT NULL COMMENT 'mac address',
  `name`                VARCHAR(100)    COLLATE utf8_bin NOT NULL COMMENT 'property name',
  `propValue`           TEXT            NOT NULL COMMENT 'property value',
  PRIMARY KEY (`serial`, `mac`, `name`),
  KEY `VALUE` (`propValue`(32)) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='device properties';

CREATE TABLE IF NOT EXISTS sDeviceSecretKey (
  `serial`              VARCHAR(16)     COLLATE utf8_bin NOT NULL COMMENT 'serial number',
  `mac`                 VARCHAR(12)     COLLATE utf8_bin NOT NULL COMMENT 'mac address',
  `secretKey`           VARCHAR(128)    NOT NULL COMMENT 'secret key data in base64 format',
  PRIMARY KEY (`serial`, `mac`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='device secret key';

CREATE TABLE IF NOT EXISTS `sEndUser` (
  `id`                  VARCHAR(255) COLLATE utf8_bin NOT NULL COMMENT 'email',
  `role`                VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'role',
  `storedKey`           VARCHAR(128) COLLATE utf8_bin NOT NULL COMMENT 'encrypt key',
  `encryptedPassword`   VARCHAR(255) COLLATE utf8_bin NOT NULL COMMENT 'encrypted password',
  `valid`               TINYINT(4) UNSIGNED NOT NULL DEFAULT '1' COMMENT 'valid',
  `creationTime`        BIGINT(20) UNSIGNED NOT NULL COMMENT 'creation time',
  PRIMARY KEY (`id`),
  KEY `PRIVILEGE` (`role`) USING BTREE,
  KEY `CREATIONDATE` (`creationTime`) USING BTREE,
  KEY `VALID` (`valid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='end user info';

CREATE TABLE IF NOT EXISTS `sEndUserProp` (
  `userId`              VARCHAR(255) COLLATE utf8_bin NOT NULL COMMENT 'email',
  `name`                VARCHAR(100) COLLATE utf8_bin NOT NULL COMMENT 'property name',
  `propValue`           TEXT COLLATE utf8_bin NOT NULL COMMENT 'property value',
  PRIMARY KEY (`userId`,`name`),
  KEY `VALUE` (`propValue`(32)) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='device properties';

CREATE TABLE IF NOT EXISTS `sOwnership` (
  `serial`              VARCHAR(16) COLLATE utf8_bin NOT NULL COMMENT 'serial',
  `mac`                 VARCHAR(12) COLLATE utf8_bin NOT NULL COMMENT 'mac',
  `userId`              VARCHAR(255) COLLATE utf8_bin NOT NULL COMMENT 'sEndUser.id',
  `type`                VARCHAR(8) COLLATE utf8_bin NOT NULL COMMENT 'ownership type',
  `creationTime`        BIGINT(20) UNSIGNED NOT NULL COMMENT 'creation time',
  PRIMARY KEY (`serial`,`mac`,`userId`),
  KEY `TYPE` (`type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='device ownership';

CREATE TABLE IF NOT EXISTS `sSystemRecord` (
  `id`                  BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'id',
  `platform`            VARCHAR(16) COLLATE utf8_bin NOT NULL COMMENT 'platform id',
  `serial`              VARCHAR(16) COLLATE utf8_bin NOT NULL COMMENT 'serial number',
  `mac`                 VARCHAR(12) COLLATE utf8_bin NOT NULL COMMENT 'mac address',
  `category`            TINYINT(4) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'category',
  `type`                VARCHAR(32) COLLATE utf8_bin NOT NULL COMMENT 'record type',
  `unixTime`            VARCHAR(15) COLLATE utf8_bin NOT NULL COMMENT 'create time',
  `detail`              TEXT CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT 'record detail in JSON string',
  PRIMARY KEY (`id`),
  KEY `DEVICE_ID` (`serial`,`mac`) USING BTREE,
  KEY `PLATFORM` (`platform`) USING BTREE,
  KEY `CATEGORY` (`category`) USING BTREE,
  KEY `TYPE` (`type`) USING BTREE,
  KEY `UNIX_TIME` (`unixTime`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='system record';

-- version 1
ALTER TABLE `sAppVersion` CHANGE `version` `version` VARCHAR(64) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT 'version';

-- version 2
ALTER TABLE `sEndUser` ADD `uuid` VARCHAR(36) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT 'uuid' AFTER `id`;
UPDATE `sEndUser` SET `uuid`=UUID();
ALTER TABLE `sEndUser` ADD UNIQUE `UUID` ( `uuid`);

-- version 3
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

-- version 4
ALTER TABLE `sAppVersion` ADD `status` TINYINT UNSIGNED NOT NULL DEFAULT '1' COMMENT '0:disable, 1:enable' AFTER `version`, ADD INDEX `STATUS` (`status`);

-- version 5
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

-- version 6
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

-- version 7
ALTER TABLE `sAppVersion` ADD `realVersion` VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT 'IPK Version' AFTER `releaseNote`;

INSERT INTO `ofVersion`(name,version) VALUES('demeter_core',7);
