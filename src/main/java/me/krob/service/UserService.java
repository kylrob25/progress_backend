package me.krob.service;

import com.mongodb.client.result.UpdateResult;
import me.krob.model.Role;
import me.krob.model.User;
import me.krob.repository.UserRepository;
import me.krob.util.MongoTemplateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private MongoTemplateUtil mongoUtil;

    @Autowired
    private UserRepository userRepository;

    public User create(User user) {
        return userRepository.save(user);
    }

    public User update(String userId, User user) {
        return userRepository.findById(userId)
                .map(t -> {
                    if (user.getForename() != null && !Objects.equals(user.getForename(), t.getForename())) {
                        t.setForename(user.getForename());
                    }
                    if (user.getSurname() != null && !Objects.equals(user.getSurname(), t.getSurname())) {
                        t.setSurname(user.getSurname());
                    }
                    if (user.getEmail() != null && !Objects.equals(user.getEmail(), t.getEmail())) {
                        t.setEmail(user.getEmail());
                    }
                    if (user.getPassword() != null && !Objects.equals(user.getPassword(), t.getPassword())) {
                        t.setPassword(user.getPassword());
                    }
                    return userRepository.save(t);
                })
                .orElseGet(() -> {
                    user.setId(userId);
                    return userRepository.save(user);
                });
    }

    public void delete(String userId) {
        userRepository.deleteById(userId);
    }

    public boolean exists(String userId) {
        return userRepository.existsById(userId);
    }

    public boolean existsByUsername(String username) { return userRepository.existsByUsername(username); }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public Optional<User> getById(String userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> getByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Roles
     **/

    public long addRole(String userId, Role role) {
        UpdateResult result = mongoUtil.addToSet(userId, "roles", role, User.class);
        return result.getModifiedCount();
    }

    public long removeRole(String userId, Role role) {
        UpdateResult result = mongoUtil.pull(userId, "roles", role, User.class);
        return result.getModifiedCount();
    }

    public Optional<Boolean> hasRole(String userId, Role role) {
        return userRepository.findById(userId).map(user -> user.getRoles().contains(role));
    }

    /**
     * Conversation
     **/
    public long addConversation(String userId, String conversationId) {
        UpdateResult result = mongoUtil.addToSet(userId, "conversationIds", conversationId, User.class);
        return result.getModifiedCount();
    }

    public long removeConversation(String userId, String conversationId) {
        UpdateResult result = mongoUtil.pull(userId, "conversationIds", conversationId, User.class);
        return result.getModifiedCount();
    }
}
