package com.example.subcontractor.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import com.example.subcontractor.domain.Config;
import com.example.subcontractor.exceptions.InternalServerErrorException;

@Repository
public class ConfigRepository {

    private static final String SQL_INSERT =
            "INSERT INTO config(id, destination_network_id, metadata, transferable) VALUES(NEXTVAL('config_seq'), ?, ?, ? )";
    private static final String SQL_GET_LATEST = "SELECT * FROM config ORDER BY id DESC LIMIT 1";

    @Autowired
    JdbcTemplate jdbcTemplate;

    public Integer write(
            final String destinationNetworkId,
            final String transferable,
            final String metadata) {

        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps =
                        connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, destinationNetworkId);
                ps.setString(2, metadata);
                ps.setString(3, transferable);
                return ps;
            }, keyHolder);
            return (Integer) keyHolder.getKeys().get("id");
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to store configuration");
        }
    }

    public Config readLatest() {
        try {
            return jdbcTemplate.queryForObject(SQL_GET_LATEST, userRowMapper);
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException("No configuration found");
        }
    }

    private RowMapper<Config> userRowMapper = ((rs, rowNum) -> {
        return new Config(
                rs.getInt("id"),
                rs.getString("destination_network_id"),
                rs.getString("metadata"),
                rs.getString("transferable"));
    });
}
