grammar T2;
startRule : TEST2 EOF;
TEST2 : 'TEST2';
WS : [ \t\f;]+ -> skip;