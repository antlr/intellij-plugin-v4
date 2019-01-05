parser grammar FooParser;

options {
    tokenVocab=FooLexer;
}

myrule: TOKEN1 Fragment1 MYHIDDEN STRING;