ALTER TABLE partners
    DROP COLUMN jibun_address,
    ADD COLUMN subway_stations JSON NULL AFTER detail_address;
