package litebans.api;

public class Events {
    private static final Events instance = new Events();
    public static Events get() { return instance; }
    public void register(Listener listener) {}

    public interface Listener {
        void entryAdded(Entry entry);
    }
}
