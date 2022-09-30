package com.github.leeonky.interpreter;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Notation {
    private final String label;

    private Notation(String label) {
        this.label = label;
    }

    public static Notation notation(String label) {
        return new Notation(label);
    }

    public String getLabel() {
        return label;
    }

    public int length() {
        return label.length();
    }

    private <P extends Procedure<?, ?, ?, ?>> Optional<Token> getToken(P procedure, Predicate<P> predicate) {
        return procedure.getSourceCode().popWord(this, () -> predicate.test(procedure));
    }

    private <P extends Procedure<?, ?, ?, ?>> Optional<Token> getToken(P procedure) {
        return getToken(procedure, p -> true);
    }

    public <N extends Node<?, N>, P extends Procedure<?, N, ?, ?>> NodeParser<N, P> node(Function<String, N> factory) {
        return procedure -> getToken(procedure).map(token ->
                factory.apply(token.getContent()).setPositionBegin(token.getPosition()));
    }

    public <N extends Node<?, N>, P extends Procedure<?, N, ?, ?>> NodeParser<N, P> wordNode(Function<String, N> factory,
                                                                                             Set<String> delimiter) {
        return procedure -> procedure.getSourceCode().tryFetch(() -> getToken(procedure).map(token ->
                notAWord(delimiter, procedure) ? null :
                        factory.apply(token.getContent()).setPositionBegin(token.getPosition())));
    }

    private <P extends Procedure<?, ?, ?, ?>> boolean notAWord(Set<String> delimiter, P procedure) {
        return procedure.getSourceCode().hasCode()
               && delimiter.stream().noneMatch(s -> procedure.getSourceCode().startsWith(s));
    }

    public <N extends Node<?, N>, O extends Operator<?, N, O>, P extends Procedure<?, N, ?, O>> OperatorParser<N, O, P>
    operator(Supplier<O> factory, Predicate<P> predicate) {
        return procedure -> getToken(procedure, predicate).map(token -> factory.get().setPosition(token.getPosition()));
    }

    public <N extends Node<?, N>, O extends Operator<?, N, O>, P extends Procedure<?, N, ?, O>> OperatorParser<N, O, P>
    operator(Supplier<O> factory) {
        return operator(factory, procedure -> true);
    }

    public <N extends Node<?, N>, O extends Operator<?, N, O>, P extends Procedure<?, N, ?, O>> OperatorParser<N, O, P>
    keywordOperator(Supplier<O> factory, Set<String> Delimiter) {
        return procedure -> procedure.getSourceCode().tryFetch(() -> operator(factory, p -> true)
                .parse(procedure).map(operator -> notAWord(Delimiter, procedure) ? null : operator));
    }

    public <N extends Node<?, N>, P extends Procedure<?, N, ?, ?>> NodeParser<N, P> with(
            NodeParser.Mandatory<N, P> mandatory) {
        return procedure -> getToken(procedure).map(t -> mandatory.parse(procedure).setPositionBegin(t.getPosition()));
    }

    public <N extends Node<?, N>, P extends Procedure<?, N, ?, ?>, PA extends Parser<P, PA, MA, T>,
            MA extends Parser.Mandatory<P, PA, MA, T>, T> PA before(PA parser) {
        return parser.castParser(procedure -> procedure.getSourceCode().tryFetch(() -> getToken(procedure)
                .flatMap(t -> parser.parse(procedure))));
    }

    public <N extends Node<?, N>, P extends Procedure<?, N, ?, ?>, PA extends Parser<P, PA, MA, T>,
            MA extends Parser.Mandatory<P, PA, MA, T>, T> PA before(MA ma) {
        return ma.castParser(procedure -> getToken(procedure).map(t -> ma.parse(procedure)));
    }

    public <N extends Node<?, N>, P extends Procedure<?, N, ?, ?>> ClauseParser<N, P> clause(
            BiFunction<Token, N, N> nodeFactory) {
        return procedure -> getToken(procedure).map(token -> input ->
                nodeFactory.apply(token, input).setPositionBegin(token.getPosition()));
    }
}
