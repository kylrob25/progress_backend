package me.krob.controller;

import me.krob.model.User;
import me.krob.model.message.Conversation;
import me.krob.model.message.Message;
import me.krob.service.ConversationService;
import me.krob.service.MessageService;
import me.krob.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600)
@RestController
@RequestMapping("/api/conversation")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<Conversation> create(@RequestBody Conversation conversation) {
        userService.getById(conversation.getCreatorId()).ifPresent(user -> {
            conversation.setCreatorId(user.getId());
            conversation.setTitle(user.getUsername() + "'s Conversation");
        });
        return ResponseEntity.ok(conversationService.create(conversation));
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<?> delete(@PathVariable String conversationId) {
        conversationService.getById(conversationId).ifPresent(conversation -> {
            conversation.getMessageIds()
                    .forEach(messageService::deleteById);
            conversation.getParticipantIds()
                    .forEach(userId -> userService.removeConversation(userId, conversationId));
        });
        conversationService.deleteById(conversationId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("{conversationId}/add")
    public ResponseEntity<?> addParticipant(@PathVariable String conversationId, @RequestBody String username){
        return null;
    }


    @GetMapping
    public List<Conversation> getAll() {
        return conversationService.getAll();
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<Conversation> getConversation(@PathVariable String conversationId){
        return conversationService.getById(conversationId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<Set<Message>> getMessages(@PathVariable String conversationId){
        return conversationService.getById(conversationId)
                .map(Conversation::getMessageIds)
                .map(messageIds -> messageIds.stream()
                        .map(messageService::getById)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet())
                )
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Set<Conversation>> getConversationsByUser(@PathVariable String userId) {
        return userService.getById(userId)
                .map(User::getConversationIds)
                .map(conversationService::getUserConversations)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
