package com.example.subcontractor.domain;

public class Poa {

    private final int id;
    private final String destinationNetworkId;

    // The rest of the PoA fields are not used at the moment.

    public Poa(final int id, final String destinationNetworkId) {
        this.id = id;
        this.destinationNetworkId = destinationNetworkId;
    }

    public int getId() {
        return id;
    }

    public String getDestinationNetworkId() {
        return destinationNetworkId;
    }

}
