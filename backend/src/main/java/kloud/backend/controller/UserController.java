package kloud.backend.controller;

import kloud.backend.controller.vm.LoginVM;
import kloud.backend.entity.User;
import kloud.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UserController {

    @Resource
    private UserService userService;

    @CrossOrigin
    @PostMapping("/login")
    public ResponseEntity<User> login(@Valid @RequestBody LoginVM loginVM) {
        Optional<User> login = userService.login(loginVM.getUsername(), loginVM.getPassword());
        return login.map(user -> new ResponseEntity<>(user, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }
}
