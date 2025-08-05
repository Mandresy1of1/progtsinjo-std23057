package com.prog.tsinjo.endpoint.rest.controller;

import com.prog.tsinjo.domain.*;
import com.prog.tsinjo.repository.*;
import com.prog.tsinjo.service.VolaService;
import lombok.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.*;

@Controller
@RequiredArgsConstructor
public class TsinjoController {
    private final DonationRepository donationRepository;
    private final HelpRepository helpRepository;
    private final DonorRepository donorRepository;
    private final PaymentRepository paymentRepository;
    private final VolaService volaService;

    @GetMapping("/")
    public String showAllTransactions(Model model) {
        model.addAttribute("transactions", getCombinedTransactions());
        model.addAttribute("donationForm", new DonationForm());
        return "index";
    }

    @PostMapping("/donate")
    public String processDonation(@ModelAttribute DonationForm form) {
        // Validation simplifiée
        if (form.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return "redirect:/?error=Montant invalide";
        }

        // 1. Enregistrement du donateur
        Donor donor = donorRepository.findByEmail(form.getEmail())
                .orElseGet(() -> new Donor(form.getFullName(), form.getEmail()));
        donor = donorRepository.save(donor);

        // 2. Création du paiement en utilisant le constructeur par défaut et les setters
        Payment payment = new Payment();
        payment.setId(form.getPaymentId());
        payment.setAmount(form.getAmount());
        payment.setMethod(Payment.Method.valueOf(form.getMethod()));
        payment.setStatus(Payment.Status.VERIFYING);
        payment = paymentRepository.save(payment);

        // 3. Enregistrement du don en utilisant le constructeur par défaut et les setters
        Donation donation = new Donation();
        donation.setDonor(donor);
        donation.setPayment(payment);
        donationRepository.save(donation);

        // 4. Vérification asynchrone avec Vola
        volaService.verifyPaymentAsync(payment.getId());

        return "redirect:/?success=true";
    }

    private List<TransactionView> getCombinedTransactions() {
        List<TransactionView> transactions = new ArrayList<>();

        // Dons (entrées d'argent)
        donationRepository.findAllByOrderByCreatedAtDesc().forEach(donation -> {
            transactions.add(TransactionView.fromDonation(donation));
        });

        // Aides (sorties d'argent)
        helpRepository.findAllByOrderByCreatedAtDesc().forEach(help -> {
            transactions.add(TransactionView.fromHelp(help));
        });

        // Tri par date décroissante
        return transactions.stream()
                .sorted(Comparator.comparing(TransactionView::getDate).reversed())
                .collect(Collectors.toList());
    }

    // DTO pour le formulaire
    @Data
    public static class DonationForm {
        private String fullName;
        private String email;
        private BigDecimal amount;
        private String method; // Doit correspondre aux valeurs de Payment.Method
        private String paymentId;
    }

    // DTO pour l'affichage des transactions
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TransactionView {
        private final String amountDisplay;
        private final String status;
        private final String paymentMethod;
        private final LocalDateTime date;
        private final String actor;
        private final String description;

        public static TransactionView fromDonation(Donation donation) {
            return new TransactionView(
                    "+" + donation.getPayment().getAmount() + "Ar",
                    getStatusDisplay(donation.getPayment().getStatus()),
                    donation.getPayment().getMethod().getDisplayName(),
                    donation.getCreatedAt(),
                    "Par " + donation.getDonor().getFullName(),
                    null
            );
        }

        public static TransactionView fromHelp(Help help) {
            return new TransactionView(
                    "-" + help.getPayment().getAmount() + "Ar",
                    null,
                    null,
                    help.getCreatedAt(),
                    "Pour " + help.getBeneficiary().getFullName(),
                    help.getDescription()
            );
        }

        private static String getStatusDisplay(Payment.Status status) {
            return switch (status) {
                case SUCCEEDED -> "confirmé";
                case FAILED -> "échoué";
                case VERIFYING -> "en vérification";
            };
        }
    }
}