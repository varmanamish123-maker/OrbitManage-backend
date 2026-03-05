package com.orbit.portfolio.config;

import com.orbit.portfolio.model.User;
import com.orbit.portfolio.model.Portfolio;
import com.orbit.portfolio.repository.UserRepository;
import com.orbit.portfolio.repository.PortfolioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DummyDataInitializer implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PortfolioRepository portfolioRepository;

    @Override
    public void run(String... args) {
        // Ensure user with id 1 exists

        User user = userRepository.findById(1L).orElseGet(() -> {
            User u = new User("Dummy User", "dummy@example.com", "dummyhash");
            try {
                java.lang.reflect.Field idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(u, 1L);
            } catch (Exception ignored) {}
            return userRepository.save(u);
        });

        // Ensure portfolio with id 1 and user id 1 exists
        portfolioRepository.findById(1L).filter(p -> p.getUser() != null && p.getUser().getId() == 1L).orElseGet(() -> {
            Portfolio portfolio = new Portfolio(user, "Demo Portfolio", "Demo portfolio for testing");
            try {
                java.lang.reflect.Field idField = Portfolio.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(portfolio, 1L);
            } catch (Exception ignored) {}
            return portfolioRepository.save(portfolio);
        });
    }
}
