package aiforge.agents;

import java.util.*;

public abstract class MemoryStore {

    protected final Map<String, List<Memory>> memories;

    public MemoryStore() {
        this.memories = new HashMap<>();
    }

    public void storeMemory(String key, Memory memory) {
        if (key == null || key.isBlank() || memory == null || memory.getMemory().isBlank()) {
            return; // Ignore invalid inputs
        }
        memories.computeIfAbsent(key, k -> new ArrayList<>()).add(memory);
    }

    public List<Memory> getMemoriesByKey(String key) {
        return memories.getOrDefault(key, new ArrayList<>());
    }

    public Map<String, List<Memory>> getAllMemories() {
        return new HashMap<>(memories);
    }

    public String getAllMemoriesAsString() {
        StringBuilder allMemories = new StringBuilder();
        memories.forEach((key, memoryList) -> {
            allMemories.append("Key: ").append(key).append("\n");
            for (Memory memory : memoryList) {
                allMemories.append("- ").append(memory.getMemory()).append("\n");
            }
        });
        return allMemories.toString().trim();
    }
}
