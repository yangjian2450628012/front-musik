CREATE TABLE IF NOT EXISTS `ENGINE_BULID_HISTORY`(
`id`  int NOT NULL AUTO_INCREMENT COMMENT 'major key' ,
`module_name` VARCHAR(50) NOT NULL COMMENT 'module name',
`table_name` VARCHAR(30) NOT NULL COMMENT 'table name',
`db_type` VARCHAR(10) NOT NULL COMMENT 'db type',
`frame_type` VARCHAR(20) NOT NULL COMMENT 'frame type',
`remark` VARCHAR(50) NOT NULL COMMENT 'remark',
PRIMARY KEY (`id`)
);