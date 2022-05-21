
/**
 * Authors: Ozan Uslan - 2019510078, Oguz Akif Tufekcioglu - 2018510060, Huseyin Saatci - 2018510054
 * Date: 2022/05/21
 * 
 * Project Definition: The Program should accept a source file called
 * code_file.ceng and produce a text file named as code.lex that contains
 * all the tokens of the code.lex listed one after the other.
 * 
 * Lexical Tokens:
 * 1 - Identifiers
 * 2 - String constants
 * 3 - Integer constants
 * 4 - Keywords
 * 5 - Operators
 * 6 - Brackets
 * 7 - EndofLine
 * 
 * Non-Tokens:
 * 1 - Comments
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

class Main {
  private static final String source_file = "code_file.ceng";
  private static final String output_file = "code.lex";

  public static void main(String[] args) {
    System.out.println("CENG Lexical Analyzer");
    System.out.println("=====================");
    // read source file
    String input = "";
    System.out.println("Reading source from file " + source_file + "...");
    try {
      input = new String(Files.readAllBytes(Paths.get(source_file)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      System.out.println("error: " + e.getMessage());
      System.exit(1);
    }

    // lexical analysis
    Lexer lexer = new Lexer().init(input);
    List<Lexeme> lexemes = new LinkedList<>();
    System.out.println("Lexing source file...");
    try {
      lexemes = lexer.lex();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.exit(1);
    }

    // write output file
    System.out.println("Writing lexemes to " + output_file + "...");
    try {
      PrintWriter writer = new PrintWriter(output_file, "UTF-8");
      for (int i = 0; i < lexemes.size(); i++) {
        Lexeme token = lexemes.get(i);
        writer.print(token + (i == lexemes.size() - 1 ? "" : "\n"));
      }
      System.out.println("Done! " + lexemes.size() + " lexemes written to output file.");
      writer.close();
    } catch (IOException e) {
      System.out.println("error: " + e.getMessage());
      System.exit(1);
    }
  }
}

class Lexer {
  private static final int MAX_IDENTIFIER_LENGTH = 25;
  private static final int MAX_INT_LENGTH = 10;
  private static final char EOF = '\0';
  private static final String[] KEYWORDS = {
      "break", "case", "char", "const", "do", "else", "enum", "float", "for", "if", "int", "double", "long", "struct",
      "return", "static", "while"
  };

  private String input;
  private int charIdx;

  public Lexer() {
  }

  public Lexer init(String input) {
    this.input = input;
    this.charIdx = 0;
    return this;
  }

  private char getChar() {
    return charIdx < input.length() ? input.charAt(charIdx) : EOF;
  }

  private char peekChar() {
    return charIdx + 1 < input.length() ? input.charAt(charIdx + 1) : EOF;
  }

  private char nextChar() {
    charIdx++;
    return getChar();
  }

  private boolean isKeyword(String s) {
    for (String keyword : KEYWORDS) {
      if (keyword.equals(s)) {
        return true;
      }
    }
    return false;
  }

  private boolean isDigit(char c) {
    return (c >= '0' && c <= '9');
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }

  private boolean isUnderscore(char c) {
    return c == '_';
  }

  private boolean isStringSymbol(char c) {
    return c == '"';
  }

  public List<Lexeme> lex() throws IllegalStateException, LexicalException {
    if (input == null)
      throw new IllegalStateException("Lexer not initialized");

    List<Lexeme> tokens = new LinkedList<>();
    char c = getChar();
    while (getChar() != EOF) {
      switch (c) {
        case ';':
          tokens.add(Lexeme.createLexeme(LexemeType.Type.EndOfLine));
          break;
        case '*':
          tokens.add(Lexeme.createLexeme(LexemeType.Type.Operator, "*"));
          break;
        case '/':
          // handle // comment, /* comment */
          if (peekChar() == '/') {
            nextChar();
            while (getChar() != '\n' && getChar() != EOF) {
              nextChar();
            }
          } else if (peekChar() == '*') {
            nextChar();
            nextChar();
            while (!(getChar() == '*' && peekChar() == '/')) {
              nextChar();
              if (getChar() == EOF) {
                throw new LexicalException("unclosed comment");
              }
            }
            nextChar();
          } else {
            tokens.add(Lexeme.createLexeme(LexemeType.Type.Operator, "/"));
          }
          break;
        case '+':
          // handle + and ++
          if (peekChar() == '+') {
            tokens.add(Lexeme.createLexeme(LexemeType.Type.Operator, "++"));
            nextChar();
          } else {
            tokens.add(Lexeme.createLexeme(LexemeType.Type.Operator, "+"));
          }
          break;
        case '-':
          // handle -, --, and negative integer values
          if (peekChar() == '-') {
            tokens.add(Lexeme.createLexeme(LexemeType.Type.Operator, "--"));
            nextChar();
          } else if (isDigit(peekChar())) {
            StringBuilder sb = new StringBuilder();
            sb.append('-');
            sb.append(nextChar());
            while (isDigit(peekChar())) {
              sb.append(nextChar());
              if (sb.length() - 1 > MAX_INT_LENGTH) {
                throw new LexicalException("integer too long");
              }
            }
            tokens.add(Lexeme.createLexeme(LexemeType.Type.IntConstant, sb.toString()));
          } else {
            tokens.add(Lexeme.createLexeme(LexemeType.Type.Operator, "-"));
          }
          break;
        case '=':
          // handle = and ==
          if (peekChar() == '=') {
            tokens.add(Lexeme.createLexeme(LexemeType.Type.Operator, "=="));
            nextChar();
          } else {
            tokens.add(Lexeme.createLexeme(LexemeType.Type.Operator, "="));
          }
          break;
        case '<':
          // handle < and <=
          if (peekChar() == '=') {
            tokens.add(Lexeme.createLexeme(LexemeType.Type.Operator, "<="));
            nextChar();
          } else {
            tokens.add(Lexeme.createLexeme(LexemeType.Type.Operator, "<"));
          }
          break;
        case '>':
          // handle > and >=
          if (peekChar() == '=') {
            tokens.add(Lexeme.createLexeme(LexemeType.Type.Operator, ">="));
            nextChar();
          } else {
            tokens.add(Lexeme.createLexeme(LexemeType.Type.Operator, ">"));
          }
          break;
        case '(':
          tokens.add(Lexeme.createLexeme(LexemeType.Type.LeftPar));
          break;
        case ')':
          tokens.add(Lexeme.createLexeme(LexemeType.Type.RightPar));
          break;
        case '{':
          tokens.add(Lexeme.createLexeme(LexemeType.Type.LeftCurlyBracket));
          break;
        case '}':
          tokens.add(Lexeme.createLexeme(LexemeType.Type.RightCurlyBracket));
          break;
        default:
          if (isAlpha(c)) {
            // identifier or keyword
            StringBuilder sb = new StringBuilder();
            sb.append(c);
            while (isAlpha(peekChar()) || isDigit(peekChar()) || isUnderscore(peekChar())) {
              sb.append(nextChar());
              if (sb.length() > MAX_IDENTIFIER_LENGTH) {
                throw new LexicalException("identifier too long");
              }
            }
            String identifier = sb.toString();
            if (isKeyword(identifier)) {
              tokens.add(Lexeme.createLexeme(LexemeType.Type.Keyword, identifier));
            } else {
              tokens.add(Lexeme.createLexeme(LexemeType.Type.Identifier, identifier));
            }
          } else if (isDigit(c)) {
            // integer
            StringBuilder sb = new StringBuilder();
            sb.append(c);
            while (isDigit(peekChar())) {
              sb.append(nextChar());
              if (sb.length() > MAX_INT_LENGTH) {
                throw new LexicalException("integer too long");
              }
            }
            tokens.add(Lexeme.createLexeme(LexemeType.Type.IntConstant, sb.toString()));
          } else if (isStringSymbol(c)) {
            // string
            StringBuilder sb = new StringBuilder();
            while (peekChar() != '"') {
              nextChar();
              if (getChar() == EOF) {
                throw new LexicalException("unclosed string");
              }
              sb.append(getChar());
            }
            tokens.add(Lexeme.createLexeme(LexemeType.Type.StringConstant, sb.toString()));
            nextChar();
          }
          // if none of the above, skip character
          break;
      }
      c = nextChar();
    }
    return tokens;
  }
}

class Lexeme {
  public LexemeType type;
  public String value;

  public Lexeme(LexemeType type, String value) {
    this.type = type;
    this.value = value;
  }

  public Lexeme(LexemeType type) {
    this(type, null);
  }

  public LexemeType getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public static Lexeme createLexeme(LexemeType.Type type) {
    return new Lexeme(new LexemeType(type));
  }

  public static Lexeme createLexeme(LexemeType.Type type, String value) {
    return new Lexeme(new LexemeType(type), value);
  }

  public String toString() {
    return type.toString() + (value != null ? (": " + value) : "");
  }
}

class LexemeType {
  public static enum Type {
    Keyword, Identifier, IntConstant, StringConstant, Operator, LeftPar, RightPar, LeftCurlyBracket, RightCurlyBracket,
    EndOfLine
  }

  private Type type;

  public LexemeType(Type type) {
    this.type = type;
  }

  public Type getType() {
    return type;
  }

  public String toString() {
    switch (type) {
      case Keyword:
        return "Keyword";
      case Identifier:
        return "Identifier";
      case IntConstant:
        return "IntConst";
      case StringConstant:
        return "StringConst";
      case Operator:
        return "Operator";
      case LeftPar:
        return "LeftPar";
      case RightPar:
        return "RightPar";
      case LeftCurlyBracket:
        return "LeftCurlyBracket";
      case RightCurlyBracket:
        return "RightCurlyBracket";
      case EndOfLine:
        return "EndOfLine";
      default:
        return "Unknown";
    }
  }
}

class LexicalException extends Exception {
  public LexicalException(String message) {
    super("lexical error: " + message);
  }
}
