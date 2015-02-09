<fold text='...' expand='false'>/*
 * [The "BSD license"]
 *  Copyright (c) 2014 Terence Parr
 *  Copyright (c) 2014 Sam Harwell
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

/** A grammar for ANTLR v4 tokens */
lexer grammar ANTLRv4Lexer;

tokens <fold text='...' expand='false'>{
	TOKEN_REF,
	RULE_REF,
	LEXER_CHAR_SET
}</fold>

@members <fold text='{...}' expand='false'>{
	/** Track whether we are inside of a rule and whether it is lexical parser.
	 *  _currentRuleType==Token.INVALID_TYPE means that we are outside of a rule.
	 *  At the first sign of a rule name reference and _currentRuleType==invalid,
	 *  we can assume that we are starting a parser rule. Similarly, seeing
	 *  a token reference when not already in rule means starting a token
	 *  rule. The terminating ';' of a rule, flips this back to invalid type.
	 *
	 *  This is not perfect logic but works. For example, "grammar T;" means
	 *  that we start and stop a lexical rule for the "T;". Dangerous but works.
	 *
	 *  The whole point of this state information is to distinguish
	 *  between [..arg actions..] and [charsets]. Char sets can only occur in
	 *  lexical rules and arg actions cannot occur.
	 */
	private int _currentRuleType = Token.INVALID_TYPE;

	public int getCurrentRuleType() {
		return _currentRuleType;
	}

	public void setCurrentRuleType(int ruleType) {
		this._currentRuleType = ruleType;
	}

	protected void handleBeginArgAction() {
		if (inLexerRule()) {
			pushMode(LexerCharSet);
			more();
		}
		else {
			pushMode(ArgAction);
			more();
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

			if (_currentRuleType == Token.INVALID_TYPE) { // if outside of rule def
				_currentRuleType = _type;                 // set to inside lexer or parser rule
			}
		}
		else if (_type == SEMI) {                  // exit rule def
			_currentRuleType = Token.INVALID_TYPE;
		}

		return super.emit();
	}

	private boolean inLexerRule() {
		return _currentRuleType == TOKEN_REF;
	}
	private boolean inParserRule() { // not used, but added for clarity
		return _currentRuleType == RULE_REF;
	}
}</fold>

DOC_COMMENT<fold text=':...' expand='true'>
	:	'/**' .*? ('*/' | EOF)
	;</fold>

BLOCK_COMMENT<fold text=':...' expand='true'>
	:	'/*' .*? ('*/' | EOF)  -> channel(HIDDEN)
	;</fold>

LINE_COMMENT<fold text=':...' expand='true'>
	:	'//' ~[\r\n]*  -> channel(HIDDEN)
	;</fold>

BEGIN_ARG_ACTION<fold text=':...' expand='true'>
	:	'[' {handleBeginArgAction();}
	;</fold>

<fold text='//...' expand='true'>// OPTIONS and TOKENS must also consume the opening brace that captures
// their option block, as this is teh easiest way to parse it separate
// to an ACTION block, despite it usingthe same {} delimiters.
//</fold>
OPTIONS<fold text=':...' expand='true'>      : 'options' [ \t\f\n\r]* '{'  ;</fold>
TOKENS<fold text=':...' expand='true'>		 : 'tokens'  [ \t\f\n\r]* '{'  ;</fold>

IMPORT<fold text=':...' expand='true'>       : 'import'               ;</fold>
FRAGMENT<fold text=':...' expand='true'>     : 'fragment'             ;</fold>
LEXER<fold text=':...' expand='true'>        : 'lexer'                ;</fold>
PARSER<fold text=':...' expand='true'>       : 'parser'               ;</fold>
GRAMMAR<fold text=':...' expand='true'>      : 'grammar'              ;</fold>
PROTECTED<fold text=':...' expand='true'>    : 'protected'            ;</fold>
PUBLIC<fold text=':...' expand='true'>       : 'public'               ;</fold>
PRIVATE<fold text=':...' expand='true'>      : 'private'              ;</fold>
RETURNS<fold text=':...' expand='true'>      : 'returns'              ;</fold>
LOCALS<fold text=':...' expand='true'>       : 'locals'               ;</fold>
THROWS<fold text=':...' expand='true'>       : 'throws'               ;</fold>
CATCH<fold text=':...' expand='true'>        : 'catch'                ;</fold>
FINALLY<fold text=':...' expand='true'>      : 'finally'              ;</fold>
MODE<fold text=':...' expand='true'>         : 'mode'                 ;</fold>

COLON<fold text=':...' expand='true'>        : ':'                    ;</fold>
COLONCOLON<fold text=':...' expand='true'>   : '::'                   ;</fold>
COMMA<fold text=':...' expand='true'>        : ','                    ;</fold>
SEMI<fold text=':...' expand='true'>         : ';'                    ;</fold>
LPAREN<fold text=':...' expand='true'>       : '('                    ;</fold>
RPAREN<fold text=':...' expand='true'>       : ')'                    ;</fold>
RARROW<fold text=':...' expand='true'>       : '->'                   ;</fold>
LT<fold text=':...' expand='true'>           : '<'                    ;</fold>
GT<fold text=':...' expand='true'>           : '>'                    ;</fold>
ASSIGN<fold text=':...' expand='true'>       : '='                    ;</fold>
QUESTION<fold text=':...' expand='true'>     : '?'                    ;</fold>
STAR<fold text=':...' expand='true'>         : '*'                    ;</fold>
PLUS<fold text=':...' expand='true'>         : '+'                    ;</fold>
PLUS_ASSIGN<fold text=':...' expand='true'>  : '+='                   ;</fold>
OR<fold text=':...' expand='true'>           : '|'                    ;</fold>
DOLLAR<fold text=':...' expand='true'>       : '$'                    ;</fold>
DOT<fold text=':...' expand='true'>		     : '.'                    ;</fold>
RANGE<fold text=':...' expand='true'>        : '..'                   ;</fold>
AT<fold text=':...' expand='true'>           : '@'                    ;</fold>
POUND<fold text=':...' expand='true'>        : '#'                    ;</fold>
NOT<fold text=':...' expand='true'>          : '~'                    ;</fold>
RBRACE<fold text=':...' expand='true'>       : '}'                    ;</fold>

<fold text='...' expand='true'>/** Allow unicode rule/token names */</fold>
ID<fold text=':...' expand='true'>	:	NameStartChar NameChar*;</fold>

fragment
NameChar<fold text=':...' expand='true'>
	:   NameStartChar
	|   '0'..'9'
	|   '_'
	|   '\u00B7'
	|   '\u0300'..'\u036F'
	|   '\u203F'..'\u2040'
	;</fold>

fragment
NameStartChar<fold text=':...' expand='true'>
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
	;</fold> <fold text='//...' expand='true'>// ignores | ['\u10000-'\uEFFFF] ;</fold>

INT<fold text=':...' expand='true'>	: [0-9]+
	;</fold>

<fold text='//...' expand='true'>// ANTLR makes no distinction between a single character literal and a
// multi-character string. All literals are single quote delimited and
// may contain unicode escape sequences of the form \uxxxx, where x
// is a valid hexadecimal number (as per Java basically).</fold>
STRING_LITERAL<fold text=':...' expand='true'>
	:  '\'' (ESC_SEQ | ~['\r\n\\])* '\''
	;</fold>

UNTERMINATED_STRING_LITERAL<fold text=':...' expand='true'>
	:  '\'' (ESC_SEQ | ~['\r\n\\])*
	;</fold>

<fold text='//...' expand='true'>// Any kind of escaped character that we can embed within ANTLR
// literal strings.</fold>
fragment
ESC_SEQ<fold text=':...' expand='true'>
	:	'\\'
		(	<fold text='//...' expand='true'>// The standard escaped character set such as tab, newline, etc.</fold>
			[btnfr"'\\]
		|	<fold text='//...' expand='true'>// A Java style Unicode escape sequence</fold>
			UNICODE_ESC
		|	<fold text='//...' expand='true'>// Invalid escape</fold>
			.
		|	<fold text='//...' expand='true'>// Invalid escape at end of file</fold>
			EOF
		)
	;</fold>

fragment
UNICODE_ESC<fold text=':...' expand='true'>
    :   'u' (HEX_DIGIT (HEX_DIGIT (HEX_DIGIT HEX_DIGIT?)?)?)?
    ;</fold>

fragment
HEX_DIGIT<fold text=':...' expand='true'> : [0-9a-fA-F]	;</fold>

WS<fold text=':...' expand='true'>  :	[ \t\r\n\f]+ -> channel(HIDDEN)	;</fold>

<fold text='//...' expand='true'>// Many language targets use {} as block delimiters and so we
// must recursively match {} delimited blocks to balance the
// braces. Additionally, we must make some assumptions about
// literal string representation in the target language. We assume
// that they are delimited by ' or " and so consume these
// in their own alts so as not to inadvertantly match {}.</fold>

ACTION<fold text=':...' expand='true'>
	:	'{'
		(	ACTION
		|	ACTION_ESCAPE
        |	ACTION_STRING_LITERAL
        |	ACTION_CHAR_LITERAL
        |	'/*' .*? '*/' <fold text='//...' expand='true'>// ('*/' | EOF)</fold>
        |	'//' ~[\r\n]*
        |	.
		)*?
		('}'|EOF)
	;</fold>

fragment
ACTION_ESCAPE<fold text=':...' expand='true'>
		:   '\\' .
		;</fold>

fragment
ACTION_STRING_LITERAL<fold text=':...' expand='true'>
        :	'"' (ACTION_ESCAPE | ~["\\])* '"'
        ;</fold>

fragment
ACTION_CHAR_LITERAL<fold text=':...' expand='true'>
        :	'\'' (ACTION_ESCAPE | ~['\\])* '\''
        ;</fold>

<fold text='//...' expand='true'>// -----------------
// Illegal Character
//
// This is an illegal character trap which is always the last rule in the
// lexer specification. It matches a single character of any value and being
// the last rule in the file will match when no other rule knows what to do
// about the character. It is reported as an error but is not passed on to the
// parser. This means that the parser to deal with the gramamr file anyway
// but we will not try to analyse or code generate from a file with lexical
// errors.
//</fold>
ERRCHAR<fold text=':...' expand='true'>
	:	.	-> channel(HIDDEN)
	;</fold>

mode ArgAction; <fold text='//...' expand='true'>// E.g., [int x, List<String> a[]]</fold>

	NESTED_ARG_ACTION<fold text=':...' expand='true'>
		:	'['                         -> more, pushMode(ArgAction)
		;</fold>

	ARG_ACTION_ESCAPE<fold text=':...' expand='true'>
		:   '\\' .                      -> more
		;</fold>

    ARG_ACTION_STRING_LITERAL<fold text=':...' expand='true'>
        :	('"' ('\\' . | ~["\\])* '"')-> more
        ;</fold>

    ARG_ACTION_CHAR_LITERAL<fold text=':...' expand='true'>
        :	('"' '\\' . | ~["\\] '"')   -> more
        ;</fold>

    ARG_ACTION<fold text=':...' expand='true'>
		:   ']'                         -> popMode
		;</fold>

	UNTERMINATED_ARG_ACTION<fold text=':...' expand='true'> <fold text='//...' expand='true'>// added this to return non-EOF token type here. EOF did something weird</fold>
		:	EOF							-> popMode
		;</fold>

    ARG_ACTION_CHAR<fold text=':...' expand='true'> <fold text='//...' expand='true'>// must be last</fold>
        :   .                           -> more
        ;</fold>


mode LexerCharSet;

	LEXER_CHAR_SET_BODY<fold text=':...' expand='true'>
		:	(	~[\]\\]
			|	'\\' .
			)
                                        -> more
		;</fold>

	LEXER_CHAR_SET<fold text=':...' expand='true'>
		:   ']'                         -> popMode
		;</fold>

	UNTERMINATED_CHAR_SET<fold text=':...' expand='true'>
		:	EOF							-> popMode
		;</fold>
