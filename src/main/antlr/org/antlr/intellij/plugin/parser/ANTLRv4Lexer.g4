/*
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
 */

/**
 * A grammar for ANTLR v4 implemented using v4 syntax
 */
lexer grammar ANTLRv4Lexer;

import LexBasic;

// Standard set of fragments
tokens {
	TOKEN_REF,
	RULE_REF,
	LEXER_CHAR_SET
}

channels {
	 OFF_CHANNEL ,
	 COMMENT
}

@members {
    // Generic type for OPTIONS, TOKENS and CHANNELS
    private static final int PREQUEL_CONSTRUCT = -10;
    private static final int OPTIONS_CONSTRUCT = -11;

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

	private boolean insideOptionsBlock = false;

	public int getCurrentRuleType() {
		return _currentRuleType;
	}

	public void setCurrentRuleType(int ruleType) {
		this._currentRuleType = ruleType;
	}

	protected void handleBeginArgument() {
		if (inLexerRule()) {
			pushMode(LexerCharSet);
			more();
		}
		else {
			pushMode(Argument);
		}
	}

	protected void handleEndArgument() {
		popMode();
		if (_modeStack.size() > 0) {
			setType(ARGUMENT_CONTENT);
		}
	}

	protected void handleEndAction() {
	    int oldMode = _mode;
        int newMode = popMode();
        boolean isActionWithinAction = _modeStack.size() > 0
            && newMode == ANTLRv4Lexer.Action
            && oldMode == newMode;

		if (isActionWithinAction) {
			setType(ACTION_CONTENT);
		}
	}

	protected void handleOptionsLBrace() {
		if (insideOptionsBlock) {
			setType(ANTLRv4Lexer.BEGIN_ACTION);
			pushMode(ANTLRv4Lexer.Action);
		}
        else {
			setType(ANTLRv4Lexer.LBRACE);
			insideOptionsBlock = true;
		}
	}

    protected void handleOptionsRBrace() {
        if (_mode == ANTLRv4Lexer.Options) {
            insideOptionsBlock = false;
        }

        setType(ANTLRv4Lexer.RBRACE);
        popMode();
    }

	@Override
	public Token emit() {
	    if ((_type == OPTIONS || _type == TOKENS || _type == CHANNELS)
	            && _currentRuleType == Token.INVALID_TYPE) { // enter prequel construct ending with an RBRACE
	        _currentRuleType = PREQUEL_CONSTRUCT;
	    }
	    else if (_type == ANTLRv4Lexer.OPTIONS && _currentRuleType == ANTLRv4Lexer.TOKEN_REF) {
             _currentRuleType = OPTIONS_CONSTRUCT;
	    } else if (_type == RBRACE) {
	        if (_currentRuleType == PREQUEL_CONSTRUCT) { // exit prequel construct
	            _currentRuleType = Token.INVALID_TYPE;
	        }
	        else if (_currentRuleType == OPTIONS_CONSTRUCT) { // exit options
	            _currentRuleType = ANTLRv4Lexer.TOKEN_REF;
	        }
	    }
        else if (_type == AT && _currentRuleType == Token.INVALID_TYPE) { // enter action
            _currentRuleType = AT;
        }
        else if (_type == END_ACTION && _currentRuleType == AT) { // exit action
            _currentRuleType = Token.INVALID_TYPE;
        }
	    else if (_type == ID) {
			String firstChar = _input.getText(Interval.of(_tokenStartCharIndex, _tokenStartCharIndex));
			if (Character.isUpperCase(firstChar.charAt(0))) {
				_type = TOKEN_REF;
			}
            else {
				_type = RULE_REF;
			}

			if (_currentRuleType == Token.INVALID_TYPE) { // if outside of rule def
				_currentRuleType = _type;                 // set to inside lexer or parser rule
			}
		}
		else if (_type == SEMI) {
		    if (_currentRuleType != OPTIONS_CONSTRUCT) { // ';' in options { .... }. Don't change anything.
			    _currentRuleType = Token.INVALID_TYPE; // exit rule def
			}
		}

		return super.emit();
	}

	private boolean inLexerRule() {
		return _currentRuleType == TOKEN_REF;
	}
	private boolean inParserRule() { // not used, but added for clarity
		return _currentRuleType == RULE_REF;
	}

	/** Override nextToken so we can alter how it handles token errors.
	 *  Instead of looking for a new (valid) token, it should return an
	 *  invalid token. Changed "if ( _type ==SKIP )" part only from 4.7.
	 */
	@Override
	public Token nextToken() {
		if (_input == null) {
			throw new IllegalStateException("nextToken requires a non-null input stream.");
		}

		// Mark start location in char stream so unbuffered streams are
		// guaranteed at least have text of current token
		int tokenStartMarker = _input.mark();
		try{
			outer:
			while (true) {
				if (_hitEOF) {
					emitEOF();
					return _token;
				}

				_token = null;
				_channel = Token.DEFAULT_CHANNEL;
				_tokenStartCharIndex = _input.index();
				_tokenStartCharPositionInLine = getInterpreter().getCharPositionInLine();
				_tokenStartLine = getInterpreter().getLine();
				_text = null;
				do {
					_type = Token.INVALID_TYPE;
//				System.out.println("nextToken line "+tokenStartLine+" at "+((char)input.LA(1))+
//								   " in mode "+mode+
//								   " at index "+input.index());
					int ttype;
					try {
						ttype = getInterpreter().match(_input, _mode);
					}
					catch (LexerNoViableAltException e) {
						notifyListeners(e);		// report error
						recover(e);
						ttype = SKIP;
					}
					if ( _input.LA(1)==IntStream.EOF ) {
						_hitEOF = true;
					}
					if ( _type == Token.INVALID_TYPE ) _type = ttype;
					if ( _type ==SKIP ) {
						_type = Token.INVALID_TYPE;
						emit();
						return _token; // return a single bad token for this unmatched input
//						continue outer;
					}
				} while ( _type ==MORE );
				if ( _token == null ) emit();
				return _token;
			}
		}
		finally {
			// make sure we release marker after match or
			// unbuffered char stream will keep buffering
			_input.release(tokenStartMarker);
		}
	}

    private boolean isFollowedByBrace() {
        int i = 1;

        while ( _input.LA(i)==' '
                || _input.LA(i)=='\t'
                || _input.LA(i)=='\f'
                || _input.LA(i)=='\n'
                || _input.LA(i)=='\r' ) {
            i++;
        }

        return _input.LA(i)=='{';
    }
}

// ======================================================
// Lexer specification
//
// -------------------------
// Comments
DOC_COMMENT
	: DocComment
	;

BLOCK_COMMENT
	: BlockComment -> channel (COMMENT)
	;

LINE_COMMENT
	: LineComment -> channel (COMMENT)
	;

// -------------------------
// Integer
//
INT
	: DecimalNumeral
	;

// -------------------------
// Literal string
//
// ANTLR makes no distinction between a single character literal and a
// multi-character string. All literals are single quote delimited and
// may contain unicode escape sequences of the form \uxxxx, where x
// is a valid hexadecimal number (per Unicode standard).
STRING_LITERAL
	: SQuoteLiteral
	;

UNTERMINATED_STRING_LITERAL
	: USQuoteLiteral
	;

// -------------------------
// Arguments
//
// Certain argument lists, such as those specifying call parameters
// to a rule invocation, or input parameters to a rule specification
// are contained within square brackets.
BEGIN_ARGUMENT
	: LBrack
	{ handleBeginArgument(); }
	;

// -------------------------
// Actions
BEGIN_ACTION
	: LBrace -> pushMode (Action)
;

// -------------------------
// Keywords
//
// Keywords may not be used as labels for rules or in any other context where
// they would be ambiguous with the keyword vs some other identifier.  OPTIONS,
// TOKENS, & CHANNELS blocks are handled idiomatically in dedicated lexical modes.
OPTIONS
	: 'options' {isFollowedByBrace()}? -> pushMode (Options)
	;

TOKENS
	: 'tokens' {isFollowedByBrace()}? -> pushMode (Tokens)
	;

CHANNELS
	: 'channels' {isFollowedByBrace()}? -> pushMode (Channels)
	;

IMPORT
	: 'import'
	;

FRAGMENT
	: 'fragment'
	;

LEXER
	: 'lexer'
	;

PARSER
	: 'parser'
	;

GRAMMAR
	: 'grammar'
	;

PROTECTED
	: 'protected'
	;

PUBLIC
	: 'public'
	;

PRIVATE
	: 'private'
	;

RETURNS
	: 'returns'
	;

LOCALS
	: 'locals'
	;

THROWS
	: 'throws'
	;

CATCH
	: 'catch'
	;

FINALLY
	: 'finally'
	;

MODE
	: 'mode'
	;

// -------------------------
// Punctuation
COLON
	: Colon
	;

COLONCOLON
	: DColon
	;
COMMA
	: Comma
	;

SEMI
	: Semi
	;

LPAREN
	: LParen
	;

RPAREN
	: RParen
	;

LBRACE
	: LBrace
	;

RBRACE
	: RBrace
	;

RARROW
	: RArrow
	;

LT
	: Lt
	;

GT
	: Gt
	;

ASSIGN
	: Equal
	;

QUESTION
	: Question
	;

STAR
	: Star
	;

PLUS_ASSIGN
	: PlusAssign
	;

PLUS
	: Plus
	;

OR
	: Pipe
	;

DOLLAR
	: Dollar
	;

RANGE
	: Range
	;

DOT
	: Dot
	;

AT
	: At
	;

POUND
	: Pound
	;

NOT
	: Tilde
	;

// -------------------------
// Identifiers - allows unicode rule/token names
ID
	: Id
	;

// -------------------------
// Whitespace
WS
	: Ws+ -> channel (OFF_CHANNEL)
	;

// -------------------------
// Illegal Characters
//
// This is an illegal character trap which is always the last rule in the
// lexer specification. It matches a single character of any value and being
// the last rule in the file will match when no other rule knows what to do
// about the character. It is reported as an error but is not passed on to the
// parser. This means that the parser to deal with the gramamr file anyway
// but we will not try to analyse or code generate from a file with lexical
// errors.
//
// Comment this rule out to allow the error to be propagated to the parser

ERRCHAR
	: . -> channel (HIDDEN)
	;
// ======================================================
// Lexer modes
// -------------------------
// Arguments

mode Argument;
	 // E.g., [int x, List<String> a[]]
	 NESTED_ARGUMENT
		 : LBrack -> type (ARGUMENT_CONTENT) , pushMode (Argument)
		 ;

	 ARGUMENT_ESCAPE
		 : EscAny -> type (ARGUMENT_CONTENT)
		 ;

	 ARGUMENT_STRING_LITERAL
		 : DQuoteLiteral -> type (ARGUMENT_CONTENT)
		 ;

	 ARGUMENT_CHAR_LITERAL
		 : SQuoteLiteral -> type (ARGUMENT_CONTENT)
		 ;

	 END_ARGUMENT
		 : RBrack
		 { handleEndArgument(); }
		 ;
		 // added this to return non-EOF token type here. EOF does something weird

	 UNTERMINATED_ARGUMENT
		 : EOF -> popMode
		 ;

	 ARGUMENT_CONTENT
		 : .
		 ;

// -------------------------
// Actions
//
// Many language targets use {} as block delimiters and so we
// must recursively match {} delimited blocks to balance the
// braces. Additionally, we must make some assumptions about
// literal string representation in the target language. We assume
// that they are delimited by ' or " and so consume these
// in their own alts so as not to inadvertantly match {}.
mode Action;
	NESTED_ACTION
		: LBrace -> type (ACTION_CONTENT) , pushMode (Action)
		;

	ACTION_ESCAPE
		: EscAny -> type (ACTION_CONTENT)
		;

	ACTION_STRING_LITERAL
		: DQuoteLiteral -> type (ACTION_CONTENT)
		;

	ACTION_CHAR_LITERAL
		: SQuoteLiteral -> type (ACTION_CONTENT)
		;

	ACTION_DOC_COMMENT
		: DocComment -> type (ACTION_CONTENT)
		;

	ACTION_BLOCK_COMMENT
		: BlockComment -> type (ACTION_CONTENT)
		;

	ACTION_LINE_COMMENT
		: LineComment -> type (ACTION_CONTENT)
		;

	END_ACTION
		: RBrace
		{ handleEndAction(); }
		;

	UNTERMINATED_ACTION
		: EOF -> popMode
		;

	ACTION_CONTENT
		: .
	    ;

// -------------------------
mode Options;
    OPT_DOC_COMMENT
        : DocComment -> type (DOC_COMMENT) , channel (COMMENT)
        ;

    OPT_BLOCK_COMMENT
        : BlockComment -> type (BLOCK_COMMENT) , channel (COMMENT)
        ;

    OPT_LINE_COMMENT
        : LineComment -> type (LINE_COMMENT) , channel (COMMENT)
        ;

    OPT_LBRACE
        : LBrace
        { handleOptionsLBrace(); }
        ;

    OPT_RBRACE
        : RBrace
        { handleOptionsRBrace(); }
        ;

    OPT_ID
        : Id -> type (ID)
        ;

    OPT_DOT
        : Dot -> type (DOT)
        ;

    OPT_ASSIGN
        : Equal -> type (ASSIGN)
        ;

    OPT_STRING_LITERAL
        : SQuoteLiteral -> type (STRING_LITERAL)
        ;

    OPT_INT
        : DecimalNumeral -> type (INT)
        ;

    OPT_STAR
        : Star -> type (STAR)
        ;

    OPT_SEMI
        : Semi -> type (SEMI)
        ;

    OPT_WS
        : Ws+ -> type (WS) , channel (OFF_CHANNEL)
        ;
// -------------------------

mode Tokens;
    TOK_DOC_COMMENT
        : DocComment -> type (DOC_COMMENT) , channel (COMMENT)
        ;

    TOK_BLOCK_COMMENT
        : BlockComment -> type (BLOCK_COMMENT) , channel (COMMENT)
        ;

    TOK_LINE_COMMENT
        : LineComment -> type (LINE_COMMENT) , channel (COMMENT)
        ;

    TOK_LBRACE
        : LBrace -> type (LBRACE)
        ;

    TOK_RBRACE
        : RBrace -> type (RBRACE) , popMode
        ;

    TOK_ID
        : Id -> type (ID)
        ;

    TOK_DOT
        : Dot -> type (DOT)
        ;

    TOK_COMMA
        : Comma -> type (COMMA)
        ;

    TOK_WS
        : Ws+ -> type (WS) , channel (OFF_CHANNEL)
        ;

// -------------------------

mode Channels;
    // currently same as Tokens mode; distinguished by keyword
    CHN_DOC_COMMENT
        : DocComment -> type (DOC_COMMENT) , channel (COMMENT)
        ;

    CHN_BLOCK_COMMENT
        : BlockComment -> type (BLOCK_COMMENT) , channel (COMMENT)
        ;

    CHN_LINE_COMMENT
        : LineComment -> type (LINE_COMMENT) , channel (COMMENT)
        ;

    CHN_LBRACE
        : LBrace -> type (LBRACE)
        ;

    CHN_RBRACE
        : RBrace -> type (RBRACE) , popMode
        ;

    CHN_ID
        : Id -> type (ID)
        ;

    CHN_DOT
        : Dot -> type (DOT)
        ;

    CHN_COMMA
        : Comma -> type (COMMA)
        ;

    CHN_WS
        : Ws+ -> type (WS) , channel (OFF_CHANNEL)
        ;

// -------------------------
mode LexerCharSet;

	LEXER_CHAR_SET_BODY
		:	(	~[\]\\] | EscAny)+ -> more
		;

	LEXER_CHAR_SET
		:	RBrack						-> popMode
		;

	UNTERMINATED_CHAR_SET
		:	EOF							-> popMode
		;
	// ------------------------------------------------------------------------------
	// Grammar specific Keywords, Punctuation, etc.

	 fragment Id
		 : NameStartChar NameChar*
		 ;

