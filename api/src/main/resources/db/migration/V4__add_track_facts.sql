ALTER TABLE tracks
    ADD COLUMN facts_version INTEGER,
    ADD COLUMN total_distance_meters DOUBLE PRECISION,
    ADD COLUMN elevation_gain_meters DOUBLE PRECISION,
    ADD COLUMN elevation_loss_meters DOUBLE PRECISION,
    ADD COLUMN minimum_elevation_meters DOUBLE PRECISION,
    ADD COLUMN maximum_elevation_meters DOUBLE PRECISION,
    ADD COLUMN maximum_uphill_grade_percent DOUBLE PRECISION,
    ADD COLUMN maximum_downhill_grade_percent DOUBLE PRECISION;

ALTER TABLE tracks
    ADD CONSTRAINT ck_tracks_facts_version
        CHECK (
            facts_version IS NULL
            OR facts_version BETWEEN 1 AND 32767
        ),

    ADD CONSTRAINT ck_tracks_facts_consistency
        CHECK (
            (
                facts_version IS NULL
                AND total_distance_meters IS NULL
                AND elevation_gain_meters IS NULL
                AND elevation_loss_meters IS NULL
                AND minimum_elevation_meters IS NULL
                AND maximum_elevation_meters IS NULL
                AND maximum_uphill_grade_percent IS NULL
                AND maximum_downhill_grade_percent IS NULL
            )
            OR
            (
                facts_version IS NOT NULL
                AND total_distance_meters IS NOT NULL
                AND total_distance_meters >= 0.0

                AND (
                    elevation_point_count = 0
                    AND elevation_gain_meters IS NULL
                    AND elevation_loss_meters IS NULL
                    AND minimum_elevation_meters IS NULL
                    AND maximum_elevation_meters IS NULL
                    OR
                    elevation_point_count > 0
                    AND elevation_gain_meters IS NOT NULL
                    AND elevation_gain_meters >= 0.0
                    AND elevation_loss_meters IS NOT NULL
                    AND elevation_loss_meters >= 0.0
                    AND minimum_elevation_meters IS NOT NULL
                    AND maximum_elevation_meters IS NOT NULL
                    AND minimum_elevation_meters
                        <= maximum_elevation_meters
                )

                AND (
                    maximum_uphill_grade_percent IS NULL
                    OR maximum_uphill_grade_percent >= 0.0
                )

                AND (
                    maximum_downhill_grade_percent IS NULL
                    OR maximum_downhill_grade_percent >= 0.0
                )
            )
        );
