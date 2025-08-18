package org.nmcpye.datarun.jpa.etl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
public abstract class AbstractEtlIntegrationTest {

    protected static final PostgreSQLContainer<?> PG = new
        PostgreSQLContainer<>("postgres:16.2")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    protected static DataSource ds;
    protected static JdbcTemplate jdbc;
    protected static NamedParameterJdbcTemplate namedJdbc;

    @BeforeAll
    public static void setupContainer() {
        PG.start();

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        ds = new HikariDataSource(cfg);

        jdbc = new JdbcTemplate(ds);
        namedJdbc = new NamedParameterJdbcTemplate(ds);
        // create schema used in tests (repeat_instance + element_data_value with partial unique indexes)
        String ddl = """
            CREATE TABLE repeat_instance (
              id varchar(26) PRIMARY KEY,
              submission_id varchar(50) NOT NULL,
              repeat_path varchar(3000) NOT NULL,
              repeat_index bigint,
              client_updated_at timestamp,
              deleted_at timestamp,
              created_date timestamp NOT NULL DEFAULT now(),
              last_modified_date timestamp NOT NULL DEFAULT now(),
              created_by varchar(100),
              last_modified_by varchar(100)
         );
         CREATE INDEX IF NOT EXISTS idx_repeat_instance_submission_path ON repeat_instance(submission_id, repeat_path);

         CREATE TABLE element_data_value (
            id bigserial PRIMARY KEY,
            submission_id varchar(50) NOT NULL,
            repeat_instance_id varchar(26),
            element_id varchar(200) NOT NULL,
            option_id varchar(100),
            value_text text,
            value_num numeric,
            value_bool boolean,
            assignment_id varchar(50),
            template_id varchar(50) NOT NULL,
            category_id varchar(200),
            deleted_at timestamp,
            created_date timestamp NOT NULL DEFAULT now(),
            last_modified_date timestamp NOT NULL DEFAULT now(),
            -- computed/stored columns for stable conflict target
            repeat_instance_key text GENERATED ALWAYS AS (COALESCE(repeat_instance_id, '')) STORED,
            selection_key text GENERATED ALWAYS AS (COALESCE(option_id, '')) STORED,
            -- row_type: 'S' single-value, 'M' multi-select row
            row_type char(1) NOT NULL DEFAULT 'S'
         );

         -- unified unique index encoding the semantics:
         --  - single-value rows => row_type='S', selection_key = ''  => unique per (submission, element, repeat)
         --  - multi-select rows => row_type='M', selection_key = option_id => unique per option_id
         CREATE UNIQUE INDEX ux_element_value_unique
           ON element_data_value (
             submission_id,
             element_id,
             repeat_instance_key,
             row_type,
             selection_key
           );

         -- helper index for lookups
         CREATE INDEX idx_element_data_value_submission_repeat ON element_data_value(submission_id, repeat_instance_id);
         """;

        // run schema DDL
//        String ddl = TestResources.SCHEMA_DDL; // alternatively inline DDL string as you used
        jdbc.execute(ddl);
    }

    @AfterAll
    public static void tearDownContainer() {
        if (ds instanceof HikariDataSource) ((HikariDataSource) ds).close();
        PG.stop();
    }

    protected void truncateTables() {
        jdbc.update("TRUNCATE TABLE element_data_value CASCADE");
        jdbc.update("TRUNCATE TABLE repeat_instance CASCADE");
    }
}

