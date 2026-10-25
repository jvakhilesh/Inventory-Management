package com.example.auth_service.controller;

import com.example.auth_service.dto.request.AdminUserRequestDTO;
import com.example.auth_service.dto.request.UpdateRoleRequestDTO;
import com.example.auth_service.dto.response.UserResponseDTO;
import com.example.auth_service.entity.User;
import com.example.auth_service.entity.enums.Provider;
import com.example.auth_service.entity.enums.Role;
import com.example.auth_service.service.UserServiceImp;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserServiceImp userService;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        if (page < 0 || size < 1 || size > 500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> usersPage = userService.getUsersByRole(Role.USER, pageable);

        return ResponseEntity.ok(usersPage.map(user -> modelMapper.map(user, UserResponseDTO.class)));
    }

    @GetMapping("/employees")
    public ResponseEntity<java.util.List<UserResponseDTO>> getEmployees() {
        return ResponseEntity.ok(userService.getUsersByRole(Role.EMPLOYEE).stream().map(u -> modelMapper.map(u, UserResponseDTO.class)).toList());
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody AdminUserRequestDTO request) {
        if (userService.findByEmail(request.getEmail()) != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        if (request.getRole() == Role.ADMIN) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin accounts cannot be created through this endpoint");
        if (request.getPassword() == null || request.getPassword().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        User user = User.builder().name(request.getName().trim()).email(request.getEmail().trim().toLowerCase()).password(passwordEncoder.encode(request.getPassword())).provider(Provider.LOCAL).isActive(true).role(request.getRole()).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(modelMapper.map(userService.save(user), UserResponseDTO.class));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @Valid @RequestBody AdminUserRequestDTO request) {
        User user = userService.getUserById(id);
        if (user.getRole() == Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts cannot be modified here");
        User duplicate = userService.findByEmail(request.getEmail());
        if (duplicate != null && !duplicate.getId().equals(id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        if (request.getRole() == Role.ADMIN) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin role cannot be assigned here");
        user.setName(request.getName().trim()); user.setEmail(request.getEmail().trim().toLowerCase()); user.setRole(request.getRole());
        if (request.getPassword() != null && !request.getPassword().isBlank()) user.setPassword(passwordEncoder.encode(request.getPassword()));
        return ResponseEntity.ok(modelMapper.map(userService.save(user), UserResponseDTO.class));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user.getRole() == Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts cannot be deleted here");
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserResponseDTO> updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequestDTO request) {
        User updatedUser = userService.updateUserRole(id, request.getRole());
        return ResponseEntity.ok(modelMapper.map(updatedUser, UserResponseDTO.class));
    }

    @GetMapping("/users/search")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(modelMapper.map(userService.getUserByEmail(email), UserResponseDTO.class));
    }
}
