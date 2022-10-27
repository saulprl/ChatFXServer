package ServerSide;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import java.util.ArrayList;

public class Topic {

    public ListProperty subbed = new SimpleListProperty();
    public StringProperty topicTitle = new SimpleStringProperty();
    public ArrayList<Connection> users = new ArrayList<>();

    public Topic() {

    }

    public Topic(String topicTitle) {
        this.topicTitle.set(topicTitle);
    }

    public ArrayList<Connection> getUserList() {
        return this.users;
    }

    public void addUser(Connection connection) {
        this.users.add(connection);
        subbed.set(null);
        subbed.set(FXCollections.observableArrayList(this.users));
    }

    public void setUserList(ArrayList<Connection> users) {
        this.users = users;
        subbed.set(null);
        subbed.set(FXCollections.observableArrayList(this.users));
    }

    public String getTopicTitle() {
        return topicTitle.get();
    }

    public void setTopicTitle(String topicTitle) {
        this.topicTitle.set(topicTitle);
    }

    public void post(String message, Connection source, boolean response, boolean fromServer) {
        for (int i = 0; i < users.size(); i++) {
            if (response) {
                if (i == users.indexOf(source))
                    users.get(i).send(message,this, source, fromServer);
            } else if (i != users.indexOf(source)) {
                users.get(i).send(message, this, source, fromServer);
            }
        }
    }

}
