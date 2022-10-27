package ServerSide;

public class ChatMain extends Mediator {

    public ChatMain(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        ChatMain chat = new ChatMain(9000);
        chat.init();
    }

}
