/*
 * [The "BSD license"]
 *  Copyright (c) 2012 Terence Parr
 *  Copyright (c) 2012 Sam Harwell
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
 */

/** A grammar for ANTLR v4 tokens suitable for use in an IDE for handling
    erroneous input well.
 */
lexer grammar GrammarLexer;

tokens {
	TOKEN_REF,
	RULE_REF,
	LEXER_CHAR_SET
}

@members {

	private int _ruleType;

	protected void handleBeginArgAction() {
		if (inLexerRule()) {
			pushMode(LexerCharSet);
			more();
		} else {
			pushMode(ArgAction);
		}
	}

	@Override
	public Token emit() {
		if (_type == ID) {
			String firstChar = _input.getText(Interval.of(_tokenStartCharIndex, _tokenStartCharIndex));
			if (Character.isUpperCase(firstChar.charAt(0))) {
				_type = TOKEN_REF;
			} else {
				_type = RULE_REF;
			}

			if (_ruleType == Token.INVALID_TYPE) {
				_ruleType = _type;
			}
		} else if (_type == SEMI) {
			_ruleType = Token.INVALID_TYPE;
		}

		return super.emit();
	}

	private boolean inLexerRule() {
		return _ruleType == TOKEN_REF;
	}

}

// +=====================+
// | Lexer specification |
// +=====================+

// --------
// Comments
//
// ANTLR comments can be multi or single line and we don't care
// which particularly. However we also accept Javadoc style comments
// of the form: /** ... */ and we do take care to distinguish those
// from ordinary multi-line comments

DOC_COMMENT
	:	'/**' .*? '*/'
	;

BLOCK_COMMENT
	:	'/*' .*? '*/' -> channel(HIDDEN)
	;

LINE_COMMENT
	:	'//' ~[\r\n]* -> channel(HIDDEN)
	;

DOUBLE_QUOTE_STRING_LITERAL
	:	'"' ('\\' . | ~'"' )*? '"'
	;

// --------------
// Argument specs
//
// Certain argument lists, such as those specifying call parameters
// to a rule invocation, or input parameters to a rule specification
// are contained within square brackets.
//
BEGIN_ARG_ACTION
	:	'[' {handleBeginArgAction();}
	;

// -------
// Actions
//

BEGIN_ACTION
	:	'{' -> pushMode(Action)
	;

// Keywords
// --------
// keywords used to specify ANTLR v4 grammars. Keywords may not be used as
// labels for rules or in any other context where they would be ambiguous
// with the keyword vs some other identifier
// OPTIONS and TOKENS must also consume the opening brace that captures
// their option block, as this is the easiest way to parse it separate
// to an ACTION block, despite it using the same {} delimiters.
//
OPTIONS      : 'options' WSNLCHAR* '{'  ;
TOKENS       : 'tokens'  WSNLCHAR* '{'  ;

IMPORT       : 'import'               ;
FRAGMENT     : 'fragment'             ;
LEXER        : 'lexer'                ;
PARSER       : 'parser'               ;
GRAMMAR      : 'grammar'              ;
PROTECTED    : 'protected'            ;
PUBLIC       : 'public'               ;
PRIVATE      : 'private'              ;
RETURNS      : 'returns'              ;
LOCALS       : 'locals'               ;
THROWS       : 'throws'               ;
CATCH        : 'catch'                ;
FINALLY      : 'finally'              ;
MODE         : 'mode'                 ;

// -----------
// Punctuation
//
// Character sequences used as separators, delimters, operators, etc
//
COLON        : ':'                    ;
COLONCOLON   : '::'                   ;
COMMA        : ','                    ;
SEMI         : ';'                    ;
LPAREN       : '('                    ;
RPAREN       : ')'                    ;
RARROW       : '->'                   ;
LT           : '<'                    ;
GT           : '>'                    ;
ASSIGN       : '='                    ;
QUESTION     : '?'                    ;
STAR         : '*'                    ;
PLUS         : '+'                    ;
PLUS_ASSIGN  : '+='                   ;
OR           : '|'                    ;
DOLLAR       : '$'                    ;
DOT		     : '.'                    ; // can be WILDCARD or DOT in qid or imported rule ref
RANGE        : '..'                   ;
AT           : '@'                    ;
POUND        : '#'                    ;
NOT          : '~'                    ;
RBRACE       : '}'                    ;

/** Allow unicode rule/token names */
ID			:	NameStartChar NameChar*;

fragment
NameChar
	:   NameStartChar
	|   '0'..'9'
	|   '_'
	|   '\u00B7'
	|   '\u0300'..'\u036F'
	|   '\u203F'..'\u2040'
	;

fragment
NameStartChar
	:   'A'..'Z'
	|   'a'..'z'
	|   '\u00C0'..'\u00D6'
	|   '\u00D8'..'\u00F6'
	|   '\u00F8'..'\u02FF'
	|   '\u0370'..'\u037D'
	|   '\u037F'..'\u1FFF'
	|   '\u200C'..'\u200D'
	|   '\u2070'..'\u218F'
	|   '\u2C00'..'\u2FEF'
	|   '\u3001'..'\uD7FF'
	|   '\uF900'..'\uFDCF'
	|   '\uFDF0'..'\uFFFD'
	; // ignores | ['\u10000-'\uEFFFF] ;

// ----------------------------
// Literals embedded in actions
//
// Note that we have made the assumption that the language used within
// actions uses the fairly standard " and ' delimiters for literals and
// that within these literals, characters are escaped using the \ character.
// There are some languages which do not conform to this in all cases, such
// as by using /string/ and so on. We will have to deal with such cases if
// if they come up in targets.
//

// Within actions, or other structures that are not part of the ANTLR
// syntax, we may encounter literal characters. Within these, we do
// not want to inadvertantly match things like '}' and so we eat them
// specifically. While this rule is called CHAR it allows for the fact that
// some languages may use/allow ' as the string delimiter.
//
fragment
ACTION_CHAR_LITERAL
	:	'\'' (ACTION_ESC | ~['\\] )* '\''
	;

// Within actions, or other structures that are not part of the ANTLR
// syntax, we may encounter literal strings. Within these, we do
// not want to inadvertantly match things like '}' and so we eat them
// specifically.
//
fragment
ACTION_STRING_LITERAL
	:	'"' (ACTION_ESC | ~["\\])* '"'
	;

// Within literal strings and characters that are not part of the ANTLR
// syntax, we must allow for escaped character sequences so that we do not
// inadvertantly recognize the end of a string or character when the terminating
// delimiter has been esacped.
//
fragment
ACTION_ESC
	:	'\\' .
	;

// -------
// Integer
//
// Obviously (I hope) match an arbitrary long sequence of digits.
//
INT
	: [0-9]+
	;

// --------------
// Literal string
//
// ANTLR makes no distinction between a single character literal and a
// multi-character string. All literals are single quote delimited and
// may contain unicode escape sequences of the form \uxxxx, where x
// is a valid hexadecimal number (as per Java basically).
STRING_LITERAL
	:  '\'' (ESC_SEQ | ~['\\])* '\''
	;

// A valid hex digit specification
//
fragment
HEX_DIGIT
	:	[0-9a-fA-F]
	;

// Any kind of escaped character that we can embed within ANTLR
// literal strings.
//
fragment
ESC_SEQ
	:	'\\'
		(	// The standard escaped character set such as tab, newline, etc.
			[btnfr"'\\]

		|	// A Java style Unicode escape sequence
			UNICODE_ESC
		)
	;

fragment
UNICODE_ESC
    :   'u' (HEX_DIGIT (HEX_DIGIT (HEX_DIGIT HEX_DIGIT?)?)?)?
    ;

// ----------
// Whitespace
//
// Characters and character constructs that are of no import
// to the parser and are used to make the grammar easier to read
// for humans.
//
WS
	:	[ \t\r\n\f]+
		-> channel(HIDDEN)
	;

// A fragment rule for recognizing both traditional whitespace and
// end of line markers, when we don't care to distinguish but don't
// want any action code going on.
//
fragment
WSNLCHAR
	:	[ \t\f\n\r]
	;

// -----------------
// Illegal Character
//
// This is an illegal character trap which is always the last rule in the
// lexer specification. It matches a single character of any value and being
// the last rule in the file will match when no other rule knows what to do
// about the character. It is reported as an error but is not passed on to the
// parser. This means that the parser to deal with the gramamr file anyway
// but we will not try to analyse or code generate from a file with lexical
// errors.
//
ERRCHAR
	:	.	-> skip
	;

mode ArgAction;

	ARG_ACTION_LT       : '<' ;
	ARG_ACTION_GT       : '>' ;
	ARG_ACTION_LPAREN   : '(' ;
	ARG_ACTION_RPAREN   : ')' ;
	ARG_ACTION_EQUALS   : '=' ;
	ARG_ACTION_COMMA    : ',' ;

	ARG_ACTION_ESCAPE
		:   '\\' .
		;

	ARG_ACTION_WORD
		:   [$a-zA-Z0-9_]
			[a-zA-Z0-9_]*
		;

	ARG_ACTION_ELEMENT
		:   ACTION_STRING_LITERAL
		|   ACTION_CHAR_LITERAL
		;

	/**
	 * This covers a group of characters which don't match any of the above rules.
	 */
	ARG_ACTION_TEXT
		:   ~(  ['"]
			|   ']'
			|   '\\'
			|   [=,<>()]
			|   [$a-zA-Z0-9_]
			|   [ \t\r\n]
			)+
		;

	ARG_ACTION_WS
		:   [ \t]+
		;

	ARG_ACTION_NEWLINE
		:   '\r' '\n'?
		|   '\n'
		;

	END_ARG_ACTION
		:   ']' -> popMode
		;

// ----------------
// Action structure
//
// Many language targets use {} as block delimiters and so we
// must recursively match {} delimited blocks to balance the
// braces. Additionally, we must make some assumptions about
// literal string representation in the target language. We assume
// that they are delimited by ' or " and so consume these
// in their own alts so as not to inadvertantly match {}.
// This mode is recursive on matching a {
mode Action;

	NESTED_ACTION
		:	'{' -> type(BEGIN_ACTION), pushMode(Action)
		;

	ACTION_DOT      : '.' ;
	ACTION_LT       : '<' ;
	ACTION_GT       : '>' ;
	ACTION_LPAREN   : '(' ;
	ACTION_RPAREN   : ')' ;
	ACTION_LBRACK   : '[' ;
	ACTION_RBRACK   : ']' ;
	ACTION_EQUALS   : '=' ;
	ACTION_COMMA    : ',' ;
	ACTION_COLON2   : '::' ;
	ACTION_COLON    : ':' ;
	ACTION_MINUS    : '-' ;

	ACTION_ESCAPE
		:	'\\' .
		;

	ACTION_WORD
		:	[a-zA-Z0-9_]+
		;

	ACTION_REFERENCE
		:	'$' ACTION_WORD?
		;

	ACTION_COMMENT
		:   BLOCK_COMMENT
		|   LINE_COMMENT
		;

	ACTION_LITERAL
		:   ACTION_STRING_LITERAL
		|   ACTION_CHAR_LITERAL
		;

	ACTION_TEXT
		:   (   '/' ~[*/]
			|   ~(  ['"]             // strings
				|   [{}]             // nested actions
				|   '\\'             // escapes
				|   '/'              // potential comments
				|   [.=,<>()\[\]:-]  // delimiters
				|   [$a-zA-Z0-9_]    // words
				|   [ \t\r\n]
				)
			)+
		;

	ACTION_WS
		:	[ \t]+
		;

	ACTION_NEWLINE
		:	'\r' '\n'?
		|	'\n'
		;

	END_ACTION
		:	'}' -> popMode
		;

mode LexerCharSet;

	LEXER_CHAR_SET_BODY
		:	(	~[\]\\]
			|	'\\' .
			)+
			-> more
		;

	END_LEXER_CHAR_SET
		:   ']' -> type(LEXER_CHAR_SET), popMode
		;

