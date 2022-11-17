import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class SimpleAssembler {
    public static void main(String args[]) throws IOException {

        /* Get instructions and registers information from ISA class */
        ISA isa = new ISA();
        isa.getInstructions();
        isa.getRegisters();

        /* Open code.txt and output.bin file */
        String asmCode = "";
        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) {
            asmCode += in.nextLine();
            asmCode += "\n";
        }
        FileWriter output = new FileWriter("code.txt");
        output.write(asmCode);
        output.close();
        File inputFile = new File("code.txt");
        Scanner sc = new Scanner(inputFile);

        /* If code file is empty */
        if (!sc.hasNext()) {
            System.out.println("");
            return;
        }

        /* Declaring List and Maps for storing code, vars and labels */
        ArrayList<String> code = new ArrayList<>();
        HashMap<String, String> vars = new HashMap<>();
        HashMap<String, String> labels = new HashMap<>();
        ArrayList<String> varArr = new ArrayList<>();
        ArrayList<String> labelArr = new ArrayList<>();
        
        int totalVars = 0; /* Num of variables */
        int currAddr = 0; /* Addr pointer */
        boolean varFlag = true; /* Flag to check all var instructions are in the beginning */
        int blankLines = 0;
        int hltCount = 0;

        while (sc.hasNextLine()) {
            String instruction = sc.nextLine();
            if (instruction.isBlank())
                blankLines++;
            else if (instruction.trim().split("\\s+")[0].equals("var")) {
                if (instruction.trim().split("\\s+").length != 2) {
                    System.out.println("Error at line " + currAddr + ": Invalid arguments for var");
                    return;
                }
                if (varFlag) { /* Count var instructions if varFlag is true */
                    String varName = instruction.trim().split("\\s+")[1];
                    if (varName.equals("R0") || varName.equals("R1") || varName.equals("R2") || varName.equals("R3")
                            || varName.equals("R4") || varName.equals("R5") || varName.equals("R6")
                            || varName.equals("FLAGS")) {
                        System.out
                                .println("Error at line " + currAddr + ": Registers cannot be used as variable names");
                        return;
                    }
                    if (vars.containsKey(varName)) {
                        System.out.println(
                                "Error at line " + currAddr + ": Variable " + varName + " is already declared");
                        return;
                    }
                    varArr.add(varName);
                    vars.put(varName, null);
                    totalVars++;
                } else {
                    System.out.println(
                            "Error at line " + currAddr + ": var declarations should be in the beginning of the code");
                    return;
                }
            } else {
                varFlag = false; /* Code section is started, there cannot be any var instruction now */
                if (instruction.trim().split("\\s+")[0].endsWith(":")) {

                    /* Remove ":" from the end and parse label name */
                    String labelName = instruction.trim().split("\\s+")[0].replaceFirst(".$", "");

                    /* Check if label name is valid */
                    if (!isa.isValidLabelName(labelName)) {
                        System.out.println("Error at line " + currAddr + ": Invalid label name");
                        return;
                    }

                    if (labels.containsKey(labelName)) {
                        System.out.println("Error at line " + currAddr + ": Label '" + labelName + "' is already used");
                        return;
                    }

                    /* If there is no instruction with label */
                    if (instruction.trim().split("\\s+").length == 1) {
                        System.out.println("Error at line " + currAddr + ": Label line cannot be empty");
                        return;
                    }

                    String binAddr = Integer.toBinaryString(currAddr);
                    String padding = "";
                    for (int j = 0; j < 8 - binAddr.length(); j++) {
                        padding += "0";
                    }
                    binAddr = padding + binAddr;
                    labels.put(labelName, binAddr);
                }
                if (instruction.trim().equals("hlt"))
                    hltCount++;
                code.add(instruction);
                currAddr++;
            }
        }
        int codeLength = code.size();

        /* Checl for hlt errors */
        String lastLine[] = code.get(codeLength - 1).trim().split("\\s+");
        if (lastLine[0].endsWith(":") && !lastLine[1].equals("hlt")) {
            System.out.println("Error: No hlt instruction in the end of the code");
            return;
        } else if (!lastLine[0].endsWith(":") && !lastLine[0].equals("hlt")) {
            System.out.println("Error: No hlt instruction in the end of the code");
            return;
        } else if (hltCount > 1) {
            System.out.println("Error: Multiple hlt statements");
            return;
        }

        /* Map addr with with vars */
        int count = 0;
        for (String var : varArr) {
            String addr = Integer.toBinaryString(codeLength + count);
            String padding = "";
            for (int j = 0; j < 8 - addr.length(); j++) {
                padding += "0";
            }
            addr = padding + addr;
            vars.put(var, addr);
            count++;
        }

        /*
         * Set program counter and current instruction
         * counter to 0 and loop through the code
         */
        String bin = ""; /* String which stores binary code */
        int CI = 0, lineNum = 1 + totalVars + blankLines; /* lineNum count variable */
        while (CI != 256) {
            String line[] = code.get(CI).trim().split("\\s+"); /* Trims and split all tokens */

            /* Check if line cotains a label */
            if (line[0].endsWith(":")) {
                line = Arrays.copyOfRange(line, 1, line.length);
            }

            String instruction = line[0]; /* Store instuction name in variable */

            /*
             * Special check because movi is a valid instruction according
             * to our HashMap
             */
            if (instruction.equals("movi")) {
                System.out.println("Error at line " + lineNum + ": Invalid Instruction \"movi\"");
                return;
            }

            /* Check if instruction is valid */
            if (isa.instructions.containsKey(instruction)) {

                /*
                 * Special check for move instruction because there are
                 * two instructions with same name
                 */
                if (instruction.equals("mov") || instruction.equals("movf")) {

                    /* Check if there are 2 arguments after mov instruction */
                    if (line.length != 3) {
                        System.out
                                .println("Error at line " + lineNum + ": Invalid number of arguments for "
                                        + instruction);
                        return;
                    }

                    /*
                     * If it is of Type B "mov" instruction
                     * Check if 2nd argument is an immediaate value
                     */
                    if (line[2].charAt(0) == '$') {
                        String reg1 = line[1];
                        if (instruction.equals("movf")) {
                            try {
                                boolean flag = true;
                                for (int i = 0; i < line[2].length(); i++) {
                                    if (line[2].charAt(i) == '.') {
                                        flag = false;
                                        break;
                                    }
                                }
                                if (flag) {
                                    System.out.println("Error at line " + currAddr + ": Not a floating point number");
                                    return;
                                }
                                double imm = Double.parseDouble(line[2].substring(1));
                                if (!isa.isValidReg(reg1) || reg1.equals("FLAGS")) {
                                    System.out.println("Error at line " + lineNum + ": Invalid register " + reg1);
                                    return;
                                }
                                if (imm > 255 || imm < 0) {
                                    System.out
                                            .println("Error at line " + lineNum
                                                    + ": Immediate value should be in range [0,255]");
                                    return;
                                }

                                /*
                                 * Convert imm value from Decimal to Binary and
                                 * add padding zeroes to make it 8 bits
                                 */
                                String decbin = decimalToBinary(imm, 5);
                                int exp = decbin.indexOf(".") - 1;
                                String e = intToBinary(exp, 3);

                                decbin = removeTrailingZeroes(decbin);
                                decbin = decbin.replaceFirst("\\.", "");
                                decbin = addChar(decbin, '.', 1);

                                String decBinWithPadding = decbin.split("\\.")[1];
                                String padding = "";
                                for (int i = 0; i < 5 - decBinWithPadding.length(); i++) {
                                    padding += "0";
                                }
                                decBinWithPadding += padding;

                                String binStr = e + decBinWithPadding;
                                /* Append binary data to result */
                                bin += isa.getInstructionCode(instruction);
                                bin += isa.getRegCode(reg1);
                                bin += binStr;
                                bin += "\n";
                            } catch (Exception e) {
                                System.out.println("Error at line " + lineNum + ": Invalid imm value");
                                return;
                            }
                        } else {
                            try {
                                /* Convert it into Integer and check its range */
                                Integer imm = Integer.parseInt(line[2].substring(1));
                                if (imm > 255 || imm < 0) {
                                    System.out
                                            .println("Error at line " + lineNum
                                                    + ": Immediate value should be in range [0,255]");
                                    return;
                                }

                                /*
                                 * Convert imm value from Decimal to Binary and
                                 * add padding zeroes to make it 8 bits
                                 */
                                String binStr = Integer.toBinaryString(imm);
                                String padding = "";
                                for (int i = 0; i < 8 - binStr.length(); i++) {
                                    padding += "0";
                                }
                                binStr = padding + binStr;

                                /*
                                 * Get OP Code values of instruction and register
                                 * from ISA Class and add it to binary code
                                 */
                                bin += isa.getInstructionCode("movi");
                                bin += isa.getRegCode(reg1);
                                bin += binStr;
                                bin += "\n";
                            } catch (Exception e) {
                                System.out.println("Error at line " + lineNum + ": Invalid imm value");
                                return;
                            }
                        }
                    }

                    /* If it is of Type C "mov" instruction */
                    else {

                        /*
                         * Store first register in reg2. Throw error is register is not valid
                         */
                        String reg2 = line[2];

                        if (!isa.isValidReg(reg2) || reg2.equals("FLAGS")) {
                            System.out.println("Error at line " + lineNum + ": Invalid register " + reg2);
                            return;
                        }
                        /*
                         * Store 1st register in reg1 and append
                         * binary data to result if register is valid
                         */
                        String reg1 = line[1];
                        if (isa.isValidReg(reg1)) {
                            bin += isa.getInstructionCode("mov");
                            bin += "00000";
                            bin += isa.getRegCode(reg1);
                            bin += isa.getRegCode(reg2);
                            bin += "\n";
                        } else {
                            System.out.println("Error at line " + lineNum + ": Invalid register " + reg1);
                            return;
                        }
                    }
                }

                /* For instructions other than "mov" */
                else {

                    /* If instruction is of Type A */
                    if (isa.GetInstructionType(instruction).equals("A")) {

                        /* Check for correct number of arguments */
                        if (line.length != 4) {
                            System.out
                                    .println("Error at line " + lineNum + ": Invalid number of arguments for "
                                            + instruction);
                            return;
                        }

                        /* Validate and store registers in reg1, reg2 and reg3 */
                        String reg1 = line[1];
                        String reg2 = line[2];
                        String reg3 = line[3];
                        if (!isa.isValidReg(reg1) || reg1.equals("FLAGS")) {
                            System.out.println("Error at line " + lineNum + ": Invalid register " + reg1);
                            return;
                        } else if (!isa.isValidReg(reg2) || reg2.equals("FLAGS")) {
                            System.out.println("Error at line " + lineNum + ": Invalid register " + reg2);
                            return;
                        } else if (!isa.isValidReg(reg3) || reg3.equals("FLAGS")) {
                            System.out.println("Error at line " + lineNum + ": Invalid register " + reg3);
                            return;
                        }

                        /* Append binary data in result */
                        bin += isa.getInstructionCode(instruction);
                        bin += "00";
                        bin += isa.getRegCode(reg1);
                        bin += isa.getRegCode(reg2);
                        bin += isa.getRegCode(reg3);
                        bin += "\n";
                    }

                    /* If instruction is of Type B */
                    else if (isa.GetInstructionType(instruction).equals("B")) {
                        /* Check for correct number of arguments */
                        if (line.length != 3) {
                            System.out
                                    .println("Error at line " + lineNum + ": Invalid number of arguments for "
                                            + instruction);
                            return;
                        }

                        /*
                         * Store first register in reg1 and convert imm
                         * value to Integer type and validate them
                         */
                        String reg1 = line[1];
                        try {
                            Integer imm = Integer.parseInt(line[2].substring(1));
                            if (!isa.isValidReg(reg1) || reg1.equals("FLAGS")) {
                                System.out.println("Error at line " + lineNum + ": Invalid register " + reg1);
                                return;
                            }
                            if (imm > 255 || imm < 0) {
                                System.out
                                        .println("Error at line " + lineNum
                                                + ": Immediate value should be in range [0,255]");
                                return;
                            }

                            /*
                             * Convert imm value from Decimal to Binary and
                             * add padding zeroes to make it 8 bits
                             */
                            String binStr = Integer.toBinaryString(imm);
                            String padding = "";
                            for (int i = 0; i < 8 - binStr.length(); i++) {
                                padding += "0";
                            }
                            binStr = padding + binStr;

                            /* Append binary data to result */
                            bin += isa.getInstructionCode(instruction);
                            bin += isa.getRegCode(reg1);
                            bin += binStr;
                            bin += "\n";
                        } catch (Exception e) {
                            System.out.println("Error at line " + lineNum + ": Invalid imm value");
                            return;
                        }

                    }

                    /* If instruction is of Type C */
                    else if (isa.GetInstructionType(instruction).equals("C")) {

                        /* Check for correct number of arguments */
                        if (line.length != 3) {
                            System.out
                                    .println("Error at line " + lineNum + ": Invalid number of arguments for "
                                            + instruction);
                            return;
                        }

                        /*
                         * Store first and second registers in reg1 and reg2
                         * and validate them
                         */
                        String reg1 = line[1];
                        String reg2 = line[2];
                        if (!isa.isValidReg(reg1)) {
                            System.out.println("Error at line " + lineNum + ": Invalid register " + reg1);
                            return;
                        }
                        if (!isa.isValidReg(reg2) || reg2.equals("FLAGS")) {
                            System.out.println("Error at line " + lineNum + ": Invalid register " + reg2);
                            return;
                        }

                        /* Append binary data to result */
                        bin += isa.getInstructionCode(instruction);
                        bin += "00000";
                        bin += isa.getRegCode(reg1);
                        bin += isa.getRegCode(reg2);
                        bin += "\n";
                    }

                    /* If instruction is of Type D */
                    else if (isa.GetInstructionType(instruction).equals("D")) {

                        /* Check for correct number of arguments */
                        if (line.length != 3) {
                            System.out
                                    .println("Error at line " + lineNum + ": Invalid number of arguments for "
                                            + instruction);
                            return;
                        }

                        /* Store and validate register */
                        String reg1 = line[1];
                        if (!isa.isValidReg(reg1) || reg1.equals("FLAGS")) {
                            System.out.println("Error at line " + lineNum + ": Invalid register " + reg1);
                            return;
                        }

                        /* Store and validate memory address */
                        String addr = vars.get(line[2]);
                        if (addr == null) {
                            System.out.println(
                                    "Error at line " + lineNum + ": Variable '" + line[2] + "' not found");
                            return;
                        }

                        if (addr.length() != 8) {
                            System.out.println("Error at line " + lineNum + ": Invalid memory address " + addr);
                            return;
                        }

                        /* Append binary data to result */
                        bin += isa.getInstructionCode(instruction);
                        bin += isa.getRegCode(reg1);
                        bin += addr;
                        bin += "\n";
                    }

                    /* If instruction is of Type E */
                    else if (isa.GetInstructionType(instruction).equals("E")) {

                        /* Check for correct number of arguments */
                        if (line.length != 2) {
                            System.out
                                    .println("Error at line " + lineNum + ": Invalid number of arguments for "
                                            + instruction);
                            return;
                        }

                        /* Store and validate memory address */
                        if (!labels.containsKey(line[1])) {
                            System.out.println("Error at line " + lineNum + ": Label '" + line[1] + "' not found");
                            return;
                        }

                        String labelAddr = labels.get(line[1]);
                        if (labelAddr.length() != 8) {
                            System.out.println("Error at line " + lineNum + ": Invalid memory address " + labelAddr);
                            return;
                        }

                        /* Append binary data to result */
                        bin += isa.getInstructionCode(instruction);
                        bin += "000";
                        bin += labelAddr;
                        bin += "\n";
                    }

                    /* If instruction is hlt, append 01010 00000000000 to result */
                    else if (isa.GetInstructionType(instruction).equals("F")) {

                        /* Check for correct number of arguments */
                        if (line.length != 1) {
                            System.out
                                    .println("Error at line " + lineNum + ": hlt does not take any argument");
                            return;
                        }

                        bin += "0101000000000000";
                        bin += "\n";
                        break;
                    }
                }
            }

            /* Instruction is invalid */
            else {
                System.out.println("Error at line " + lineNum + ": Invalid Instruction " + instruction);
                return;
            }

            /* Increment current instruction and program counter pointers */
            CI++;
            lineNum++;
        }

        /* Write binary data to output.bin file */
        sc.close();
        System.out.println(bin);

    }

    // taken from geeksforgeeks.com
    public static String decimalToBinary(double num, int k_prec) {
        String binary = "";
        int Integral = (int) num;
        double fractional = num - Integral;
        while (Integral > 0) {
            int rem = Integral % 2;
            binary += ((char) (rem + '0'));
            Integral /= 2;
        }
        binary = reverse(binary);
        binary += ('.');
        while (k_prec-- > 0) {
            fractional *= 2;
            int fract_bit = (int) fractional;
            if (fract_bit == 1) {
                fractional -= fract_bit;
                binary += (char) (1 + '0');
            } else {
                binary += (char) (0 + '0');
            }
        }
        return binary;
    }

    public static String reverse(String input) {
        char[] temparray = input.toCharArray();
        int left, right = 0;
        right = temparray.length - 1;
        for (left = 0; left < right; left++, right--) {
            char temp = temparray[left];
            temparray[left] = temparray[right];
            temparray[right] = temp;
        }
        return String.valueOf(temparray);
    }

    public static String removeLeadingZeroes(String s) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() > 0 && sb.charAt(0) == '0') {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }

    public static String removeTrailingZeroes(String s) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '0') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public static String addChar(String str, char ch, int position) {
        StringBuilder sb = new StringBuilder(str);
        sb.insert(position, ch);
        return sb.toString();
    }

    public static String intToBinary(int num, int bits) {
        String bin = Integer.toBinaryString(num);
        String padding = "";
        for (int i = 0; i < bits - bin.length(); i++) {
            padding += "0";
        }
        return padding + bin;
    }
}
