grammar T1;
startRule : TEST EOF;
TEST : 'TEST';
WS : [ \t\f;]+ -> skip;