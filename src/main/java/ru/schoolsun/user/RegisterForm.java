package ru.schoolsun.user;

import lombok.Data;

@Data
public class RegisterForm {

    private String fullName;
    private String email;
    private String password;
    /** STUDENT или TEACHER */
    private String role = "STUDENT";
}
