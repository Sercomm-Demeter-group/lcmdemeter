ALTER TABLE `sAppVersion` CHANGE `version` `version` VARCHAR(64) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT 'version';

UPDATE `ofVersion` SET version=1 WHERE name='demeter_core';