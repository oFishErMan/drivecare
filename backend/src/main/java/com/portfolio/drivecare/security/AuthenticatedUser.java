package com.portfolio.drivecare.security;

import java.io.Serializable;

public record AuthenticatedUser(Long id, String email, String displayName) implements Serializable {
}
