ALTER TABLE `sEndUser` ADD `uuid` VARCHAR(36) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT 'uuid' AFTER `id`;
UPDATE `sEndUser` SET `uuid`=UUID();
ALTER TABLE `sEndUser` ADD UNIQUE `UUID` ( `uuid`);

UPDATE `ofVersion` SET version=2 WHERE name='demeter_core';