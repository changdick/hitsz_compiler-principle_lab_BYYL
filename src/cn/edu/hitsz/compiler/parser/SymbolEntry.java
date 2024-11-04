package cn.edu.hitsz.compiler.parser;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.NonTerminal;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;


/**
 * 语法分析的符号栈中，既要放非终结符又要放终结符，所以做了这样一个数据类，非终结符和终结符都可以放进去
 */
class SymbolEntry {
    Token token;
    NonTerminal variable;

    SourceCodeType type;

    private SymbolEntry(Token token, NonTerminal variable, SourceCodeType type){
        this.token = token;
        this.variable = variable;
        this.type = type;
    }

    public SymbolEntry(Token token){
        this(token, null, null);
    }

    public SymbolEntry(NonTerminal variable){
        this(null, variable, null);
    }
    public SymbolEntry(NonTerminal variable, SourceCodeType type){
        this(null, variable, type);
    }

    public Token getToken() {
        return token;
    }

    public boolean isToken(){
        return token != null;
    }
}