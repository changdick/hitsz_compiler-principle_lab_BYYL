package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Action;
import cn.edu.hitsz.compiler.parser.table.LRTable;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

//TODO: 实验二: 实现 LR 语法分析驱动程序

/**
 * LR 语法分析驱动程序
 * <br>
 * 该程序接受词法单元串与 LR 分析表 (action 和 goto 表), 按表对词法单元流进行分析, 执行对应动作, 并在执行动作时通知各注册的观察者.
 * <br>
 * 你应当按照被挖空的方法的文档实现对应方法, 你可以随意为该类添加你需要的私有成员对象, 但不应该再为此类添加公有接口, 也不应该改动未被挖空的方法,
 * 除非你已经同助教充分沟通, 并能证明你的修改的合理性, 且令助教确定可能被改动的评测方法. 随意修改该类的其它部分有可能导致自动评测出错而被扣分.
 */
public class SyntaxAnalyzer {
    private final SymbolTable symbolTable;
    private final List<ActionObserver> observers = new ArrayList<>();

    // 输入串的一个缓冲区，读入后直接按顺序加进来，并补一个结束符。token类
//   分析的时候用的是符号栈，既有非终结符也有终结符，入符号栈还要做一个转换
    private final Deque<Token> tokenQueue = new ArrayDeque<>();
    private final Deque<Status> statusStack = new ArrayDeque<>(); // 状态栈 ， 初始化的时候要补一个初始状态
    private final Deque<VTSymbol> SymbolStack = new ArrayDeque<>();   // 符号栈
    private LRTable lrTable;
    public SyntaxAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 注册新的观察者
     *
     * @param observer 观察者
     */
    public void registerObserver(ActionObserver observer) {
        observers.add(observer);
        observer.setSymbolTable(symbolTable);
    }

    /**
     * 在执行 shift 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param currentToken  当前词法单元
     */
    public void callWhenInShift(Status currentStatus, Token currentToken) {
        for (final var listener : observers) {
            listener.whenShift(currentStatus, currentToken);
        }
    }

    /**
     * 在执行 reduce 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param production    待规约的产生式
     */
    public void callWhenInReduce(Status currentStatus, Production production) {
        for (final var listener : observers) {
            listener.whenReduce(currentStatus, production);
        }
    }

    /**
     * 在执行 accept 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     */
    public void callWhenInAccept(Status currentStatus) {
        for (final var listener : observers) {
            listener.whenAccept(currentStatus);
        }
    }

    public void loadTokens(Iterable<Token> tokens) {
        // TODO: 加载词法单元
        // 你可以自行选择要如何存储词法单元, 譬如使用迭代器, 或是栈, 或是干脆使用一个 list 全存起来
        // 需要注意的是, 在实现驱动程序的过程中, 你会需要面对只读取一个 token 而不能消耗它的情况,
        // 在自行设计的时候请加以考虑此种情况
//        throw new NotImplementedException();
        // 直接把输入的token流依次入队列
        for (Token token : tokens) {
            tokenQueue.add(token);
        }
        // 添加 EOF token
        tokenQueue.add(Token.eof());
    }

    public void loadLRTable(LRTable table) {
        // TODO: 加载 LR 分析表
        // 你可以自行选择要如何使用该表格:
        // 是直接对 LRTable 调用 getAction/getGoto, 抑或是直接将 initStatus 存起来使用
//        throw new NotImplementedException();
        // 初始化 LR 分析表和状态栈
        this.lrTable = table;
        statusStack.push(lrTable.getInit());  // 初始状态
    }

    // 这是个驱动程序，就是根据已经构造好的分析表，根据维护的栈顶跑起来。不包括记录规约使用的语法规则，以及语义分析之类的。
    // 这个程序只负责跑（查表、维护栈），具体的功能过程是作为观察者，在对应环节调用了通知观察者的方法，由观察者具体实现。
    public void run() {
        // TODO: 实现驱动程序
        // 你需要根据上面的输入来实现 LR 语法分析的驱动程序
        // 请分别在遇到 Shift, Reduce, Accept 的时候调用上面的 callWhenInShift, callWhenInReduce, callWhenInAccept
        // 否则用于为实验二打分的产生式输出可能不会正常工作
//        throw new NotImplementedException();
        // 语法分析的主循环
        // 主要是每次从tokenQueue先看一个token，从statusStack查栈顶符号，用lrTable提供的方法直接查动作。
        // 查到了动作后，每个对象做什么操作需要实现。

        // 调用callWhenInShift, callWhenInReduce, callWhenInAccept 是后面语义分析用来后面生成输出用的，使用了观察者模式。

        while (!tokenQueue.isEmpty()) {
            // peek()取头部，不移除  poll移除头部返回  add加尾部
            Status currentStatus = statusStack.peek();  // 查看当前状态
            Action action = lrTable.getAction(statusStack.peek(), tokenQueue.peek());  // 根据状态和当前 token 获取 action

            switch (action.getKind()) {
                case Shift:
                    handleShift(action);
                    break;
                case Reduce:
                    handleReduce(action);
                    break;
                case Accept:
                    handleAccept(currentStatus);
                    return;  // 分析结束
                default:
                    throw new IllegalStateException("Unexpected action kind: " + action.getKind());
            }
        }
    }
    private void handleShift(Action action) {
        // 执行 Shift 动作：状态入栈。token 出队，并压入符号栈
//
        callWhenInShift(statusStack.peek(), tokenQueue.peek()); // 看上去好像语义分析慢一步，要等符号表更新


        Status nextState = action.getStatus(); // action.getStatus只有当action是shift类型才能调用，action已经存储好了要移去的状态，调用即可获得
        statusStack.push(nextState);   // 将新状态压入状态栈
        SymbolStack.push(new VTSymbol(tokenQueue.poll()));   // 读入的是token，因为语法分析只需要token，但是构建符号放符号栈还可以同时做语义分析
        // 调用poll方法出队，在返回的同时，将token出队


    }
    private void handleReduce(Action action) {
        // 执行 Reduce 动作：获取产生式，执行规约。规约后应该将状态栈出栈，再用栈顶查goto表，把新状态压入栈中。 把符号栈中右部的也出栈，把左部压入符号栈


        Production production = action.getProduction();  // Action对象已经存储好了产生式，调用get方法获取
        callWhenInReduce(statusStack.peek(), production);

        //  注意Production是记录 record， 访问记录的字段是这么访问的
       /*访问字段的方式确实看起来像调用方法。
       这是因为记录类型会自动生成访问器方法（getter），用于访问记录中的各个字段。当你使用recordObject.fieldName()的形式时，实际上是在调用自动生成的访问器方法来获取该字段的值。8*/
        for (int i = 0; i < production.body().size(); i++) {
            statusStack.pop();  // 弹出与产生式右部相应数量的状态
            SymbolStack.pop();  // 弹出符号
        }
        // 将产生式左部符号入栈
        SymbolStack.push(new VTSymbol(production.head()));
        // 根据此时的两个栈顶查goto表，获得新状态入栈
        statusStack.push(lrTable.getGoto(statusStack.peek(), production.head()));
    }

    private void handleAccept(Status currentStatus) {
        // 执行 Accept 动作，通知所有观察者
        callWhenInAccept(currentStatus);
        tokenQueue.poll();  // 移除 EOF token
    }

}
