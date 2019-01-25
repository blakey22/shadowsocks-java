/*
 * Copyright (c) 2015, Blake
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The name of the author may not be used to endorse or promote
 * products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package cc.springcloud.socks.misc;

import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper class
 */
public class Util {

    public static String prettyPrintJson(JSONObject json) {
        StringWriter writer = new StringWriter() {
            private final static String indent = "  ";
            private final String LINE_SEP = System.getProperty("line.separator");
            private int indentLevel = 0;

            @Override
            public void write(int c) {
                char ch = (char) c;
                if (ch == '[' || ch == '{') {
                    super.write(c);
                    super.write(LINE_SEP);
                    indentLevel++;
                    writeIndentation();
                }
                else if (ch == ']' || ch == '}') {
                    super.write(LINE_SEP);
                    indentLevel--;
                    writeIndentation();
                    super.write(c);
                }
                else if (ch == ':') {
                    super.write(c);
                    super.write(" ");
                }
                else if (ch == ',') {
                    super.write(c);
                    super.write(LINE_SEP);
                    writeIndentation();
                }
                else {
                    super.write(c);
                }

            }
            private void writeIndentation()
            {
                for (int i = 0; i < indentLevel; i++)
                {
                    super.write(indent);
                }
            }
        };
        json.write(writer);
        return writer.toString();
    }

    public static boolean saveFile(String fn, String content) {
        PrintWriter writer;
        try {
            writer = new PrintWriter(fn);
            writer.println(content);
            writer.close();
        } catch (FileNotFoundException e) {
            return false;
        }
        return true;
    }

    public static String getFileContent(String fn) {
        Path path = Paths.get(fn);
        try {
            return new String(Files.readAllBytes(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
