package com.novabank.customer.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCustomerProfileRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]*$", message = "First name contains unsupported characters")
        String firstName,
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]*$", message = "Last name contains unsupported characters")
        String lastName,
        @NotBlank @Email @Size(max = 160)
        String email,
        @Size(max = 32) @Pattern(regexp = "^$|^[+0-9(). -]{7,32}$", message = "Phone number format is invalid")
        String phone,
        @NotBlank @Size(max = 160)
        String addressLine1,
        @Size(max = 160)
        String addressLine2,
        @NotBlank @Size(max = 80)
        String city,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "State must be a two-letter uppercase code")
        String state,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9 -]{3,16}$", message = "Postal code format is invalid")
        String postalCode,
        @NotBlank @Size(max = 80)
        String country
) {
}
