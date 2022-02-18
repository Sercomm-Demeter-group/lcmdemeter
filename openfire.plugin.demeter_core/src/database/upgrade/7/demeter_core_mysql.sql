ALTER TABLE `sAppVersion` ADD `realVersion` VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT 'IPK Version' AFTER `releaseNote`;


UPDATE `ofVersion` SET version=7 WHERE name='demeter_core';