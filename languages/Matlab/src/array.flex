package array;

import static array.ArrayParser.Terminals.*;

import beaver.Symbol;
import beaver.Scanner;

%%

//general header info
%public
%final
%class ArrayScanner

//required for beaver compatibility
%extends Scanner
%unicode
%function nextToken
%type Symbol
%yylexthrow Scanner.Exception

//for debugging - track line and column
%line
%column

%{
  //TODO-AC: factor out shared code

  //// Returning symbols ///////////////////////////////////////////////////////

  //Create a symbol using the current line and column number, as computed by JFlex
  //No attached value
  //Symbol is assumed to start and end on the same line
  //e.g. symbol(SEMICOLON)
  private Symbol symbol(short type) {
    return symbol(type, null);
  }
  
  //Create a symbol using the current line and column number, as computed by JFlex
  //Attached value gives content information
  //Symbol is assumed to start and end on the same line
  //e.g. symbol(IDENTIFIER, "x")
  private Symbol symbol(short type, Object value) {
    //NB: JFlex is zero-indexed, but we want one-indexed
    int startLine = yyline + 1;
    int startCol = yycolumn + 1;
    int endLine = startLine;
    int endCol = startCol + yylength() - 1;
    return symbol(type, value, startLine, startCol, endLine, endCol);
  }
  
  //Create a symbol using explicit position information (one-indexed)
  private Symbol symbol(short type, Object value, int startLine, int startCol, int endLine, int endCol) {
    //if we saw something that forces the next single-quote to mean MTRANSPOSE, then set transposeNext
    transposeNext = TYPE_PRECEDING_TRANSPOSE.contains(type);
    int startPos = Symbol.makePosition(startLine, startCol);
    int endPos = Symbol.makePosition(endLine, endCol);
    return new Symbol(type, startPos, endPos, value);
  }
  
  //// Position ////////////////////////////////////////////////////////////////
  
  //records the position of a symbol
  private static class PositionRecord {
      int startLine = -1;
      int startCol = -1;
      int endLine = -1;
      int endCol = -1;
  }
  
  //the position of the current symbol
  private PositionRecord pos = new PositionRecord();
  
  //populate the start line and column fields of the Position record with
  //values from JFlex
  private void markStartPosition() {
    //correct to one-indexed
    pos.startLine = yyline + 1;
    pos.startCol = yycolumn + 1;
  }
  
  //populate the start line and column fields of the Position record with
  //values from JFlex
  private void markEndPosition() {
    //correct to one-indexed
    pos.endLine = yyline + 1;
    pos.endCol = (yycolumn + 1) + yylength() - 1;
  }
  
  //like symbol(type), but uses the position stored in pos rather than
  //the position computed by JFlex
  private Symbol symbolFromMarkedPositions(short type) {
    return symbolFromMarkedPositions(type, null);
  }
  
  //like symbol(type, value), but uses the position stored in pos rather than
  //the position computed by JFlex
  private Symbol symbolFromMarkedPositions(short type, Object value) {
    return symbol(type, value, pos.startLine, pos.startCol, pos.endLine, pos.endCol);
  }
  
  //like symbol(type), but uses the start position stored in pos rather than
  //the start position computed by JFlex and an explicit length param rather
  //than yylength
  private Symbol symbolFromMarkedStart(short type, int length) {
    return symbolFromMarkedStart(type, null, length);
  }
  
  //like symbol(type, value), but uses the start position stored in pos rather than
  //the start position computed by JFlex and an explicit length param rather
  //than yylength
  private Symbol symbolFromMarkedStart(short type, Object value, int length) {
    return symbol(type, value, pos.startLine, pos.startCol, pos.startLine, pos.startCol + length - 1);
  }
  
  //// Errors //////////////////////////////////////////////////////////////////
  
  //throw an exceptions with position information from JFlex
  private void error(String msg) throws Scanner.Exception {
    //correct to one-indexed
    throw new Scanner.Exception(yyline + 1, yycolumn + 1, msg);
  }
  
  //throw an exceptions with position information from JFlex
  //columnOffset is added to the column
  private void error(String msg, int columnOffset) throws Scanner.Exception {
  //correct to one-indexed
    throw new Scanner.Exception(yyline + 1, yycolumn + 1 + columnOffset, msg);
  }
  
  //// Comment nesting /////////////////////////////////////////////////////////
  
  //number of '%}'s expected
  private int bracketCommentNestingDepth = 0;
  //bracket comment string consumed so far
  private StringBuffer bracketCommentBuf = null;
  
  //// State transitions ///////////////////////////////////////////////////////
  
  //stack entry: stack identifier + symbol position
  private static class StateRecord {
    int stateNum;
    PositionRecord pos;
    
    StateRecord(int stateNum, PositionRecord pos) {
        this.stateNum = stateNum;
        this.pos = pos;
    }
  }
  
  //most of our states are used for bracketing
  //this gives us a way to nest bracketing states
  private java.util.Stack<StateRecord> stateStack = new java.util.Stack<StateRecord>();
  
  void saveStateAndTransition(int newState) {
    stateStack.push(new StateRecord(yystate(), pos));
    pos = new PositionRecord();
    yybegin(newState);
  }
  
  void restoreState() {
    StateRecord rec = stateStack.pop();
    yybegin(rec.stateNum);
    pos = rec.pos;
  }
  
  //// Transpose ///////////////////////////////////////////////////////////////
  
  //if any of these symbols is seen, then an immediately following single-quote
  //will be interpreted as MTRANSPOSE
  //if any other symbol is seen, then a single-quote will be interpreted as the
  //beginning of a string literal
  private static final java.util.Set<Short> TYPE_PRECEDING_TRANSPOSE = new java.util.HashSet<Short>();
  static {
    //NB: cannot contain DOT
    TYPE_PRECEDING_TRANSPOSE.add(IDENTIFIER);
    TYPE_PRECEDING_TRANSPOSE.add(NUMBER);
    TYPE_PRECEDING_TRANSPOSE.add(RPAREN);
    TYPE_PRECEDING_TRANSPOSE.add(RSQUARE);
    TYPE_PRECEDING_TRANSPOSE.add(RCURLY);
    TYPE_PRECEDING_TRANSPOSE.add(ARRAYTRANSPOSE);
    TYPE_PRECEDING_TRANSPOSE.add(MTRANSPOSE);
  }
  
  private boolean transposeNext = false;
  
  //// String literals /////////////////////////////////////////////////////////
  
  //for accumulating the contents of a string literal
  private StringBuffer strBuf = new StringBuffer();
%}

LineTerminator = \r|\n|\r\n
OtherWhiteSpace = [ \t\f]

//NB: acceptable to conflict with ... - matlab just treats .... as a comment containing .
Ellipsis = \.\.\.
EscapedLineTerminator = {Ellipsis}.*{LineTerminator}

Letter = [a-zA-Z]
Digit = [0-9]
HexDigit = {Digit} | [a-fA-F]
Identifier = ([_$] | {Letter}) ([_$] | {Letter} | {Digit})*
SciExp = [Ee][+-]?{Digit}+
Imaginary = [iIjJ]
IntNumber = {Digit}+
FPNumber = (({Digit}+\.?{Digit}*) | (\.?{Digit}+)){SciExp}?
HexNumber = 0[xX]{HexDigit}+
Number = ({IntNumber} | {FPNumber} | {HexNumber}) {Imaginary}?;

Comment=% | %[^%{\r\n].*
OpenBracketComment = %\{
CloseBracketComment = %\}

ValidEscape=\\[bfnrt\\\"]

//within a bracket comment (i.e. %{)
%xstate COMMENT_NESTING
//within a string literal
%xstate INSIDE_STRING

%%

//TODO-AC: anything that doesn't call symbol might have to explicitly set transposeNext (probably to false)

//... comment
{EscapedLineTerminator} {
    transposeNext = false;
    return symbol(ELLIPSIS_COMMENT, yytext().substring(yytext().indexOf("..."), yylength() - 1));
}

//whitespace
{LineTerminator} { return symbol(LINE_TERMINATOR); }
{OtherWhiteSpace} { return symbol(OTHER_WHITESPACE); }

//numeric literals
{Number} { return symbol(NUMBER, yytext()); }

//MTRANSPOSE or STRING (start)
"'" {
    //NB: cannot be a string if we're expecting a transpose - even if string is a longer match
    if(transposeNext) {
        return symbol(MTRANSPOSE);
    } else {
        saveStateAndTransition(INSIDE_STRING);
        strBuf = new StringBuffer();
        markStartPosition();
    }
}

//remainder of string literal (i.e. after initial single quote)
<INSIDE_STRING> {
    "''" { strBuf.append(yytext()); }
    "'" {
        markEndPosition();
        Symbol sym = symbolFromMarkedPositions(STRING, strBuf.toString());
        restoreState();
        return sym;
    }
    {ValidEscape} { strBuf.append(yytext()); }
    \\ { error("Invalid escape sequence"); }
    . { strBuf.append(yytext()); }
    <<EOF>> { 
        error("Unterminated string literal: '" + strBuf + "'");
    }
}

//single-line comment
{Comment} { transposeNext = false; return symbol(COMMENT, yytext()); }

//start multiline comment
{OpenBracketComment} {
    transposeNext = false; 
    saveStateAndTransition(COMMENT_NESTING);
    markStartPosition();
    bracketCommentNestingDepth++;
    bracketCommentBuf = new StringBuffer(yytext());
}

//continue multiline (help) comment
<COMMENT_NESTING> {
    [^%]+ { bracketCommentBuf.append(yytext()); }
    % { bracketCommentBuf.append(yytext()); }
    {OpenBracketComment} { bracketCommentNestingDepth++; bracketCommentBuf.append(yytext()); }
    {CloseBracketComment} { 
        bracketCommentNestingDepth--;
        bracketCommentBuf.append(yytext());
        if(bracketCommentNestingDepth == 0) {
            markEndPosition();
            Symbol sym = symbolFromMarkedPositions(BRACKET_COMMENT, bracketCommentBuf.toString());
            restoreState();
            return sym;
        }
    }
    <<EOF>> {
        //don't finish scanning if there's an unclosed comment
        if(bracketCommentNestingDepth != 0) {
            error(bracketCommentNestingDepth + " levels of comments not closed");
        }
        return symbol(EOF);
    }
}

//bracketing
\( { return symbol(LPAREN); }
\) { return symbol(RPAREN); }
\[ { return symbol(LSQUARE); }
\] { return symbol(RSQUARE); }
\{ { return symbol(LCURLY); }
\} { return symbol(RCURLY); }

//stmt terminators
, { return symbol(COMMA); }
; { return symbol(SEMICOLON); }

//misc punctuation
: { return symbol(COLON); }
@ { return symbol(AT); }

//from http://www.mathworks.com/access/helpdesk/help/techdoc/ref/arithmeticoperators.html
"+" { return symbol(PLUS); }
"-" { return symbol(MINUS); }
"*" { return symbol(MTIMES); }
".*" { return symbol(ETIMES); }
"/" { return symbol(MDIV); }
"./" { return symbol(EDIV); }
"\\" { return symbol(MLDIV); }
".\\" { return symbol(ELDIV); }
"^" { return symbol(MPOW); }
".^" { return symbol(EPOW); }
//"'" { return symbol(MTRANSPOSE); } //mixed with string rule above
".'" { return symbol(ARRAYTRANSPOSE); }

//from http://www.mathworks.com/access/helpdesk/help/techdoc/ref/relationaloperators.html
"<=" { return symbol(LE); }
">=" { return symbol(GE); }
"<" { return symbol(LT); }
">" { return symbol(GT); }
"==" { return symbol(EQ); }
"~=" { return symbol(NE); }

//from http://www.mathworks.com/access/helpdesk/help/techdoc/matlab_prog/f0-40063.html
"&" { return symbol(AND); }
"|" { return symbol(OR); }
"~" { return symbol(NOT); }
"&&" { return symbol(SHORTAND); }
"||" { return symbol(SHORTOR); }

\. { return symbol(DOT); }

{Identifier} { return symbol(IDENTIFIER, yytext()); }

/* error fallback */
.|\n { error("Illegal character found inside array '" + yytext() + "'"); }

<<EOF>> { return symbol(EOF); }