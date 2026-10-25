package com.example.auth_service.service;

import com.example.auth_service.entity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.auth_service.entity.User;
import com.example.auth_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service 
@RequiredArgsConstructor 
public class UserServiceImp implements UserDetailsService{
    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository
                    .findByEmail(username)
                    .orElseThrow(()-> new UsernameNotFoundException("user with this name : "+ username+" is not found"));
    }
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(()->
                        new RuntimeException("User is not found with the id " + id)
                );
    }
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    public User save(User newUser) {
        return userRepository.save(newUser);
    }


    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }

    public User updateUserRole(Long id, Role newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("Role is required");
        }

        if (newRole != Role.USER && newRole != Role.EMPLOYEE) {
            throw new IllegalArgumentException(
                    "Only USER and EMPLOYEE roles can be assigned here"
            );
        }

        User user = getUserById(id);

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException(
                    "Admin role cannot be changed through this endpoint"
            );
        }

        user.setRole(newRole);
        return userRepository.save(user);
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findAll().stream().filter(user -> user.getRole() == role).toList();
    }

    public void deleteUser(Long id) {
        userRepository.delete(getUserById(id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + email));
    }
}
