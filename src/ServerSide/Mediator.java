package ServerSide;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public abstract class Mediator {

    ServerSocket serverSocket;
    int port;
    static ArrayList<Connection> connections = new ArrayList<>();

    public void init() {
        Socket socket;

        try {
            serverSocket = new ServerSocket(port);

            while (true) {
                socket = serverSocket.accept();
                boolean logged = true;
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());
//                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                String clientName = dis.readUTF();

                for (Connection con : connections) {
                    if (con.getClientName().equals(clientName)) {
                        logged = false;
                        break;
                    }
                }

                if (logged) {
                    dos.writeUTF(String.format("Bienvenido, %s.", clientName));
                    System.out.println("Cliente conectado.");
                    dos.flush();

                    Connection connection = new Connection(socket, clientName, dis, dos);
                    connection.start();
                    connections.add(connection);

                    Thread joinConnection = new Thread(() -> {
                        try {
                            connection.join();
                            connections.remove(connection);
                        } catch (InterruptedException intEx) {
                            intEx.printStackTrace();
                        }
                    });

                    joinConnection.start();
                } else {
                    dos.writeUTF(String.format("Ya existe un usuario con el nombre '%s'.", clientName));
                    dos.flush();
                }
            }
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }

}
