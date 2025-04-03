package com.irounik.dice4j;

import com.irounik.dice4j.wire.Cmd;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

public class DiceCli {

    private DiceClient client;

    public DiceCli(String host, Integer port) {
        try {
            this.client = new DiceClient(host, port);
            Runtime.getRuntime().addShutdownHook(new Thread(this::exit));
        } catch (IOException exception) {
            System.err.println("Unable to establish connection to DiceDB");
        }
    }


    private void exit() {
        System.out.println("Exiting!");
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Cleanup complete. Exiting.");
    }

    void start() {
        try {
            Scanner sc = new Scanner(System.in);
            while (true) {
                String input = sc.nextLine();
                if (input.equals("exit")) {
                    exit();
                    return;
                }

                Cmd.Response response = client.fireString(input);
                boolean shouldWatch = DiceClient.buildCmd(input)
                        .map(Cmd.Command::getCmd)
                        .map(String::toUpperCase)
                        .map(it -> it.endsWith(".WATCH"))
                        .orElse(false);

                if (shouldWatch) {
                    BlockingQueue<Cmd.Response> responses = client.watchCh();
                    processResponses(responses);
                }
                System.out.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processResponses(BlockingQueue<Cmd.Response> responses) {
        while (true) {
            Cmd.Response res;
            try {
                res = responses.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println(res);
            if (!res.getErr().isEmpty()) {
                break;
            }
        }
    }

    public static void main(String[] args) {
        DiceCli diceCli = new DiceCli("localhost", 7379);
        diceCli.start();
    }

}