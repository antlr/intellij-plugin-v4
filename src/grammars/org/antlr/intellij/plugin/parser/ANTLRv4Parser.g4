/*
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
 */

/** A grammar for ANTLR v4 written in ANTLR v4 */
grammar GrammarParser;

// The main entry point for parsing a v4 grammar.
grammarSpec
	:	DOC_COMMENT?
		grammarType id ';'
		prequelConstruct*
		ruleSpec*
		modeSpec*
        EOF
	;

grammarType
	:	'lexer' 'grammar'
	|	'parser' 'grammar'
	|	'grammar'
	;

prequelConstruct
	:	optionsDef
	|	// A list of grammars to which this grammar will delegate certain
		// parts of the parsing sequence - a set of imported grammars
		imports

	|	// The declaration of any token types we need that are not already
		// specified by a preceeding grammar
		tokenDefs

	|	action // e.g., @header or @lexer::header
	;

// A list of options that affect analysis and/or code generation
optionsDef
	:	'options' '{' (option ';')* '}'
	;

option
	:	id '=' optionValue
	;

optionValue
	:	id
	|	STRING_LITERAL
	|	INT
	;

// A list of grammars to which this grammar will delegate certain
// parts of the parsing sequence - a set of imported grammars
imports
	:	'import' grammarName (',' grammarName)* ';'
	;

// A possibly named grammar file that should be imported to this gramamr
// and delgated to for the rules it specifies
grammarName
	:	id '=' id
	|	id
	;

/** The declaration of any token types we need that are not already
 *  specified by a preceeding grammar, such as when a parser declares
 *  imaginary tokens with which to construct the AST, or a rewriting
 *  tree parser adds further imaginary tokens to ones defined in a prior
 *  {tree} parser.
 */
tokenDefs
	:	'tokens' id (',' id)* ','? '}'
	;

actionBlock
	:	BEGIN_ACTION
			(	actionBlock
			|	actionExpression
			|	actionScopeExpression
			|	ACTION_WS
			|	ACTION_NEWLINE
			|	ACTION_COMMENT
			|	ACTION_LITERAL
			|	ACTION_TEXT
			|	ACTION_LT
			|	ACTION_GT
			|	ACTION_LPAREN
			|	ACTION_RPAREN
			|	ACTION_LBRACK
			|	ACTION_RBRACK
			|	ACTION_EQUALS
			|	ACTION_','
			|	ACTION_ESCAPE
			|	ACTION_WORD
			|	ACTION_REFERENCE
			|	ACTION_COLON
			|	ACTION_COLON2
			|	ACTION_MINUS
			|	ACTION_DOT
			)*
		END_ACTION
	;

actionExpression
	:	ref=ACTION_REFERENCE ignored* op=ACTION_DOT ignored* member=ACTION_WORD
	;

actionScopeExpression
	:	ref=ACTION_REFERENCE ignored* (ACTION_LBRACK ignored* (neg=ACTION_MINUS ignored*)? index=ACTION_WORD ignored* ACTION_RBRACK ignored*)? op=ACTION_COLON2 ignored* member=ACTION_WORD
	;

argActionBlock
	:	BEGIN_ARG_ACTION
			(	ARG_ACTION_ELEMENT
			|	ARG_ACTION_TEXT
			|	ARG_ACTION_LT
			|	ARG_ACTION_GT
			|	ARG_ACTION_LPAREN
			|	ARG_ACTION_RPAREN
			|	ARG_ACTION_EQUALS
			|	ARG_ACTION_','
			|	ARG_ACTION_ESCAPE
			|	ARG_ACTION_WORD
			|	ARG_ACTION_WS
			|	ARG_ACTION_NEWLINE
			)*
		END_ARG_ACTION
	;

argActionParameters
	:	BEGIN_ARG_ACTION
			ignored* (parameters+=argActionParameter ignored* (ARG_ACTION_',' ignored* parameters+=argActionParameter ignored*)*)?
		END_ARG_ACTION
	;

argActionParameter
	:	type=argActionParameterType? ignored* name=ARG_ACTION_WORD
	;

argActionParameterType
	:	argActionParameterTypePart (ignored* argActionParameterTypePart)*
	;

argActionParameterTypePart
	:	ARG_ACTION_WORD
	|	ARG_ACTION_LT argActionParameterType? ARG_ACTION_GT
	|	ARG_ACTION_LPAREN argActionParameterType? ARG_ACTION_RPAREN
	;

ignored
	:	ACTION_WS
	|	ACTION_NEWLINE
	|	ACTION_COMMENT
	|	ARG_ACTION_WS
	|	ARG_ACTION_NEWLINE
	;

// A declaration of a language target specifc section,
// such as @header, @includes and so on. We do not verify these
// sections, they are just passed on to the language target.
/** Match stuff like @parser::members {int i;} */
action
	:	AT (actionScopeName COLONCOLON)? id actionBlock
	;

/** Sometimes the scope names will collide with keywords; allow them as
 *  ids for action scopes.
 */
actionScopeName
	:	id
	|	LEXER
	|	PARSER
	;

modeSpec
	:	MODE id ';' ruleSpec+
	;

rules
	:	ruleSpec*
	;

ruleSpec
	:	parserRuleSpec
	|	lexerRule
	;

// The specification of an EBNF rule in ANTLR style, with all the
// rule level parameters, declarations, actions, rewrite specs and so
// on.
//
// Note that here we allow any number of rule declaration sections (such
// as scope, returns, etc) in any order and we let the upcoming semantic
// verification of the AST determine if things are repeated or if a
// particular functional element is not valid in the context of the
// grammar type, such as using returns in lexer rules and so on.
parserRuleSpec
	:	// A rule may start with an optional documentation comment
		DOC_COMMENT?

		// Following the documentation, we can declare a rule to be
		// public, private and so on. This is only valid for some
		// language targets of course but the target will ignore these
		// modifiers if they make no sense in that language.
		ruleModifiers?

		// Next comes the rule name. Here we do not distinguish between
		// parser or lexer rules, the semantic verification phase will
		// reject any rules that make no sense, such as lexer rules in
		// a pure parser or tree parser.
		name=RULE_REF

		// Immediately following the rulename, there may be a specification
		// of input parameters for the rule. We do not do anything with the
		// parameters here except gather them for future phases such as
		// semantic verifcation, type assignment etc. We require that
		// the input parameters are the next syntactically significant element
		// following the rule id.
		argActionParameters?

		ruleReturns?

		throwsSpec?

		localsSpec?

		// Now, before the rule specification itself, which is introduced
		// with a COLON, we may have zero or more configuration sections.
		// As usual we just accept anything that is syntactically valid for
		// one form of the rule or another and let the semantic verification
		// phase throw out anything that is invalid.

		// At the rule level, a programmer may specify a number of sections, such
		// as scope declarations, rule return elements, @ sections (which may be
		// language target specific) and so on. We allow any number of these in any
		// order here and as usual rely onthe semantic verification phase to reject
		// anything invalid using its addinotal context information. Here we are
		// context free and just accept anything that is a syntactically correct
		// construct.
		//
		rulePrequels

		COLON

		// The rule is, at the top level, just a list of alts, with
		// finer grained structure defined within the alts.
		ruleBlock

		';'

		exceptionGroup
	;

// Many language targets support exceptions and the rule will
// generally be able to throw the language target equivalent
// of a recognition exception. The grammar programmar can
// specify a list of exceptions to catch or a generic catch all
// and the target language code generation template is
// responsible for generating code that makes sense.
exceptionGroup
	:	exceptionHandler* finallyClause?
	;

// Specifies a handler for a particular type of exception
// thrown by a rule
exceptionHandler
	:	CATCH argActionBlock actionBlock
	;

finallyClause
	:	FINALLY actionBlock
	;

rulePrequels
	:	rulePrequel*
	;

// An individual rule level configuration as referenced by the ruleActions
// rule above.
//
rulePrequel
	:	optionsDef
	|	ruleAction
	;

// A rule can return elements that it constructs as it executes.
// The return values are specified in a 'returns' prequel element,
// which contains ',' separated declarations, where the declaration
// is target language specific. Here we see the returns declaration
// as a single lexical action element, to be processed later.
//
ruleReturns
	:	RETURNS argActionParameters
	;

// --------------
// Exception spec
//
// Some target languages, such as Java and C# support exceptions
// and they are specified as a prequel element for each rule that
// wishes to throw its own exception type. Note that the name of the
// exception is just a single word, so the header section of the grammar
// must specify the correct import statements (or language equivalent).
// Target languages that do not support exceptions just safely ignore
// them.
//
throwsSpec
	:	THROWS id (',' id)*
	;

localsSpec
	:	LOCALS argActionParameters
	;

// @ Sections are generally target language specific things
// such as local variable declarations, code to run before the
// rule starts and so on. Fir instance most targets support the
// @init {} section where declarations and code can be placed
// to run before the rule is entered. The C target also has
// an @declarations {} section, where local variables are declared
// in order that the generated code is C89 copmliant.
//
/** Match stuff like @init {int i;} */
ruleAction
	:	AT id actionBlock
	;

// A set of access modifiers that may be applied to rule declarations
// and which may or may not mean something to the target language.
// Note that the parser allows any number of these in any order and the
// semantic pass will throw out invalid combinations.
//
ruleModifiers
	:	ruleModifier+
	;

// An individual access modifier for a rule. The 'fragment' modifier
// is an internal indication for lexer rules that they do not match
// from the input but are like subroutines for other lexer rules to
// reuse for certain lexical patterns. The other modifiers are passed
// to the code generation templates and may be ignored by the template
// if they are of no use in that language.
ruleModifier
	:	PUBLIC
	|	PRIVATE
	|	PROTECTED
	|	FRAGMENT
	;

// A set of alts, rewritten as a BLOCK for generic processing
// in tree walkers. Used by the rule 'rule' so that the list of
// alts for a rule appears as a BLOCK containing the alts and
// can be processed by the generic BLOCK rule. Note that we
// use a separate rule so that the BLOCK node has start and stop
// boundaries set correctly by rule post processing of rewrites.
ruleBlock
	:	ruleAltList
	;

ruleAltList
	:	labeledAlt (OR labeledAlt)*
	;

labeledAlt
	:	alternative (POUND id)?
	;

lexerRule
	:	DOC_COMMENT? FRAGMENT?
		name=TOKEN_REF COLON lexerRuleBlock ';'
	;

lexerRuleBlock
	:	lexerAltList
	;

lexerAltList
	:	lexerAlt (OR lexerAlt)*
	;

lexerAlt
	:	lexerElements? lexerCommands?
	;

lexerElements
	:	lexerElement+
	;

lexerElement
	:	labeledLexerElement ebnfSuffix?
	|	lexerAtom ebnfSuffix?
	|	lexerBlock ebnfSuffix?
	|	actionBlock QUESTION? // actions only allowed at end of outer alt actually,
							  // but preds can be anywhere
	;

labeledLexerElement
	:	id ass=('='|PLUS_'=')
		(	lexerAtom
		|	block
		)
	;

lexerBlock
	:	LPAREN lexerAltList RPAREN
	;

// channel=HIDDEN, skip, more, mode(INSIDE), push(INSIDE), pop
lexerCommands
	:	RARROW lexerCommand (',' lexerCommand)*
	;

lexerCommand
	:	lexerCommandName LPAREN lexerCommandExpr RPAREN
	|	lexerCommandName
	;

lexerCommandName
	:	id
	|	MODE
	;

lexerCommandExpr
	:	id
	|	INT
	;

altList
	:	alternative (OR alternative)*
	;

// An individual alt with an optional rewrite clause for the
// elements of the alt.
alternative
	:	elements
	|			// empty alt
	;

elements
	:	element+
	;

element
	:	labeledElement
		(	ebnfSuffix
		|
		)
	|	atom
		(	ebnfSuffix
		|
		)
	|	ebnf
	|	actionBlock QUESTION? // SEMPRED is actionBlock followed by QUESTION
	;

labeledElement
	:	label=id ass=('='|PLUS_'=')
		(	atom
		|	block
		)
	;

// A block of gramamr structure optionally followed by standard EBNF
// notation, or ANTLR specific notation. I.E. ? + ^ and so on
ebnf
	:	block
		// And now we see if we have any of the optional suffixs and rewrite
		// the AST for this rule accordingly
		(	blockSuffix
		|
		)
	;

// The standard EBNF suffixes with additional components that make
// sense only to ANTLR, in the context of a grammar block.
blockSuffix
	:	ebnfSuffix // Standard EBNF
	;

ebnfSuffix
	:	QUESTION
	|	STAR
	|	PLUS
	;

lexerAtom
	:	range
	|	terminal
	|	RULE_REF
	|	notSet
	|	LEXER_CHAR_SET
	|	// Wildcard '.' means any character in a lexer, any
		// token in parser and any node or subtree in a tree parser
		// Because the terminal rule is allowed to be the node
		// specification for the start of a tree rule, we must
		// later check that wildcard was not used for that.
		DOT elementOptions?
	;

atom
	:	// Qualified reference delegate.rule. This must be
		// lexically contiguous (no spaces either side of the DOT)
		// otherwise it is two references with a wildcard in between
		// and not a qualified reference.
		range // Range x..y - only valid in lexers
	|	terminal
	|	ruleref
	|	notSet
	|	// Wildcard '.' means any character in a lexer, any
		// token in parser and any node or subtree in a tree parser
		// Because the terminal rule is allowed to be the node
		// specification for the start of a tree rule, we must
		// later check that wildcard was not used for that.
		DOT elementOptions?
	;

// --------------------
// Inverted element set
//
// A set of characters (in a lexer) or terminal tokens, if a parser,
// that are then used to create the inverse set of them.
notSet
	:	NOT setElement
	|	NOT blockSet
	;

blockSet
	:	LPAREN setElement (OR setElement)* RPAREN
	;

setElement
	:	TOKEN_REF
	|	STRING_LITERAL
	|	range
	|	LEXER_CHAR_SET
	;

// -------------
// Grammar Block
//
// Anywhere where an element is valid, the grammar may start a new block
// of alts by surrounding that block with ( ). A new block may also have a set
// of options, which apply only to that block.
//
block
	:	LPAREN
		( optionsDef? ruleAction* COLON )?
		altList
		RPAREN
	;

// ----------------
// Parser rule ref
//
// Reference to a parser rule with optional arguments and optional
// directive to become the root node or ignore the tree produced
//
ruleref
	:	RULE_REF argActionBlock?
	;

// ---------------
// Character Range
//
// Specifies a range of characters. Valid for lexer rules only, but
// we do not check that here, the tree walkers shoudl do that.
// Note also that the parser also allows through more than just
// character literals so that we can produce a much nicer semantic
// error about any abuse of the .. operator.
//
range
	: STRING_LITERAL RANGE STRING_LITERAL
	;

terminal
	:   TOKEN_REF elementOptions?
	|   STRING_LITERAL elementOptions?
	;

// Terminals may be adorned with certain options when
// reference in the grammar: TOK<,,,>
elementOptions
	:	LT elementOption (',' elementOption)* GT
	;

// WHen used with elements we can specify what the tree node type can
// be and also assign settings of various options  (which we do not check here)
elementOption
	:	// This format indicates the default node option
		id

	|	// This format indicates option assignment
		id '=' (id | STRING_LITERAL)
	;

// The name of the grammar, and indeed some other grammar elements may
// come through to the parser looking like a rule reference or a token
// reference, hence this rule is used to pick up whichever it is and rewrite
// it as a generic ID token.
id
	:	RULE_REF
	|	TOKEN_REF
	;
