package com.github.leeonky.interpreter;

import static com.github.leeonky.interpreter.Notation.notation;
import static java.util.Arrays.asList;

public class BaseTest {
    public static SourceCode createSourceCode(String code) {
        return SourceCode.createSourceCode(code, asList(notation("#"), notation("//")));
    }

    protected TestProcedure givenProcedureWithCode(String s) {
        return new TestProcedure(createSourceCode(s));
    }

    protected TestProcedure givenProcedureWithBlankCode(String s) {
        SourceCode sourceCode = createSourceCodeWithBlank(s);
        return new TestProcedure(sourceCode);
    }

    public SourceCode createSourceCodeWithBlank(String s) {
        SourceCode sourceCode = createSourceCode("blabla" + s);
        sourceCode.popWord(Notation.notation("blabla"));
        return sourceCode;
    }
}
