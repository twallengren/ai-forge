package aiforge.agents;

public class Memory {

    private String memory;

    public Memory() {
        this.memory = ""; // Start with an empty memory
    }

    public Memory(String initialMemory) {
        this.memory = initialMemory;
    }

    public String getMemory() {
        return memory;
    }

    public void append(String newMemory) {
        if (newMemory == null || newMemory.isBlank()) {
            return; // Ignore empty or null updates
        }
        if (memory.isBlank()) {
            memory = newMemory.trim();
        } else {
            memory += "\n" + newMemory.trim();
        }
    }
}
