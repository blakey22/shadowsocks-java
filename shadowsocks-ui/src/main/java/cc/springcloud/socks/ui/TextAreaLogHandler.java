package cc.springcloud.socks.ui;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class TextAreaLogHandler extends StreamHandler {
    TextArea textArea = null;

    public void setTextArea(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void publish(LogRecord record) {
        final LogRecord lg = record;
        super.publish(record);
        flush();

        if (textArea != null) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    // limited log size to 64k
                    if (textArea.getText().length() > 65535) {
                        textArea.clear();
                    }
                    textArea.appendText(getFormatter().format(lg));
                }
            });
        }
    }
}
