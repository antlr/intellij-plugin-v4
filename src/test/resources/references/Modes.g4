lexer grammar Modes;

tokens { T1 }
channels { C1 }

TOKEN1: 'token1' -> pushMode(MY_MODE);

mode MY_MODE;

TOKEN2: 'token2' -> type(TOKEN1);

mode MY_OTHER_MODE;

TOKEN3: 'token3' -> type(TOKEN2), type(T1), channel(C1), pushMode(MY_MODE);