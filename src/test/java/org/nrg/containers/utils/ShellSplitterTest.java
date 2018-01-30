package org.nrg.containers.utils;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ShellSplitterTest {
    @Test
    public void blankYieldsEmptyArgs() {
        assertThat(ShellSplitter.shellSplit(""), is(Matchers.<String>empty()));
    }

    @Test
    public void whitespacesOnlyYieldsEmptyArgs() {
        assertThat(ShellSplitter.shellSplit("  \t \n"), is(Matchers.<String>empty()));
    }

    @Test
    public void normalTokens() {
        assertThat(ShellSplitter.shellSplit("a\tbee  cee"), is(equalTo(Arrays.asList("a", "bee", "cee"))));
    }

    @Test
    public void doubleQuotes() {
        assertThat(ShellSplitter.shellSplit("\"hello world\""), is(equalTo(Collections.singletonList("hello world"))));
    }

    @Test
    public void singleQuotes() {
        assertThat(ShellSplitter.shellSplit("'hello world'"), is(equalTo(Collections.singletonList("hello world"))));
    }


    @Test
    public void escapedDoubleQuotes() {
        assertThat(ShellSplitter.shellSplit("\"\\\"hello world\\\""), is(equalTo(Collections.singletonList("\"hello world\""))));
    }

    @Test
    public void noEscapeWithinSingleQuotes() {
        assertThat(ShellSplitter.shellSplit("'hello \\\" world'"), is(equalTo(Collections.singletonList("hello \\\" world"))));
    }

    @Test
    public void backToBackQuotedStringsShouldFormSingleToken() {
        assertThat(ShellSplitter.shellSplit("\"foo\"'bar'baz"), is(equalTo(Collections.singletonList("foobarbaz"))));
        assertThat(ShellSplitter.shellSplit("\"three\"' 'four"), is(equalTo(Collections.singletonList("three four"))));
    }

    @Test
    public void escapedSpacesDoNotBreakUpTokens() {
        assertThat(ShellSplitter.shellSplit("three\\ four"), is(equalTo(Collections.singletonList("three four"))));
    }
}