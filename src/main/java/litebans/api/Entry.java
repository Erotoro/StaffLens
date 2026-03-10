package litebans.api;

import java.util.UUID;

public class Entry {
    public String getType() { return ""; }
    public String getExecutorUUID() { return UUID.randomUUID().toString(); }
    public String getExecutorName() { return ""; }
    public String getUuid() { return ""; }
    public String getReason() { return ""; }
    public String getDurationString() { return ""; }
}
