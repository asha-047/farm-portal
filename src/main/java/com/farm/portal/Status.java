package com.farm.portal;

public enum Status {
    HARVESTED, IN_TRANSIT, AT_RETAIL;

    public Status next() {
        return switch (this) {
            case HARVESTED -> IN_TRANSIT;
            case IN_TRANSIT -> AT_RETAIL;
            case AT_RETAIL -> null;
        };
    }
}
