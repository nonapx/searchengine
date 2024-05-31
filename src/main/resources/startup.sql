drop procedure if exists tmp_create_index;

DELIMITER $$

CREATE PROCEDURE CreateIndexIfNotExists(
    IN tableName VARCHAR(64),
    IN indexName VARCHAR(64),
    IN columnName VARCHAR(64)
)
BEGIN
    DECLARE IndexCount INT;

    SELECT COUNT(1) INTO IndexCount FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema=DATABASE() AND table_name=tableName AND index_name=indexName;

    IF IndexCount = 0 THEN
        SET @s = CONCAT('CREATE INDEX ', indexName, ' ON ', tableName, ' (', columnName, ')');
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

call CreateIndexIfNotExists('page', 'page_site_path_uc', 'site_id, path(500)');