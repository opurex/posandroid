package com.opurex.client.data.DataSavable;

import com.opurex.client.models.User;
import com.opurex.client.utils.exception.DataCorruptedException;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by nsvir on 12/10/15.
 * n.svirchevsky@gmail.com
 */
public class UserDataTest extends AbstractDataTest {

    UserData userData;
    List<User> userList;
    User[] defaultUsers = {
            new User("id", "name", "password", "permission"),
            new User("3456-345676-34567-34567", "charles", "azerty", "null")
    };

    @Override
    public String getTmpFilename() {
        return "user.json";
    }

    @Override
    public void setup() throws IOException {
        super.setup();
        userList = new ArrayList<>();
        for (User user : defaultUsers) {
            userList.add(user);
        }
    }

    @Test
    public void saveTest() throws DataCorruptedException, FileNotFoundException {
        replayContext();
        userData = new UserData();
        userData.setFile(createDefaultTmpFile());
        userData.setUsers(userList);
        userData.save();
        userData.setUsers(new ArrayList<User>());
        userData.load();
        List<User> test = userData.users(fakeContext);
        assertEquals(userList, test);
    }
}