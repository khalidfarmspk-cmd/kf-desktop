package config;

import java.util.UUID;

/**
 * ID helpers for cloud-sync-ready records.
 */
public final class Ids {

    private Ids() {
    }

    public static String newUuid() {
        return UUID.randomUUID().toString();
    }
}
