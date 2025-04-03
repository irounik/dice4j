package com.irounik.dice4j.io;

import com.google.protobuf.InvalidProtocolBufferException;
import com.irounik.dice4j.wire.Cmd.Command;
import com.irounik.dice4j.wire.Cmd.Response;

import java.io.*;
import java.net.Socket;

public class IronhawkIO {
    private static final int MAX_REQUEST_SIZE = 32 * 1024 * 1024; // 32 MB
    private static final int IO_BUFFER_SIZE = 16 * 1024; // 16 KB

    public static Response read(Socket socket) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        InputStream inputStream = socket.getInputStream();
        byte[] buffer = new byte[IO_BUFFER_SIZE];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            if (result.size() + bytesRead > MAX_REQUEST_SIZE) {
                throw new IOException("Request too large");
            }
            result.write(buffer, 0, bytesRead);
            if (bytesRead < buffer.length) {
                break;
            }
        }

        if (result.size() == 0) {
            throw new EOFException();
        }

        try {
            return Response.parseFrom(result.toByteArray());
        } catch (InvalidProtocolBufferException e) {
            throw new IOException("Failed to unmarshal response", e);
        }
    }

    public static void write(Socket socket, Command command) throws IOException {
        byte[] data = command.toByteArray();
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(data);
        outputStream.flush();
    }
}
