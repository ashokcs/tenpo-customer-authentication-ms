package cl.tenpo.customerauthentication.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/customer-authentication")
@Slf4j
public class CustomerAuthenticactionController {

    @PostMapping(value = "/login")
    public ResponseEntity login() {
        return ResponseEntity.ok(null);
    }

    @PostMapping(value="/2fa")
    public ResponseEntity createChallenge(){
        return ResponseEntity.ok(null);
    }

    @PutMapping(value = "/2fa")
    public ResponseEntity validateChallenge(){
        return ResponseEntity.ok(null);
    }

    @DeleteMapping(value = "/2fa")
    public ResponseEntity abortChallenge(){
        return ResponseEntity.ok(null);
    }

    @GetMapping(value = "/2fa/challenge_types")
    public ResponseEntity listChallenge(){
        return ResponseEntity.ok(null);
    }

    @PostMapping(value = "/callback")
    public ResponseEntity callBackAuthentication(){
        return ResponseEntity.ok(null);
    }

}
