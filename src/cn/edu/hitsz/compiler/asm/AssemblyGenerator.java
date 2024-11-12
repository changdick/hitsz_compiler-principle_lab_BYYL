package cn.edu.hitsz.compiler.asm;


import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.ir.InstructionKind;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {
    private final List<String> usedRegisters = new ArrayList<>();  // 用于追踪已经使用的寄存器？ 我们需要这个吗
    private final List<String> freeRegisters = new ArrayList<>();  // 用于追踪空闲寄存器
    private final List<String> assemblyInstructions = new ArrayList<>();

    private final Map<String, String> registerMap = new HashMap<>();
    private List<Instruction> instructions;


    private static final String[] REGISTER_POOL = {
            "t0", "t1", "t2", "t3", "t4", "t5", "t6"
    };

    public AssemblyGenerator() {
        // 初始化空闲寄存器池
        freeRegisters.addAll(Arrays.asList(REGISTER_POOL));
    }

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
//        throw new NotImplementedException();
        instructions = originInstructions;
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
//        throw new NotImplementedException();
        // 依次处理每一条指令
        for (Instruction instruction : instructions) {
            generateAssembly(instruction);
            releaseUnusedRegisters(instruction);
        }
    }

    /**
     * 生成汇编代码
     */
    public void generateAssembly(Instruction instruction) {
        InstructionKind kind = instruction.getKind();
        String target = null;
        String src1 = null;
        String src2 = null;
        boolean lhsImmediate = false;
        boolean rhsImmediate = false;

        // 根据指令的种类来决定如何获取操作数, 取数的get跟指令的种类有关， InstructionKind有提供方法判断操作数是几个。
        if (kind.isBinary()) {
            // 对于二元操作，获取 LHS 和 RHS
            src1 = getOperand(instruction.getLHS());
            src2 = getOperand(instruction.getRHS());
            target = getOperand(instruction.getResult());
            lhsImmediate = instruction.getLHS().isImmediate();
            rhsImmediate = instruction.getRHS().isImmediate();
        } else if (kind.isUnary()) {
            // 对于 MOV 指令，只获取从操作数
            src2 = getOperand(instruction.getFrom());
            target = getOperand(instruction.getResult());
            rhsImmediate = instruction.getFrom().isImmediate();
        } else if (kind.isReturn()) {
            // 对于 RET 指令，直接获取返回值
            src1 = getOperand(instruction.getReturnValue());
        }

        // 如果是 ADD 指令且 lhs 是立即数，交换 src1 和 src2
        if (kind == InstructionKind.ADD && lhsImmediate) {
            String tempSrc = src1;
            src1 = src2;
            src2 = tempSrc;

            boolean tempImmediate = lhsImmediate;
            lhsImmediate = rhsImmediate;
            rhsImmediate = tempImmediate;
        }

        switch (kind) {
            case ADD:
                if (rhsImmediate) {
                    assemblyInstructions.add("addi " + target + ", " + src1 + ", " + src2);
                } else {
                    assemblyInstructions.add("add " + target + ", " + src1 + ", " + src2);
                }
                break;

            case SUB:
                if (rhsImmediate) {
                    // 使用 addi 指令，并将立即数取负
                    assemblyInstructions.add("addi " + target + ", " + src1 + ", -" + src2);
                } else if (lhsImmediate) {
                    // 如果 src1 是立即数，用 li 加载立即数到寄存器，再用 sub
                    // 此时，target已经取了寄存器，其实不必申请临时寄存器，而是直接把立即数加载到target中，
                    // 然后用target减去减数，存target，一样完成减法，不需要临时寄存器。
//                    String tempReg = allocateRegister();
                    assemblyInstructions.add("li " + target + ", " + src1);
                    assemblyInstructions.add("sub " + target + ", " + target + ", " + src2);
//                    freeRegister(tempReg);  // 释放临时寄存器
                } else {
                    assemblyInstructions.add("sub " + target + ", " + src1 + ", " + src2);
                }
                break;

            case MUL:
                if (rhsImmediate) {
//                    String tempReg = allocateRegister();
                    assemblyInstructions.add("li " + target + ", " + src2);
                    assemblyInstructions.add("mul " + target + ", " + src1 + ", " + target);
//                    freeRegister(tempReg);  // 释放临时寄存器
                } else {
                    assemblyInstructions.add("mul " + target + ", " + src1 + ", " + src2);
                }
                break;

            case MOV:
                if (rhsImmediate) {
                    // 如果是立即数，则使用 li 指令
                    assemblyInstructions.add("li " + target + ", " + src2);
                } else {
                    assemblyInstructions.add("mv " + target + ", " + src2);
                }
                break;

            case RET:

                assemblyInstructions.add("mv a0, " + src1);
                break;
        }
    }
    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
//        throw new NotImplementedException();
        try (FileWriter writer = new FileWriter(path)) {
            for (String line : assemblyInstructions) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void freeRegister(String reg) {
        usedRegisters.remove(reg);
        freeRegisters.add(reg);
    }


    private String allocateRegister() {
        if (freeRegisters.isEmpty()) {
            throw new RuntimeException("No available registers");
        }
        // 从空闲寄存器池中取出一个寄存器
        String reg = freeRegisters.removeLast(); // 从列表末尾取出
        usedRegisters.add(reg);
        return reg;
    }

    private String getRegister(String variable) {
        if (registerMap.containsKey(variable)) {
            return registerMap.get(variable);
        } else {
            String reg = allocateRegister();
            registerMap.put(variable, reg);
            return reg;
        }

    }

    private String getOperand(IRValue value) {
        // 返回操作数的字符串表示
        // 假设有方法返回 IRValue 的字符串表示
        // 如果操作数是立即数，直接返回其值
        if (value.isImmediate()) {
            return value.toString();  // 假设 toString 返回的是立即数的值
        }
        // 有可能是存在寄存器里的，其他情况直接返回存它的寄存器。存它的寄存器可能是

        // 如果是其他类型的操作数，我们分配一个新的寄存器
        return getRegister(value.toString());  // 为该操作数分配一个新的寄存器

    }


    private void releaseUnusedRegisters(Instruction currentInstruction) {
        Set<String> usedInFuture = new HashSet<>();

        // 遍历当前指令之后的每条指令，检查变量的使用情况
        for (int i = instructions.indexOf(currentInstruction) + 1; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            InstructionKind kind = instruction.getKind();

            // 根据指令种类获取使用的变量
            if (kind.isBinary()) {
                usedInFuture.add(instruction.getLHS().toString());
                usedInFuture.add(instruction.getRHS().toString());
                usedInFuture.add(instruction.getResult().toString());
            } else if (kind.isUnary()) {
                usedInFuture.add(instruction.getFrom().toString());
                usedInFuture.add(instruction.getResult().toString());
            } else if (kind.isReturn()) {
                if (instruction.getReturnValue() != null) {
                    usedInFuture.add(instruction.getReturnValue().toString());
                }
            }
        }
        // 遍历 registerMap 中的变量，检查是否仍在使用
        Iterator<Map.Entry<String, String>> iterator = registerMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String variable = entry.getKey();
            String register = entry.getValue();

            if (!usedInFuture.contains(variable)) {
                // 如果变量不再使用，释放寄存器
                iterator.remove(); // 从 registerMap 中移除该变量
                freeRegister(register); // 释放寄存器
            }
        }
    }
}


