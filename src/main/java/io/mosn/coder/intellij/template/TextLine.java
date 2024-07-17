package io.mosn.coder.intellij.template;

/**
 * @author yiji@apache.org
 */
public class TextLine {

    /**
     * continue notify ReplaceAction
     */
    public static final TextLine Next = new TextLine(true);

    /**
     * stop notify ReplaceAction
     */
    public static final TextLine Terminate = new TextLine(false);

    private boolean next;

    private String line;

    private TextLine(boolean next) {
        this.next = next;
    }

    private TextLine(String line, boolean next) {
        this.next = next;
        this.line = line;
    }

    public TextLine with(String line) {
        return new TextLine(line, this.next);
    }

    public String text() {
        return this.line;
    }

    public boolean isTerminate() {
        return !this.next;
    }
}
