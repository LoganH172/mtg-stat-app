USE commander;

CREATE TABLE IF NOT EXISTS match_rooms (
    match_id CHAR(36) NOT NULL,
    room_code CHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_by_player_id VARCHAR(255) NOT NULL,
    request_id CHAR(36) NOT NULL,
    playgroup_id VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (match_id),
    UNIQUE KEY match_rooms_room_code (room_code),
    UNIQUE KEY match_rooms_creator_request (created_by_player_id, request_id),
    CONSTRAINT match_rooms_creator FOREIGN KEY (created_by_player_id)
        REFERENCES players (uuid) ON DELETE CASCADE,
    CONSTRAINT match_rooms_playgroup FOREIGN KEY (playgroup_id)
        REFERENCES playgroup (uuid) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS match_room_members (
    match_id CHAR(36) NOT NULL,
    player_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (match_id, player_id),
    CONSTRAINT match_room_members_room FOREIGN KEY (match_id)
        REFERENCES match_rooms (match_id) ON DELETE CASCADE,
    CONSTRAINT match_room_members_player FOREIGN KEY (player_id)
        REFERENCES players (uuid) ON DELETE CASCADE
) ENGINE=InnoDB;
