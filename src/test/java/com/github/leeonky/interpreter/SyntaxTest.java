package com.github.leeonky.interpreter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.github.leeonky.interpreter.Rules.*;
import static com.github.leeonky.interpreter.Syntax.many;
import static com.github.leeonky.interpreter.Syntax.single;
import static java.util.Collections.emptyMap;
import static java.util.Optional.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SyntaxTest extends BaseTest {

    @Nested
    class SingleNodeParser {

        @Test
        void should_be_empty_node_parser_when_single_empty_node_parser_and_should_not_invoke_any_close_method() {
            TestProcedure testProcedure = givenProcedureWithCode("");

            NodeParser<TestNode, TestProcedure> nodeParser = procedure -> {
                assertThat(procedure).isSameAs(testProcedure);
                return empty();
            };

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser<TestNode,
                    TestProcedure>, TestNode> single = spy(single(nodeParser));

            assertThat(single.as().parse(testProcedure)).isEmpty();
            verify(single, never()).isClose(any());
            verify(single, never()).close(any());
        }

        @Test
        void should_be_present_node_parser_when_single_present_node_parser_and_should_invoke_two_close_method() {
            TestProcedure testProcedure = givenProcedureWithCode("");
            TestNode node = new TestNode();
            NodeParser<TestNode, TestProcedure> nodeParser = procedure -> {
                assertThat(procedure).isSameAs(testProcedure);
                return of(node);
            };

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser<TestNode,
                    TestProcedure>, TestNode> single = spy(single(nodeParser));

            assertThat(single.as().parse(testProcedure).get()).isSameAs(node);

            verify(single).isClose(testProcedure);
            verify(single).close(testProcedure);
        }
    }

    @Nested
    class SingleMA {

        @Test
        void should_be_mandatory_node_parser_when_single_mandatory_node_parser_and_should_invoke_two_close_method() {
            TestProcedure testProcedure = givenProcedureWithCode("");
            TestNode node = new TestNode();
            NodeParser.Mandatory<TestNode, TestProcedure> nodeParser = procedure -> {
                assertThat(procedure).isSameAs(testProcedure);
                return node;
            };

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode> single = spy(single(nodeParser));

            assertThat(single.as().parse(testProcedure)).isSameAs(node);

            verify(single).isClose(testProcedure);
            verify(single).close(testProcedure);
        }
    }

    @Nested
    class ManyOP {

        int nodeCode;

        @Test
        void return_empty_when_op_is_empty() {
            TestProcedure testProcedure = givenProcedureWithCode("");
            NodeParser<TestNode, TestProcedure> nodeParser = procedure -> {
                assertThat(procedure).isSameAs(testProcedure);
                return empty();
            };

            TestNode node = new TestNode();

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<TestNode,
                    TestProcedure>, List<TestNode>> many = spy(many(nodeParser));

            assertThat(many.as(list -> {
                assertThat(list).isEmpty();
                return node;
            }).parse(testProcedure)).isSameAs(node);
            verify(many).close(testProcedure);
        }

        @Test
        void return_empty_when_start_with_close() {
            TestProcedure testProcedure = givenProcedureWithCode("");
            NodeParser<TestNode, TestProcedure> nodeParser = procedure -> {
                assertThat(procedure).isSameAs(testProcedure);
                return of(new TestNode());
            };

            TestNode node = new TestNode();

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<TestNode,
                    TestProcedure>, List<TestNode>> many = spy(many(nodeParser));

            when(many.isClose(testProcedure)).thenReturn(true);

            assertThat(many.as(list -> {
                assertThat(list).isEmpty();
                return node;
            }).parse(testProcedure)).isSameAs(node);
        }

        @Test
        void return_one_node_when_no_split() {
            TestNode node = new TestNode();
            TestProcedure testProcedure = givenProcedureWithCode("");
            NodeParser<TestNode, TestProcedure> nodeParser = procedure -> {
                assertThat(procedure).isSameAs(testProcedure);
                return of(node);
            };

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<TestNode,
                    TestProcedure>, List<TestNode>> many = spy(many(nodeParser));

            when(many.isSplitter(testProcedure)).thenReturn(false);

            TestNode result = new TestNode();

            assertThat(many.as(list -> {
                assertThat(list).containsExactly(node);
                return result;
            }).parse(testProcedure)).isSameAs(result);
        }

        @Test
        void return_with_list_with_procedure_index() {
            nodeCode = 2;

            TestProcedure testProcedure = givenProcedureWithCode("");

            NodeParser<TestNode, TestProcedure> nodeParser = procedure -> {
                assertThat(procedure).isSameAs(testProcedure);
                if (nodeCode-- > 0)
                    return ofNullable(new TestNode(procedure.getColumn()));
                else
                    return empty();
            };

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<TestNode,
                    TestProcedure>, List<TestNode>> many = spy(many(nodeParser));

            TestNode node = new TestNode();
            assertThat(many.as(list -> {
                assertThat(list).hasSize(2).extracting("content").containsExactly(0, 1);
                return node;
            }).parse(testProcedure)).isSameAs(node);

            verify(many, times(3)).isClose(testProcedure);
            verify(many).close(testProcedure);
        }
    }

    @Nested
    class ManyMA {

        int nodeCode;

        @Test
        void return_empty_when_start_with_close() {
            TestProcedure testProcedure = givenProcedureWithCode("");
            NodeParser.Mandatory<TestNode, TestProcedure> mandatory = procedure -> {
                assertThat(procedure).isSameAs(testProcedure);
                return new TestNode();
            };

            TestNode node = new TestNode();

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<TestNode,
                    TestProcedure>, List<TestNode>> many = spy(many(mandatory));

            when(many.isClose(testProcedure)).thenReturn(true);

            assertThat(many.as(list -> {
                assertThat(list).isEmpty();
                return node;
            }).parse(testProcedure)).isSameAs(node);
        }

        @Test
        void return_one_node_when_no_split() {
            TestNode node = new TestNode();
            TestProcedure testProcedure = givenProcedureWithCode("");
            NodeParser.Mandatory<TestNode, TestProcedure> mandatory = procedure -> {
                assertThat(procedure).isSameAs(testProcedure);
                return node;
            };

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<TestNode,
                    TestProcedure>, List<TestNode>> many = spy(many(mandatory));

            when(many.isSplitter(testProcedure)).thenReturn(false);

            TestNode result = new TestNode();

            assertThat(many.as(list -> {
                assertThat(list).containsExactly(node);
                return result;
            }).parse(testProcedure)).isSameAs(result);
        }

        @Test
        void return_with_list_with_procedure_index() {
            nodeCode = 2;

            TestProcedure testProcedure = givenProcedureWithCode("");

            NodeParser.Mandatory<TestNode, TestProcedure> mandatory = procedure -> {
                assertThat(procedure).isSameAs(testProcedure);
                if (nodeCode-- > 0)
                    return new TestNode(procedure.getColumn());
                return null;
            };

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<TestNode,
                    TestProcedure>, List<TestNode>> many = spy(many(mandatory));

            when(many.isSplitter(testProcedure)).thenAnswer(a -> nodeCode > 0);

            TestNode node = new TestNode();
            assertThat(many.as(list -> {
                assertThat(list).hasSize(2).extracting("content").containsExactly(0, 1);
                return node;
            }).parse(testProcedure)).isSameAs(node);

            verify(many, times(3)).isClose(testProcedure);
            verify(many).close(testProcedure);
        }
    }

    @Nested
    class EndWithNotation {

        @Test
        void raise_error_when_not_end_with() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWith(nt("a")));

            assertThat(assertThrows(SyntaxException.class, () -> syntax.close(givenProcedureWithCode(""))))
                    .hasMessageContaining("Should end with `a`");
        }

        @Test
        void move_position_when_close() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWith(nt("a")));

            TestProcedure testProcedure = givenProcedureWithCode("a");
            syntax.close(testProcedure);

            assertThat(testProcedure.getSourceCode().hasCode()).isFalse();
        }

        @Test
        void return_close_when_no_code() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWith(nt(" a")));

            assertThat(syntax.isClose(givenProcedureWithCode(" "))).isTrue();
        }

        @Test
        void return_not_close_when_not_close() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWith(nt("a")));

            assertThat(syntax.isClose(givenProcedureWithCode("b"))).isFalse();
        }

        @Test
        void return_be_closed_when_close() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWith(nt("a")));

            assertThat(syntax.isClose(givenProcedureWithCode(" a"))).isTrue();
        }
    }

    @Nested
    class EndBeforeNotation {

        @Test
        void raise_error_when_not_end_with() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endBefore(nt("a")));

            assertThat(assertThrows(SyntaxException.class, () -> syntax.close(givenProcedureWithCode(""))))
                    .hasMessageContaining("Should end with `a`");
        }

        @Test
        void should_not_move_position_when_close() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endBefore(nt("a")));

            TestProcedure testProcedure = givenProcedureWithCode("a");
            syntax.isClose(testProcedure);
            syntax.close(testProcedure);

            assertThat(testProcedure.getSourceCode().startsWith("a")).isTrue();
        }

        @Test
        void return_not_close_when_no_code() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endBefore(nt(" a")));

            assertThat(syntax.isClose(givenProcedureWithCode(" "))).isFalse();
        }

        @Test
        void return_not_close_when_not_before_notation() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endBefore(nt("a")));

            assertThat(syntax.isClose(givenProcedureWithCode("b"))).isFalse();
        }

        @Test
        void return_be_closed_when_before_notation() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endBefore(nt("a")));

            assertThat(syntax.isClose(givenProcedureWithCode(" a"))).isTrue();
        }

        @Test
        void return_be_closed_when_before_one_of_notations() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endBefore(nt("a"), nt("b")));

            assertThat(syntax.isClose(givenProcedureWithCode(" b"))).isTrue();
        }
    }

    @Nested
    class EndBeforeString {

        @Test
        void raise_error_when_not_end_with() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endBefore("a"));

            assertThat(assertThrows(SyntaxException.class, () -> syntax.close(givenProcedureWithCode(""))))
                    .hasMessageContaining("Should end with `a`");
        }

        @Test
        void different_quote_char_when_target_contains_special_char() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endBefore("`"));

            assertThat(assertThrows(SyntaxException.class, () -> syntax.close(givenProcedureWithCode(""))))
                    .hasMessageContaining("Should end with '`'");
        }

        @Test
        void should_not_move_position_when_close() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endBefore("a"));

            TestProcedure testProcedure = givenProcedureWithCode("a");
            syntax.isClose(testProcedure);
            syntax.close(testProcedure);

            assertThat(testProcedure.getSourceCode().startsWith("a")).isTrue();
        }

        @Test
        void return_close_when_no_code() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endBefore("a"));

            assertThat(syntax.isClose(givenProcedureWithCode(""))).isTrue();
        }

        @Test
        void return_not_close_when_not_before_target() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endBefore("a"));

            assertThat(syntax.isClose(givenProcedureWithCode("b"))).isFalse();
        }

        @Test
        void return_not_close_when_has_space_before_target() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endBefore("a"));

            TestProcedure procedure = givenProcedureWithCode("x a");
            procedure.getSourceCode().popChar(emptyMap());

            assertThat(syntax.isClose(procedure)).isFalse();
        }

        @Test
        void return_be_closed_when_before_notation() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endBefore("a"));

            assertThat(syntax.isClose(givenProcedureWithCode("a"))).isTrue();
        }
    }

    @Nested
    class EndWithString {

        @Test
        void raise_error_when_not_end_with() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWith("a"));

            assertThat(assertThrows(SyntaxException.class, () -> syntax.close(givenProcedureWithCode(""))))
                    .hasMessageContaining("");
        }

        @Test
        void move_position_when_close() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWith("a"));

            TestProcedure testProcedure = givenProcedureWithCode("a");
            syntax.close(testProcedure);

            assertThat(testProcedure.getSourceCode().hasCode()).isFalse();
        }

        @Test
        void return_false_when_no_code() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWith("a"));

            assertThat(syntax.isClose(givenProcedureWithCode(""))).isTrue();
        }

        @Test
        void return_true_when_has_blank_code() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWith("a"));

            assertThat(syntax.isClose(givenProcedureWithBlankCode(" "))).isFalse();
        }

        @Test
        void return_false_when_not_close() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWith("a"));

            assertThat(syntax.isClose(givenProcedureWithCode("b"))).isFalse();
        }

        @Test
        void return_false_when_blank_before_close() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWith("a"));

            assertThat(syntax.isClose(givenProcedureWithBlankCode(" a"))).isFalse();
        }

        @Test
        void return_true_when_close() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWith("a"));

            assertThat(syntax.isClose(givenProcedureWithCode("a"))).isTrue();
        }
    }

    @Nested
    class EndWithLine {

        @Test
        void raise_error_when_not_new_line() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWithLine());

            TestProcedure testProcedure = givenProcedureWithCode("a");
            assertThat(syntax.isClose(testProcedure)).isFalse();
            assertThat(assertThrows(SyntaxException.class, () -> syntax.close(testProcedure)))
                    .hasMessageContaining("");
        }

        @Test
        void return_true_when_new_line_and_pop_up_new_line() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWithLine());

            TestProcedure testProcedure = givenProcedureWithCode("\n");
            assertThat(syntax.isClose(testProcedure)).isTrue();
            assertThat(testProcedure.getSourceCode().hasCode()).isFalse();
            syntax.close(testProcedure);
        }

        @Test
        void return_true_when_no_code() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWithLine());

            TestProcedure testProcedure = givenProcedureWithCode("");
            assertThat(syntax.isClose(testProcedure)).isTrue();
            syntax.close(testProcedure);
        }
    }

    @Nested
    class EndOfRow {
        NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

        Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                TestProcedure>, NodeParser.Mandatory<TestNode,
                TestProcedure>, TestNode, NodeParser.Mandatory<
                TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endOfRow(nt("|")));

        @Test
        void should_raise_error_when_not_really_close() {
            TestProcedure testProcedure = givenProcedureWithCode("\n");

            assertThatThrownBy(() -> syntax.close(testProcedure));
        }

        @Test
        void should_be_closed_when_end_of_line_and_can_move_code_to_new_line() {
            TestProcedure testProcedure = givenProcedureWithCode("a \nnew line");
            testProcedure.getSourceCode().popChar(new HashMap<>());

            assertThat(syntax.isClose(testProcedure)).isTrue();
            syntax.close(testProcedure);

            assertThat(testProcedure.getSourceCode().popChar(new HashMap<>())).isEqualTo('n');
        }

        @Test
        void should_be_closed_when_end_of_code_and_can_be_invoke_close() {
            TestProcedure testProcedure = givenProcedureWithCode("");

            assertThat(syntax.isClose(testProcedure)).isTrue();
            syntax.close(testProcedure);
        }

        @Test
        void should_be_closed_when_has_new_line_before_splitter_and_should_not_move_code() {
            TestProcedure testProcedure = givenProcedureWithCode("a \n|");

            assertThat(syntax.isClose(testProcedure)).isTrue();
            syntax.close(testProcedure);
            assertThat(testProcedure.getSourceCode().popChar(new HashMap<>())).isEqualTo('a');
        }

        @Test
        void should_be_closed_when_has_mac_new_line_before_splitter_and_should_not_move_code() {
            TestProcedure testProcedure = givenProcedureWithCode("a \r|");

            assertThat(syntax.isClose(testProcedure)).isTrue();
            syntax.close(testProcedure);
            assertThat(testProcedure.getSourceCode().popChar(new HashMap<>())).isEqualTo('a');
        }
    }

    @Nested
    class EndWithOptionalLine {

        @Test
        void return_false_when_not_new_line() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWithOptionalLine());

            TestProcedure testProcedure = givenProcedureWithCode("a");
            assertThat(syntax.isClose(testProcedure)).isFalse();
            syntax.close(testProcedure);
        }

        @Test
        void return_true_when_new_line_and_pop_up_new_line() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWithOptionalLine());

            TestProcedure testProcedure = givenProcedureWithCode("\n");
            assertThat(syntax.isClose(testProcedure)).isTrue();
            assertThat(testProcedure.getSourceCode().hasCode()).isFalse();
            syntax.close(testProcedure);
        }

        @Test
        void return_true_when_no_code() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(endWithOptionalLine());

            TestProcedure testProcedure = givenProcedureWithCode("");
            assertThat(syntax.isClose(testProcedure)).isTrue();
            syntax.close(testProcedure);
        }
    }

    @Nested
    class SplitBy {

        @Test
        void return_false_when_not_match() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(splitBy(nt("a")));

            TestProcedure testProcedure = givenProcedureWithCode("x");
            assertThat(syntax.isSplitter(testProcedure)).isFalse();
            assertThat(testProcedure.getSourceCode().popChar(emptyMap())).isEqualTo('x');
        }

        @Test
        void return_true_when_matches() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);
            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(splitBy(nt("a")));
            TestProcedure testProcedure = givenProcedureWithCode(" a");

            assertThat(syntax.isSplitter(testProcedure)).isTrue();
            assertThat(testProcedure.getSourceCode().hasCode()).isFalse();
        }
    }

    @Nested
    class OptionalSplitBy {

        @Test
        void return_true_when_not_match() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(optionalSplitBy(nt("a")));

            TestProcedure testProcedure = givenProcedureWithCode("x");

            assertThat(syntax.isSplitter(testProcedure)).isTrue();
            assertThat(testProcedure.getSourceCode().popChar(emptyMap())).isEqualTo('x');
        }

        @Test
        void return_true_when_matches() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(optionalSplitBy(nt("a")));

            TestProcedure testProcedure = givenProcedureWithCode(" a");
            assertThat(syntax.isSplitter(testProcedure)).isTrue();
            assertThat(testProcedure.getSourceCode().hasCode()).isFalse();
        }
    }

    @Nested
    class MandatorySplitBy {

        @Test
        void return_true_when_matches() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(mandatorySplitBy(nt("a")));

            TestProcedure testProcedure = givenProcedureWithCode(" a");
            assertThat(syntax.isSplitter(testProcedure)).isTrue();
            assertThat(testProcedure.getSourceCode().hasCode()).isFalse();
        }

        @Test
        void raise_error_when_no_splitter() {
            NodeParser<TestNode, TestProcedure> nodeParser = mock(NodeParser.class);

            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser.Mandatory<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(mandatorySplitBy(nt("a")));

            assertThat(assertThrows(SyntaxException.class, () -> syntax.isSplitter(givenProcedureWithCode("x"))))
                    .hasMessageContaining("");
        }
    }

    @Nested
    class AtLeast {

        @Test
        void return_when_enough() {
            NodeParser<TestNode, TestProcedure> nodeParser = nt("a").node(TestNode::new);
            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(Rules.atLeast(2));

            TestProcedure testProcedure = givenProcedureWithCode("a a");
            Optional<TestNode> parse = syntax.as(TestNode::new).parse(testProcedure);
            assertThat((List<TestNode>) parse.get().getContent()).extracting(TestNode::getContent).containsExactly("a", "a");
            assertThat(testProcedure.getSourceCode().hasCode()).isFalse();
        }

        @Test
        void return_empty_when_not_enough() {
            NodeParser<TestNode, TestProcedure> nodeParser = nt("a").node(TestNode::new);
            Syntax<TestNode, TestProcedure, NodeParser<TestNode,
                    TestProcedure>, NodeParser.Mandatory<TestNode,
                    TestProcedure>, TestNode, NodeParser<
                    TestNode, TestProcedure>, List<TestNode>> syntax = many(nodeParser).and(Rules.atLeast(2));

            TestProcedure testProcedure = givenProcedureWithCode("a");
            assertThat(syntax.as(TestNode::new).parse(testProcedure)).isEmpty();
            assertThat(testProcedure.getSourceCode().nextPosition()).isEqualTo(0);
        }
    }

    @Nested
    class EnabledBefore {

        @Test
        void return_node_when_end_with_target_notation() {
            NodeParser<TestNode, TestProcedure> nodeParser = nt("a").node(TestNode::new);
            TestProcedure procedure = givenProcedureWithCode("a|");

            Optional<TestNode> testNode = single(nodeParser).and(enabledBefore(nt("|"))).as(Function.identity()).parse(procedure);

            assertThat(testNode.get().getContent()).isEqualTo("a");

            assertThat(procedure.getSourceCode().popChar(emptyMap())).isEqualTo('|');
        }

        @Test
        void return_empty_when_not_end_with_target_notation_and_source_move_back() {
            NodeParser<TestNode, TestProcedure> nodeParser = nt("a").node(TestNode::new);
            TestProcedure procedure = givenProcedureWithCode("a+");

            Optional<TestNode> testNode = single(nodeParser).and(enabledBefore(nt("|"))).as(Function.identity()).parse(procedure);

            assertThat(testNode).isEmpty();

            assertThat(procedure.getSourceCode().popChar(emptyMap())).isEqualTo('a');
        }

        @Test
        void return_empty_when_node_parser_is_empty() {
            TestProcedure testProcedure = givenProcedureWithCode("");
            NodeParser<TestNode, TestProcedure> nodeParser = procedure -> {
                assertThat(procedure).isSameAs(testProcedure);
                return empty();
            };

            Optional<TestNode> testNode = single(nodeParser).and(enabledBefore(nt("|"))).as(Function.identity()).parse(testProcedure);

            assertThat(testNode).isEmpty();
        }
    }
}