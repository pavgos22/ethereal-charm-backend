package com.ethereal.order.service;

import com.ethereal.order.config.EmailConfiguration;
import com.ethereal.order.entity.Order;
import com.ethereal.order.entity.OrderItems;
import com.ethereal.order.entity.SimpleProductDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailConfiguration emailConfiguration;
    private final RestTemplate restTemplate;

    @Value("${front.url}")
    private String FRONTEND_URL;

    @Value("classpath:static/mail-order.html")
    private Resource orderTemplate;

    public SimpleProductDTO getProductDetails(String productUuid) {
        String url = "http://localhost:8888/api/v1/product/" + productUuid;
        try {
            ResponseEntity<SimpleProductDTO> response = restTemplate.getForEntity(url, SimpleProductDTO.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch product details for UUID: {}", productUuid, e);
            throw new RuntimeException("Unable to fetch product details");
        }
    }

    private String generateOrderDetailsHtml(Order order, List<OrderItems> items) {
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html><body>");
        htmlBuilder.append("<h1>Szczegóły Twojego zamówienia</h1>");
        htmlBuilder.append("<p>Numer zamówienia: ").append(order.getOrders()).append("</p>");
        htmlBuilder.append("<p>Status zamówienia: <strong>").append(order.getStatus()).append("</strong></p>");
        htmlBuilder.append("<p>Kwota zamówienia: <strong>").append(calculateTotalPrice(items)).append(" zł</strong></p>");

        htmlBuilder.append("<h2>Dane klienta:</h2>");
        htmlBuilder.append("<p>Imię: ").append(order.getFirstName()).append("</p>");
        htmlBuilder.append("<p>Nazwisko: ").append(order.getLastName()).append("</p>");
        if (order.isCompany()) {
            htmlBuilder.append("<p>Nazwa firmy: ").append(order.getCompanyName()).append("</p>");
            htmlBuilder.append("<p>NIP: ").append(order.getNip()).append("</p>");
        } else {
            htmlBuilder.append("<p>Osoba prywatna</p>");
        }

        htmlBuilder.append("<h2>Adres dostawy:</h2>");
        htmlBuilder.append("<p>Miasto: ").append(order.getCity()).append("</p>");
        htmlBuilder.append("<p>Ulica: ").append(order.getStreet()).append("</p>");
        htmlBuilder.append("<p>Numer domu/mieszkania: ").append(order.getNumber()).append("</p>");
        htmlBuilder.append("<p>Kod pocztowy: ").append(order.getPostCode()).append("</p>");

        htmlBuilder.append("<h2>Produkty w zamówieniu:</h2>");
        htmlBuilder.append("<ul>");
        for (OrderItems item : items) {
            SimpleProductDTO productDetails = getProductDetails(item.getProduct());
            String imageUrl = "http://localhost:8088/api/v1/image?uuid=" + productDetails.getImageUrl();

            htmlBuilder.append("<li>")
                    .append("<img src='").append(imageUrl).append("' alt='Zdjęcie produktu' width='50' height='50' /> ")
                    .append(productDetails.getName())
                    .append(" - Ilość: ").append(item.getQuantity())
                    .append(", Cena: ").append(item.getPriceSummary()).append(" zł</li>");
        }
        htmlBuilder.append("</ul>");

        htmlBuilder.append("</body></html>");
        return htmlBuilder.toString();
    }

    private double calculateTotalPrice(List<OrderItems> items) {
        return items.stream().mapToDouble(OrderItems::getPriceSummary).sum();
    }

    public void sendActivation(String mail, Order order, List<OrderItems> items) {
        log.info("--START sendOrder");
        try {
            String html = generateOrderDetailsHtml(order, items);
            emailConfiguration.sendMail(mail, html, "Szczegóły zamówienia", true);
        } catch (Exception e) {
            log.error("Nie udało się wysłać e-maila z zamówieniem", e);
            throw new RuntimeException(e);
        }
        log.info("--STOP sendOrder");
    }
}
