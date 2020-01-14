package cl.tenpo.customerauthentication.api.controller;

import cl.tenpo.customerauthentication.api.dto.AbortChallengeRequest;
import cl.tenpo.customerauthentication.api.dto.CreateChallengeRequest;
import cl.tenpo.customerauthentication.api.dto.CustomerLoginRequest;
import cl.tenpo.customerauthentication.api.dto.ValidateChallengeRequest;
import cl.tenpo.customerauthentication.service.Customer2faService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/customer-authentication")
@Slf4j
public class CustomerAuthenticactionController {

    @Autowired
    private Customer2faService customer2faService;

    @PostMapping(value = "/login")
    public ResponseEntity login(@RequestBody CustomerLoginRequest request) {
        log.info("[login] IN");
        return ResponseEntity.ok(customer2faService.login(request));
    }

    @PostMapping(value="/2fa")
    public ResponseEntity createChallenge(@RequestHeader("x-mine-user-id") UUID userId, @RequestBody CreateChallengeRequest request) {
        log.info("[createChallenge] IN");
        customer2faService.createChallenge(userId, request);
        return new ResponseEntity(HttpStatus.CREATED);
    }

    @PutMapping(value = "/2fa")
    public ResponseEntity validateChallenge(@RequestHeader("x-mine-user-id") UUID userId, @RequestBody ValidateChallengeRequest request) {
        log.info("[validateChallenge] IN");
        return ResponseEntity.status(HttpStatus.CREATED).body(customer2faService.validateChallenge(userId, request));
    }

    @DeleteMapping(value = "/2fa")
    public ResponseEntity abortChallenge(@RequestHeader("x-mine-user-id") UUID userId, @RequestBody AbortChallengeRequest request) {
        log.info("[abortChallenge] IN");
        return ResponseEntity.status(HttpStatus.CREATED).body(customer2faService.abortChallenge(userId, request));
    }

    @GetMapping(value = "/2fa/challenge_types")
    public ResponseEntity listChallenge(@RequestHeader("x-mine-user-id") UUID userId) {
        log.info("[listChallenge] IN");
        return ResponseEntity.ok(customer2faService.listChallenge(userId));
    }

    @PostMapping(value = "/callback")
    public ResponseEntity callBackAuthentication() {
        log.info("[callBackAuthentication] IN");
        return new ResponseEntity(HttpStatus.CREATED);
    }

}
