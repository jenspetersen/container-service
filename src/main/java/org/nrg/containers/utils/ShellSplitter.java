package org.nrg.containers.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Intended to split a string into tokens the way a shell would.
 * Taken from https://gist.github.com/raymyers/8077031
 *
 * For use cases, see tests in org.nrg.containers.utils.ShellSplitterTest.
 */
public class ShellSplitter {
    public static List<String> shellSplit(CharSequence string) {
        final List<String> tokens = new ArrayList<>();

        boolean escaping = false;
        char quoteChar = ' ';
        boolean quoting = false;
        StringBuilder current = new StringBuilder() ;
        for (int i = 0; i<string.length(); i++) {
            char c = string.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
            } else if (c == '\\' && !(quoting && quoteChar == '\'')) {
                escaping = true;
            } else if (quoting && c == quoteChar) {
                quoting = false;
            } else if (!quoting && (c == '\'' || c == '"')) {
                quoting = true;
                quoteChar = c;
            } else if (!quoting && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }
}