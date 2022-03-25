package com.example.psqljwt.domain;

public class Poa {

    private Integer id;
    private String poa;

    public Poa(final Integer id, final String poa) {
        this.id = id;
        this.poa = poa;
    }

    public Integer getId() {
        return id;
    }

    public String getPoa() {
        return poa;
    }

}
