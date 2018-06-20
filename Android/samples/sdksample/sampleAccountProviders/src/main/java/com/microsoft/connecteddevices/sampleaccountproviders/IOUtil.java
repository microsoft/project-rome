//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.sampleaccountproviders;

import android.support.annotation.Keep;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

@Keep
public final class IOUtil {

    /**
     * Writes UTF-8 output data to an output stream.
     * This method is synchronous, and should only be used on small data sizes.
     *
     * @param stream Stream to write data to
     * @param data Data to write
     * @throws IOException Thrown if the output stream is unavailable, or encoding the data fails
     */
    public static void writeUTF8Stream(OutputStream stream, String data) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"))) {
            writer.write(data);
        }
    }

    /**
     * Reads the contents of a UTF-8 input stream.
     * This method is synchronous, and should only be used on small data sizes.
     *
     * @param stream Input stream to read from
     * @return All data received from the stream
     * @throws IOException Thrown if the input stream is unavailable, or decoding the data fails
     */
    public static String readUTF8Stream(InputStream stream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }
}