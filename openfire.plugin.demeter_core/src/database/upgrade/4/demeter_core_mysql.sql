ALTER TABLE `sAppVersion` ADD `status` TINYINT UNSIGNED NOT NULL DEFAULT '1' COMMENT '0:disable, 1:enable' AFTER `version`, ADD INDEX `STATUS` (`status`);

UPDATE `ofVersion` SET version=4 WHERE name='demeter_core';