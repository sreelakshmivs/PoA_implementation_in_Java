package com.example.psqljwt.repositories;

import com.example.psqljwt.domain.Config;
import com.example.psqljwt.exceptions.EtAuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class ConfigRepository {

    private static final String SQL_INSERT =
            "INSERT INTO config(id, destinationNetworkId, metadata, transferable) VALUES(NEXTVAL('poa_SEQ'), ?, ?, ? )";
    private static final String SQL_GET_LATEST = "SELECT * FROM poa ORDER BY id DESC LIMIT 1";

    @Autowired
    JdbcTemplate jdbcTemplate;

    public Integer write(
            final String destinationNetworkId,
            final String transferable,
            final String metadata) throws EtAuthException {

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
            throw new EtAuthException("Invalid details. Failed to store configuration");
        }
    }

    public Config readLatest() throws EtAuthException {
        try {
            return jdbcTemplate.queryForObject(SQL_GET_LATEST, userRowMapper);
        } catch (EmptyResultDataAccessException e) {
            throw new EtAuthException("No configuration found");
        }
    }

    private RowMapper<Config> userRowMapper = ((rs, rowNum) -> {
        return new Config(
                rs.getInt("id"),
                rs.getString("destinationNetworkId"),
                rs.getString("metadata"),
                rs.getString("transferable"));
    });
}
