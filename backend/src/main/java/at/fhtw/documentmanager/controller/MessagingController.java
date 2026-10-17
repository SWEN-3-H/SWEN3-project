package at.fhtw.documentmanager.controller;


import at.fhtw.documentmanager.messaging.Send;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messaging")
@RequiredArgsConstructor
public class MessagingController {

    private final Send sender;

    @PostMapping("/hello")
    public ResponseEntity<String> hello(@RequestParam(defaultValue = "Hello World!") String text) {
        sender.send(text);
        return ResponseEntity.accepted().body("sent");
    }
}
