
/* This file contains all information about the ISA in HashMaps
   and methods to access the HashMap */
import java.util.HashMap;

public class ISA {
    HashMap<String, String[]> instructions = new HashMap<>(20);
    HashMap<String, String[]> registers = new HashMap<>(8);

    public HashMap<String, String[]> getInstructions() {
        instructions.put("add", new String[] { "10000", "A" });
        instructions.put("sub", new String[] { "10001", "A" });
        instructions.put("movi", new String[] { "10010", "B" });
        instructions.put("mov", new String[] { "10011", "C" });
        instructions.put("ld", new String[] { "10100", "D" });
        instructions.put("st", new String[] { "10101", "D" });
        instructions.put("mul", new String[] { "10110", "A" });
        instructions.put("div", new String[] { "10111", "C" });
        instructions.put("rs", new String[] { "11000", "B" });
        instructions.put("ls", new String[] { "11001", "B" });
        instructions.put("xor", new String[] { "11010", "A" });
        instructions.put("or", new String[] { "11011", "A" });
        instructions.put("and", new String[] { "11100", "A" });
        instructions.put("not", new String[] { "11101", "C" });
        instructions.put("cmp", new String[] { "11110", "C" });
        instructions.put("jmp", new String[] { "11111", "E" });
        instructions.put("jlt", new String[] { "01100", "E" });
        instructions.put("jgt", new String[] { "01101", "E" });
        instructions.put("je", new String[] { "01111", "E" });
        instructions.put("hlt", new String[] { "01010", "F" });
        instructions.put("addf", new String[] { "00000", "A"});
        instructions.put("subf", new String[] { "00001", "A"});
        instructions.put("movf", new String[] { "00010", "B"});
        return instructions;
    }

    public HashMap<String, String[]> getRegisters() {
        registers.put("R0", new String[] { "000", null });
        registers.put("R1", new String[] { "001", null });
        registers.put("R2", new String[] { "010", null });
        registers.put("R3", new String[] { "011", null });
        registers.put("R4", new String[] { "100", null });
        registers.put("R5", new String[] { "101", null });
        registers.put("R6", new String[] { "110", null });
        registers.put("FLAGS", new String[] { "111", null });
        return registers;
    }

    public String getRegCode(String reg) {
        return registers.get(reg)[0];
    }

    public String getRegValue(String reg) {
        return registers.get(reg)[1];
    }

    public boolean isValidReg(String reg) {
        return registers.containsKey(reg);
    }

    public void setRegValue(String reg, int value) {
        String OPCode = getRegCode(reg);
        registers.put(reg, new String[] { OPCode, Integer.toString(value) });
    }

    public String getInstructionCode(String instruction) {
        return instructions.get(instruction)[0];
    }

    public String GetInstructionType(String instruction) {
        return instructions.get(instruction)[1];
    }

    public boolean isValidLabelName(String labelName) {
        return true;
    }
}
