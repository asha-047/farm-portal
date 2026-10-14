package com.farm.portal;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Adds the logged-in user's name and role to every page
@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute
    public void addUser(Model model, Authentication auth) {
        if (auth == null) return;
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        model.addAttribute("username", auth.getName());
        model.addAttribute("role", role);
        model.addAttribute("canAdd", role.equals("FARMER") || role.equals("ADMIN"));
        model.addAttribute("isAdmin", role.equals("ADMIN"));
    }
}
