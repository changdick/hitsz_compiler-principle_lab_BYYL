package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.ArrayDeque;
import java.util.Deque;

// TODO: 实验三: 实现语义分析

// 语义分析栈的数据结构：只用到终结符 token  和类型type
public class SemanticAnalyzer implements ActionObserver {
    private SymbolTable symbolTable;

    private final Deque<VTSymbol> symbolStack = new ArrayDeque<>();   // 符号栈,还是用symbolEntry，其实根本用不到符号，只用得到类型
    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
//        throw new NotImplementedException();

    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
//        throw new NotImplementedException();

        // 规约时要看是哪一条产生式，4，5号产生式要做类型的传递
        int productionIndex = production.index();  // 获取产生式号判断
        if (productionIndex == 5) {
            // D -> int;
            symbolStack.pop();   // 弹出 int这个token
            symbolStack.push(new VTSymbol(production.head(), SourceCodeType.Int));
        } else if (productionIndex == 4) {
            //S -> D id; 要更新符号表
            VTSymbol id = symbolStack.pop(); // 弹出栈顶的符号id
            VTSymbol D  = symbolStack.pop(); // 弹出栈顶的D
            symbolTable.get(id.getToken().getText()).setType(D.type);   //  从符号表里获取id，设置id的type为D的type
            symbolStack.push(new VTSymbol(production.head()));


        } else {
            for (int i = 0; i < production.body().size(); i++) {

                symbolStack.pop();  // 弹出符号

            }
            symbolStack.push(new VTSymbol(production.head()));
        }


    }

    /**
     *
     * @param currentStatus 调用时语法分析栈的栈顶状态
     * @param currentToken  当前的词法单元
     */
    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作
//        throw new NotImplementedException();
        symbolStack.push(new VTSymbol(currentToken));  // 把移进的终结符压入符号栈

    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
//        throw new NotImplementedException();
        this.symbolTable = table;
    }

}

