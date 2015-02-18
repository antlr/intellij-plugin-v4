<fold text='...' expand='false'>/*
 * [The "BSD license"]
 *  Copyright (c) 2013 Terence Parr
 *  Copyright (c) 2013 Sam Harwell
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */</fold>

<fold text='...' expand='true'>/** A grammar for ANTLR v4 written in ANTLR v4.
*/</fold>
parser grammar ANTLRv4Parser;

options {<fold text='...' expand='true'>
	tokenVocab=ANTLRv4Lexer;
}</fold>

<fold text='//...' expand='true'>// The main entry point for parsing a v4 grammar.</fold>
grammarSpec<fold text=':...;' expand='true'>
	:	DOC_COMMENT?
		grammarType id SEMI
		prequelConstruct*
		rules
		modeSpec*
		EOF
	;</fold>

grammarType<fold text=':...;' expand='true'>
	:	(	LEXER GRAMMAR
		|	PARSER GRAMMAR
		|	GRAMMAR
		)
	;</fold>

<fold text='//...' expand='true'>// This is the list of all constructs that can be declared before
// the set of rules that compose the grammar, and is invoked 0..n
// times by the grammarPrequel rule.</fold>
prequelConstruct<fold text=':...;' expand='true'>
	:	optionsSpec
	|	delegateGrammars
	|	tokensSpec
	|	action
	;</fold>

<fold text='//...' expand='true'>// A list of options that affect analysis and/or code generation</fold>
optionsSpec<fold text=':...;' expand='true'>
	:	OPTIONS (option SEMI)* RBRACE
	;</fold>

option<fold text=':...;' expand='true'>
	:	id ASSIGN optionValue
	;</fold>

optionValue<fold text=':...;' expand='true'>
	:	id (DOT id)*
	|	STRING_LITERAL
	|	ACTION
	|	INT
	;</fold>

delegateGrammars<fold text=':...;' expand='true'>
	:	IMPORT delegateGrammar (COMMA delegateGrammar)* SEMI
	;</fold>

delegateGrammar<fold text=':...;' expand='true'>
	:	id ASSIGN id
	|	id
	;</fold>

tokensSpec<fold text=':...;' expand='true'>
	:	TOKENS id (COMMA id)* COMMA? RBRACE
	;</fold>

<fold text='...' expand='true'>/** Match stuff like @parser::members {int i;} */</fold>
action<fold text=':...;' expand='true'>
	:	AT (actionScopeName COLONCOLON)? id ACTION
	;</fold>

<fold text='...' expand='true'>/** Sometimes the scope names will collide with keywords; allow them as
 *  ids for action scopes.
 */</fold>
actionScopeName<fold text=':...;' expand='true'>
	:	id
	|	LEXER
	|	PARSER
	;</fold>

modeSpec<fold text=':...;' expand='true'>
	:	MODE id SEMI lexerRule*
	;</fold>

rules<fold text=':...;' expand='true'>
	:	ruleSpec*
	;</fold>

ruleSpec<fold text=':...;' expand='true'>
	:	parserRuleSpec
	|	lexerRule
	;</fold>

parserRuleSpec<fold text=':...;' expand='true'>
	:	DOC_COMMENT?
        ruleModifiers? RULE_REF ARG_ACTION?
        ruleReturns? throwsSpec? localsSpec?
		rulePrequel*
		COLON
            ruleBlock
		SEMI
		exceptionGroup
	;</fold>

exceptionGroup<fold text=':...;' expand='true'>
	:	exceptionHandler* finallyClause?
	;</fold>

exceptionHandler<fold text=':...;' expand='true'>
	:	CATCH ARG_ACTION ACTION
	;</fold>

finallyClause<fold text=':...;' expand='true'>
	:	FINALLY ACTION
	;</fold>

rulePrequel<fold text=':...;' expand='true'>
	:	optionsSpec
	|	ruleAction
	;</fold>

ruleReturns<fold text=':...;' expand='true'>
	:	RETURNS ARG_ACTION
	;</fold>

throwsSpec<fold text=':...;' expand='true'>
	:	THROWS id (COMMA id)*
	;</fold>

localsSpec<fold text=':...;' expand='true'>
	:	LOCALS ARG_ACTION
	;</fold>

<fold text='...' expand='true'>/** Match stuff like @init {int i;} */</fold>
ruleAction<fold text=':...;' expand='true'>
	:	AT id ACTION
	;</fold>

ruleModifiers<fold text=':...;' expand='true'>
	:	ruleModifier+
	;</fold>

<fold text='//...' expand='true'>// An individual access modifier for a rule. The 'fragment' modifier
// is an internal indication for lexer rules that they do not match
// from the input but are like subroutines for other lexer rules to
// reuse for certain lexical patterns. The other modifiers are passed
// to the code generation templates and may be ignored by the template
// if they are of no use in that language.</fold>
ruleModifier<fold text=':...;' expand='true'>
	:	PUBLIC
	|	PRIVATE
	|	PROTECTED
	|	FRAGMENT
	;</fold>

ruleBlock<fold text=':...;' expand='true'>
	:	ruleAltList
	;</fold>

ruleAltList<fold text=':...;' expand='true'>
	:	labeledAlt (OR labeledAlt)*
	;</fold>

labeledAlt<fold text=':...;' expand='true'>
	:	alternative (POUND id)?
	;</fold>

lexerRule<fold text=':...;' expand='true'>
	:	DOC_COMMENT? FRAGMENT?
		TOKEN_REF COLON lexerRuleBlock SEMI
	;</fold>

lexerRuleBlock<fold text=':...;' expand='true'>
	:	lexerAltList
	;</fold>

lexerAltList<fold text=':...;' expand='true'>
	:	lexerAlt (OR lexerAlt)*
	;</fold>

lexerAlt<fold text=':...;' expand='true'>
	:	lexerElements lexerCommands?
	|
	;</fold>

lexerElements<fold text=':...;' expand='true'>
	:	lexerElement+
	;</fold>

lexerElement<fold text=':...;' expand='true'>
	:	labeledLexerElement ebnfSuffix?
	|	lexerAtom ebnfSuffix?
	|	lexerBlock ebnfSuffix?
	|	ACTION QUESTION? <fold text='//...' expand='true'>// actions only allowed at end of outer alt actually,
                         // but preds can be anywhere</fold>
	;</fold>

labeledLexerElement<fold text=':...;' expand='true'>
	:	id (ASSIGN|PLUS_ASSIGN)
		(	lexerAtom
		|	block
		)
	;</fold>

lexerBlock<fold text=':...;' expand='true'>
	:	LPAREN lexerAltList RPAREN
	;</fold>

<fold text='//...' expand='true'>// E.g., channel(HIDDEN), skip, more, mode(INSIDE), push(INSIDE), pop</fold>
lexerCommands<fold text=':...;' expand='true'>
	:	RARROW lexerCommand (COMMA lexerCommand)*
	;</fold>

lexerCommand<fold text=':...;' expand='true'>
	:	lexerCommandName LPAREN lexerCommandExpr RPAREN
	|	lexerCommandName
	;</fold>

lexerCommandName<fold text=':...;' expand='true'>
	:	id
	|	MODE
	;</fold>

lexerCommandExpr<fold text=':...;' expand='true'>
	:	id
	|	INT
	;</fold>

altList<fold text=':...;' expand='true'>
	:	alternative (OR alternative)*
	;</fold>

alternative<fold text=':...;' expand='true'>
	:	elementOptions? element*
	;</fold>

element<fold text=':...;' expand='true'>
	:	labeledElement
		(	ebnfSuffix
		|
		)
	|	atom
		(	ebnfSuffix
		|
		)
	|	ebnf
	|	ACTION QUESTION? <fold text='//...' expand='true'>// SEMPRED is ACTION followed by QUESTION</fold>
	;</fold>

labeledElement<fold text=':...;' expand='true'>
	:	id (ASSIGN|PLUS_ASSIGN)
		(	atom
		|	block
		)
	;</fold>

ebnf<fold text=':...;' expand='true'>:	block blockSuffix?
	;</fold>

blockSuffix<fold text=':...;' expand='true'>
	:	ebnfSuffix <fold text='//...' expand='true'>// Standard EBNF</fold>
	;</fold>

ebnfSuffix<fold text=':...;' expand='true'>
	:	QUESTION QUESTION?
  	|	STAR QUESTION?
   	|	PLUS QUESTION?
	;</fold>

lexerAtom<fold text=':...;' expand='true'>
	:	range
	|	terminal
	|	RULE_REF
	|	notSet
	|	LEXER_CHAR_SET
	|	DOT elementOptions?
	;</fold>

atom<fold text=':...;' expand='true'>
	:	range <fold text='//...' expand='true'>// Range x..y - only valid in lexers</fold>
	|	terminal
	|	ruleref
	|	notSet
	|	DOT elementOptions?
	;</fold>

notSet<fold text=':...;' expand='true'>
	:	NOT setElement
	|	NOT blockSet
	;</fold>

blockSet<fold text=':...;' expand='true'>
	:	LPAREN setElement (OR setElement)* RPAREN
	;</fold>

setElement<fold text=':...;' expand='true'>
	:	TOKEN_REF elementOptions?
	|	STRING_LITERAL elementOptions?
	|	range
	|	LEXER_CHAR_SET
	;</fold>

block<fold text=':...;' expand='true'>
	:	LPAREN
		( optionsSpec? ruleAction* COLON )?
		altList
		RPAREN
	;</fold>

ruleref<fold text=':...;' expand='true'>
	:	RULE_REF ARG_ACTION? elementOptions?
	;</fold>

range<fold text=':...;' expand='true'>
	: STRING_LITERAL RANGE STRING_LITERAL
	;</fold>

terminal<fold text=':...;' expand='true'>
	:   TOKEN_REF elementOptions?
	|   STRING_LITERAL elementOptions?
	;</fold>

<fold text='//...' expand='true'>// Terminals may be adorned with certain options when
// reference in the grammar: TOK<,,,></fold>
elementOptions<fold text=':...;' expand='true'>
	:	LT elementOption (COMMA elementOption)* GT
	;</fold>

elementOption<fold text=':...;' expand='true'>
	:	<fold text='//...' expand='true'>// This format indicates the default node option</fold>
		id
	|	<fold text='//...' expand='true'>// This format indicates option assignment</fold>
		id ASSIGN (id | STRING_LITERAL)
	;</fold>

id<fold text=':...;' expand='true'>	:	RULE_REF
	|	TOKEN_REF
	;</fold>
