package com.farm.portal;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProduceController {
    private final ProduceRepository repo;

    public ProduceController(ProduceRepository repo) { this.repo = repo; }

    @GetMapping("/")
    public String home() { return "redirect:/produce"; }

    @GetMapping("/produce")
    public String list(@RequestParam(required = false) String q, Model m) {
        List<Produce> items = (q == null || q.isBlank())
                ? repo.findAll()
                : repo.findByNameContainingIgnoreCaseOrFarmContainingIgnoreCase(q, q);
        m.addAttribute("items", items);
        m.addAttribute("q", q);
        return "list";
    }

    @GetMapping("/produce/new")
    public String newForm(Model m) {
        m.addAttribute("produce", new Produce());
        return "form";
    }

    @GetMapping("/produce/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        m.addAttribute("produce", repo.findById(id).orElseThrow());
        return "form";
    }

    @PostMapping("/produce")
    public String save(@ModelAttribute Produce p) {
        if (p.getId() != null) {
            Produce old = repo.findById(p.getId()).orElseThrow();
            old.setName(p.getName());
            old.setFarm(p.getFarm());
            old.setHarvestDate(p.getHarvestDate());
            repo.save(old);
        } else {
            p.setStatus(Status.HARVESTED);
            repo.save(p);
        }
        return "redirect:/produce";
    }

    // Role-based status workflow: Farmer -> Distributor -> Retailer
    @PostMapping("/produce/{id}/advance")
    public String advance(@PathVariable Long id, @RequestParam String role, RedirectAttributes ra) {
        Produce p = repo.findById(id).orElseThrow();
        Status s = p.getStatus();
        boolean allowed = role.equals("ADMIN")
                || (role.equals("DISTRIBUTOR") && s == Status.HARVESTED)
                || (role.equals("RETAILER") && s == Status.IN_TRANSIT);
        if (allowed && s.next() != null) {
            p.setStatus(s.next());
            repo.save(p);
            ra.addFlashAttribute("msg", "Status updated");
        } else {
            ra.addFlashAttribute("msg", "Not allowed: " + role + " cannot move a batch that is " + s);
        }
        return "redirect:/produce";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model m) {
        m.addAttribute("total", repo.count());
        m.addAttribute("harvested", repo.countByStatus(Status.HARVESTED));
        m.addAttribute("inTransit", repo.countByStatus(Status.IN_TRANSIT));
        m.addAttribute("atRetail", repo.countByStatus(Status.AT_RETAIL));
        return "dashboard";
    }
}
