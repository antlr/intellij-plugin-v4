parser grammar FooParser2;

options {
    tokenVocab='FooLexer';
}

myrule: TOKEN1 Fragment1 MYHIDDEN STRING;