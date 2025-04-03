package com.irounik.dice4j;

import com.irounik.dice4j.io.IronhawkIO;
import com.irounik.dice4j.wire.Cmd.Command;
import com.irounik.dice4j.wire.Cmd.Response;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DiceClient implements AutoCloseable {
    private final String id;
    private final Socket conn;
    private Socket watchConn;
    private BlockingQueue<Response> watchQueue;
    private final String host;
    private final int port;

    public DiceClient(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.conn = new Socket(host, port);
        this.id = UUID.randomUUID().toString();
        handshake("command", conn);
    }

    private void handshake(String mode, Socket connection) throws IOException {
        Command handshakeCmd = Command.newBuilder()
                .setCmd("HANDSHAKE")
                .addArgs(id)
                .addArgs(mode)
                .build();
        Response resp = fire(handshakeCmd, connection);
        if (!resp.getErr().isEmpty()) {
            throw new IOException("Could not complete the handshake: " + resp.getErr());
        }
    }

    public Response fire(Command cmd) throws IOException {
        return fire(cmd, conn);
    }

    private Response fire(Command cmd, Socket socket) throws IOException {
        IronhawkIO.write(socket, cmd);
        return IronhawkIO.read(socket);
    }

    public Response fireString(String cmdStr) throws IOException {
        Optional<Command> command = buildCmd(cmdStr);
        if (command.isEmpty()) {
            return Response.newBuilder()
                    .setErr("Invalid arguments!")
                    .build();
        }

        return fire(command.get());
    }

    public static Optional<Command> buildCmd(String cmdStr) {
        String[] tokens = cmdStr.trim().split(" ");
        if (tokens.length == 0) {
            return Optional.empty();
        }
        String cmd = tokens[0].toUpperCase();
        List<String> args = tokens.length > 1 ? Arrays.asList(tokens).subList(1, tokens.length) : List.of();
        return Optional.of(Command.newBuilder()
                .setCmd(cmd)
                .addAllArgs(args)
                .build());
    }

    public BlockingQueue<Response> watchCh() throws IOException {
        if (watchQueue != null) {
            return watchQueue;
        }
        watchQueue = new LinkedBlockingQueue<>();
        watchConn = new Socket(host, port);
        handshake("watch", watchConn);
        new Thread(this::watch).start();
        return watchQueue;
    }

    private void watch() {
        System.out.println("Entering watch mode!");
        try {
            while (true) {
                Response resp = IronhawkIO.read(watchConn);
                watchQueue.put(resp);
                if (!resp.getErr().isEmpty()) {
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        conn.close();
        if (watchConn != null) {
            watchConn.close();
        }
    }

}
