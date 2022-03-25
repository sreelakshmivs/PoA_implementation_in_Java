package com.example.psqljwt.repositories;

import com.example.psqljwt.domain.Poa;
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
public class PoaRepository {

    private static final String SQL_CREATE = "INSERT INTO poa(id, poa) VALUES(NEXTVAL('poa_seq'), ? )";
    private static final String SQL_GET_LATEST = "SELECT * FROM poa ORDER BY id DESC LIMIT 1";

    @Autowired
    JdbcTemplate jdbcTemplate;

    public Integer writePoa(final String poa) throws EtAuthException {
        try {
            final KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(SQL_CREATE, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, poa);
                return ps;
            }, keyHolder);
            return (Integer) keyHolder.getKeys().get("id");
        }catch (Exception e) {
            throw new EtAuthException("Invalid details. Failed to create PoA");
        }
    }

    public String readLatestPoa() throws EtAuthException {
        try {
            final Poa poa = jdbcTemplate.queryForObject(SQL_GET_LATEST, userRowMapper);
            return poa.getPoa();
        }catch (EmptyResultDataAccessException e) {
            throw new EtAuthException("Invalid clientId/password");
        }
    }

    private RowMapper<Poa> userRowMapper = ((rs, rowNum) -> {
        return new Poa(rs.getInt("id"), rs.getString("poa"));
    });
}
