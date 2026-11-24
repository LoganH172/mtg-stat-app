USE commander;

-- Store the selected commander while the match is open.
SET @add_room_commander = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'match_room_members'
          AND COLUMN_NAME = 'commander_id'
    ),
    'DO 0',
    'ALTER TABLE match_room_members
        ADD COLUMN commander_id VARCHAR(255) NULL,
        ADD CONSTRAINT match_room_members_commander
            FOREIGN KEY (commander_id) REFERENCES commander_card_data (uuid) ON DELETE SET NULL'
);
PREPARE add_room_commander FROM @add_room_commander;
EXECUTE add_room_commander;
DEALLOCATE PREPARE add_room_commander;

-- The match UUID identifies the result. Different matches can finish in the same second.
SET @allow_shared_result_time = IF(
    EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'games'
          AND INDEX_NAME = 'timestamp_UNIQUE'
    ),
    'ALTER TABLE games DROP INDEX timestamp_UNIQUE',
    'DO 0'
);
PREPARE allow_shared_result_time FROM @allow_shared_result_time;
EXECUTE allow_shared_result_time;
DEALLOCATE PREPARE allow_shared_result_time;
