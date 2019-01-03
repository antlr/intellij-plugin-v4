lexer grammar FooLexer;

tokens { STRING }
channels { MYHIDDEN }

TOKEN1: 'TOKEN1';

fragment Fragment1
    : Fragment2
    | TOKEN1
    ;

fragment Fragment2
    : 'FOO'
    | 'BAR'
    ;

SINGLE : '\'' .*? '\'' -> type(STRING), channel(MYHIDDEN);

