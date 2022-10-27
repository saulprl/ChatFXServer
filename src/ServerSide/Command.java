package ServerSide;

import org.apache.commons.cli.*;

public class Command {

    public Options options;

    public Command() {
        Option message = Option.builder("m")
                .desc("This argument refers to the message body.")
                .argName("messageBody")
                .required(false)
                .hasArg()
                .build();

        Option file = Option.builder("f")
                .desc("This argument refers to a file name.")
                .argName("fileName")
                .required(false)
                .hasArg()
                .build();

        Option topic = Option.builder("t")
                .desc("This argument refers to the topic name.")
                .argName("topicName")
                .required(false)
                .hasArg()
                .build();

        Option list = Option.builder("l")
                .desc("This argument allows you to list all existing topics.")
                .required(false)
                .build();

        Option listUsers = Option.builder("u")
                .desc("This argument allows you to list all subscribers on a topic.")
                .required(false)
                .build();


        options = new Options();
        options.addOption(message);
        options.addOption(file);
        options.addOption(topic);
        options.addOption(list);
        options.addOption(listUsers);
    }

    public CommandLine parse(String[] args) {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;

        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException pEx) {
            pEx.printStackTrace();
        }

        return commandLine;
    }

}
