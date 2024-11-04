package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.*;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private final Deque<SymbolEntry> symbolStack = new ArrayDeque<>();   // 符号栈,和语义分析一样

    private final Stack<IRValue> irStack = new Stack<>();  // IR生成过程中用的栈 这个值本来可以作为一个字段放到SymbolEntry，但是又会导致前面用到SymbolEntry的太冗余

    private final List<Instruction> instructions = new ArrayList<>();   // 指令存在这个表
    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
//        throw new NotImplementedException();
        // 移位时，ir生成只需要把token存进来就行
        symbolStack.push(new SymbolEntry(currentToken));
        irStack.push(null);                       // ir占位 ， 只有规约的时候才会知道ir应该是什么

    }


    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
//        throw new NotImplementedException();
        switch (production.index()) {
            case 15 -> {
//                B -> IntConst; 值出现 此情况弹出栈顶，将数字值压回 数字值从token获取
                Token token = symbolStack.pop().getToken();
                IRImmediate immediate = IRImmediate.of(Integer.parseInt(token.getText()));   //用token作为数字，构建ir立即数
                irStack.pop();
                irStack.push(immediate);  // 把栈顶占位的弹了，压回ir数字

                symbolStack.push(new SymbolEntry(production.head()));

            }
            case 14 -> {
//                B -> id;  只需要把id的名字传给B
                Token token = symbolStack.pop().getToken();  //弹出栈顶，得到的是id
                IRVariable variable = IRVariable.named(token.getText());
                irStack.pop();
                irStack.push(variable);  // 结构和产生式15基本一样

                symbolStack.push(new SymbolEntry(production.head()));
            }
            case 13 -> {
//                B -> ( E );  和14、15基本一样，但是E 不一定是IRImmediate或者IRVariable，就用IRValue,弹栈要弹出3个
                symbolStack.pop();symbolStack.pop();symbolStack.pop();  // 符号栈没有含义，只负责占位置，弹3个
                irStack.pop();
                IRValue value = irStack.pop();   // 第二个其实就是E的值，E.value要存下来，待会压回去作为B.value
                irStack.pop();
                irStack.push(value);

                symbolStack.push(new SymbolEntry(production.head()));

            }
            case 10, 12 -> {
//                E -> A; A -> B;  一个的值传给另一个，比13还基础的情况
                symbolStack.pop();  //符号栈没有含义
                IRValue value = irStack.pop();
                irStack.push(value);            //弹出一个右部值，压回去作为左部值

                symbolStack.push(new SymbolEntry(production.head()));
            }
            case 11 -> {
//                A -> A * B;
                symbolStack.pop();symbolStack.pop();symbolStack.pop();  //三个符号可以全部弹出。 第二个弹出的肯定是token *
                IRValue bValue = irStack.pop();  // B.value
                irStack.pop();        // 这个是 *
                IRValue aValue = irStack.pop();   // A.value
                IRVariable newValue = IRVariable.temp();
                irStack.push(newValue);  //压入栈中

                instructions.add(Instruction.createMul(newValue, aValue, bValue));  // 创建指令，并加入指令表

                symbolStack.push(new SymbolEntry(production.head()));
            }
            case 9 -> {
                // 算术运算都跟11 类似   E -> E - A;
                symbolStack.pop();symbolStack.pop();symbolStack.pop();  //三个符号可以全部弹出。 第二个弹出的肯定是token *
                IRValue aValue = irStack.pop();  // A.value
                irStack.pop();        // 这个是 -
                IRValue eValue = irStack.pop();   // 右部E.value
                IRVariable newValue = IRVariable.temp();
                irStack.push(newValue);  //压入栈中

                instructions.add(Instruction.createSub(newValue, eValue, aValue));

                symbolStack.push(new SymbolEntry(production.head()));

            }
            case 8 -> {
//                E -> E + A;
                symbolStack.pop();symbolStack.pop();symbolStack.pop();  //三个符号可以全部弹出。 第二个弹出的肯定是token *
                IRValue aValue = irStack.pop();  // A.value
                irStack.pop();        // 这个是 +
                IRValue eValue = irStack.pop();   // 右部E.value
                IRVariable newValue = IRVariable.temp();
                irStack.push(newValue);  //压入栈中

                instructions.add(Instruction.createAdd(newValue, eValue, aValue));

                symbolStack.push(new SymbolEntry(production.head()));
            }

            case 6 -> {
//                S -> id = E;
                symbolStack.pop();  // pop E
                symbolStack.pop();  // pop =
                Token token = symbolStack.pop().getToken();
                IRValue eValue = irStack.pop(); //ir栈第一次弹出是E.value
                irStack.pop(); //第二次弹出是=
                irStack.pop(); // 第三次弹出的是id占位用的

                IRVariable variable = IRVariable.named(token.getText());  //通过token获得variable的名
                instructions.add(Instruction.createMov(variable, eValue));  //构造赋值指令

                irStack.push(null);  // S只需要占位置

                symbolStack.push(new SymbolEntry(production.head()));
            }
            case 7 -> {
//                S -> return E;
                symbolStack.pop();symbolStack.pop();  //符号栈无含义，直接弹出2个

                IRValue value = irStack.pop();
                irStack.pop();
                irStack.push(null);

                instructions.add(Instruction.createRet(value));

                symbolStack.push(new SymbolEntry(production.head()));

            }
            default -> {
                // 对于没有定义翻译过程的指令，只需要跟着语法分析过程走就行，做好栈的维护
                for (int i = 0; i < production.body().size(); i++) {
                    symbolStack.pop();
                    irStack.pop();
                }
                irStack.push(null);
                symbolStack.push(new SymbolEntry(production.head()));

            }


        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
//        throw new NotImplementedException();
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
//        throw new NotImplementedException();

    }

    public List<Instruction> getIR() {
        // TODO
//        throw new NotImplementedException();
        return instructions;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

