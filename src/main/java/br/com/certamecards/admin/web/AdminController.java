package br.com.certamecards.admin.web;

import br.com.certamecards.admin.service.AdminService;
import br.com.certamecards.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/api/admin/admins")
    public List<AdminUserResponse> list() {
        return adminService.listAdmins().stream().map(AdminUserResponse::from).toList();
    }

    @PostMapping("/api/admin/admins")
    public AdminUserResponse grant(
            @AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody GrantAdminRequest request) {
        return AdminUserResponse.from(adminService.grant(principal.id(), request.email()));
    }

    @DeleteMapping("/api/admin/admins/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID userId) {
        adminService.revoke(principal.id(), userId);
    }
}
