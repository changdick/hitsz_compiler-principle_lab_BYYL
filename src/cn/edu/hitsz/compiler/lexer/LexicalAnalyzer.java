package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;

    // 输入源代码，初始为空，读取文件时追加
    private String sourceCode = "";

    public List<Token> tokens = new ArrayList<>();

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        StringBuilder inputCodeBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                inputCodeBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        sourceCode = inputCodeBuilder.toString();
    }


    /**
     * 执行词法分析，准备好用于返回的 token 列表
     * 需要维护实验一所需的符号表条目，而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        int i = 0;
        char[] word = sourceCode.toCharArray();
        State currentState = State.START;

        while (i < sourceCode.length()) {
            char ch = word[i];

            // 状态转移
            switch (currentState) {
                case START -> {
                    if (Character.isWhitespace(ch)) {
                        //  若ch为空格，直接读下一个字符，保持为START状态
                        i++;
                    } else if (isSinglePunctuation(ch)) {
//                        currentState = State.SINGLE_PUNCTUATION;
                        // 如果是可以用simple方法直接构造的Token，直接把他构造了，读下一个字符，并且要注意分号
                        // 能够用Token.simple()方法的情况，但是要判断';'传入'semicolon'
                        if (ch == ';') {
                            tokens.add(Token.simple("Semicolon"));
                        } else {
                            tokens.add(Token.simple(String.valueOf(ch)));
                        }
                        i++;

                    } else if (Character.isLetter(ch)) {
                        currentState = State.ID;
                    } else if (Character.isDigit(ch)) {
                        currentState = State.INTCONST;
                    } else {
                        currentState = State.ERROR;
                    }
                }
                // 能够用Token.simple()方法的情况，但是要判断';'传入'semicolon'
                case INTCONST -> {
                    int index = i;
                    for (; i < sourceCode.length() && Character.isLetter(word[i]); i++) {}
                    String digit = sourceCode.substring(index, i);
                    tokens.add(Token.normal("IntConst", digit));    // 将(index, i)的内容即数字字段读下创建TOken
                    currentState = State.START;                                // 回到start状态
                }
                case ID -> {
                    int index = i;
                    for (; i < sourceCode.length() && Character.isLetter(word[i]); i++) {}

                    String key = sourceCode.substring(index, i);     // 将[a-zA-Z]+ 的字段(index,i)存下来

                    // 如果已经在符号表里面的，如int和return 就可以直接用simple方法构建
                    if (TokenKind.isAllowed(key)) {
                        tokens.add(Token.simple(key));
                    } else {
                        tokens.add(Token.normal("id", key));
                        if (!symbolTable.has(key)) {
                            symbolTable.add(key);
                        }
                    }
                    currentState = State.START;
                }
                case ERROR -> {
                    System.out.println("errorInput");
                    i++; // 跳过错误字符
                    currentState = State.START;
                }
            }
        }

        // 插入结束符
        tokens.add(Token.eof());
    }

    /**
     *
     * @param ch run()过程中每次读取的一个字符
     * 判断ch是否能够用 Token.simple()方法构建Token
     * @return bool值
     */
    // 判断是否为单字符标点
    private boolean isSinglePunctuation(char ch) {
        return ch == ',' || ch == ';' || ch == '=' || ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '(' || ch == ')';
    }

    // 定义状态
    private enum State {
        START,
        INTCONST,
        ID,
        ERROR
    }



    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
//        throw new NotImplementedException();
        return tokens;
    }


    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
