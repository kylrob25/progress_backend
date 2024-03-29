package me.krob.controller;

import me.krob.model.Role;
import me.krob.model.auth.AuthResponse;
import me.krob.model.client.Client;
import me.krob.model.Payment;
import me.krob.model.client.ClientUpdate;
import me.krob.service.ClientService;
import me.krob.service.PaymentService;
import me.krob.service.TrainerService;
import me.krob.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600)
@RestController
@RequestMapping("/api/client")
public class ClientController {

    @Autowired
    private TrainerService trainerService;

    @Autowired
    private UserService userService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private PaymentService paymentService;

    @Deprecated
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Client client) {
        String userId = client.getUserId();

        if (userId == null || !userService.exists(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AuthResponse("Cannot find User entity with that ID."));
        }

        if (clientService.existsByUserId(userId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthResponse("Client entity with that ID already exists."));
        }

        String trainerId = client.getTrainerId();

        if (trainerId == null || !trainerService.exists(trainerId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AuthResponse("Cannot find Trainer entity with that ID."));
        }

        Client created = clientService.create(client);

        return trainerService.hasClientId(trainerId, client.getId()).map(has -> {
            if (has) {
                clientService.deleteById(created.getId());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthResponse("Client entity with that ID already exists."));
            }

            trainerService.addClientId(trainerId, client.getId());
            userService.addRole(userId, Role.CLIENT);
            return ResponseEntity.ok(created);
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse("Issue with Trainer entity.")));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<?> getClient(@PathVariable String clientId) {
        return clientService.getById(clientId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/userid/{userId}")
    public ResponseEntity<Client> getClientByUserId(@PathVariable String userId) {
        return clientService.getByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{clientId}/update")
    public ResponseEntity<?> updateClientDetails(@PathVariable String clientId, @RequestBody ClientUpdate clientUpdate) {
        if (!clientService.exists(clientId)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AuthResponse("Cannot find Client entity with that ID."));
        }

        long calories = clientService.updateCalories(clientId, clientUpdate.getCalories());
        long weight = clientService.updateWeight(clientId, clientUpdate.getWeight());

        if (calories < 1 && weight < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse("No updates were made to the Client entity."));
        }

        return ResponseEntity.ok(new AuthResponse("Updated Client entity."));
    }

    @PutMapping("/{clientId}/payments")
    public ResponseEntity<Payment> createPayment(@PathVariable String clientId, @RequestBody Payment payment) {
        return clientService.getById(clientId).map(client -> {
            Payment created = paymentService.create(payment);
            trainerService.addPaymentId(created.getTrainerId(), created.getId());
            clientService.addPaymentId(clientId, created.getId());
            return ResponseEntity.ok(created);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{clientId}/payments/{paymentId}")
    public ResponseEntity<?> completePayment(@PathVariable String clientId, @PathVariable String paymentId) {
        // TODO: Handle payments that are already completed
        return clientService.getById(clientId).map(client -> {
                if (client.getPaymentIds().contains(paymentId)) {
                    paymentService.completePayment(paymentId);
                    return ResponseEntity.ok().build();
                }
                return ResponseEntity.notFound().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{clientId}/payments")
    public ResponseEntity<Set<String>> getPayments(@PathVariable String clientId) {
        return clientService.getById(clientId)
                .map(Client::getPaymentIds)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
