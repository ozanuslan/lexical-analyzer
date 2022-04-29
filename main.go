package main

import (
	"errors"
	"fmt"
)

type TokenType int

const (
	// Identifiers
	Identifier TokenType = iota
	// String constants
	StringConstant
	// Integer constants
	IntConstant
	// Keywords
	Keyword
	// Operators
	Operator
	// Brackets
	LeftPar
	RightPar
	LeftCurlyBracket
	RightCurlyBracket
	// End of line
	EndOfLine
)

const (
	MAX_IDENTIFIER_LENGTH = 25
	MAX_INT_LENGTH        = 10
)

type Token struct {
	Type  TokenType
	Value string
}

func (t *Token) String() string {
	str := "Token{Type: " + t.Type.String()
	if t.Value != "" {
		str += ", Value: " + t.Value
	}
	str += "}"

	return str
}

var keywords = [17]string{"break", "case", "char", "const", "do", "else", "enum", "float", "for", "if", "int", "double", "long", "struct", "return", "static", "while"}

func isKeyword(input string) bool {
	for _, keyword := range keywords {
		if keyword == input {
			return true
		}
	}
	return false
}

func (t TokenType) String() string {
	switch t {
	case Identifier:
		return "Identifier"
	case StringConstant:
		return "StringConstant"
	case IntConstant:
		return "IntConstant"
	case Keyword:
		return "Keyword"
	case Operator:
		return "Operator"
	case LeftPar:
		return "LeftPar"
	case RightPar:
		return "RightPar"
	case LeftCurlyBracket:
		return "LeftCurlyBracket"
	case RightCurlyBracket:
		return "RightCurlyBracket"
	case EndOfLine:
		return "EndOfLine"
	default:
		return "Unknown"
	}
}

func isDigit(c byte) bool {
	return c >= '0' && c <= '9'
}

func isAlpha(c byte) bool {
	return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
}

func isEndOfLine(c byte) bool {
	return c == ';'
}

func isStringSymbol(c byte) bool {
	return c == '"'
}

func isUnderscore(c byte) bool {
	return c == '_'
}

func isNewLine(c byte) bool {
	return c == '\n'
}

func lexicalError(message string, line int) error {
	return errors.New("lexical error: " + message + " [line: " + fmt.Sprint(line) + "]")
}

func withinRange(input string, index int) bool {
	return index >= 0 && index < len(input)
}

func lex(input string) ([]Token, error) {
	tokens := []Token{}
	line := 1
	for i := 0; i < len(input); i++ {
		c := input[i]
		switch {
		case isEndOfLine(c):
			tokens = append(tokens, Token{Type: EndOfLine})
		case c == '+':
			// handle + and ++
			if withinRange(input, i+1) && input[i+1] == '+' {
				tokens = append(tokens, Token{Type: Operator, Value: "++"})
				i++
			} else {
				tokens = append(tokens, Token{Type: Operator, Value: "+"})
			}
		case c == '-':
			// handle - and --
			if withinRange(input, i+1) && input[i+1] == '-' {
				tokens = append(tokens, Token{Type: Operator, Value: "--"})
				i++
			} else {
				tokens = append(tokens, Token{Type: Operator, Value: "-"})
			}
		case c == '*':
			tokens = append(tokens, Token{Type: Operator, Value: "*"})
		case c == '/':
			// handles / operator, /* */ comments and // comments
			if withinRange(input, i+1) && input[i+1] == '*' { // Multiline comment
				i += 2 // skip "/*"
				for withinRange(input, i+1) && input[i] != '*' && input[i+1] != '/' {
					if input[i] == '\n' {
						line++
					}
					i++
				}
				i += 1 // skip "*/"
				if !withinRange(input, i) {
					return nil, lexicalError("multiline comment not terminated", line)
				}
			} else if withinRange(input, i+1) && input[i+1] == '/' { // Single line comment
				for withinRange(input, i) && !isNewLine(input[i]) {
					i++
				}
				line++
			} else { // "/" operator
				tokens = append(tokens, Token{Type: Operator, Value: "/"})
			}
		case c == '=':
			// handle = and ==
			if withinRange(input, i+1) && input[i+1] == '=' {
				tokens = append(tokens, Token{Type: Operator, Value: "=="})
				i++
			} else {
				tokens = append(tokens, Token{Type: Operator, Value: "="})
			}
		case c == '<':
			// handle < and <=
			if withinRange(input, i+1) && input[i+1] == '=' {
				tokens = append(tokens, Token{Type: Operator, Value: "<="})
				i++
			} else {
				tokens = append(tokens, Token{Type: Operator, Value: "<"})
			}
		case c == '>':
			// handle > and >=
			if withinRange(input, i+1) && input[i+1] == '=' {
				tokens = append(tokens, Token{Type: Operator, Value: ">="})
				i++
			} else {
				tokens = append(tokens, Token{Type: Operator, Value: ">"})
			}
		case c == '(':
			tokens = append(tokens, Token{Type: LeftPar, Value: "("})
		case c == ')':
			tokens = append(tokens, Token{Type: RightPar, Value: ")"})
		case c == '{':
			tokens = append(tokens, Token{Type: LeftCurlyBracket, Value: "{"})
		case c == '}':
			tokens = append(tokens, Token{Type: RightCurlyBracket, Value: "}"})
		case isAlpha(c) || isUnderscore(c): // Identifier
			start := i
			for withinRange(input, i) && (isAlpha(input[i]) || isDigit(input[i]) || isUnderscore(input[i])) {
				i++
			}
			if (i - start) > MAX_IDENTIFIER_LENGTH {
				return nil, lexicalError("identifier too long", line)
			}
			i-- // go back one step to offset the for loop
			slice := input[start : i+1]
			if isKeyword(slice) {
				tokens = append(tokens, Token{Type: Keyword, Value: slice})
			} else {
				tokens = append(tokens, Token{Type: Identifier, Value: slice})
			}
		case isDigit(c): // Int Constant
			start := i
			for withinRange(input, i) && isDigit(input[i]) {
				i++
			}
			if i-start > MAX_INT_LENGTH {
				return nil, lexicalError("int constant too long", line)
			}
			tokens = append(tokens, Token{Type: IntConstant, Value: input[start:i]})
			i-- // offset overstepping
		case isStringSymbol(c): // String constant
			start := i
			i++
			for withinRange(input, i) && !isStringSymbol(input[i]) {
				if input[i] == '\n' {
					line++
				}
				i++
			}
			if !withinRange(input, i) {
				return nil, lexicalError("string constant not terminated", line)
			}
			tokens = append(tokens, Token{Type: StringConstant, Value: input[start+1 : i]})
		case isNewLine(c):
			line++
		default:
			// ignore
		}
	}
	return tokens, nil
}

func main() {
	input := "const int number=asdfgasdfgasdfgasdfgasdfgasdfg+\"dsa\"+number_2;//comment\na=123sad; a<=b;/*sadghaskjh\ndgasjkhdgsa\n\n*/;;;char c//dsadasghjd\n /*\n\n\n\n\ndasdsad*/"
	lexemes, err := lex(input)

	if err != nil {
		fmt.Println(err)
	} else {
		for _, lexeme := range lexemes {
			fmt.Println(lexeme.String())
		}
	}
}
