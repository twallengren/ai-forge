package aiforge.agents;

import java.util.List;

public class ShortTermMemory extends MemoryStore {

    private static final String SELF_KEY = "Self";

    public void storeMemory(Memory memory) {
        super.storeMemory(SELF_KEY, memory);
    }
}
