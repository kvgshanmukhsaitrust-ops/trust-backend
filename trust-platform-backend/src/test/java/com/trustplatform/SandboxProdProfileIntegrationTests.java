package com.trustplatform;

import com.trustplatform.payment.PaymentSimulator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(properties = {
        "SPRING_MAIL_HOST=localhost",
        "SPRING_MAIL_PORT=587",
        "SPRING_MAIL_USERNAME=dummy",
        "SPRING_MAIL_PASSWORD=dummy",
        "APP_MAIL_FROM_NAME=dummy",
        "APP_MAIL_FROM_EMAIL=dummy@example.com",
        "CLOUDINARY_URL=cloudinary://123:abc@def",
        "GOOGLE_CLIENT_ID=dummy",
        "GOOGLE_CLIENT_SECRET=dummy",
        "JWT_SECRET=dummy_secret_dummy_secret_dummy_secret_dummy_secret",
        "JWT_EXPIRATION=360000",
        "JWT_REFRESH_EXPIRATION=360000",
        "VERIFICATION_EXPIRATION=360000",
        "FRONTEND_URL=http://localhost:3000",
        "RAZORPAY_KEY_ID=dummy",
        "RAZORPAY_KEY_SECRET=dummy",
        "RAZORPAY_WEBHOOK_SECRET=dummy",
        "DB_URL=jdbc:mysql://localhost:3306/trustplatform",
        "DB_USER=root",
        "DB_PASSWORD=Rohith@358",
        "app.crypto.pan-key=prod_secure_pan_key_must_be_32_bytes_long"
})
@ActiveProfiles("prod")
public class SandboxProdProfileIntegrationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void whenProdProfileActive_thenPaymentSimulatorBeanIsAbsent() {
        boolean containsBean = applicationContext.containsBean("paymentSimulator")
                || !applicationContext.getBeansOfType(PaymentSimulator.class).isEmpty();
        assertFalse(containsBean, "PaymentSimulator bean must not be loaded when the 'prod' profile is active.");
    }
}
