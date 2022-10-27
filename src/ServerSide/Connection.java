package ServerSide;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.apache.commons.cli.CommandLine;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Connection extends Thread implements Serializable {

    public static ObservableList<Connection> connections = FXCollections.observableArrayList();
    public static ObservableList<Topic> topics = FXCollections.observableArrayList();
    private StringProperty clientName = new SimpleStringProperty();
    private Socket client;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Topic own;
    private ArrayList<Topic> ownTopics;

    public Connection(Socket client, String clientName, DataInputStream dis, DataOutputStream dos) {
        this.client = client;
        this.clientName.set(clientName);
        this.dis = dis;
        this.dos = dos;
        this.ownTopics = new ArrayList<>();

        connections.add(this);

        topics.addListener((ListChangeListener<? super Topic>) observable -> {
            respond(String.format("TRQ:1001:%d:", topics.size()));

            for (Topic t : topics) {
                // If the user is subbed, it also checks if they're the topic's admin.
                if (t.getUserList().contains(this)) {
                    if (ownTopics.contains(t)) {
                        respond(String.format("%s (admin) (subbed)", t.getTopicTitle()));
                    } else {
                        respond(String.format("%s (subbed)", t.getTopicTitle()));
                    }
                    continue;
                }

                // If they aren't subbed, it just returns the name.
                respond(String.format("%s", t.getTopicTitle()));
            }
        });

        Topic broadcast = topics.stream()
                .filter(current -> "broadcast".equals(current.getTopicTitle()))
                .findAny()
                .orElse(null);

        // If broadcast hasn't been created, it's created and added to the topic list.
        if (broadcast == null) {
            broadcast = new Topic("broadcast");
            topics.add(broadcast);
        }

        broadcast.getUserList().add(this);
        broadcast.post("se ha conectado.", this, false, false);

        this.own = topics.stream()
                .filter(current -> clientName.equals(current.getTopicTitle()))
                .findAny()
                .orElse(null);

        // If the user's own topic doesn't exist, it's created and added to both topic lists.
        if (this.own == null) {
            this.own = new Topic(clientName);
            this.ownTopics.add(this.own);
            topics.add(this.own);
        }

        this.own.getUserList().add(this);
    }

    public void run() {
        try {
            boolean done = false;
            Command command = new Command();

            System.out.println("Cliente: " + connections.size());

            while (!done) {
                String cmd = dis.readUTF();

                if (!cmd.equals(".exit")) {
                    Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
                    Matcher matcher = regex.matcher(cmd);

                    ArrayList<String> args = new ArrayList<>();

                    while (matcher.find()) {
                        if (matcher.group(1) != null) {
                            args.add(matcher.group(1));
                        } else if (matcher.group(2) != null) {
                            args.add(matcher.group(2));
                        } else {
                            args.add(matcher.group());
                        }
                    }

                    CommandLine commandLine;
                    if (args.isEmpty())
                        continue;

                    switch (args.get(0)) {
                        case "send":
                            commandLine = command.parse(args.toArray(new String[0]));

                            if (commandLine != null) {
                                if (commandLine.hasOption("m")) {
                                    String message = commandLine.getOptionValue("m").trim();

                                    if (commandLine.hasOption("t")) {
                                        String topicTitle = commandLine.getOptionValue("t");

                                        Topic receiver = topics.stream()
                                                .filter(current -> topicTitle.equals(current.getTopicTitle()))
                                                .findAny().orElse(null);

                                        if (receiver != null && receiver.getUserList().contains(this)) {
                                            receiver.post(message, this, false, false);
                                            System.out.printf("<%s> to <%s>: %s\n", getClientName(),
                                                    topicTitle, message);
                                        } else if (receiver != null) {
                                            respond(String.format("No estás suscrito a '%s'.", topicTitle));
                                        } else {
                                            respond(String.format("El grupo '%s' no existe.", topicTitle));
                                        }

                                    } else {
                                        topics.stream()
                                                .filter(current -> "broadcast".equals(current.getTopicTitle()))
                                                .findAny().ifPresent(receiver -> receiver.post(message,
                                                this, false, false));

                                        System.out.printf("<%s> to <%s>: %s\n", getClientName(), "broadcast",
                                                message);
                                    }
                                }
                            }
                            break;
                        case "create":
                            commandLine = command.parse(args.toArray(new String[0]));

                            if (commandLine != null) {
                                if (commandLine.hasOption("t")) {
                                    String topicTitle = commandLine.getOptionValue("t");

                                    Topic requested = topics.stream()
                                            .filter(current -> topicTitle.equals(current.getTopicTitle()))
                                            .findAny()
                                            .orElse(null);

                                    if (requested != null) {
                                        respond(String.format("GAE:1007:%s:", topicTitle)); // Group Already Exists
                                    } else {
                                        requested = new Topic(topicTitle);
                                        requested.getUserList().add(this);

                                        ownTopics.add(requested);
                                        topics.add(requested);

                                        respond(String.format("CG:1006:%s:", topicTitle)); // Created Group
                                        respond(String.format("STG:1002:%s:", topicTitle)); // Subbed To Group

                                        System.out.printf("<%s> created <%s>\n", getClientName(), topicTitle);
                                    }
                                }
                            }
                            break;
                        case "sub":
                            commandLine = command.parse(args.toArray(new String[0]));

                            if (commandLine != null) {
                                if (commandLine.hasOption("t")) {
                                    String topicTitle = commandLine.getOptionValue("t");

                                    Topic requested = topics.stream()
                                            .filter(current -> topicTitle.equals(current.getTopicTitle()))
                                            .findAny()
                                            .orElse(null);

                                    if (requested != null) {
                                        if (!requested.getUserList().contains(this)) {
                                            requested.getUserList().add(this);
                                            requested.post("se ha unido al grupo.",
                                                    this, false, false);

                                            respond(String.format("STG:1002:%s:", topicTitle)); // Subbed To Group

                                            System.out.printf("<%s> subbed to <%s>\n", getClientName(), topicTitle);
                                        } else {
                                            respond(String.format("JG:1003:%s:", topicTitle)); // Joined Group
                                        }
                                    } else {
                                        respond(String.format("GDNE:1004:%s:", topicTitle)); // Group Does Not Exist
                                    }
                                }
                            }
                            break;
                        case "unsub":
                            commandLine = command.parse(args.toArray(new String[0]));

                            if (commandLine != null) {
                                if (commandLine.hasOption("t")) {
                                    String topicTitle = commandLine.getOptionValue("t");

                                    Topic requested = topics.stream()
                                            .filter(current -> topicTitle.equals(current.getTopicTitle()))
                                            .findAny()
                                            .orElse(null);

                                    if (requested != null) {
                                        if (requested == this.own || this.ownTopics.contains(requested)) {
                                            respond(String.format("No puedes desuscribirte de '%s'.",
                                                    topicTitle));
                                        } else if (requested.getUserList().contains(this)) {
                                            requested.getUserList().remove(this);

                                            respond(String.format("UFG:1009:%s", topicTitle)); // Unsubbed From Group
                                            requested.post("abandonó el grupo.",
                                                    this, false, true);

                                            System.out.printf("<%s> unsubbed from <%s>\n", getClientName(),
                                                    topicTitle);
                                        } else {
                                            respond(String.format("NSTG:1010:%s", topicTitle)); // Not Subbed To Group
                                        }
                                    } else {
                                        respond(String.format("GDNE:1004:%s", topicTitle)); // Group Does Not Exist
                                    }
                                }
                            }
                            break;
                        case "remove":
                            commandLine = command.parse(args.toArray(new String[0]));

                            if (commandLine != null) {
                                if (commandLine.hasOption("t")) {
                                    String topicTitle = commandLine.getOptionValue("t");

                                    if (topicTitle.equals("broadcast")) {
                                        respond(String.format("No tienes permisos para eliminar '%s'.", topicTitle));
                                    } else {
                                        Topic requested = topics.stream()
                                                .filter(current -> topicTitle.equals(current.getTopicTitle()))
                                                .findAny()
                                                .orElse(null);

                                        if (requested != null) {
                                            if (requested != this.own && this.ownTopics.contains(requested)) {
                                                requested.post(String.format("El grupo '%s' fue eliminado.",
                                                        topicTitle), this, false, true);

                                                requested.post(String.format("RG:1005:%s", topicTitle),
                                                        this, false, true);
                                                respond(String.format("RG:1005:%s", topicTitle));

                                                requested.getUserList().clear();
                                                topics.remove(requested);

                                                System.out.printf("<%s> removed <%s>\n", getClientName(), topicTitle);
                                            } else {
                                                respond(String.format("UTRG:1008:%s",
                                                        topicTitle)); // Unable To Remove Group
                                            }
                                        } else {
                                            respond(String.format("GDNE:1004:%s", topicTitle)); // Group Does Not Exist
                                        }
                                    }
                                }
                            }
                            break;
                        case "topic":
                            commandLine = command.parse(args.toArray(new String[0]));

                            if (commandLine != null) {
                                if (commandLine.hasOption("l")) {
                                    System.out.printf("<%s> requested all topics\n", getClientName());
                                    respond(String.format("TRQ:1001:%d:", topics.size()));

                                    // Returns all topics and classifies them accordingly.
                                    for (Topic t : topics) {

                                        // If the user is subbed, it also checks if they're the topic's admin.
                                        if (t.getUserList().contains(this)) {
                                            if (ownTopics.contains(t)) {
                                                respond(String.format("%s (admin) (subbed)", t.getTopicTitle()));
                                            } else {
                                                respond(String.format("%s (subbed)", t.getTopicTitle()));
                                            }
                                            continue;
                                        }

                                        // If they aren't subbed, it just returns the name.
                                        respond(String.format("%s", t.getTopicTitle()));
                                    }
                                } else if (commandLine.hasOption("u")) {
                                    if (commandLine.hasOption("t")) {
                                        String topicTitle = commandLine.getOptionValue("t");
                                        System.out.printf("<%s> requested all users subbed to <%s>\n", getClientName(), topicTitle);

                                        Topic requested = topics.stream()
                                                .filter(current -> topicTitle.equals(current.getTopicTitle()))
                                                .findAny().orElse(null);

                                        if (requested != null) {
                                            for (int i = 0; i < requested.getUserList().size(); i++) {
                                                respond(requested.getUserList().get(i).getClientName());
                                            }
                                        } else {
                                            respond("El grupo solicitado no existe.");
                                        }
                                    } else {
                                        respond("El comando requiere el nombre de un grupo existente.");
                                    }
                                }
                            }
                            break;
                        default:
                            respond("Comando inválido.");
                    }
                } else {
                    respond("Te has desconectado.");
                    System.out.printf("<%s> disconnected\n", getClientName());
                    respond("UDFS:1000");

                    for (Topic t : ownTopics) {
                        topics.remove(t);
                    }
                    connections.remove(this);
                    done = true;

                    disconnect();
                    if (topics.size() > 1) {
                        for (Topic topic : topics) {
                            if (topic.getUserList().contains(this)) {
                                topic.post("se ha desconectado.", this, false, false);
                                topic.getUserList().remove(this);
                            }
                        }
                    } else if (topics.size() == 1) {
                        topics.clear();
                    }
                }
            }
        } catch (IOException ioEx) {
            System.out.printf("Ha ocurrido un error de conexión. El cliente %s será desconectado.", getClientName());
        }
    }

    // Sends a message to the topic. Can be from the server or the sender.
    public void send(String message, Topic topic, Connection source, boolean fromServer) {
        try {
            String src = fromServer ? "Servidor" : source.getClientName();
            this.dos.writeUTF(String.format("%s:%s: %s", topic.getTopicTitle(), src, message));
            this.dos.flush();
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }

    // Disconnect client.
    private void disconnect() throws IOException {
        try {
            dos.close();
        } finally {
            try {
                dis.close();
            } finally {
                client.close();
            }
        }
    }

    // A response from the server to the sender.
    private void respond(String message) {
        topics.stream()
                .filter(current -> getClientName().equals(current.getTopicTitle()))
                .findAny()
                .ifPresent(requester -> requester.post(message,
                        this,  true, true));
    }

    public String getClientName() {
        return this.clientName.get();
    }


}
